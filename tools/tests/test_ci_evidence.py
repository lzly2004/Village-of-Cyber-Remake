"""Evidence parsing, integrity and first-party IO fixtures; no network/JVM/browser."""
import contextlib
import copy
import hashlib
import importlib.util
import io
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch
import urllib.request
import urllib.error
import zipfile

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT/'tools/ci'))
import evidence as E
import download as D
spec = importlib.util.spec_from_file_location('evidence_run_smoke',ROOT/'tools/ci/run.py')
RUN=importlib.util.module_from_spec(spec);spec.loader.exec_module(RUN)
HEAD,BASE='1'*40,'2'*40
ENV={'CI_EXPECTED_HEAD':HEAD,'GITHUB_REPOSITORY':'owner/repo','GITHUB_RUN_ID':'123','GITHUB_RUN_ATTEMPT':'2','CI_JOB_STATUS':'success'}


class Fixture:
    def setUp(self):
        self.files={'build/ci/tools.json':b'{"python":"fixture","runner_image_version":"fixture"}'}
        self.context={'head_sha':HEAD,'base_sha':BASE}
        self.plan={'status':'planned','tested_sha':HEAD,'base_sha':BASE,'tier_policy_sha256':'hash',
                   'plan':{'tier':'frontend','suites':[{'id':'diff-check'},{'id':'frontend-unit'}],'skipped':[{'id':'gate','reason':'not_triggered'}]}}
        self.result={**copy.deepcopy(self.plan),'status':'passed','run_dir':'build/verify/run','input_audit':{'fingerprints':{'java_ref':'j'}},
                     'suites':[]}
        for name,text in [('diff-check',''),('frontend-unit','# tests 1\n# pass 1\n# fail 0\n# cancelled 0\n# skipped 0\n')]:
            b=text.encode();self.files['build/verify/run/'+name+'.log']=b
            self.result['suites'].append({'id':name,'status':'passed','argv':['fixture'], 'seconds':0.1,'log':name+'.log','log_sha256':E.digest(b),'exit_code':0})
        self.outcome={'status':'passed','result':'build/verify/run/result.json','artifact_name':'verify-123-2'}
        self.sync()

    def sync(self):
        self.files.update({'build/ci/context.json':json.dumps(self.context).encode(),'build/ci/plan.json':json.dumps(self.plan).encode(),
                           'build/verify/run/result.json':json.dumps(self.result).encode()})
        self.outcome['result_sha256']=E.digest(self.files['build/verify/run/result.json'])
        self.files['build/ci/outcome.json']=json.dumps(self.outcome).encode()

    def report(self,env=None):return E.assemble(self.files.__getitem__,self.files,env or ENV)

    def proof(self,directory,extra=None):
        p=Path(directory)/'proof.zip'
        with zipfile.ZipFile(p,'w') as z:
            for n,b in self.files.items():z.writestr(n,b)
            if extra:z.writestr(*extra)
        run={'id':123,'run_attempt':2,'status':'completed','conclusion':'success','head_sha':HEAD,'repository':{'full_name':'owner/repo'}}
        a={'id':7,'name':'verify-123-2','size_in_bytes':p.stat().st_size,'digest':'sha256:'+E.digest(p.read_bytes()),'expired':False,'workflow_run':{'id':123,'head_sha':HEAD}}
        return p,run,a



