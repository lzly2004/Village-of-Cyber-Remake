"""Local CI contracts, event fixtures and hash/extraction failures; no JVM or network.

Run by script path: python3 -B tools/tests/test_ci_workflow.py.
The ci-tests command deliberately uses this entry; -m unittest is unsupported
because the sibling test_ci_evidence import depends on the script directory.
"""
import copy
import hashlib
import importlib.util
import io
import sys
import json
from pathlib import Path
import subprocess
import tarfile
import tempfile
import unittest
from unittest.mock import patch
import zipfile

import yaml  # CI pins PyYAML; local tests also support the system BaseLoader.

ROOT = Path(__file__).resolve().parents[2]


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, ROOT / path)
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


CONTEXT = module('ci_context', 'tools/ci/context.py')
BOOT = module('ci_bootstrap', 'tools/ci/bootstrap.py')
RUN = module('ci_run', 'tools/ci/run.py')
LOCK = json.loads((ROOT / 'tools/ci/toolchain.json').read_text())
FLOW = yaml.load((ROOT / '.github/workflows/verify.yml').read_text(), Loader=yaml.BaseLoader)
HEAD, BASE, MERGE = '1' * 40, '2' * 40, '3' * 40


class GitEnvironmentTest(unittest.TestCase):
    def test_git_version_effective_limit_and_unset_are_recorded_without_mutation(self):
        for code, raw, expected in [(1, '', None), (0, '10000\n', 10000), (0, '0\n', 0)]:
            with patch.object(BOOT.subprocess, 'check_output', return_value='git version fixture\n') as version, patch.object(BOOT.subprocess, 'run', return_value=subprocess.CompletedProcess([], code, raw, '')) as config:
                self.assertEqual({'git_version': 'git version fixture', 'git_diff_rename_limit': expected}, BOOT.git_environment())
                self.assertEqual(['git', '--version'], version.call_args.args[0])
                self.assertEqual(['git', 'config', '--get', 'diff.renameLimit'], config.call_args.args[0])
        for code, raw in [(2, ''), (0, 'not-a-number'), (0, '-1')]:
            with patch.object(BOOT.subprocess, 'check_output', return_value='git version fixture'), patch.object(BOOT.subprocess, 'run', return_value=subprocess.CompletedProcess([], code, raw, '')):
                with self.assertRaises(ValueError): BOOT.git_environment()


