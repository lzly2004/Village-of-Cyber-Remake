#!/usr/bin/env python3
"""WEB-026: sequential MP3 conversion, source mapping and fail-closed verification.

FFMPEG selects a full FFmpeg with libmp3lame, astats and asdr (tested 7.0.2).
Build publishes only to a new directory after check; source WAVs are never written.
"""
import argparse
import hashlib
import json
import math
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import wave

INPUTS = ('src/main/java/com/vocr/resources/BgmTrack.java', 'src/main/java/com/vocr/resources/SoundEffect.java')
POLICY = {'version': 1, 'format': 'mp3', 'encoder': 'libmp3lame', 'bgmBitrates': [128000,160000,192000],
          'soundBitrate': 192000, 'threads': 1, 'singleBgmBudget': 5*1024**2,
          'bgmBudget': 64*1024**2, 'soundBudget': 1024**2, 'textBudget': 1024**2,
          'staticBudget': 100*1024**2, 'minSdrDb': 14}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def file_sha(path):
    with path.open('rb') as stream:
        digest = hashlib.sha256()
        for chunk in iter(lambda: stream.read(1024*1024), b''):
            digest.update(chunk)
    return digest.hexdigest()


def text_inputs(root):
    return {path: sha((root/path).read_text(encoding='utf-8').encode('utf-8')) for path in INPUTS}


def safe_source(root, folder, name):
    require(isinstance(name, str) and name and '/' not in name and '\\' not in name
            and name not in ('.','..'), 'Invalid source filename')
    base = root/'src/main/resources'/folder
    path = base/name
    require(base.is_dir() and not base.is_symlink() and path.is_file() and not path.is_symlink()
            and path.resolve().parent == base.resolve() and name in {p.name for p in base.iterdir()},
            'Missing, unsafe or case-mismatched source: '+folder+'/'+name)
    return path


def catalog(root):
    groups, playlists = {}, {}
    for group, source in zip(('bgm','sounds'), INPUTS):
        text = (root/source).read_text(encoding='utf-8')
        # Restrict to the enum declaration, reject any unparsed member instead of silently skipping it.
        body = text.split('public enum ',1)[1].split('{',1)[1].split(';',1)[0]
        pattern = r'\b([A-Z][A-Z_0-9]*)\(\s*"([^"\r\n]+\.wav)"\s*(?:,\s*"([^"\r\n]+\.txt)"\s*)?\)'
        entries = re.findall(pattern, body)
        remainder = re.sub(pattern,'',body)
        remainder = re.sub(r'//[^\n]*|/\*[\s\S]*?\*/','',remainder)
        require(entries and not remainder.replace(',','').strip(), 'Unparsed enum member: '+group)
        require(len({key for key,_,_ in entries}) == len(entries), 'Duplicate audio identifier')
        groups[group] = {}
        for key, filename, description in entries:
            require(group == 'bgm' or not description, 'Unexpected effect description')
            path = safe_source(root, 'BGM' if group == 'bgm' else 'sounds', filename)
            if description:
                safe_source(root,'help',description)
            groups[group][key] = (path, description)
        if group == 'bgm':
            for key in ('LATE_GAME_PLAYLIST','NORMAL_GAME_PLAYLIST'):
                match = re.search(key+r'\s*=\s*List\.of\(([^)]+)\)',text)
                require(match, 'Missing playlist: '+key)
                names = [name.strip() for name in match[1].split(',')]
                require(len(names) == len(set(names)) and all(name in groups[group] for name in names), 'Invalid playlist')
                playlists[key] = names
    return groups, playlists


def wav_info(path):
    try:
        opened = wave.open(str(path),'rb')
    except (EOFError,wave.Error) as error:
        raise ValueError('Invalid/truncated PCM WAV: '+path.name) from error
    with opened as source:
        channels, rate, frames, width = source.getnchannels(), source.getframerate(), source.getnframes(), source.getsampwidth()
        require(source.getcomptype() == 'NONE' and width == 2 and channels == 2 and rate in (44100,48000)
                and frames > 0, 'Unsupported source WAV: '+path.name)
        count = 0
        for data in iter(lambda: source.readframes(65536), b''):
            count += len(data)
        require(count == frames*channels*width, 'Truncated WAV: '+path.name)
    return {'channels': channels, 'sampleRate': rate, 'samples': frames, 'duration': round(frames/rate,9)}


def run(ffmpeg, args):
    try:
        result = subprocess.run([ffmpeg,'-nostdin','-hide_banner','-nostats',*args],
                                capture_output=True, text=True, timeout=120)
    except (OSError, subprocess.TimeoutExpired) as error:
        raise ValueError('FFmpeg unavailable or timed out') from error
    require(result.returncode == 0, 'FFmpeg failed (decode/encode error); exit '+str(result.returncode))
    return result.stderr