class EvidenceTest(Fixture, unittest.TestCase):
    def release_fixture(self):
        self.result['plan']['tier']='full';self.plan['plan']['tier']='full';self.outcome['release_required']=True;self.sync()
        log='build/verify/release-smoke-'+('d'*32)+'/packaged-smoke.log';self.files[log]=b'  1 passed (1s)\n'
        report={'status':'passed','source_sha':HEAD,'archive':{'name':'vocr-web.zip','bytes':123,'sha256':'a'*64},
                'manifest_sha256':'b'*64,'verification':{'resultSha256':self.outcome['result_sha256']},
                'packaged_smoke':{'status':'passed','tests':1,'log':log,'sha256':E.digest(self.files[log])}}
        data=json.dumps(report).encode();self.files['build/ci/web-release.json']=data
        self.outcome['web_release']={'report':'build/ci/web-release.json','sha256':E.digest(data)}
        self.files['build/ci/release-upload.json']=json.dumps({'repository':'owner/repo','run_id':'123','attempt':'2',
            'tested_head':HEAD,'artifact_name':'release-123-2','id':'8','digest':'c'*64,
            'url':'https://github.com/owner/repo/actions/runs/123/artifacts/8'}).encode();self.sync()

    def test_release_report_and_separate_upload_identity_are_bound_without_binary_in_evidence(self):
        self.release_fixture();report=self.report();self.assertEqual('passed',report['web_release']['status'])
        self.assertEqual('8',report['web_release']['artifact']['id']);self.assertNotIn('vocr-web.zip',self.files)
        self.assertIn('[Separate release artifact]',E.markdown(report))
        self.files['build/ci/evidence.json']=json.dumps(report).encode()
        with tempfile.TemporaryDirectory() as t:
            p,r,a=self.proof(t);value=D.validate(p,r,a,HEAD,'owner/repo');value.pop('download_proof');self.assertEqual(report,value)

    def test_release_upload_failure_after_green_packaging_cannot_pass(self):
        self.release_fixture();del self.files['build/ci/release-upload.json']
        report=self.report({**ENV,'CI_JOB_STATUS':'failure'})
        self.assertEqual('failed',report['status']);self.assertEqual('web-release',report['first_failure']['suite'])
        self.assertEqual('failed',report['web_release']['status'])

    def test_release_upload_cli_writes_only_separate_identity_and_checks_url(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);(root/'build/ci').mkdir(parents=True)
            env={**ENV,'GITHUB_ACTIONS':'true','CI_ARTIFACT_ID':'8','CI_ARTIFACT_URL':'https://github.com/owner/repo/actions/runs/123/artifacts/8','CI_ARTIFACT_DIGEST':'c'*64}
            with patch.object(E,'ROOT',root),patch.dict(os.environ,env,clear=True),patch.object(sys,'argv',['evidence.py','--release-upload']):self.assertEqual(0,E.main())
            value=json.loads((root/'build/ci/release-upload.json').read_text())
            self.assertEqual('release-123-2',value['artifact_name']);self.assertEqual(HEAD,value['tested_head'])
            self.assertFalse((root/'build/ci/receipt.json').exists())
            with patch.object(E,'ROOT',root),patch.dict(os.environ,{**env,'CI_ARTIFACT_URL':'https://evil/'},clear=True),patch.object(sys,'argv',['evidence.py','--release-upload']):self.assertEqual(1,E.main())

    def test_release_failure_after_green_tests_is_still_failed_and_no_release_can_be_claimed(self):
        self.release_fixture();self.outcome.update(status='failed',reason='WEB_RELEASE_BUILD_OR_SMOKE_FAILED');self.sync()
        report=self.report();self.assertEqual('failed',report['status']);self.assertEqual('web-release',report['first_failure']['suite'])
        self.assertTrue(all(s['status']=='passed' for s in report['suites']))

    def test_release_missing_changed_report_wrong_head_digest_upload_or_incomplete_smoke_is_refused(self):
        for change in ('missing','hash','head','upload-run','upload-head','upload-url','smoke','smoke-log','smoke-missing'):
            self.setUp();self.release_fixture()
            if change=='missing':del self.files['build/ci/release-upload.json']
            elif change=='hash':self.files['build/ci/web-release.json']+=b' '
            elif change in ('smoke-log','smoke-missing'):
                log=json.loads(self.files['build/ci/web-release.json'])['packaged_smoke']['log']
                if change=='smoke-log':self.files[log]=b'0 passed\n'
                else:del self.files[log]
            elif change in ('head','smoke'):
                value=json.loads(self.files['build/ci/web-release.json'])
                if change=='head':value['source_sha']=BASE
                else:value['packaged_smoke']['tests']=0
                data=json.dumps(value).encode();self.files['build/ci/web-release.json']=data
                self.outcome['web_release']['sha256']=E.digest(data);self.sync()
            else:
                value=json.loads(self.files['build/ci/release-upload.json']);value[{'upload-run':'run_id','upload-head':'tested_head','upload-url':'url'}[change]]='wrong'
                self.files['build/ci/release-upload.json']=json.dumps(value).encode()
            with self.subTest(change=change),self.assertRaises(ValueError):self.report()

    def test_nonfull_does_not_publish_and_legacy_four_has_no_release_schema_change(self):
        self.assertEqual({'status':'not_built','reason':'not_full_tier'},self.report()['web_release'])
        legacy=E.assemble(self.files.__getitem__,self.files,ENV,assembly_revision=4);self.assertNotIn('web_release',legacy)
        self.files['build/ci/evidence.json']=json.dumps(legacy).encode()
        with tempfile.TemporaryDirectory() as t:
            p,r,a=self.proof(t);value=D.validate(p,r,a,HEAD,'owner/repo');value.pop('download_proof');self.assertEqual(legacy,value)

    def test_counts_have_sources_unknown_is_not_zero_and_manual_gates_never_close(self):
        self.result['manual_gates']={'A08':'passed'};self.sync();r=self.report()
        self.assertEqual('passed',r['status']);self.assertIsNone(r['suites'][0]['counts'])
        self.assertEqual('node-tap-log',r['suites'][1]['counts']['source'])
        self.assertTrue(all(x=='not_covered' for x in r['manual_gates'].values()))
        self.assertEqual(HEAD,r['tested_sha']);self.assertEqual('gate',r['policy_skipped_suites'][0]['id'])

    def test_not_covered_deduplicates_without_reordering_or_closing_gates(self):
        self.result['not_covered']=['release approval','extra check','release approval','capacity/long game','last check'];self.sync()
        r=self.report()
        self.assertEqual(5,r['assembly_revision'])
        self.assertEqual(['real PR/fork event unless separately evidenced','release approval',
                          'physical output/listening/DPI','capacity/long game','extra check','last check'],r['not_covered'])
        self.assertTrue(all(x=='not_covered' for x in r['manual_gates'].values()))

    def test_markdown_links_are_run_then_artifact_then_digest_before_suites(self):
        r=self.report();r['download_proof']={'artifact_id':7,'artifact_digest':'sha256:fixture'}
        s=E.markdown(r)
        self.assertLess(s.index('[GitHub run]'),s.index('[Evidence artifact]'))
        self.assertLess(s.index('[Evidence artifact]'),s.index('ZIP digest:'))
        self.assertLess(s.index('ZIP digest:'),s.index('| Suite |'))

    def test_diff_check_summary_counts_issues_files_and_preserves_first_location(self):
        text='\x1b[31msrc/a.java:48: trailing whitespace.\x1b[0m\r\n+    \r\nsrc/a.java:50: space before tab in indent.\r\nsrc/b file.java:9: new blank line at EOF.\r\n'
        d=E.failure_detail(text,diff_check=True)
        self.assertEqual('src/a.java:48',d['case'])
        self.assertIn('3 issues / 2 files',d['first_error'])
        for reason in ['trailing whitespace: 1','space before tab in indent: 1','new blank line at EOF: 1']:
            self.assertIn(reason,d['first_error'])
        self.assertEqual({'case':None,'first_error':None},E.failure_detail(text))
        self.assertEqual({'case':None,'first_error':None},E.failure_detail('src/a:0: trailing whitespace.\nsrc/a:2: arbitrary message.',diff_check=True))
        d=E.failure_detail('src/a:1: trailing whitespace, space before tab in indent.\n',diff_check=True)
        self.assertEqual('src/a:1',d['case']);self.assertIn('1 issues / 1 files',d['first_error'])
        self.assertIn('trailing whitespace: 1; space before tab in indent: 1',d['first_error'])
        r=self.report();self.assertIsNone(r['first_failure'])

    def test_diff_check_summary_is_bounded_escaped_and_does_not_take_over_other_suites(self):
        text='<img>|[x]'+('a'*500)+':7: trailing whitespace.\n'
        d=E.failure_detail(text,diff_check=True)
        self.assertLessEqual(len(d['case']),400);self.assertLessEqual(len(d['first_error']),400)
        r=self.report();r['first_failure']={'suite':'diff-check',**d}
        self.assertNotIn('<img>',E.markdown(r));self.assertIn('\\|',E.markdown(r))
        d=E.failure_detail('not ok 1 - existing case\n  error: controlled\n')
        self.assertEqual('existing case',d['case']);self.assertEqual('error: controlled',d['first_error'])

    def test_unittest_playwright_and_tap_failure_counts_and_case_detail(self):
        self.assertEqual({'tests':3,'passed':1,'failures':1,'errors':0,'skipped':1,'source':'python-unittest-log'},E.counts('Ran 3 tests in 0.1s\n\nFAILED (failures=1, skipped=1)\n'))
        self.assertEqual(2,E.counts('  2 passed (1.0s)\n  1 skipped\n')['passed'])
        self.assertIsNone(E.counts('network timed out; no tests discovered'))
        self.assertEqual('foo (Test)',E.failure_detail('FAIL: foo (Test)\nAssertionError: 64 != 0')['case'])
        self.assertEqual('case',E.failure_detail('not ok 1 - case\n  error: bad')['case'])
        self.assertEqual('error: bad',E.failure_detail('not ok 1 - case\n  error: bad')['first_error'])
        with self.assertRaisesRegex(ValueError,'TAP_COUNT'):E.counts('# tests 9\n# pass 1\n# fail 0\n# skipped 0\n# cancelled 0\n')

    def test_failed_result_preserves_first_case_and_distinguishes_unexecuted(self):
        s=self.result['suites'][0];s['status']='failed';s['exit_code']=1
        b=b'FAIL: useful_case (Test)\nAssertionError: controlled\nRan 1 test in 0.1s\n\nFAILED (failures=1)\n'
        self.files['build/verify/run/diff-check.log']=b;s['log_sha256']=E.digest(b)
        self.result['suites']=self.result['suites'][:1];self.result['status']='failed';self.outcome['status']='failed';self.sync()
        r=self.report({**ENV,'CI_JOB_STATUS':'failure'})
        self.assertEqual('useful_case (Test)',r['first_failure']['case']);self.assertEqual(['frontend-unit'],r['unexecuted_suites'])

    def test_prep_failure_or_missing_output_cannot_become_passed(self):
        r=E.assemble(lambda n:b'',[],{**ENV,'CI_JOB_STATUS':'failure'})
        self.assertEqual('failed',r['status']);self.assertEqual([],r['suites']);self.assertIsNone(r['tested_sha'])
        self.assertEqual('not_run',E.assemble(lambda n:b'',[],ENV)['status'])

    def test_head_plan_log_result_hash_and_execution_completeness_are_required(self):
        mutations=[lambda:self.context.update(head_sha=BASE),lambda:self.plan.update(tier_policy_sha256='other'),
                   lambda:self.result['suites'].reverse(),lambda:self.result['suites'].pop(),
                   lambda:self.result['suites'][0].update(status='running'),lambda:self.outcome.update(status='failed')]
        for i,mutation in enumerate(mutations):
            self.setUp();mutation();self.sync()
            with self.subTest(i=i),self.assertRaises(ValueError):self.report()
        self.setUp();self.files['build/verify/run/diff-check.log']=b'corrupt'
        with self.assertRaisesRegex(ValueError,'LOG_HASH'):self.report()
        self.setUp();self.files['build/ci/verify.stdout']=b'{}'
        with self.assertRaisesRegex(ValueError,'STDOUT_RESULT'):self.report()
        self.setUp();self.files['build/verify/run/result.json']+=b' '
        with self.assertRaisesRegex(ValueError,'RESULT_HASH'):self.report()

    def test_xml_counts_use_bodies_and_failure_case_not_only_headers(self):
        xml=b'<testsuite tests="2" failures="1" errors="0" skipped="0"><testcase name="ok"/><testcase classname="C" name="bad"><failure message="broken"/></testcase></testsuite>'
        c,f=E.xml_counts(lambda n:xml,['TEST-a.xml']);self.assertEqual(2,c['tests']);self.assertEqual('C.bad',f['case'])
        with self.assertRaisesRegex(ValueError,'XML_BODY_COUNT'):E.xml_counts(lambda n:xml.replace(b'failures="1"',b'failures="0"'),['a'])
        with self.assertRaisesRegex(ValueError,'XML_ENTITY'):E.xml_counts(lambda n:b'<!DOCTYPE x>'+xml,['a'])

    def test_paths_and_symlinks_refused_without_reading_other_files(self):
        for name in ['../x','/tmp/x','a//b','a/./b','a\\b','a\nx']:
            with self.subTest(name=name),self.assertRaises(ValueError):E.safe_path(name)
        with tempfile.TemporaryDirectory() as t:
            root=Path(t);(root/'real').write_text('x');(root/'link').symlink_to(root/'real')
            with self.assertRaises(ValueError):E.local_reader(root)('link')

    def test_duplicate_json_keys_and_non_object_reports_are_explicitly_refused(self):
        for data in ['{"status":"failed","status":"passed"}','[]','null']:
            with self.subTest(data=data),self.assertRaises(ValueError):E.load_json(data)

    def test_markdown_bounds_and_escapes_untrusted_failure_text(self):
        r=self.report();r['first_failure']={'suite':'<script>','case':'`x` | [a]','first_error':'Error: https://host/?secret=example'}
        s=E.markdown(r);self.assertNotIn('<script>',s);self.assertNotIn('secret=example',s);self.assertIn('not_covered',s)

    def test_lfs_identity_aggregate_counts_deduplicated_payloads_not_requests(self):
        self.result['input_audit']['lfs']={'entries':[{'sha256':'a'*64,'bytes':3},{'sha256':'a'*64,'bytes':3},{'sha256':'b'*64,'bytes':4}]}
        self.sync();r=self.report()['lfs_summary']
        self.assertEqual((3,2,7),(r['declared_pointer_paths'],r['unique_payloads'],r['unique_payload_bytes']))
        self.assertEqual(E.digest(('a'*64+'\t3\n'+'b'*64+'\t4\n').encode()),r['oid_size_aggregate_sha256'])
        self.assertIsNone(r['remote_fetch_http_status_counts']);self.assertIsNone(r['account_usage'])

    def test_real_producer_cli_reads_only_evidence_not_tools_cache(self):
        with tempfile.TemporaryDirectory() as t:
            root=Path(t)
            for name,data in self.files.items():
                p=root/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(data)
            cache=root/'build/ci/downloads/private.env';cache.parent.mkdir();cache.write_text('DO_NOT_READ_CACHE')
            with patch.object(E,'ROOT',root),patch.dict(os.environ,{**ENV,'GITHUB_ACTIONS':'true'},clear=True),patch.object(sys,'argv',['evidence.py']):
                self.assertEqual(0,E.main())
            value=(root/'build/ci/evidence.json').read_text();self.assertNotIn('DO_NOT_READ_CACHE',value)
            self.assertEqual('passed',json.loads(value)['status']);self.assertTrue((root/'build/ci/summary.md').is_file())
        self.files.pop('build/ci/tools.json')
        with self.assertRaisesRegex(ValueError,'TOOLCHAIN_EVIDENCE'):self.report()

    def test_validation_failure_writes_failed_summary_without_manufacturing_passed_counts(self):
        with tempfile.TemporaryDirectory() as t:
            root=Path(t);d=root/'build/ci';d.mkdir(parents=True);(d/'context.json').write_text('not JSON')
            with patch.object(E,'ROOT',root),patch.dict(os.environ,{**ENV,'GITHUB_ACTIONS':'true'},clear=True),patch.object(sys,'argv',['evidence.py']),contextlib.redirect_stderr(io.StringIO()):
                self.assertEqual(1,E.main())
            r=json.loads((d/'evidence.json').read_text());self.assertEqual('failed',r['status']);self.assertEqual([],r['suites']);self.assertTrue((d/'summary.md').is_file())

    def test_post_upload_receipt_is_separate_and_checks_exact_url_digest_head(self):
        with tempfile.TemporaryDirectory() as t:
            root=Path(t);(root/'build/ci').mkdir(parents=True)
            env={**ENV,'GITHUB_ACTIONS':'true','CI_ARTIFACT_ID':'7','CI_ARTIFACT_URL':'https://github.com/owner/repo/actions/runs/123/artifacts/7','CI_ARTIFACT_DIGEST':'a'*64}
            with patch.object(E,'ROOT',root),patch.dict(os.environ,env,clear=True),patch.object(sys,'argv',['evidence.py','--receipt']):self.assertEqual(0,E.main())
            r=json.loads((root/'build/ci/receipt.json').read_text());self.assertEqual('a'*64,r['digest']);self.assertEqual(HEAD,r['tested_head'])
            with patch.object(E,'ROOT',root),patch.dict(os.environ,{**env,'CI_ARTIFACT_URL':'https://evil/'},clear=True),patch.object(sys,'argv',['x','--receipt']):self.assertEqual(1,E.main())


