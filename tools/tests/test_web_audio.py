import contextlib
import importlib.util
import io
import json
import math
import os
from pathlib import Path
import shutil
import struct
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch
import wave

ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location('web_audio',ROOT/'tools/build-web-audio.py')
AUDIO = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(AUDIO)
FFMPEG = os.environ.get('FFMPEG','ffmpeg')


def wav(path):
    path.parent.mkdir(parents=True,exist_ok=True)
    with wave.open(str(path),'wb') as output:
        output.setnchannels(2); output.setsampwidth(2); output.setframerate(44100)
        output.writeframes(b''.join(struct.pack('<hh',*(int(12000*math.sin(2*math.pi*i*440/44100)),)*2) for i in range(11025)))


class AudioTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temporary = tempfile.TemporaryDirectory(prefix='web-audio-tests-')
        cls.base = Path(cls.temporary.name)/'root'
        java = cls.base/'src/main/java/com/vocr/resources'; java.mkdir(parents=True)
        (java/'BgmTrack.java').write_text('''public enum BgmTrack { MENU("menu.wav"), NORMAL("曲目.wav", "曲目.txt");
            private static final List<BgmTrack> LATE_GAME_PLAYLIST = List.of(MENU);
            private static final List<BgmTrack> NORMAL_GAME_PLAYLIST = List.of(NORMAL); }''',encoding='utf-8')
        (java/'SoundEffect.java').write_text('public enum SoundEffect { CLICK("click.wav"); }',encoding='utf-8')
        for folder,name in [('BGM','menu.wav'),('BGM','曲目.wav'),('sounds','click.wav')]:
            wav(cls.base/'src/main/resources'/folder/name)
        help_dir=cls.base/'src/main/resources/help'; help_dir.mkdir()
        (help_dir/'曲目.txt').write_text('曲目\n',encoding='utf-8')
        cls.built = Path(cls.temporary.name)/'built'
        with contextlib.redirect_stderr(io.StringIO()): AUDIO.build(cls.base,cls.built,FFMPEG)

    @classmethod
    def tearDownClass(cls): cls.temporary.cleanup()

    def setUp(self):
        self.temp=tempfile.TemporaryDirectory(prefix='web-audio-case-')
        self.addCleanup(self.temp.cleanup)
        self.root=Path(self.temp.name)/'root'; self.output=Path(self.temp.name)/'output'
        shutil.copytree(self.base,self.root); shutil.copytree(self.built,self.output)

    def manifest(self): return json.loads((self.output/'manifest.json').read_text(encoding='utf-8'))
    def write_manifest(self,data): (self.output/'manifest.json').write_bytes(AUDIO.manifest_bytes(data))
    def check(self): return AUDIO.check(self.root,self.output,FFMPEG)

    def test_real_catalog_is_complete_and_playlists_ordered(self):
        groups,lists=AUDIO.catalog(ROOT)
        self.assertEqual([len(groups[g]) for g in ('bgm','sounds')],[23,9])
        self.assertEqual([len(lists[k]) for k in ('LATE_GAME_PLAYLIST','NORMAL_GAME_PLAYLIST')],[4,13])
        self.assertEqual(sum(bool(description) for _,description in groups['bgm'].values()),13)

    def test_build_is_deterministic_and_content_addressed(self):
        second=Path(self.temp.name)/'second'
        with contextlib.redirect_stderr(io.StringIO()): AUDIO.build(self.root,second,FFMPEG)
        first={p.relative_to(self.output).as_posix():p.read_bytes() for p in self.output.rglob('*') if p.is_file()}
        self.assertEqual(first,{p.relative_to(second).as_posix():p.read_bytes() for p in second.rglob('*') if p.is_file()})
        report=self.check()
        self.assertEqual(report['bgmTracks'],2); self.assertEqual(report['effects'],1)
        self.assertEqual(report['uniqueAudioFiles'],2)  # identical BGM source PCM shares output bytes.

    def test_refuses_existing_and_source_output(self):
        for output in [self.output,self.root/'src/main/resources/new']:
            with self.assertRaises(ValueError): AUDIO.build(self.root,output,FFMPEG)

    def test_missing_and_case_mismatch_source(self):
        source=self.root/'src/main/resources/BGM/menu.wav'; source.rename(source.with_name('Menu.wav'))
        with self.assertRaisesRegex(ValueError,'Missing'): self.check()

    def test_unparsed_enum_and_path_injection_fail(self):
        path=self.root/AUDIO.INPUTS[0]
        original=path.read_text()
        for changed in [original.replace('MENU("menu.wav")','BROKEN'),original.replace('menu.wav','../menu.wav')]:
            path.write_text(changed)
            with self.assertRaises(ValueError): self.check()

    def test_source_and_description_drift(self):
        description=self.root/'src/main/resources/help/曲目.txt'; description.write_text('changed')
        with self.assertRaises(ValueError): self.check()
        description.write_text('曲目\n')
        path=self.root/'src/main/resources/BGM/menu.wav'; raw=bytearray(path.read_bytes()); raw[-1]^=1; path.write_bytes(raw)
        with self.assertRaisesRegex(ValueError,'source drift'): self.check()

    def test_crlf_text_inputs_are_compatible(self):
        for name in AUDIO.INPUTS:
            path=self.root/name; path.write_bytes(path.read_bytes().replace(b'\n',b'\r\n'))
        description=self.root/'src/main/resources/help/曲目.txt'; description.write_bytes('曲目\r\n'.encode())
        self.check()

    def test_truncated_and_lfs_sources_fail(self):
        path=self.root/'src/main/resources/BGM/menu.wav'; original=path.read_bytes()
        for data in [original[:-10],b'version https://git-lfs.github.com/spec/v1\n']:
            path.write_bytes(data)
            with self.assertRaises((ValueError,wave.Error,EOFError)): self.check()

    def test_output_hash_and_frame_headers_are_checked(self):
        data=self.manifest(); entry=data['bgm']['MENU']; path=self.output/entry['file']
        original=path.read_bytes(); path.write_bytes(original[:-1])
        with self.assertRaises(ValueError): self.check()
        digest=AUDIO.sha(b'not audio'); target=self.output/'bgm'/(digest+'.mp3'); target.write_bytes(b'not audio')
        entry.update(file='bgm/'+digest+'.mp3',sha256=digest,bytes=9); self.write_manifest(data)
        with self.assertRaises(ValueError): self.check()

    def test_full_decode_is_not_replaced_by_header_acceptance(self):
        data=self.manifest(); entry=data['bgm']['MENU']; old=self.output/entry['file']
        raw=old.read_bytes(); corrupt=bytearray(raw)
        # Keep each frame header but erase all side info/payload, including gapless sample metadata.
        offset=0
        while offset<len(raw):
            header=int.from_bytes(raw[offset:offset+4],'big')
            length=144*128000//44100+((header>>9)&1)
            corrupt[offset+4:offset+length]=b'\0'*(length-4); offset+=length
        AUDIO.mp3_info(corrupt)
        digest=AUDIO.sha(corrupt); path=self.output/'bgm'/(digest+'.mp3'); path.write_bytes(corrupt)
        for item in data['bgm'].values(): item.update(file='bgm/'+digest+'.mp3',sha256=digest,bytes=len(corrupt))
        old.unlink(); self.write_manifest(data)
        with self.assertRaises(ValueError): self.check()

    def test_output_paths_symlinks_and_residue_fail(self):
        (self.output/'stale.txt').write_text('stale')
        with self.assertRaises(ValueError): self.check()
        (self.output/'stale.txt').unlink()
        path=self.output/'manifest.json'; original=path.read_bytes(); path.unlink()
        outside=Path(self.temp.name)/'external.json'; outside.write_bytes(original); path.symlink_to(outside)
        with self.assertRaises(ValueError): self.check()

    def test_all_budget_groups_fail_closed(self):
        original=self.manifest()
        for key in ['singleBgmBudget','bgmBudget','soundBudget','textBudget']:
            with patch.dict(AUDIO.POLICY,{key:1}):
                data=json.loads(json.dumps(original)); data['policy']=AUDIO.POLICY.copy(); self.write_manifest(data)
                with self.assertRaisesRegex(ValueError,'budget'): self.check()

    def test_manifest_selection_and_metadata_not_trusted(self):
        original=self.manifest()
        for edit in [lambda d:d['bgm'].pop('MENU'),lambda d:d['playlists']['NORMAL_GAME_PLAYLIST'].reverse(),
                     lambda d:d['bgm']['MENU'].update(samples=1),lambda d:d['bgm']['MENU'].update(file='../bad.mp3')]:
            data=json.loads(json.dumps(original)); edit(data)
            # Fixture has one normal member; use an explicit extra to exercise playlist mismatch.
            if data==original: data['playlists']['NORMAL_GAME_PLAYLIST'].append('MENU')
            self.write_manifest(data)
            with self.assertRaises(ValueError): self.check()

    def test_distribution_counts_all_text_and_static_files(self):
        public=self.root/'web-client/public/assets/audio'; public.parent.mkdir(parents=True); shutil.copytree(self.output,public)
        dist=self.root/'web-client/dist'; shutil.copytree(self.output,dist/'assets/audio')
        self.assertGreater(AUDIO.distribution(self.root,dist,FFMPEG)['totalStaticBytes'],0)
        (dist/'huge.txt').write_bytes(b'x'*(1024**2))
        with self.assertRaisesRegex(ValueError,'text budget'): AUDIO.distribution(self.root,dist,FFMPEG)

    def test_cli_fails_clearly_and_does_not_publish_on_encoder_error(self):
        target=Path(self.temp.name)/'new'
        result=subprocess.run([sys.executable,str(ROOT/'tools/build-web-audio.py'),'build','--root',str(self.root),
                               '--output',str(target),'--ffmpeg','/missing/web026-encoder'],capture_output=True,text=True)
        self.assertNotEqual(result.returncode,0); self.assertIn('WEB_AUDIO_ERROR:',result.stderr)
        self.assertFalse(target.exists()); self.assertFalse(list(target.parent.glob('.web-audio-*')))

    def test_quality_policy_raises_bitrate_instead_of_lowering_floor(self):
        analyze=AUDIO.analyze
        def measurement(ffmpeg,source,target,info):
            result=analyze(ffmpeg,source,target,info)
            if AUDIO.mp3_info(target.read_bytes())[0]==128000: result['minSdrDb']=13
            return result
        output=Path(self.temp.name)/'raised'
        with patch.object(AUDIO,'analyze',side_effect=measurement),contextlib.redirect_stderr(io.StringIO()):
            AUDIO.build(self.root,output,FFMPEG)
        manifest=json.loads((output/'manifest.json').read_text())
        self.assertTrue(all(v['bitrate']==160000 for v in manifest['bgm'].values()))
        AUDIO.check(self.root,output,FFMPEG)

    def test_quality_failure_leaves_no_published_artifacts(self):
        analyze=AUDIO.analyze
        def measurement(*args): return {**analyze(*args),'minSdrDb':5}
        output=Path(self.temp.name)/'bad-quality'
        with patch.object(AUDIO,'analyze',side_effect=measurement),contextlib.redirect_stderr(io.StringIO()):
            with self.assertRaisesRegex(ValueError,'quality'): AUDIO.build(self.root,output,FFMPEG)
        self.assertFalse(output.exists());self.assertFalse(list(output.parent.glob('.web-audio-*')))

    def test_cli_empty_wav_has_explicit_nonzero_diagnostic(self):
        (self.root/'src/main/resources/BGM/menu.wav').write_bytes(b'')
        result=subprocess.run([sys.executable,str(ROOT/'tools/build-web-audio.py'),'check','--root',str(self.root),
                               '--output',str(self.output),'--ffmpeg',FFMPEG],capture_output=True,text=True)
        self.assertNotEqual(result.returncode,0);self.assertIn('WEB_AUDIO_ERROR:',result.stderr)
        self.assertIn('Invalid/truncated PCM WAV',result.stderr)
        self.assertNotIn('Traceback',result.stderr)


if __name__=='__main__': unittest.main()