def encoder_info(ffmpeg):
    # -version goes to stdout, unlike diagnostics; do not write absolute paths into the manifest.
    result = subprocess.run([ffmpeg,'-version'],capture_output=True,text=True,check=True,timeout=10)
    require(result.stdout.startswith('ffmpeg version '),'Invalid FFmpeg executable')
    return {'version': result.stdout.splitlines()[0], 'binarySha256': file_sha(Path(shutil.which(ffmpeg) or ffmpeg))}


def encode(ffmpeg, source, target, bitrate):
    run(ffmpeg,['-v','error','-threads','1','-i',str(source),'-map_metadata','-1',
                '-c:a','libmp3lame','-b:a',str(bitrate),'-threads','1','-write_xing','1',
                '-id3v2_version','0','-fflags','+bitexact','-flags:a','+bitexact',str(target)])


def analyze(ffmpeg, source, target, info):
    log = run(ffmpeg,['-v','info','-xerror','-err_detect','explode','-threads','1','-i',str(target),
        '-filter_threads','1','-af','astats=measure_perchannel=none:measure_overall=Number_of_samples+Peak_level+RMS_level+Number_of_NaNs+Number_of_Infs',
        '-map_metadata','-1','-threads','1','-f','null','-'])
    def value(label):
        match = re.search(r'\] '+re.escape(label)+r':\s*([-+\w.]+)',log)
        require(match, 'Missing decoded measurement: '+label)
        return float(match[1])
    require(value('Number of samples') == info['samples'], 'Decoded sample count mismatch')
    require(value('Number of NaNs') == 0 and value('Number of Infs') == 0, 'Invalid decoded samples')
    peak, rms = value('Peak level dB'), value('RMS level dB')
    require(math.isfinite(peak) and math.isfinite(rms), 'Empty/silent audio')
    compare = run(ffmpeg,['-threads','1','-i',str(source),'-threads','1','-i',str(target),
        '-filter_complex_threads','1','-filter_complex',
        '[0:a]aformat=sample_fmts=fltp[a];[1:a]aformat=sample_fmts=fltp[b];[a][b]asdr',
        '-map_metadata','-1','-threads','1','-f','null','-'])
    scores = [float(x) for x in re.findall(r'SDR ch\d+: ([-+\w.]+) dB',compare)]
    require(len(scores) == info['channels'] and all(math.isfinite(x) for x in scores), 'Invalid audio SDR measurement')
    return {'decodedSamples': info['samples'], 'peakDb': round(peak,5), 'rmsDb': round(rms,5),
            'minSdrDb': round(min(scores),4)}


def mp3_info(data):
    # All frames, not just an ID3/header sniff; only our MPEG-1 Layer III CBR policy is accepted.
    require(data and not data.startswith(b'ID3'), 'Unexpected metadata or empty MP3')
    offset, frames, signature = 0, 0, None
    bitrates = [0,32,40,48,56,64,80,96,112,128,160,192,224,256,320]
    while offset < len(data):
        require(offset+4 <= len(data), 'Truncated MP3 frame header')
        header = int.from_bytes(data[offset:offset+4],'big')
        require(header >> 21 == 0x7ff and (header >> 19)&3 == 3 and (header >> 17)&3 == 1,
                'Invalid MPEG-1 Layer III frame')
        index, rate_index = (header >> 12)&15, (header >> 10)&3
        require(0 < index < 15 and rate_index < 3, 'Invalid MP3 bitrate/rate')
        bitrate, rate = bitrates[index]*1000, [44100,48000,32000][rate_index]
        channels = 1 if (header >> 6)&3 == 3 else 2
        current = (bitrate,rate,channels)
        signature = signature or current
        require(signature == current, 'Unexpected MP3 stream changes')
        length = 144*bitrate//rate + ((header >> 9)&1)
        require(offset+length <= len(data), 'Truncated MP3 frame payload')
        offset += length; frames += 1
    require(frames >= 2, 'No MP3 audio frames')
    return signature


def manifest_bytes(value):
    return (json.dumps(value,ensure_ascii=False,sort_keys=True,indent=2)+'\n').encode('utf-8')


def output_file(output, name):
    require(isinstance(name,str) and re.fullmatch(r'(bgm|sounds|descriptions)/[a-f0-9]{64}\.(mp3|txt)',name), 'Unsafe output path')
    path = output/name
    require(not output.is_symlink() and not path.parent.is_symlink() and path.is_file() and not path.is_symlink()
            and path.resolve().parent.parent == output.resolve(), 'Missing/unsafe output: '+name)
    return path


