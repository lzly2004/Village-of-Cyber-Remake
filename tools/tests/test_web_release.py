"""Release ZIP integrity/budget/negative contracts. No JVM, browser or network."""
import importlib.util
import copy
import hashlib
import json
from pathlib import Path
import tempfile
import unittest
import zipfile
from unittest.mock import patch

ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('web_release', ROOT/'tools/web_release.py')
R=importlib.util.module_from_spec(spec);spec.loader.exec_module(R)


class ReleaseTest(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup)
        self.root=Path(self.temp.name)/'repo';self.root.mkdir()
        for name,data in {'LICENSE':b'MIT fixture','build.gradle':b'build','settings.gradle':b'settings','gradle.properties':b'properties',
                          'config/probability.json':b'{"probabilities":[]}',
                          'src/main/java/Main.java':b'class Main {}','src/web/java/Server.java':b'class Server {}',
                          'web-client/dist/index.html':b'<html>fixture</html>',
                          'web-client/dist/assets/app.js':b'console.log("fixture")',
                          'web-client/dist/assets/app.css':b'body {}',
                          'web-client/dist/assets/images/image.webp':b'webp',
                          'web-client/dist/assets/audio/bgm/a.mp3':b'mp3',
                          'web-client/dist/assets/audio/sounds/a.mp3':b'mp3',
                          'web-client/dist/assets/images/manifest.json':b'{}',
                          'web-client/package.json':b'{"version":"0.1.0","dependencies":{"vue":"3.5.44"},"devDependencies":{"vite":"8.3.5"}}'}.items():self.write(name,data)
        self.write('build/web-runtime/server.jar',self.jar({'com/vocr/web/GuestGameServer.class':b'x','com/vocr/application/GameSession.class':b'x','com/vocr/logging/WebDiagnostics.class':b'x'}))
        self.write('build/web-runtime/lib/dependency-1.jar',self.jar({'fixture.class':b'x'}))
        self.head='a'*40;self.fingerprints={n:{'sha256':'b'*64,'files':1} for n in ('java','frozen','inputs','build_policy')}
        self.audit={'sha':self.head,'worktree':{'git_status_clean':True,'matches_ref_bytes':True,'differences':[]},'fingerprints':self.fingerprints}
        self.result={'status':'passed','dry_run':False,'tested_sha':self.head,'run_dir':'build/verify/fixture',
                     'input_audit':copy.deepcopy(self.audit),'final_audit':copy.deepcopy(self.audit),
                     'plan':{'tier':'full','suites':[{'id':n} for n in R.REQUIRED_SUITES]},'suites':[]}
        for name in R.REQUIRED_SUITES:
            data=(name+' passed\n').encode();self.write('build/verify/fixture/'+name+'.log',data)
            self.result['suites'].append({'id':name,'status':'passed','log':name+'.log','log_sha256':R.sha(data)})
        self.result_file=self.root/'build/verify/fixture/result.json';self.save_result();self.stamp()
        next(s for s in self.result['suites'] if s['id']=='web-build')['frontend_files']={n:{'bytes':p.stat().st_size,'sha256':R.hash_file(p)} for n,p in R.scan(self.root/'web-client/dist').items()};self.save_result()
    def write(self,name,data):
        p=self.root/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(data);return p
    def jar(self,files):
        import io
        stream=io.BytesIO()
        with zipfile.ZipFile(stream,'w') as z:
            for n,b in files.items():z.writestr(n,b)
        return stream.getvalue()
    def save_result(self):self.result_file.write_text(json.dumps(self.result))
    def stamp(self):
        runtime=self.root/'build/web-runtime'
        self.runtime_stamp={'schema':1,'sourceSha':self.head,'javaVersion':'21.fixture','gradleVersion':'8.8',
                    'sourceFiles':{n:R.hash_file(self.root/n) for n in R.runtime_sources(self.root)},
                    'files':{str(p.relative_to(runtime)):R.hash_file(p) for p in runtime.rglob('*.jar')}}
        (runtime/'runtime-build.json').write_text(json.dumps(self.runtime_stamp))
        next(s for s in self.result['suites'] if s['id']=='java-full')['runtime_build_sha256']=R.hash_file(runtime/'runtime-build.json');self.save_result()
    def build(self,name='candidate'):
        with patch.object(R,'current_audit',return_value=copy.deepcopy(self.audit)),patch.object(R,'resource_gates'):
            return R.build_candidate(self.root,self.result_file,self.root/'build'/name)
    def release_cli(self):
        import sys
        with patch.dict(sys.modules,{'web_release':R}):
            spec=importlib.util.spec_from_file_location('release_cli_test',ROOT/'tools/build-web-release.py')
            cli=importlib.util.module_from_spec(spec);spec.loader.exec_module(cli)
        cli.ROOT=self.root
        return cli

    def test_candidate_published_only_after_actual_smoke_result_and_unchanged_extracted_files(self):
        cli=self.release_cli()
        def successful(argv,**kwargs):
            kwargs['stdout'].write(b'  1 passed (1s)\n')
            return type('Result',(),{'returncode':0})()
        with patch.object(R,'current_audit',return_value=copy.deepcopy(self.audit)),patch.object(R,'resource_gates'),patch.object(cli.subprocess,'run',side_effect=successful):
            report=cli.build(self.result_file,self.root/'build/final')
        self.assertEqual('passed',report['status']);self.assertEqual(1,report['packaged_smoke']['tests'])
        self.assertEqual(R.hash_file(self.root/'build/final/vocr-web.zip'),report['archive']['sha256'])

    def test_failed_smoke_or_mutated_extracted_payload_never_publishes_output(self):
        cli=self.release_cli()
        for mutation in ('failed','changed'):
            def execute(argv,**kwargs):
                if mutation=='changed':Path(kwargs['env']['VOCR_RELEASE_DIR'],'frontend/index.html').write_bytes(b'changed')
                kwargs['stdout'].write(b'  1 passed (1s)\n')
                return type('Result',(),{'returncode':1 if mutation=='failed' else 0})()
            output=self.root/'build'/('never-'+mutation)
            with patch.object(R,'current_audit',return_value=copy.deepcopy(self.audit)),patch.object(R,'resource_gates'),patch.object(cli.subprocess,'run',side_effect=execute),self.assertRaisesRegex(ValueError,'PACKAGED_SMOKE_FAILED|EXTRACTED_FILES_CHANGED_DURING_SMOKE'):
                cli.build(self.result_file,output)
            self.assertFalse(output.exists())

    def test_valid_archive_is_deterministic_and_binds_source_config_and_verified_result(self):
        a=self.build();b=self.build('other');self.assertEqual(R.hash_file(a),R.hash_file(b))
        v=R.check_release(a);self.assertEqual(self.head,v['source']['sha']);self.assertEqual(R.hash_file(self.result_file),v['verification']['resultSha256'])
        self.assertEqual(R.hash_file(self.root/'config/probability.json'),v['files']['config/probability.json']['sha256'])
        self.assertEqual(3,v['budgets']['bgmBytes']);self.assertEqual(3,v['budgets']['soundsBytes'])
    def test_result_failure_dry_run_missing_suite_old_head_or_final_drift_is_refused(self):
        for mutate in [lambda r:r.update(status='failed'),lambda r:r.update(dry_run=True),lambda r:r.update(tested_sha='c'*40),
                       lambda r:r['suites'].pop(),lambda r:r['suites'][0].update(status='failed'),
                       lambda r:r['final_audit']['fingerprints']['java'].update(sha256='c'*64)]:
            old=copy.deepcopy(self.result);mutate(self.result);self.save_result()
            with self.assertRaises(ValueError):self.build()
            self.assertFalse((self.root/'build/candidate').exists());self.result=old;self.save_result()
    def test_dirty_source_and_noncanonical_or_corrupt_result_logs_are_refused(self):
        for key in ('git_status_clean','matches_ref_bytes'):
            self.audit['worktree'][key]=False
            with self.assertRaises(ValueError):self.build()
            self.audit['worktree'][key]=True
        self.write('build/verify/fixture/diff-check.log',b'changed')
        with self.assertRaises(ValueError):self.build()
    def test_ignored_frontend_output_added_removed_or_changed_after_green_build_is_refused(self):
        path=self.root/'web-client/dist/assets/app.js';old=path.read_bytes();path.write_bytes(b'changed')
        with self.assertRaisesRegex(ValueError,'FRONTEND_OUTPUT_DRIFT'):self.build()
        path.write_bytes(old);extra=self.write('web-client/dist/extra.txt',b'accidental private export')
        with self.assertRaisesRegex(ValueError,'FRONTEND_OUTPUT_DRIFT'):self.build()
        extra.unlink();path.unlink()
        with self.assertRaises(ValueError):self.build()

    def test_stale_runtime_source_config_jar_or_stamp_is_refused(self):
        for name in ['src/web/java/Server.java','config/probability.json','build/web-runtime/lib/dependency-1.jar']:
            path=self.root/name;old=path.read_bytes();path.write_bytes(old+b'changed')
            with self.assertRaises(ValueError):self.build()
            path.write_bytes(old)
        self.runtime_stamp['sourceSha']='c'*40;(self.root/'build/web-runtime/runtime-build.json').write_text(json.dumps(self.runtime_stamp))
        with self.assertRaises(ValueError):self.build()
    def test_output_overwrite_source_tree_and_symlink_ancestors_are_refused(self):
        self.build()
        with self.assertRaises(ValueError):self.build()
        with patch.object(R,'current_audit',return_value=self.audit),patch.object(R,'resource_gates'):
            with self.assertRaises(ValueError):R.build_candidate(self.root,self.result_file,self.root/'src/new')
            with self.assertRaises(ValueError):R.build_candidate(self.root,self.result_file,self.root/'build/../src/new')
        (self.root/'build/link').symlink_to(self.root/'build',target_is_directory=True)
        with patch.object(R,'current_audit',return_value=self.audit),patch.object(R,'resource_gates'):
            with self.assertRaises(ValueError):R.build_candidate(self.root,self.result_file,self.root/'build/link/output')
    def test_input_symlinks_case_collisions_and_unknown_static_formats_are_refused(self):
        bad=self.root/'web-client/dist/assets/leak';bad.symlink_to(self.root/'config/probability.json')
        with self.assertRaises(ValueError):self.build()
        bad.unlink();self.write('web-client/dist/INDEX.html',b'case')
        with self.assertRaises(ValueError):self.build()
        (self.root/'web-client/dist/INDEX.html').unlink();self.write('web-client/dist/assets/late-font.woff2',b'font')
        with self.assertRaisesRegex(ValueError,'UNCLASSIFIED_STATIC_FILE'):self.build()
    def test_test_classes_or_original_media_in_backend_are_refused(self):
        for name in ['com/vocr/web/SeededGuestGameServer.class','org/junit/Test.class','BGM/original.wav','images/hidden.png']:
            original=(self.root/'build/web-runtime/server.jar').read_bytes()
            with zipfile.ZipFile(self.root/'build/web-runtime/server.jar','a') as z:z.writestr(name,b'bad')
            self.stamp()
            with self.assertRaises(ValueError):self.build()
            self.write('build/web-runtime/server.jar',original)
        self.stamp()
    def test_archive_tamper_missing_extra_duplicate_case_and_path_escape_are_refused(self):
        valid=self.build()
        with zipfile.ZipFile(valid) as z:files={n:z.read(n) for n in z.namelist()}
        for change in ['tamper','missing','extra','duplicate','case','escape']:
            target=self.root/'build'/('bad-'+change+'.zip')
            with zipfile.ZipFile(target,'w') as z:
                for n,b in files.items():
                    if change=='missing' and n=='frontend/index.html':continue
                    z.writestr(n,b+b'changed' if change=='tamper' and n=='frontend/index.html' else b)
                if change in ['extra','duplicate','case','escape']:
                    z.writestr({'extra':'extra.txt','duplicate':'frontend/index.html','case':'frontend/INDEX.html','escape':'../escape'}[change],b'bad')
            with self.assertRaises(ValueError):R.check_release(target)
    def test_encrypted_symlink_oversize_and_duplicate_json_keys_are_refused(self):
        valid=self.build()
        with patch.object(R,'MAX_FILE',1),self.assertRaises(ValueError):R.check_release(valid)
        target=self.root/'build/link.zip'
        with zipfile.ZipFile(target,'w') as z:
            entry=zipfile.ZipInfo('link');entry.create_system=3;entry.external_attr=0o120777<<16;z.writestr(entry,b'../outside')
        with self.assertRaises(ValueError):R.check_release(target)
        with self.assertRaises(ValueError):R.load_json(b'{"schema":1,"schema":2}')
        raw=bytearray(valid.read_bytes());local=raw.index(b'PK\x03\x04');central=raw.index(b'PK\x01\x02');raw[local+6]|=1;raw[central+8]|=1
        encrypted=self.root/'build/encrypted.zip';encrypted.write_bytes(raw)
        with self.assertRaises(ValueError):R.check_release(encrypted)
    def test_budget_counts_all_backup_formats_code_dialogue_and_combined_text(self):
        files={'index.html':b'html','app.js':b'dialogue','assets/images/a.webp':b'img',
               'assets/audio/bgm/a.mp3':b'aaaa','assets/audio/bgm/backup/a.ogg':b'bbbb',
               'assets/audio/sounds/a.mp3':b'cc','assets/images/manifest.json':b'{}'}
        self.assertEqual(8,R.budgets(files)['bgmBytes'])
        for key in ['static','code','gzip','images','singleImage','bgm','singleBgm','sounds','text']:
            with patch.dict(R.BUDGET,{key:1}),self.assertRaises(ValueError):R.budgets(files)
    def test_extract_checks_every_byte_before_writing_and_refuses_existing_destination(self):
        a=self.build();target=self.root/'build/extracted';R.extract_checked(a,target)
        self.assertTrue((target/'backend/run.sh').is_file())
        with self.assertRaises(ValueError):R.extract_checked(a,target)
        b=self.root/'build/bad.zip';b.write_bytes(b'not zip')
        with self.assertRaises(ValueError):R.extract_checked(b,self.root/'build/no-output')
        self.assertFalse((self.root/'build/no-output').exists())
    def test_cli_requires_this_round_approval_and_has_no_skip_smoke_or_fake_success_switch(self):
        import os,subprocess,sys
        env=dict(os.environ);env.pop('VOCR_APPROVED_FULL_CHECK',None)
        cli=[sys.executable,'-B',str(ROOT/'tools/build-web-release.py')]
        for args,expected in [(['build'],'FULL_CHECK_APPROVAL_REQUIRED'),(['build','--verified-result',str(self.result_file)],'PACKAGE_APPROVAL_REQUIRED')]:
            result=subprocess.run(cli+args,env=env,capture_output=True,text=True)
            self.assertEqual(1,result.returncode);self.assertIn(expected,result.stderr)
        result=subprocess.run(cli+['build','--skip-smoke'],env=env,capture_output=True,text=True)
        self.assertEqual(2,result.returncode)

    def test_launchers_pin_config_heap_and_classpath_without_gradle_or_source_dependency(self):
        a=self.build()
        with zipfile.ZipFile(a) as z:
            sh=z.read('backend/run.sh').decode();cmd=z.read('backend/run.cmd').decode()
        for text in (sh,cmd):
            self.assertIn('-Xmx256m',text);self.assertIn('com.vocr.web.GuestGameServer',text)
            self.assertIn(R.hash_file(self.root/'config/probability.json'),text)
            self.assertNotIn('gradlew',text)
        self.assertIn('backend/server.jar:backend/lib/*',sh);self.assertIn('backend\\server.jar;backend\\lib\\*',cmd)


if __name__=='__main__':unittest.main()