class DownloadTest(Fixture, unittest.TestCase):
    def test_preparation_failure_lists_are_stable_without_rewriting_legacy_reports(self):
        files = {'build/ci/context.json': json.dumps(self.context).encode()}
        env = {**ENV, 'CI_JOB_STATUS': 'cancelled'}
        for revision in (1, 2, 3, 4):
            report = E.assemble(files.__getitem__, files, env, assembly_revision=revision)
            self.assertEqual('failed', report['status'])
            self.assertEqual([], report['suites'])
            self.assertEqual('preparation-or-entry', report['first_failure']['suite'])
            for key in ('unexecuted_suites', 'policy_skipped_suites'):
                if revision >= 4:
                    self.assertEqual([], report[key])
                else:
                    self.assertNotIn(key, report)
            self.files = {**files, 'build/ci/evidence.json': json.dumps(report).encode()}
            with tempfile.TemporaryDirectory() as t:
                p, run, artifact = self.proof(t)
                run['conclusion'] = 'cancelled'
                value = D.validate(p, run, artifact, HEAD, 'owner/repo')
                value.pop('download_proof')
                self.assertEqual(report, value)

    def test_zip_is_recomputed_without_extracting_or_executing(self):
        self.files['build/ci/evidence.json']=json.dumps(self.report()).encode()
        with tempfile.TemporaryDirectory() as t:
            p,r,a=self.proof(t);value=D.validate(p,r,a,HEAD,'owner/repo')
            self.assertEqual('passed',value['status']);self.assertFalse(value['download_proof']['archive_executed_or_extracted'])
            self.assertIn('https://github.com/owner/repo/actions/runs/123/artifacts/7',E.markdown(value))
            self.assertIn(a['digest'],E.markdown(value))
            self.assertEqual([p],list(Path(t).iterdir()))

    def test_legacy_report_recomputes_exactly_and_revision_or_content_tampering_fails(self):
        self.result['not_covered']=['release approval','release approval'];self.sync()
        legacy=E.assemble(self.files.__getitem__,self.files,ENV,assembly_revision=1)
        self.assertNotIn('assembly_revision',legacy)
        self.assertEqual(3,legacy['not_covered'].count('release approval'))
        with tempfile.TemporaryDirectory() as t:
            self.files['build/ci/evidence.json']=json.dumps(legacy).encode()
            p,r,a=self.proof(t);value=D.validate(p,r,a,HEAD,'owner/repo')
            value.pop('download_proof');self.assertEqual(legacy,value)
            for revision in [0,6,True,None,2.0]:
                self.files['build/ci/evidence.json']=json.dumps({**legacy,'assembly_revision':revision}).encode()
                p,r,a=self.proof(t)
                with self.subTest(revision=revision),self.assertRaisesRegex(ValueError,'UNSUPPORTED_ASSEMBLY_REVISION'):
                    D.validate(p,r,a,HEAD,'owner/repo')
            for changed in [{**legacy,'status':'failed'},{**legacy,'not_covered':['release approval']},
                            {**legacy,'assembly_revision':2}]:
                self.files['build/ci/evidence.json']=json.dumps(changed).encode();p,r,a=self.proof(t)
                with self.assertRaisesRegex(ValueError,'DOES_NOT_RECOMPUTE'):D.validate(p,r,a,HEAD,'owner/repo')

    def test_revision_two_failed_diff_report_remains_exact_but_new_reports_have_detail(self):
        b=b'src/a.java:48: trailing whitespace.\n+   \nsrc/a.java:50: trailing whitespace.\n+ \nsrc/b.java:7: trailing whitespace.\n'
        self.files['build/verify/run/diff-check.log']=b
        self.result['suites']=self.result['suites'][:1]
        self.result['suites'][0].update(status='failed',exit_code=2,log_sha256=E.digest(b))
        self.result['status']='failed';self.outcome['status']='failed';self.sync()
        for revision in (2,3):
            env={**ENV,'CI_JOB_STATUS':'failure'}
            report=E.assemble(self.files.__getitem__,self.files,env,assembly_revision=revision)
            self.files['build/ci/evidence.json']=json.dumps(report).encode()
            with tempfile.TemporaryDirectory() as t:
                p,r,a=self.proof(t);r['conclusion']='failure';value=D.validate(p,r,a,HEAD,'owner/repo')
                value.pop('download_proof');self.assertEqual(report,value)
            self.assertEqual('failed',value['status']);self.assertEqual(2,value['suites'][0]['exit_code'])
            self.assertEqual(['frontend-unit'],value['unexecuted_suites'])
            if revision==2:self.assertIsNone(value['first_failure']['case'])
            else:
                self.assertEqual('src/a.java:48',value['first_failure']['case'])
                self.assertIn('3 issues / 2 files',value['first_failure']['first_error'])

    def test_wrong_attempt_run_head_digest_size_expiry_or_stored_report_refused(self):
        with tempfile.TemporaryDirectory() as t:
            p,r,a=self.proof(t)
            for changes in [{'name':'verify-123-1'},{'expired':True},{'digest':'sha256:'+'0'*64},{'size_in_bytes':1},{'workflow_run':{'id':999,'head_sha':HEAD}}]:
                with self.subTest(changes=changes),self.assertRaises(ValueError):D.validate(p,r,{**a,**changes},HEAD,'owner/repo')
            with self.assertRaises(ValueError):D.validate(p,{**r,'head_sha':BASE},a,HEAD,'owner/repo')
            self.files['build/ci/evidence.json']=b'{"status":"passed"}'
            p,r,a=self.proof(t)
            with self.assertRaisesRegex(ValueError,'DOES_NOT_RECOMPUTE'):D.validate(p,r,a,HEAD,'owner/repo')

    def test_zip_traversal_duplicate_case_and_entry_budgets_refused(self):
        with tempfile.TemporaryDirectory() as t:
            for extra in [('../escape',b'x'),('build/ci/PLAN.json',b'x'),('.git/config',b'not allowed')]:
                p,r,a=self.proof(t,extra)
                with self.subTest(extra=extra[0]),self.assertRaises(ValueError):D.validate(p,r,a,HEAD,'owner/repo')
            p,r,a=self.proof(t)
            with patch.object(D,'MAX_ENTRY',1),self.assertRaises(ValueError):D.validate(p,r,a,HEAD,'owner/repo')
            self.assertFalse((Path(t).parent/'escape').exists())

    def test_zip_symlink_is_rejected_without_touching_link_target(self):
        with tempfile.TemporaryDirectory() as t:
            p,r,a=self.proof(t)
            entry=zipfile.ZipInfo('build/ci/link');entry.create_system=3;entry.external_attr=(0o120777<<16)
            with zipfile.ZipFile(p,'a') as z:z.writestr(entry,b'/tmp/should-not-be-read')
            a['digest']='sha256:'+E.digest(p.read_bytes());a['size_in_bytes']=p.stat().st_size
            with self.assertRaisesRegex(ValueError,'SYMLINK'):D.validate(p,r,a,HEAD,'owner/repo')

    def test_redirect_never_carries_token_to_blob_or_untrusted_hosts(self):
        req=urllib.request.Request('https://api.github.com/repos/owner/repo/actions/artifacts/7/zip',headers={'Authorization':'Bearer fixture'})
        handler=D.Redirect();next_req=handler.redirect_request(req,None,302,'',{},'https://safe.blob.core.windows.net/file?sig=fixture')
        self.assertIsNone(next_req.get_header('Authorization'));self.assertEqual('Bearer fixture',req.get_header('Authorization'))
        for url in ['http://safe.blob.core.windows.net/x','https://evil.invalid/x','https://user:pw@github.com/x']:
            with self.assertRaises(ValueError):handler.redirect_request(req,None,302,'',{},url)

    def test_403_has_explicit_auth_diagnostic_and_no_secret_or_third_party_fallback(self):
        opener=unittest.mock.Mock();opener.open.side_effect=urllib.error.HTTPError('https://api.github.com/?secret=fixture',403,'token fixture',{},None)
        with patch.object(D.urllib.request,'build_opener',return_value=opener),self.assertRaisesRegex(ValueError,'^AUTHENTICATION_OR_ACTIONS_READ_REQUIRED$'):
            D.get('repos/owner/repo/actions/artifacts/7/zip','fixture')
        self.assertEqual(1,opener.open.call_count)

    def test_local_zip_can_use_fresh_public_metadata_without_downloading_with_token(self):
        with tempfile.TemporaryDirectory() as t:
            p,r,a=self.proof(t);r.update(head_repository={'full_name':'owner/repo'},head_branch='workflow/test')
            def read(path,token=None,limit=None):
                if path.endswith('/actions/runs/123'):return json.dumps(r).encode()
                if '/git/ref/' in path:return json.dumps({'object':{'sha':HEAD}}).encode()
                if path.endswith('/artifacts?per_page=100'):return json.dumps({'artifacts':[a,{'name':'receipt-123-2'}]}).encode()
                raise AssertionError('No archive download or third-party request expected: '+path)
            args=['--repository','owner/repo','--run-id','123','--expected-head',HEAD,'--output',str(Path(t)/'out'),'--zip',str(p)]
            with patch.object(D,'get',side_effect=read),contextlib.redirect_stdout(io.StringIO()):self.assertEqual(0,D.main(args))
            proof=json.loads((Path(t)/'out/evidence.json').read_text())['download_proof']
            self.assertEqual('GitHub REST read in this invocation',proof['metadata_origin']);self.assertEqual('local supplied ZIP',proof['archive_origin'])

    def test_offline_cli_refuses_overwrite_and_writes_handoff_with_no_freshness_claim(self):
        with tempfile.TemporaryDirectory() as t:
            p,r,a=self.proof(t);root=Path(t);(root/'run.json').write_text(json.dumps(r));(root/'artifact.json').write_text(json.dumps(a));out=root/'out'
            args=['--repository','owner/repo','--run-id','123','--expected-head',HEAD,'--output',str(out),'--zip',str(p),'--run-json',str(root/'run.json'),'--artifact-json',str(root/'artifact.json')]
            with contextlib.redirect_stdout(io.StringIO()):self.assertEqual(0,D.main(args))
            self.assertTrue((out/'handoff.md').exists());self.assertIn('not_checked_offline',json.loads((out/'evidence.json').read_text())['download_proof']['head_freshness'])
            with contextlib.redirect_stderr(io.StringIO()):self.assertEqual(2,D.main(args))


class ProgressTest(unittest.TestCase):
    def test_actual_python_child_tees_progress_stderr_and_keeps_json_stdout_and_exit_code(self):
        for exit_code in (0,7):
            with self.subTest(exit_code=exit_code),tempfile.TemporaryDirectory() as t:
                root=Path(t)
                script='import sys; print("{\\"status\\":\\"fixture\\"}"); print("suite_started",file=sys.stderr); sys.exit('+str(exit_code)+')'
                with (root/'out').open('wb') as out,(root/'err').open('wb') as err,contextlib.redirect_stderr(io.StringIO()) as display:
                    r=RUN.execute([sys.executable,'-c',script],root,out,err)
                self.assertEqual(exit_code,r.returncode);self.assertEqual({'status':'fixture'},json.loads((root/'out').read_text()))
                self.assertEqual('suite_started\n',(root/'err').read_text());self.assertEqual('suite_started\n',display.getvalue())



if __name__=='__main__':unittest.main()
