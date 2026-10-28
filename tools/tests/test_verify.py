"""Planner/gate fixtures. No ffmpeg, JVM, browser, network or global config needed."""
import copy
import contextlib
import io
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import time
import unittest
from unittest.mock import patch

TOOLS = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location('verify', TOOLS / 'verify.py')
VERIFY = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(VERIFY)


class VerifyTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name) / 'repo'
        self.root.mkdir()
        self.git('init', '-q')
        self.git('config', 'user.name', 'Fixture')
        self.git('config', 'user.email', 'fixture@example.invalid')
        self.git('config', 'core.autocrlf', 'false')
        self.git('config', 'core.eol', 'lf')
        for name in ('verify.sh', 'verify.py', 'verify-tiers.json', 'audit-ci-inputs.py'):
            self.write('tools/' + name, (TOOLS / name).read_bytes())
        self.write('.gitignore', b'build/\n')
        self.write('src/main/java/A.java', b'class A {}\n')
        self.write('docs/guide.md', b'Guide\n')
        self.commit()
        self.base = self.git('rev-parse', 'HEAD').decode().strip()

    def write(self, name, data):
        path = self.root / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
        return path

    def git(self, *args):
        return subprocess.check_output(['git', '-C', str(self.root), *args], stderr=subprocess.PIPE)

    def commit(self):
        self.git('add', '.')
        self.git('commit', '-qm', 'fixture')

    def cli(self, *args, env=None):
        clean_env = dict(os.environ)
        clean_env.pop('VOCR_APPROVED_FULL_CHECK', None)
        clean_env.pop('VOCR_WEB_SERVER_COMMAND', None)
        clean_env.pop('DISPLAY', None)
        clean_env.update(env or {})
        return subprocess.run(['bash', str(self.root / 'tools/verify.sh'), '--base', self.base, *args],
                              cwd=self.temp.name, env=clean_env, capture_output=True, text=True)

    def result(self, names):
        audit = VERIFY.inventory(self.root, 'HEAD')
        return {'base_sha': self.base, 'tested_sha': audit['sha'], 'input_audit': audit,
                'plan': {'requires_lfs': False, 'suites': [{'id': n, 'argv': VERIFY.COMMANDS.get(n)} for n in names]}}

    def run_result(self, names, runner=None):
        directory = self.root / 'build/run'
        directory.mkdir(parents=True, exist_ok=True)
        r = self.result(names)
        code = VERIFY.run_suites(self.root, r, directory, runner=runner or (lambda *args: 0), env={})
        return code, r, json.loads((directory / 'result.json').read_text())

    def oracles(self, fingerprint=None):
        directory = self.root / 'build/web017'
        directory.mkdir(parents=True, exist_ok=True)
        value = fingerprint or VERIFY.inventory(self.root, 'HEAD')['fingerprints']['java_ref']
        for name in VERIFY.AUDIT.ORACLE_FILES:
            (directory / name).write_text(json.dumps({'sourceFingerprint': value}))
        return directory

    def test_successful_build_binds_exact_frontend_outputs_and_rejects_links_and_case_collisions(self):
        self.write('web-client/dist/index.html',b'fixture')
        self.commit()  # Fixture has no production dist ignore; keep source audit clean.
        code,result,saved=self.run_result(['web-build'])
        self.assertEqual(0,code)
        self.assertEqual({'index.html':{'bytes':7,'sha256':hashlib.sha256(b'fixture').hexdigest()}},saved['suites'][0]['frontend_files'])
        self.write('web-client/dist/INDEX.html',b'collision')
        with self.assertRaisesRegex(VERIFY.Refused,'DUPLICATE'):VERIFY.output_inventory(self.root/'web-client/dist')
        (self.root/'web-client/dist/INDEX.html').unlink()
        (self.root/'web-client/dist/link').symlink_to(self.root/'docs/guide.md')
        with self.assertRaisesRegex(VERIFY.Refused,'SYMLINK'):VERIFY.output_inventory(self.root/'web-client/dist')

    def test_full_plan_requires_release_tests_and_fresh_runtime_stamp_from_same_java_command(self):
        planned=VERIFY.plan([],True)
        self.assertEqual(24,len(planned['suites']))
        self.assertIn('hosting-smoke',[x['id'] for x in planned['suites']])
        self.assertIn('hosting-tests',[x['id'] for x in planned['suites']])
        self.assertIn('release-tests',[x['id'] for x in planned['suites']])
        self.assertIn('prepareWebRuntime',VERIFY.COMMANDS['java-full'])
        code,result,saved=self.run_result(['java-full'])
        self.assertEqual(1,code);self.assertEqual('WEB_RUNTIME_STAMP_MISSING',saved['suites'][0]['error'])
        stamp=self.write('build/web-runtime/runtime-build.json',b'fixture')
        code,result,saved=self.run_result(['java-full'])
        self.assertEqual(0,code);self.assertEqual(hashlib.sha256(stamp.read_bytes()).hexdigest(),saved['suites'][0]['runtime_build_sha256'])

    def test_opt_in_progress_is_stderr_json_and_running_entry_is_saved_before_child(self):
        directory = self.root / 'build/progress'
        directory.mkdir(parents=True)
        result = self.result(['diff-check'])
        def execute(argv, root, env, log):
            running = json.loads((directory/'result.json').read_text())
            self.assertEqual('running', running['suites'][0]['status'])
            return 0
        with contextlib.redirect_stderr(io.StringIO()) as err, contextlib.redirect_stdout(io.StringIO()) as out:
            code = VERIFY.run_suites(self.root, result, directory, runner=execute, env={'VOCR_VERIFY_PROGRESS':'1'})
        self.assertEqual(0, code)
        self.assertEqual('', out.getvalue())
        self.assertEqual(['suite_started','suite_finished','verification_finished'], [json.loads(s)['event'] for s in err.getvalue().splitlines()])

    def test_progress_failure_does_not_start_later_suite(self):
        directory = self.root/'build/progress-failure'
        directory.mkdir(parents=True)
        result = self.result(['diff-check','frontend-unit'])
        with contextlib.redirect_stderr(io.StringIO()) as err:
            code = VERIFY.run_suites(self.root,result,directory,runner=lambda *args:1,env={'VOCR_VERIFY_PROGRESS':'1'})
        self.assertEqual(1,code)
        events=[json.loads(s) for s in err.getvalue().splitlines()]
        self.assertEqual(2,len(events));self.assertEqual('failed',events[-1]['status'])
        self.assertEqual(1,len(result['suites']))

    def test_document_only_real_wrapper_executes_diff_and_emits_reusable_audit(self):
        self.write('docs/guide.md', b'Updated\n')
        self.commit()
        p = self.cli('--json')
        self.assertEqual(0, p.returncode, p.stderr)
        r = json.loads(p.stdout)
        self.assertEqual('passed', r['status'])
        self.assertEqual(['diff-check'], [s['id'] for s in r['suites']])
        self.assertEqual(r['tested_sha'], r['input_audit']['sha'])
        self.assertEqual(r['input_audit']['fingerprints'], r['final_audit']['fingerprints'])
        self.assertEqual('lf', r['eol_policy']['eol'])
        self.assertTrue(all(v == 'not_covered' for v in r['manual_gates'].values()))
        self.assertFalse(Path(r['suites'][0]['log']).is_absolute())
        self.assertEqual(hashlib.sha256(b'').hexdigest(), r['suites'][0]['log_sha256'])

    def test_dry_run_is_planned_not_passed_and_writes_nothing(self):
        self.write('tools/verify.py', (self.root / 'tools/verify.py').read_bytes() + b'\n')
        self.commit()
        before = (self.root / '.git/index').read_bytes()
        p = self.cli('--dry-run', '--json')
        self.assertEqual(0, p.returncode, p.stderr)
        r = json.loads(p.stdout)
        self.assertEqual('planned', r['status'])
        self.assertEqual('tooling', r['plan']['tier'])
        self.assertEqual(['diff-check', 'audit-tests', 'verify-tests'], [s['id'] for s in r['plan']['suites']])
        self.assertIn('not_triggered', r['plan']['skipped'][0]['reason'])
        self.assertFalse((self.root / 'build').exists())
        self.assertEqual(before, (self.root / '.git/index').read_bytes())

    def test_dirty_tracked_untracked_and_crlf_refused_before_any_suite_or_output(self):
        for name, data in [('src/main/java/A.java', b'class A { int x; }\n'),
                           ('private.txt', b'private'), ('src/main/java/A.java', b'class A {}\r\n')]:
            with self.subTest(name=name, data=data):
                path = self.write(name, data)
                original = path.read_bytes()
                p = self.cli('--dry-run', '--json')
                self.assertEqual(2, p.returncode)
                self.assertIn('DIRTY_OR_DIFFERENT_WORKTREE', p.stderr)
                self.assertFalse((self.root / 'build').exists())
                self.assertEqual(original, path.read_bytes())
                if name == 'private.txt':
                    path.unlink()
                else:
                    path.write_bytes(b'class A {}\n')

    def test_fingerprint_mismatch_is_refused_even_if_clean_flags_are_true(self):
        report = VERIFY.inventory(self.root, 'HEAD')
        report['fingerprints']['java_worktree'] = '0' * 64
        with self.assertRaisesRegex(VERIFY.Refused, 'DIRTY'):
            VERIFY.require_inputs(report)

    def test_clean_committed_crlf_java_is_refused_without_normalization(self):
        self.write('src/main/java/A.java', b'class A {}\r\n')
        self.commit()
        p = self.cli('--dry-run')
        self.assertEqual(2, p.returncode)
        self.assertIn('LF_JAVA_SOURCE_REQUIRED', p.stderr)
        self.assertIn(b'\r\n', (self.root / 'src/main/java/A.java').read_bytes())

    def test_conversion_setting_is_refused_even_if_bytes_currently_match(self):
        self.git('config', 'core.autocrlf', 'true')
        p = self.cli('--dry-run')
        self.assertEqual(2, p.returncode)
        self.assertIn('LF_CHECKOUT_POLICY_REQUIRED', p.stderr)

    def test_missing_lfs_pointer_refused_without_fetch_or_jvm(self):
        data = b'payload'
        digest = hashlib.sha256(data).hexdigest()
        pointer = f'version https://git-lfs.github.com/spec/v1\noid sha256:{digest}\nsize {len(data)}\n'.encode()
        p = self.write('src/main/resources/music.wav', pointer)
        self.commit()
        r = self.cli('--dry-run', '--json')
        self.assertEqual(2, r.returncode)
        self.assertIn('LFS_PAYLOAD_NOT_READY', r.stderr)
        self.assertEqual(pointer, p.read_bytes())
        self.assertFalse((self.root / '.git/lfs').exists())

    def test_unknown_paths_force_full_and_manual_paths_cannot_hide_actual_diff(self):
        self.write('src/main/java/A.java', b'class A { int x; }\n')
        self.commit()
        r = json.loads(self.cli('--dry-run', '--json', '--paths', 'docs/guide.md').stdout)
        self.assertEqual('full', r['plan']['tier'])
        self.assertEqual({'src/main/java/A.java', 'docs/guide.md'}, {p['path'] for p in r['plan']['paths']})
        unknown = VERIFY.plan(['novel/unknown.ext'])
        self.assertEqual('full', unknown['tier'])
        self.assertEqual(['novel/unknown.ext'], unknown['unknown_paths'])
        self.assertTrue(unknown['requires_full_java_approval'])

    def test_deleted_renamed_old_path_still_triggers_full(self):
        self.git('mv', 'src/main/java/A.java', 'docs/A.md')
        self.commit()
        r = json.loads(self.cli('--dry-run', '--json').stdout)
        self.assertEqual('full', r['plan']['tier'])
        self.assertEqual({'src/main/java/A.java', 'docs/A.md'}, {p['path'] for p in r['plan']['paths']})

    def test_full_dry_run_does_not_grant_approval_real_run_refuses_before_suites(self):
        p = self.cli('--full', '--dry-run', '--json')
        self.assertEqual(0, p.returncode, p.stderr)
        self.assertTrue(json.loads(p.stdout)['plan']['requires_full_java_approval'])
        p = self.cli('--full', '--json')
        self.assertEqual(2, p.returncode)
        self.assertIn('FULL_JAVA_PACKAGE_APPROVAL_REQUIRED', p.stderr)
        self.assertFalse((self.root / 'build').exists())

    def test_full_requires_display_and_rejects_server_override(self):
        p = self.cli('--full', env={'VOCR_APPROVED_FULL_CHECK': '1'})
        self.assertIn('DISPLAY_REQUIRED_FOR_FULL_JAVA', p.stderr)
        p = self.cli('--full', env={'VOCR_APPROVED_FULL_CHECK': '1', 'DISPLAY': ':999', 'VOCR_WEB_SERVER_COMMAND': 'unsafe'})
        self.assertIn('SERVER_OVERRIDE_NOT_ACCEPTED', p.stderr)
        self.assertFalse((self.root / 'build').exists())

    def test_production_smoke_is_required_only_in_full_and_runs_serially(self):
        full = VERIFY.plan(['web-client/production-tests/experience.spec.js'])
        ids = [s['id'] for s in full['suites']]
        self.assertEqual('full', full['tier'])
        self.assertLess(ids.index('web-build'), ids.index('production-smoke'))
        self.assertLess(ids.index('default-e2e'), ids.index('production-smoke'))
        self.assertLess(ids.index('production-smoke'), ids.index('java-full'))
        self.assertNotIn('production-smoke', [s['id'] for s in VERIFY.plan(['docs/a.md'])['suites']])
        self.assertIn('--config=playwright.production.config.js', VERIFY.COMMANDS['production-smoke'])

    def test_full_order_has_real_file_before_java_and_oracle_before_gate(self):
        r = VERIFY.plan(['src/web/java/Protocol.java'])
        ids = [s['id'] for s in r['suites']]
        for a, b in [('files-repeat', 'browser-file'), ('browser-file', 'java-full'), ('java-full', 'java-results'),
                     ('java-results', 'oracle-audit'), ('oracle-audit', 'gate'), ('gate', 'desktop-exchange')]:
            self.assertLess(ids.index(a), ids.index(b))
        self.assertNotIn('oracle-generate', ids)
        self.assertEqual(['bash', 'tools/run-gradle-safe.sh'], VERIFY.COMMANDS['java-full'][:2])
        self.assertIn('--rerun-tasks', VERIFY.COMMANDS['java-full'])
        self.assertNotIn('shell', json.dumps(r))

    def test_frontend_shared_changes_get_targeted_oracle_then_gate_not_full_java(self):
        r = VERIFY.plan(['web-client/src/App.vue'])
        ids = [s['id'] for s in r['suites']]
        self.assertEqual('frontend', r['tier'])
        self.assertFalse(r['requires_full_java_approval'])
        self.assertLess(ids.index('oracle-generate'), ids.index('oracle-audit'))
        self.assertLess(ids.index('oracle-audit'), ids.index('gate'))
        self.assertIn('com.vocr.web.Web017JourneyTest', VERIFY.COMMANDS['oracle-generate'])

    def test_sensitive_frozen_input_changes_visible_and_never_auto_authorized(self):
        r = VERIFY.plan(['src/test/resources/golden.json', 'config/probability.json'])
        self.assertEqual(['config/probability.json', 'src/test/resources/golden.json'], r['sensitive_paths_requiring_review'])
        self.assertEqual('full', r['tier'])

    def test_command_failure_stops_immediately_preserves_first_log_and_never_runs_next(self):
        seen = []
        def fail(argv, root, env, log):
            seen.append(argv)
            log.write(b'controlled first failure\n')
            return 17
        code, result, stored = self.run_result(['audit-tests', 'verify-tests'], fail)
        self.assertEqual(1, code)
        self.assertEqual(1, len(seen))
        self.assertEqual('audit-tests', stored['first_failure']['suite'])
        self.assertEqual(17, stored['suites'][0]['exit_code'])
        self.assertIn('controlled first failure', stored['suites'][0]['tail'])
        self.assertEqual(result, stored)

    def test_missing_executable_is_explicit_failure_not_a_skip(self):
        def missing(*args):
            raise FileNotFoundError('fixture tool unavailable')
        code, _, stored = self.run_result(['audit-tests'], missing)
        self.assertEqual(1, code)
        self.assertEqual('failed', stored['suites'][0]['status'])
        self.assertIn('fixture tool unavailable', stored['first_failure']['reason'])

    def test_final_audit_rejects_child_mutation_after_zero_exit(self):
        def mutation(argv, root, env, log):
            self.write('src/main/java/A.java', b'class A { int changed; }\n')
            return 0
        code, _, r = self.run_result(['diff-check'], mutation)
        self.assertEqual(1, code)
        self.assertEqual('final-input-audit', r['first_failure']['suite'])

    def test_oracle_missing_stale_bad_names_refused_clean_same_source_passes(self):
        for mode in ('missing', 'stale', 'renamed', 'valid'):
            with self.subTest(mode=mode):
                shutil.rmtree(self.root / 'build', ignore_errors=True)
                if mode != 'missing':
                    directory = self.oracles('0' * 64 if mode == 'stale' else None)
                    if mode == 'renamed':
                        (directory / 'jianyi-mixed.json').rename(directory / 'unexpected.json')
                code, _, r = self.run_result(['oracle-audit'])
                self.assertEqual(0 if mode == 'valid' else 1, code)
                if mode != 'valid':
                    self.assertEqual('ORACLE_STALE_OR_MISSING', r['first_failure']['reason'])

    def test_exchange_file_missing_stale_symlink_invalid_never_falls_back(self):
        start = time.time_ns()
        path = 'build/web035/browser-export.json'
        with self.assertRaisesRegex(VERIFY.Refused, 'MISSING'):
            VERIFY.require_file(self.root, path, start)
        p = self.write(path, json.dumps([{'version': 3}] + [None] * 8).encode())
        os.utime(p, ns=(start - 10**9, start - 10**9))
        with self.assertRaisesRegex(VERIFY.Refused, 'STALE'):
            VERIFY.require_file(self.root, path, start)
        p.unlink()
        p.symlink_to(self.root / 'docs/guide.md')
        with self.assertRaisesRegex(VERIFY.Refused, 'MISSING'):
            VERIFY.require_file(self.root, path, 0)
        p.unlink()
        p.write_bytes(b'[]')
        with self.assertRaisesRegex(VERIFY.Refused, 'INVALID'):
            VERIFY.require_file(self.root, path, 0)
        p.write_text(json.dumps([{'version': 3}] + [None] * 8))
        self.assertEqual(path, VERIFY.require_file(self.root, path, 0)['path'])

    def test_java_xml_requires_fresh_nonzero_and_zero_skips(self):
        folder = 'build/test-results/test/TEST-fixture.xml'
        for stats, accepted in [('tests="2" failures="0" errors="0" skipped="0"', True),
                                ('tests="2" failures="0" errors="0" skipped="1"', False),
                                ('tests="0" failures="0" errors="0" skipped="0"', False),
                                ('tests="2" failures="1" errors="0" skipped="0"', False)]:
            self.write(folder, ('<testsuite ' + stats + '/>').encode())
            if accepted:
                self.assertEqual(2, VERIFY.java_results(self.root, 0)['tests'])
            else:
                with self.assertRaises(VERIFY.Refused):
                    VERIFY.java_results(self.root, 0)
        with self.assertRaisesRegex(VERIFY.Refused, 'STALE'):
            VERIFY.java_results(self.root, time.time_ns() + 10**9)

    def test_unsafe_run_directory_refused_and_outside_files_not_touched(self):
        self.write('docs/guide.md', b'Updated\n')
        self.commit()
        outside = Path(self.temp.name) / 'outside'
        outside.mkdir()
        (self.root / 'build').symlink_to(outside, target_is_directory=True)
        self.write('.git/info/exclude', b'build\n')  # Reach directory guard past the independent dirty-tree gate.
        p = self.cli()
        self.assertEqual(2, p.returncode)
        self.assertIn('UNSAFE_RUN_DIRECTORY', p.stderr)
        self.assertEqual([], list(outside.iterdir()))

    def test_paths_and_refs_cannot_be_options_or_escape_repository(self):
        for p in ('../outside', '/absolute', './README.md', 'a//b', 'bad\\path', 'bad\npath'):
            with self.subTest(path=p), self.assertRaises(VERIFY.Refused):
                VERIFY.plan([p])
        r = self.cli('--base=-h', '--dry-run')
        self.assertEqual(2, r.returncode)
        self.assertIn('INVALID_BASE_REF', r.stderr)

    def test_missing_git_keeps_auditor_tool_diagnostic(self):
        empty = Path(self.temp.name) / 'empty-path'
        empty.mkdir()
        p = subprocess.run([sys.executable, '-B', str(self.root / 'tools/verify.py')],
                           env={**os.environ, 'PATH': str(empty)}, capture_output=True, text=True)
        self.assertEqual(2, p.returncode)
        self.assertEqual('TOOL_MISSING', json.loads(p.stderr)['error'])
        self.assertEqual('git', json.loads(p.stderr)['tool'])

    def test_invalid_policy_shapes_and_removed_full_gate_fail_closed(self):
        original = VERIFY.policy()
        for change in ('pattern-string', 'remove-gate', 'remove-production', 'unknown-not-full'):
            value = copy.deepcopy(original)
            if change == 'pattern-string':
                value['rules'][0]['patterns'] = '*'
            elif change == 'remove-production':
                value['tiers']['full'].remove('production-smoke')
            elif change == 'remove-gate':
                value['tiers']['full'].remove('gate')
            else:
                value['unknown'] = 'docs'
            with self.subTest(change=change), patch.object(VERIFY.json, 'loads', return_value=value):
                with self.assertRaises(VERIFY.Refused):
                    VERIFY.policy()

    def test_entry_cannot_claim_another_repository_sha(self):
        p = self.cli('--repo', self.temp.name, '--dry-run')
        self.assertEqual(2, p.returncode)
        self.assertIn('unrecognized arguments', p.stderr)
        self.assertFalse((self.root / 'build').exists())

    def test_missing_base_has_specific_diagnostic(self):
        r = self.cli('--base=missing-ref', '--dry-run')
        self.assertEqual(2, r.returncode)
        self.assertEqual('INVALID_BASE_REF', json.loads(r.stderr)['error'])

    def test_empty_actual_diff_refused_even_with_manual_paths_explicit_full_allowed(self):
        for paths in ([], ['--paths', 'docs/guide.md']):
            r = self.cli('--dry-run', *paths)
            self.assertEqual(2, r.returncode)
            self.assertEqual('NO_CHANGES_TO_VERIFY', json.loads(r.stderr)['error'])
        with self.assertRaisesRegex(VERIFY.Refused, 'NO_CHANGES'):
            VERIFY.plan([])
        r = self.cli('--dry-run', '--full', '--json')
        self.assertEqual(0, r.returncode, r.stderr)
        self.assertEqual('full', json.loads(r.stdout)['plan']['tier'])

    def test_exchange_self_changes_require_fresh_full_chain(self):
        for path in ('tools/playwright.exchange.config.mjs', 'tools/workflow-tests/replay-roundtrip.spec.mjs'):
            with self.subTest(path=path):
                r = VERIFY.plan([path])
                self.assertEqual('full', r['tier'])
                self.assertTrue(r['requires_full_java_approval'])
                self.assertIn('desktop-exchange', [s['id'] for s in r['suites']])
                self.assertIn('java-full', [s['id'] for s in r['suites']])

    def test_leaf_frontend_changes_do_not_force_gate_shared_flow_still_does(self):
        for path in ('web-client/src/HelpDialog.vue', 'web-client/src/labels.js', 'web-client/src/style.css',
                     'web-client/src/BgmControl.vue', 'web-client/src/ReplayLibrary.vue'):
            with self.subTest(path=path):
                r = VERIFY.plan([path])
                self.assertEqual('frontend', r['tier'])
                self.assertNotIn('gate', [s['id'] for s in r['suites']])
                self.assertIn('default-e2e', [s['id'] for s in r['suites']])
        for path in ('web-client/src/characterDisplay.js', 'web-client/src/useGuestGame.js', 'web-client/src/App.vue',
                     'web-client/gate-tests/release-gate.spec.js'):
            self.assertIn('gate', [s['id'] for s in VERIFY.plan([path])['suites']])

    def test_invalid_policy_suite_never_runs(self):
        bad = copy.deepcopy(VERIFY.policy())
        bad['tiers']['docs'] = ['not-a-suite']
        with patch.object(VERIFY.json, 'loads', return_value=bad):
            with self.assertRaisesRegex(VERIFY.Refused, 'UNKNOWN_SUITE'):
                VERIFY.policy()


if __name__ == '__main__':
    unittest.main()