class ContextTest(unittest.TestCase):
    def read(self, *args):
        if args == ('rev-parse', 'HEAD'):
            return HEAD
        if args[0] == 'check-ref-format':
            return args[-1]
        if args[:2] == ('rev-parse', '--verify'):
            return BASE
        raise AssertionError(args)

    def test_pr_uses_real_base_head_instead_of_merge_or_parent(self):
        event = {'pull_request': {'base': {'sha': BASE}, 'head': {'sha': HEAD}}, 'title': '$(untrusted)'}
        r = CONTEXT.resolve(event, 'pull_request', {'GITHUB_SHA': MERGE}, self.read)
        self.assertEqual((BASE, HEAD), (r['base_sha'], r['head_sha']))
        self.assertEqual('pull_request.base.sha', r['base_source'])
        self.assertFalse(r['force_full'])

    def test_push_existing_branch_uses_actual_before(self):
        r = CONTEXT.resolve({'before': BASE, 'after': HEAD}, 'push', {}, self.read)
        self.assertEqual(BASE, r['base_sha'])
        self.assertEqual('push.before', r['base_source'])

    def test_new_branch_explicit_default_base_not_arbitrary_parent(self):
        calls = []
        def read(*args):
            calls.append(args)
            return self.read(*args)
        r = CONTEXT.resolve({'before': CONTEXT.ZERO, 'after': HEAD, 'repository': {'default_branch': 'main'}}, 'push', {}, read)
        self.assertEqual('new_branch:origin/default_branch', r['base_source'])
        self.assertIn(('rev-parse', '--verify', 'refs/remotes/origin/main^{commit}'), calls)
        self.assertFalse(any('HEAD^' in arg for args in calls for arg in args))

    def test_missing_or_rewritten_base_never_falls_back(self):
        def read(*args):
            if args[0] == 'rev-parse' and args[1] == '--verify':
                raise subprocess.CalledProcessError(128, ['git'])
            return self.read(*args)
        with self.assertRaisesRegex(ValueError, 'BASE_COMMIT_UNAVAILABLE'):
            CONTEXT.resolve({'before': BASE, 'after': HEAD}, 'push', {}, read)

    def test_manual_full_explicit_and_validated(self):
        r = CONTEXT.resolve({'inputs': {'base_sha': BASE, 'full': 'true'}}, 'workflow_dispatch', {'GITHUB_SHA': HEAD}, self.read)
        self.assertTrue(r['force_full'])
        for bad in ('yes', 1, {}, None):
            with self.subTest(full=bad), self.assertRaisesRegex(ValueError, 'INVALID_FULL_INPUT'):
                CONTEXT.resolve({'inputs': {'base_sha': BASE, 'full': bad}}, 'workflow_dispatch', {'GITHUB_SHA': HEAD}, self.read)

    def test_bad_sha_checkout_merge_and_deleted_event_refused(self):
        for bad in ('HEAD^', '-h', HEAD + '\n', '$(echo secret)', CONTEXT.ZERO):
            with self.subTest(sha=bad), self.assertRaisesRegex(ValueError, 'INVALID_EVENT_SHA'):
                CONTEXT.resolve({'before': BASE, 'after': bad}, 'push', {}, self.read)
        with self.assertRaisesRegex(ValueError, 'CHECKOUT_IS_NOT_EVENT_HEAD'):
            CONTEXT.resolve({'before': BASE, 'after': MERGE}, 'push', {}, self.read)
        with self.assertRaisesRegex(ValueError, 'DELETED_PUSH'):
            CONTEXT.resolve({'before': BASE, 'after': HEAD, 'deleted': True}, 'push', {}, self.read)

    def test_unsafe_default_branch_refused_before_git_option_handling(self):
        for branch in ('--all', 'main\necho private'):
            with self.assertRaisesRegex(ValueError, 'INVALID_DEFAULT_BRANCH'):
                CONTEXT.resolve({'before': CONTEXT.ZERO, 'after': HEAD, 'repository': {'default_branch': branch}}, 'push', {}, self.read)

    def test_production_smoke_requires_browser_node_and_java_without_seed_override(self):
        flags = CONTEXT.flags({'suites': [{'id': 'production-smoke'}], 'requires_lfs': True})
        for key in ('CI_NEEDS_JAVA', 'CI_NEEDS_NODE', 'CI_NEEDS_BROWSER'):
            self.assertTrue(flags[key])
        config = (ROOT / 'web-client/playwright.production.config.js').read_text()
        self.assertIn('run-gradle-safe.sh runWeb --args=17072 --offline', config)
        self.assertNotIn('runWebTestServer', config)
        self.assertIn('PRODUCTION_SMOKE_SERVER_OVERRIDE_FORBIDDEN', config)
        self.assertIn('reuseExistingServer: false', config)
        self.assertIn('../build/verify/production-browser', config)

    def test_hosting_requires_actual_browser_java_pinned_tool_and_keeps_keys_outside_artifacts(self):
        flags=CONTEXT.flags({'suites':[{'id':'hosting-smoke'}],'requires_lfs':True})
        for key in ('CI_NEEDS_HOSTING','CI_NEEDS_JAVA','CI_NEEDS_NODE','CI_NEEDS_BROWSER'):self.assertTrue(flags[key])
        self.assertFalse(CONTEXT.flags({'suites':[{'id':'hosting-tests'}],'requires_lfs':False})['CI_NEEDS_HOSTING'])
        config=(ROOT/'tools/playwright.hosting.config.mjs').read_text()
        self.assertIn('ignoreHTTPSErrors:false',config);self.assertIn('--ignore-certificate-errors-spki-list=',config)
        self.assertNotIn("'--ignore-certificate-errors'",config)
        self.assertIn('run-gradle-safe.sh runWeb --args=17075 --offline',config)
        self.assertIn('HOSTING_SERVER_OVERRIDE_FORBIDDEN',config)
        self.assertIn('build/hosting-private/',config)
        workflow=(ROOT/'.github/workflows/verify.yml').read_text()
        self.assertIn('tools/hosting-toolchain.json',workflow)
        uploads=[x for x in FLOW['jobs']['verify']['steps'] if 'upload-artifact' in x.get('uses','')]
        self.assertTrue(uploads)
        for upload in uploads:self.assertNotIn('hosting-private',upload['with']['path'])

    def test_scope_flags_skip_heavy_tools_for_docs_and_require_full_chain_for_ci(self):
        docs = CONTEXT.flags(CONTEXT.VERIFY.plan(['docs/guide.md']))
        self.assertFalse(any(docs.values()))
        full = CONTEXT.flags(CONTEXT.VERIFY.plan(['.github/workflows/verify.yml']))
        self.assertTrue(all(full.values()))
        ci_test = CONTEXT.flags(CONTEXT.VERIFY.plan(['tools/tests/test_ci_workflow.py']))
        self.assertTrue(ci_test['CI_NEEDS_PIP'])
        self.assertFalse(ci_test['CI_NEEDS_JAVA'])