def check(root, output, ffmpeg):
    groups, playlists = catalog(root)
    require(output.is_dir() and not output.is_symlink(), 'Missing/unsafe audio output')
    manifest_path = output/'manifest.json'
    require(manifest_path.is_file() and not manifest_path.is_symlink(), 'Missing/unsafe audio manifest')
    raw = manifest_path.read_bytes(); manifest = json.loads(raw)
    require(isinstance(manifest,dict), 'Invalid manifest object')
    require(set(manifest) == {'schemaVersion','policy','encoder','selectionInputs','selectionInputNormalization','playlists','bgm','sounds'}, 'Unexpected manifest fields')
    require(type(manifest['schemaVersion']) is int and manifest['schemaVersion'] == 1 and manifest['policy'] == POLICY
            and manifest['selectionInputNormalization'] == 'utf8-lf', 'Unsupported audio manifest policy')
    encoder = manifest['encoder']
    require(isinstance(encoder,dict) and set(encoder) == {'version','binarySha256'} and isinstance(encoder['version'],str)
            and encoder['version'].startswith('ffmpeg version ') and isinstance(encoder['binarySha256'],str)
            and re.fullmatch('[a-f0-9]{64}',encoder['binarySha256']), 'Invalid encoder identity')
    require(manifest['selectionInputs'] == text_inputs(root) and manifest['playlists'] == playlists, 'Audio selection drift')
    expected_files = {'manifest.json'}; sizes = {'bgm':{},'sounds':{},'descriptions':{}}
    for group, entries in groups.items():
        require(set(manifest[group]) == set(entries), 'Incomplete/extra audio mapping: '+group)
        for key, (source, description) in entries.items():
            entry = manifest[group][key]; info = wav_info(source)
            require(isinstance(entry,dict) and isinstance(entry.get('analysis'),dict), 'Invalid audio entry: '+key)
            require(entry['source'] == source.relative_to(root/'src/main/resources').as_posix()
                    and entry['sourceSha256'] == file_sha(source) and entry['sourceBytes'] == source.stat().st_size,
                    'Audio source drift: '+key)
            require(all(entry[k] == v for k,v in info.items()), 'Audio metadata drift: '+key)
            path = output_file(output,entry['file']); data = path.read_bytes()
            digest = sha(data)
            require(entry['file'] == group+'/'+digest+'.mp3' and entry['sha256'] == digest
                    and entry['bytes'] == len(data) and entry['mime'] == 'audio/mpeg', 'Output digest/size/mime mismatch: '+key)
            bitrate = entry['bitrate']
            require(bitrate in (POLICY['bgmBitrates'] if group == 'bgm' else [POLICY['soundBitrate']])
                    and mp3_info(data) == (bitrate,info['sampleRate'],info['channels']), 'MP3 format mismatch: '+key)
            measurement = analyze(ffmpeg,source,path,info)
            require(measurement['minSdrDb'] >= POLICY['minSdrDb'], 'Audio SDR below policy floor: '+key)
            require(entry['analysis'].keys() == measurement.keys() and all(abs(entry['analysis'][k]-v) <= .001 for k,v in measurement.items()), 'Audio measurement mismatch: '+key)
            require(group != 'bgm' or len(data) <= POLICY['singleBgmBudget'], 'Single BGM budget exceeded: '+key)
            sizes[group][entry['file']] = len(data); expected_files.add(entry['file'])
            if description:
                item = entry['description']; text_path = safe_source(root,'help',description)
                text = text_path.read_text(encoding='utf-8').encode('utf-8')
                require(item == {'source':'help/'+description,'sha256':sha(text),'bytes':len(text),
                                 'file':'descriptions/'+sha(text)+'.txt'}, 'Description mapping drift: '+key)
                require(output_file(output,item['file']).read_bytes() == text, 'Description bytes mismatch: '+key)
                sizes['descriptions'][item['file']] = len(text); expected_files.add(item['file'])
            else:
                require(entry['description'] is None, 'Unexpected description: '+key)
    actual_files = set()
    for path in output.rglob('*'):
        require(not path.is_symlink(), 'Symlink in audio output')
        if path.is_file(): actual_files.add(path.relative_to(output).as_posix())
        else: require(path.is_dir() and path.relative_to(output).as_posix() in sizes, 'Unexpected output directory')
    require(actual_files == expected_files, 'Unexpected/stale audio files')
    totals = {group:sum(values.values()) for group,values in sizes.items()}
    require(totals['bgm'] <= POLICY['bgmBudget'] and totals['sounds'] <= POLICY['soundBudget'], 'Audio bundle budget exceeded')
    require(len(raw)+totals['descriptions'] <= POLICY['textBudget'], 'Audio text budget exceeded')
    return {'bgmTracks':len(groups['bgm']),'effects':len(groups['sounds']),'bgmBytes':totals['bgm'],
            'effectBytes':totals['sounds'],'manifestBytes':len(raw),'descriptionBytes':totals['descriptions'],
            'largestBgmBytes':max(sizes['bgm'].values()),'uniqueAudioFiles':len(sizes['bgm'])+len(sizes['sounds'])}