class WorkflowTest(unittest.TestCase):
    def setUp(self):
        self.job = FLOW['jobs']['verify']
        self.steps = self.job['steps']

    def step(self, part):
        return next(s for s in self.steps if part in s['name'])

    def test_single_job_read_permissions_timeout_and_concurrency(self):
        self.assertEqual(['verify'], list(FLOW['jobs']))
        self.assertEqual({'contents': 'read'}, FLOW['permissions'])
        self.assertEqual(LOCK['runner'], self.job['runs-on'])
        self.assertEqual('45', self.job['timeout-minutes'])
        self.assertEqual('true', FLOW['concurrency']['cancel-in-progress'])
        self.assertNotIn('strategy', self.job)
        self.assertNotIn('VOCR_APPROVED_FULL_CHECK', self.job['env'])
        for step in self.steps:
            if step['name'].startswith('Execute once'):
                self.assertEqual('1', step['env']['VOCR_APPROVED_FULL_CHECK'])
            else:
                self.assertNotIn('VOCR_APPROVED_FULL_CHECK', step.get('env', {}))
        self.assertNotIn('pull_request_target', FLOW['on'])
        self.assertNotIn('workflow_run', FLOW['on'])

    def test_all_actions_pinned_to_verified_commit_and_versions_fixed(self):
        for s in self.steps:
            if 'uses' in s:
                name, sha = s['uses'].split('@')
                self.assertEqual(LOCK['actions'][name]['sha'], sha)
                self.assertRegex(sha, r'^[0-9a-f]{40}$')
        self.assertEqual(LOCK['python'], self.step('Fixed Python')['with']['python-version'])
        self.assertEqual(LOCK['node'], self.step('Fixed Node')['with']['node-version'])
        self.assertEqual(LOCK['java'], self.step('Fixed JDK')['with']['java-version'])
        self.assertEqual('false', self.step('Fixed JDK')['with']['check-latest'])

    def test_checkout_real_pr_head_and_history_without_persistent_credentials(self):
        c = self.step('Checkout')['with']
        self.assertIn('pull_request.head.sha', c['ref'])
        self.assertNotIn('merge', c['ref'])
        self.assertEqual('0', c['fetch-depth'])
        self.assertEqual('false', c['persist-credentials'])
        self.assertEqual('false', c['lfs'])

    def test_cache_then_real_hydration_and_no_cached_test_outputs(self):
        names = [s['name'] for s in self.steps]
        self.assertLess(names.index(self.step('raw LFS')['name']), names.index(self.step('Hydrate actual')['name']))
        hydrate = self.step('Hydrate actual')['run']
        self.assertIn('git lfs install --local --skip-smudge', hydrate)
        self.assertIn('git lfs fetch origin HEAD', hydrate)
        self.assertIn('git lfs fsck', hydrate)
        cache_paths = [s['with']['path'] for s in self.steps if s.get('uses', '').startswith('actions/cache@')]
        self.assertTrue(any('.git/lfs/objects' in s for s in cache_paths))
        for path in cache_paths:
            self.assertNotIn('browser-export', path)
            self.assertNotIn('desktop-actual', path)
            self.assertNotIn('build/verify', path)
            self.assertNotIn('test-results', path)

    def test_plan_before_warm_and_execution_all_in_same_job(self):
        names = [s['name'] for s in self.steps]
        self.assertLess(names.index(self.step('Verified dry-run')['name']), names.index(self.step('Warm Java')['name']))
        self.assertLess(names.index(self.step('Warm Java')['name']), names.index(self.step('Execute once')['name']))
        self.assertIn('--base "$CI_BASE_SHA" --dry-run --json', self.step('Verified dry-run')['run'])
        self.assertIn('tools/run-gradle-safe.sh testClasses', self.step('Warm Java')['run'])
        execute = self.step('Execute once')['run']
        self.assertIn('xvfb-run', execute)
        self.assertNotIn('--suite', execute)
        self.assertFalse(any(s.get('uses', '').startswith('actions/download-artifact@') for s in self.steps))

    def test_failure_artifacts_use_existing_results_but_not_caches_or_fat_jar(self):
        s = self.step('Upload existing')
        self.assertEqual('always()', s['if'])
        paths = s['with']['path']
        self.assertIn('build/verify/**', paths)
        self.assertIn('build/ci/plan.json', paths)
        self.assertIn('build/ci/outcome.json', paths)
        self.assertNotIn('downloads', paths)
        self.assertNotIn('build/ci/**', paths)
        self.assertNotIn('build/libs', paths)
        self.assertEqual('14', s['with']['retention-days'])

    def test_evidence_before_upload_and_separate_receipt_after_upload(self):
        names=[s['name'] for s in self.steps]
        report=self.step('Assemble evidence')
        upload=self.step('Upload existing')
        receipt=self.step('Record the uploaded')
        preserve=self.step('Preserve the separate')
        self.assertEqual('always()',report['if'])
        self.assertEqual('evidence_upload',upload['id'])
        self.assertLess(names.index(report['name']),names.index(upload['name']))
        self.assertLess(names.index(upload['name']),names.index(receipt['name']))
        self.assertIn('outputs.artifact-digest',receipt['env']['CI_ARTIFACT_DIGEST'])
        self.assertNotIn('receipt.json',upload['with']['path'])
        self.assertEqual('build/ci/receipt.json',preserve['with']['path'])
        self.assertEqual('14',preserve['with']['retention-days'])

    def test_release_upload_is_success_only_separate_and_precedes_evidence(self):
        names=[s['name'] for s in self.steps];upload=self.step('Upload verified Web release');identity=self.step('Record verified release')
        self.assertIn('success()',upload['if']);self.assertIn('steps.execute.outputs.release_ready',upload['if'])
        self.assertEqual('error',upload['with']['if-no-files-found']);self.assertIn('build/releases/ci/vocr-web.zip',upload['with']['path'])
        self.assertNotIn('build/releases',self.step('Upload existing')['with']['path'])
        self.assertLess(names.index(upload['name']),names.index(identity['name']));self.assertLess(names.index(identity['name']),names.index(self.step('Assemble evidence')['name']))
        self.assertIn('--release-upload',identity['run']);self.assertIn('steps.release_upload.outputs.artifact-digest',identity['env']['CI_ARTIFACT_DIGEST'])
        self.assertNotIn('VOCR_APPROVED_FULL_CHECK',identity.get('env',{}))

    def test_requirements_are_exact_hash_locked_ci_only_wheels(self):
        lines = (ROOT / 'tools/ci/requirements-linux-x64.txt').read_text().splitlines()
        requirements = [x for x in lines if x and not x.startswith('#')]
        self.assertEqual(3, len(requirements))
        for line in requirements:
            self.assertRegex(line, r'^[-A-Za-z]+==[0-9.]+ --hash=sha256:[0-9a-f]{64}$')
        self.assertIn('--require-hashes --only-binary=:all:', self.step('hashed CI-only')['run'])


class BootstrapTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)

    def test_download_and_cache_verify_exact_bytes_without_refetching(self):
        data = b'fixed archive'
        spec = {'url': 'https://official.invalid/archive', 'sha256': hashlib.sha256(data).hexdigest()}
        target = self.root / 'download'
        BOOT.archive(spec, target, lambda *args, **kwargs: io.BytesIO(data))
        BOOT.archive(spec, target, lambda *args, **kwargs: self.fail('cache should not refetch'))
        target.write_bytes(b'corrupt')
        with self.assertRaisesRegex(ValueError, 'CACHED_ARCHIVE_HASH'):
            BOOT.archive(spec, target)
        self.assertEqual(b'corrupt', target.read_bytes())

    def test_bad_download_does_not_publish_partial_or_hide_failure(self):
        spec = {'url': 'https://official.invalid/archive', 'sha256': '0' * 64}
        target = self.root / 'download'
        with self.assertRaisesRegex(ValueError, 'ARCHIVE_HASH'):
            BOOT.archive(spec, target, lambda *args, **kwargs: io.BytesIO(b'bad'))
        self.assertFalse(target.exists())
        self.assertFalse(target.with_suffix('.partial').exists())

    def test_gradle_zip_path_traversal_rejected_before_extraction(self):
        file = self.root / 'package.zip'
        with zipfile.ZipFile(file, 'w') as z:
            z.writestr('gradle-8.8/bin/gradle', b'launcher')
            z.writestr('gradle-8.8/../../outside', b'escape')
        with self.assertRaisesRegex(ValueError, 'ARCHIVE_PATH'):
            BOOT.gradle(file, self.root / 'output', '8.8')
        self.assertFalse((self.root / 'output').exists())
        self.assertFalse((self.root / 'outside').exists())

    def test_gradle_valid_launcher_executable_and_no_overwrite(self):
        file = self.root / 'package.zip'
        with zipfile.ZipFile(file, 'w') as z:
            z.writestr('gradle-8.8/bin/gradle', b'launcher')
        binary = BOOT.gradle(file, self.root, '8.8')
        self.assertTrue(binary.stat().st_mode & 0o100)
        with self.assertRaisesRegex(ValueError, 'ALREADY_EXISTS'):
            BOOT.gradle(file, self.root, '8.8')

    def test_lfs_only_exact_regular_binary_with_matching_hash(self):
        file = self.root / 'package.tar'
        data = b'fixture binary'
        with tarfile.open(file, 'w') as t:
            info = tarfile.TarInfo('git-lfs-3.8.1/git-lfs')
            info.size = len(data)
            t.addfile(info, io.BytesIO(data))
        spec = {'version': '3.8.1', 'binary_sha256': hashlib.sha256(data).hexdigest()}
        binary = BOOT.lfs(file, self.root / 'bin/git-lfs', spec)
        self.assertEqual(data, binary.read_bytes())
        with self.assertRaisesRegex(ValueError, 'LFS_OUTPUT_ALREADY_EXISTS'):
            BOOT.lfs(file, binary, spec)
        with self.assertRaisesRegex(ValueError, 'BINARY_HASH'):
            BOOT.lfs(file, self.root / 'different', {**spec, 'binary_sha256': '0' * 64})

    def test_no_newline_in_environment_file_values(self):
        file = self.root / 'env'
        with self.assertRaisesRegex(ValueError, 'INVALID_ENVIRONMENT'):
            BOOT.append_environment(file, {'PATH': 'safe\nEVIL=value'})
        self.assertFalse(file.exists())


class RunTest(unittest.TestCase):
    def test_plan_execution_and_context_head_must_agree(self):
        plan = {'status': 'planned', 'tested_sha': HEAD, 'base_sha': BASE, 'plan': {'tier': 'full'}, 'tier_policy_sha256': 'hash'}
        result = {**plan, 'status': 'passed'}
        context = {'head_sha': HEAD, 'base_sha': BASE}
        RUN.compare(plan, result, context)
        for field in ('tested_sha', 'base_sha', 'tier_policy_sha256', 'status', 'plan'):
            changed = copy.deepcopy(result)
            changed[field] = 'different'
            with self.subTest(field=field), self.assertRaisesRegex(ValueError, 'MISMATCH'):
                RUN.compare(plan, changed, context)

    def test_ci_wrapper_executes_once_with_exact_base_and_records_success_or_failure(self):
        for mode in ('success', 'suite-failure', 'invalid-output'):
            with self.subTest(mode=mode), tempfile.TemporaryDirectory() as temporary:
                root = Path(temporary)
                directory = root / 'build/ci'
                directory.mkdir(parents=True)
                plan = {'status': 'planned', 'tested_sha': HEAD, 'base_sha': BASE,
                        'plan': {'tier': 'docs', 'suites': []}, 'tier_policy_sha256': 'hash'}
                context = {'head_sha': HEAD, 'base_sha': BASE, 'force_full': False}
                (directory / 'plan.json').write_text(json.dumps(plan))
                (directory / 'context.json').write_text(json.dumps(context))
                calls = []
                def execute(argv, **kwargs):
                    calls.append(argv)
                    if mode == 'suite-failure':
                        kwargs['stderr'].write(b'controlled suite failure')
                        return subprocess.CompletedProcess(argv, 9)
                    if mode == 'invalid-output':
                        kwargs['stdout'].write(b'not JSON')
                        return subprocess.CompletedProcess(argv, 0)
                    result = {**plan, 'status': 'passed', 'run_dir': 'build/verify/run'}
                    file = root / result['run_dir'] / 'result.json'
                    file.parent.mkdir(parents=True)
                    file.write_text(json.dumps(result))
                    kwargs['stdout'].write(json.dumps(result).encode())
                    return subprocess.CompletedProcess(argv, 0)
                env = {'GITHUB_ACTIONS': 'true', 'GITHUB_RUN_ID': '123', 'GITHUB_RUN_ATTEMPT': '2'}
                with patch.object(RUN, 'ROOT', root), patch.object(RUN, 'execute', side_effect=execute), patch.dict(RUN.os.environ, env, clear=True):
                    code = RUN.main()
                self.assertEqual(1, len(calls))
                self.assertEqual(['bash', 'tools/verify.sh', '--base', BASE, '--json'], calls[0])
                outcome = json.loads((directory / 'outcome.json').read_text())
                self.assertEqual('verify-123-2', outcome['artifact_name'])
                self.assertEqual(0 if mode == 'success' else 1, code)
                self.assertEqual('passed' if mode == 'success' else 'failed', outcome['status'])
                if mode == 'suite-failure':
                    self.assertEqual(9, outcome['verification_exit_code'])

    def test_packaging_failure_prevents_success_and_is_not_a_second_verification(self):
        with tempfile.TemporaryDirectory() as temporary:
            root=Path(temporary);d=root/'build/ci';d.mkdir(parents=True)
            plan={'status':'planned','tested_sha':HEAD,'base_sha':BASE,'plan':{'tier':'full','suites':[]},'tier_policy_sha256':'hash'}
            (d/'plan.json').write_text(json.dumps(plan));(d/'context.json').write_text(json.dumps({'head_sha':HEAD,'base_sha':BASE,'force_full':False}))
            def execute(argv,**kw):
                result={**plan,'status':'passed','run_dir':'build/verify/run'};p=root/'build/verify/run/result.json';p.parent.mkdir(parents=True);p.write_text(json.dumps(result));kw['stdout'].write(json.dumps(result).encode());return subprocess.CompletedProcess(argv,0)
            env={'GITHUB_ACTIONS':'true','GITHUB_RUN_ID':'123','GITHUB_RUN_ATTEMPT':'2'}
            with patch.object(RUN,'ROOT',root),patch.object(RUN,'execute',side_effect=execute) as verify,patch.object(RUN,'package_release',side_effect=ValueError('WEB_RELEASE_BUILD_OR_SMOKE_FAILED')) as package,patch.dict(RUN.os.environ,env,clear=True):
                self.assertEqual(1,RUN.main());self.assertEqual(1,verify.call_count);self.assertEqual(1,package.call_count)
            outcome=json.loads((d/'outcome.json').read_text());self.assertEqual('failed',outcome['status']);self.assertTrue(outcome['release_required']);self.assertNotIn('web_release',outcome)

    def test_package_report_must_match_same_result_head_and_actual_archive(self):
        with tempfile.TemporaryDirectory() as temporary:
            root=Path(temporary);d=root/'build/ci';d.mkdir(parents=True)
            result=root/'build/verify/run/result.json';result.parent.mkdir(parents=True);result.write_bytes(b'{}')
            output=root/'build/releases/ci';output.mkdir(parents=True);archive=output/'vocr-web.zip';archive.write_bytes(b'fixture')
            report={'status':'passed','source_sha':HEAD,'verification':{'resultSha256':RUN.digest(result)},
                    'archive':{'sha256':RUN.digest(archive),'bytes':7},'packaged_smoke':{'status':'passed','tests':1}}
            (output/'release-report.json').write_text(json.dumps(report))
            with patch.object(RUN,'ROOT',root),patch.object(RUN.subprocess,'run',return_value=subprocess.CompletedProcess([],0)) as run:
                value=RUN.package_release(result,HEAD,d)
                self.assertEqual('build/ci/web-release.json',value['report'])
                self.assertEqual(1,run.call_count);self.assertIn('--verified-result',run.call_args[0][0]);self.assertIn(str(result),run.call_args[0][0])
                self.assertNotIn('tools/verify.sh',run.call_args[0][0])
                archive.write_bytes(b'changed')
                with self.assertRaisesRegex(ValueError,'MISMATCH'):RUN.package_release(result,HEAD,d)

    def test_jar_boundary_and_hash_without_publishing_binary(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            file = root / 'app.jar'
            with zipfile.ZipFile(file, 'w') as z:
                z.writestr('com/vocr/app/Game.class', b'fixture')
            with patch.object(RUN, 'ROOT', root):
                r = RUN.jar_report(file)
                self.assertEqual('app.jar', r['path'])
                self.assertEqual(hashlib.sha256(file.read_bytes()).hexdigest(), r['sha256'])
                with zipfile.ZipFile(file, 'a') as z:
                    z.writestr('io/javalin/Server.class', b'not desktop')
                with self.assertRaisesRegex(ValueError, 'CONTENT_BOUNDARY'):
                    RUN.jar_report(file)


# unittest discovers the focused evidence/reader contracts in this existing CI suite.
import test_ci_evidence
EvidenceTest = test_ci_evidence.EvidenceTest
DownloadTest = test_ci_evidence.DownloadTest
ProgressTest = test_ci_evidence.ProgressTest
import test_playtest
PlaytestCandidateTest = test_playtest.CandidateTest
PlaytestStoreTest = test_playtest.LocalStoreTest
PlaytestRuntimeTest = test_playtest.RuntimeTest
PlaytestPreviewBuildTest = test_playtest.PreviewBuildTest
PreviewEvidenceTest = test_ci_evidence.PreviewEvidenceTest

if __name__ == '__main__':
    unittest.main()