def build(root, output, ffmpeg):
    resources = (root/'src/main/resources').resolve()
    require(output.resolve() != resources and resources not in output.resolve().parents, 'Output must not be inside source resources')
    require(not output.exists() and not output.is_symlink(), 'Output already exists; choose a new directory')
    require(not any(parent.is_symlink() for parent in output.absolute().parents), 'Symlink in output ancestors')
    groups, playlists = catalog(root)
    output.parent.mkdir(parents=True,exist_ok=True)
    temporary = Path(tempfile.mkdtemp(prefix='.web-audio-',dir=output.parent))
    try:
        for name in ('bgm','sounds','descriptions'): (temporary/name).mkdir()
        manifest = {'schemaVersion':1,'policy':POLICY,'encoder':encoder_info(ffmpeg),'selectionInputNormalization':'utf8-lf',
                    'selectionInputs':text_inputs(root),'playlists':playlists,'bgm':{},'sounds':{}}
        for group, entries in groups.items():
            for key,(source,description) in entries.items():
                info = wav_info(source)
                target = temporary/group/(key+'.mp3')
                analysis = None
                for bitrate in POLICY['bgmBitrates'] if group == 'bgm' else [POLICY['soundBitrate']]:
                    if target.exists(): target.unlink()  # This invocation's current trial file only.
                    encode(ffmpeg,source,target,bitrate)
                    data = target.read_bytes()
                    if group == 'bgm' and len(data) > POLICY['singleBgmBudget']: continue
                    measurement = analyze(ffmpeg,source,target,info)
                    if measurement['minSdrDb'] >= POLICY['minSdrDb']:
                        analysis = measurement; break
                require(analysis is not None, 'No encoding meets quality/single-file budget: '+key)
                digest = sha(data); name = group+'/'+digest+'.mp3'
                target.rename(temporary/name)
                item = None
                if description:
                    text = safe_source(root,'help',description).read_text(encoding='utf-8').encode('utf-8')
                    item = {'source':'help/'+description,'sha256':sha(text),'bytes':len(text),'file':'descriptions/'+sha(text)+'.txt'}
                    (temporary/item['file']).write_bytes(text)
                manifest[group][key] = {**info,'source':source.relative_to(root/'src/main/resources').as_posix(),
                    'sourceSha256':file_sha(source),'sourceBytes':source.stat().st_size,'file':name,'sha256':digest,
                    'bytes':len(data),'bitrate':bitrate,'mime':'audio/mpeg','description':item,
                    'analysis':analysis}
                print('Converted '+group+'/'+key,file=sys.stderr,flush=True)
        (temporary/'manifest.json').write_bytes(manifest_bytes(manifest))
        report = check(root,temporary,ffmpeg)
        temporary.rename(output)
        return report
    finally:
        if temporary.exists(): shutil.rmtree(temporary)  # Only this invocation's temporary directory.


def distribution(root, dist, ffmpeg):
    public = root/'web-client/public/assets/audio'; output = dist/'assets/audio'
    result = check(root,output,ffmpeg)
    require((public/'manifest.json').read_bytes() == (output/'manifest.json').read_bytes(), 'Distribution audio manifest differs')
    text_bytes = sum(p.stat().st_size for p in dist.rglob('*') if p.is_file() and p.suffix in ('.json','.txt'))
    static_bytes = sum(p.stat().st_size for p in dist.rglob('*') if p.is_file())
    require(text_bytes <= POLICY['textBudget'], 'Combined image/audio/help text budget exceeded')
    require(static_bytes <= POLICY['staticBudget'], 'Total static budget exceeded')
    return {**result,'allTextBytes':text_bytes,'totalStaticBytes':static_bytes}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command',choices=('build','check','distribution'))
    parser.add_argument('--root',type=Path,default=Path(__file__).resolve().parents[1])
    parser.add_argument('--output',type=Path)
    parser.add_argument('--dist',type=Path)
    parser.add_argument('--ffmpeg',default=os.environ.get('FFMPEG','ffmpeg'))
    args = parser.parse_args(); root = args.root.resolve()
    output = args.output or root/'web-client/public/assets/audio'
    try:
        report = distribution(root,args.dist or root/'web-client/dist',args.ffmpeg) if args.command == 'distribution' else (build if args.command == 'build' else check)(root,output,args.ffmpeg)
        print(json.dumps(report,ensure_ascii=False,sort_keys=True))
    except (ValueError,OSError,KeyError,TypeError,IndexError,ZeroDivisionError,EOFError,wave.Error,subprocess.SubprocessError) as error:
        print('WEB_AUDIO_ERROR: '+str(error),file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
