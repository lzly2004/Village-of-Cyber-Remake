"""Internal playtest contracts; temp directories and owned fake children only."""
import copy
import importlib.util
import io
import json
import os
from pathlib import Path
import socket
import signal
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
import playtest_candidate as P
import web_release as R


def jar(names):
    b = io.BytesIO()
    with zipfile.ZipFile(b, 'w') as z:
        for n in names: z.writestr(n, b'fixture')
    return b.getvalue()


def preview_files():
    files = {'backend/server.jar': jar(['com/vocr/web/GuestGameServer.class', 'com/vocr/application/GameSession.class',
                                      'com/vocr/logging/WebDiagnostics.class']),
             'backend/lib/parser.jar': jar(['Parser.class']), 'frontend/index.html': b'<html></html>',
             'frontend/assets/app.js': b'fixture', 'config/probability.json': b'{}', 'LICENSE': b'fixture',
             'PREVIEW-README.txt': b'Internal preview, not full.'}
    files['backend/run.sh'], files['backend/run.cmd'] = R.launchers(R.sha(files['config/probability.json']))
    stamp = {'schema': 1, 'sourceSha': 'a' * 40,
             'sourceFiles': {'config/probability.json': R.sha(b'{}')},
             'files': {n[8:]: R.sha(b) for n, b in files.items() if n.endswith('.jar')}}
    files['metadata/runtime-build.json'] = R.json_bytes(stamp)
    m = {'schema': 1, 'kind': P.PREVIEW,
         'source': {'sha': 'a' * 40, 'fingerprints': {**{n: {'sha256': 'b' * 64, 'files': 1}
                     for n in ['frozen', 'inputs', 'build_policy']}, 'java_ref': 'b' * 64,
                     'java_worktree': 'b' * 64, 'java_files': 1}},
         'runtime': {'javaMinimum': 21, 'stampSha256': R.sha(files['metadata/runtime-build.json'])},
         'verification': {'tier': 'frontend', 'status': 'passed', 'resultSha256': 'c' * 64,
                          'suites': [{'id': n, 'status': 'passed'} for n in sorted(P.REQUIRED_FRONTEND)], 'javaFull': False},
         'budgets': R.budgets({n[9:]: b for n, b in files.items() if n.startswith('frontend/')}),
         'budgetLimits': R.BUDGET, 'files': {n: {'bytes': len(b), 'sha256': R.sha(b)} for n, b in files.items()},
         'notCovered': ['java-full', 'manual A06/A07/A08/046/049']}
    return reseal(files, m)


def reseal(files, manifest=None):
    files = copy.deepcopy(files)
    if manifest is not None: files['preview-manifest.json'] = R.json_bytes(manifest)
    files['SHA256SUMS'] = ''.join(R.sha(b) + '  ' + n + '\n' for n, b in sorted(files.items()) if n != 'SHA256SUMS').encode()
    return files


class CandidateTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory(); self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)

    def archive(self, files):
        p = self.root / (str(len(list(self.root.iterdir()))) + '.zip'); R.write_archive(files, p); return p

    def test_preview_is_complete_and_cannot_be_read_as_full(self):
        path = self.archive(preview_files())
        kind, m, _ = P.check_candidate(path)
        self.assertEqual(('preview', 'a' * 40, False), (kind, m['source']['sha'], m['verification']['javaFull']))
        with self.assertRaisesRegex(ValueError, 'MANIFEST_MISSING'): R.check_release(path)

    def test_preview_refuses_full_claim_missing_suite_changed_bytes_and_mismatched_stamp(self):
        for mutate in [lambda m: m.update(kind='vocr-web-runtime'), lambda m: m['verification'].update(tier='full'),
                       lambda m: m['verification'].update(javaFull=True), lambda m: m['verification']['suites'].pop(),
                       lambda m: m['source'].update(sha='d' * 40), lambda m: m['runtime'].update(stampSha256='e' * 64),
                       lambda m: m.update(schema=True), lambda m: m.update(unexpected=True)]:
            files = preview_files(); m = R.load_json(files['preview-manifest.json']); mutate(m)
            with self.subTest(mutate=mutate), self.assertRaises((ValueError, TypeError)):
                P.check_preview(self.archive(reseal(files, m)))
        files = preview_files(); files['frontend/assets/app.js'] = b'changed'
        with self.assertRaisesRegex(ValueError, 'HASH_MISMATCH'): P.check_preview(self.archive(files))

    def test_preview_refuses_seeded_backend_even_when_manifest_resealed(self):
        files = preview_files()
        files['backend/server.jar'] = jar(['com/vocr/web/GuestGameServer.class', 'com/vocr/application/GameSession.class',
                                          'com/vocr/logging/WebDiagnostics.class', 'com/vocr/web/SeededGuestGameServer.class'])
        m = R.load_json(files['preview-manifest.json'])
        m['files']['backend/server.jar'] = {'bytes': len(files['backend/server.jar']), 'sha256': R.sha(files['backend/server.jar'])}
        with self.assertRaisesRegex(ValueError, 'SERVER_JAR_BOUNDARY'): P.check_preview(self.archive(reseal(files, m)))

    def test_preview_rejects_extra_files_ambiguous_kind_and_zip_paths(self):
        for name in ['release-manifest.json', 'data/private.json']:
            f = preview_files(); f[name] = b'{}'
            with self.assertRaises(ValueError): P.check_candidate(self.archive(f))
        p = self.root / 'unsafe.zip'
        with zipfile.ZipFile(p, 'w') as z: z.writestr('../escape', b'no')
        with self.assertRaisesRegex(ValueError, 'UNSAFE_PATH'): P.check_candidate(p)

    def test_extracted_candidate_drift_and_missing_file_fail_closed(self):
        files = preview_files(); dest = self.root / 'payload'
        for n, b in files.items():
            p = dest / n; p.parent.mkdir(parents=True, exist_ok=True); p.write_bytes(b)
        P.check_extracted(dest)
        (dest / 'frontend/index.html').write_bytes(b'late change')
        with self.assertRaisesRegex(ValueError, 'HASH_MISMATCH'): P.check_extracted(dest)


class LocalStoreTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory(); self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        import playtest_store as S
        self.S = S; self.home = S.home_path(self.root / 'home')
        self.files = preview_files(); self.archive = self.root / 'preview.zip'; R.write_archive(self.files, self.archive)
        self.key = 'preview-' + 'a' * 40 + '-' + R.hash_file(self.archive)
        folder = S.private_directory(S.private_directory(self.home / 'candidates') / self.key)
        (folder / 'vocr-preview.zip').write_bytes(self.archive.read_bytes())
        for n, b in self.files.items():
            p = folder / 'payload' / n; p.parent.mkdir(parents=True, exist_ok=True); p.write_bytes(b)
        S.atomic_json(folder / 'candidate.json', {'schema': 1, 'key': self.key, 'kind': 'preview', 'sourceSha': 'a' * 40,
            'run': 1, 'attempt': 1, 'archive': {'name': 'vocr-preview.zip', 'sha256': R.hash_file(self.archive)}})

    def test_same_candidate_profile_persists_and_changed_package_has_distinct_profile(self):
        a, downloads = self.S.profiles(self.home, self.key)
        (a / 'local-history.fixture').write_text('do not delete')
        b, again = self.S.profiles(self.home, self.key)
        self.assertEqual((a, downloads), (b, again)); self.assertEqual('do not delete', (b / 'local-history.fixture').read_text())
        second = self.key[:-64] + 'f' * 64
        c, _ = self.S.profiles(self.home, second); self.assertNotEqual(a, c)
        self.assertFalse((c / 'local-history.fixture').exists())

    def test_dangling_browser_lock_refuses_without_deletion_or_process_signal(self):
        p, _ = self.S.profiles(self.home, self.key); lock = p / 'SingletonLock'; lock.symlink_to('host-123')
        with self.assertRaisesRegex(ValueError, 'PROFILE_IN_USE'): self.S.profiles(self.home, self.key)
        self.assertTrue(lock.is_symlink())

    def test_other_candidate_browser_lock_refuses_switch_without_copying_or_deleting(self):
        old, _ = self.S.profiles(self.home, self.key)
        lock = old / 'SingletonLock'; lock.symlink_to('host-123')
        other = self.key[:-64] + 'f' * 64
        with self.assertRaisesRegex(ValueError, 'PROFILE_IN_USE'): self.S.profiles(self.home, other)
        self.assertTrue(lock.is_symlink())
        self.assertFalse((self.home / 'profiles' / other).exists())

    def test_feedback_default_is_unreviewed_and_only_explicit_attachments_are_copied(self):
        attachment = self.root / 'my chosen replay.json'; attachment.write_bytes(b'explicit user fixture')
        path = self.S.feedback(self.home, self.key, [attachment]); value = R.load_json(path.read_bytes())
        self.assertEqual('not_started', value['play']['completion'])
        for n in ('visual', 'sound', 'controls'): self.assertEqual('not_checked', value[n]['status'])
        self.assertEqual(1, len(value['attachments']))
        self.assertEqual(attachment.read_bytes(), (path.parent / value['attachments'][0]['name']).read_bytes())
        self.assertNotIn('sessionId', path.read_text()); self.assertNotIn('credential', value['candidate'])

    def test_import_without_explicit_first_party_confirmation_never_creates_candidate(self):
        p = self.root / 'untrusted.json'; p.write_bytes(b'{"schema":1,"repository":"lzly2004/Village-of-Cyber-Remake"}')
        before = sorted((self.home / 'candidates').iterdir())
        with self.assertRaisesRegex(ValueError, 'SOURCE_UNCONFIRMED'):
            self.S.import_candidate(self.home, self.archive, self.archive, self.archive, p)
        self.assertEqual(before, sorted((self.home / 'candidates').iterdir()))

    def test_archive_or_extracted_tree_changes_refuse_start_and_feedback(self):
        _, payload = self.S.candidate(self.home, self.key)
        (payload / 'frontend/index.html').write_bytes(b'changed')
        with self.assertRaisesRegex(ValueError, 'HASH_MISMATCH|CANDIDATE_CHANGED'): self.S.feedback(self.home, self.key)


class RuntimeTest(unittest.TestCase):
    def test_plain_routes_match_043_boundaries_but_do_not_claim_tls(self):
        import playtest_service as V
        import web_hosting as H
        with tempfile.TemporaryDirectory() as d:
            root = Path(d); front = root / 'front'; (front / 'assets').mkdir(parents=True)
            (front / 'index.html').write_bytes(b'html'); cert = root / 'fixture.pem'; cert.write_bytes(b'fixture')
            original = H.configuration(front, cert, cert)['apps']['http']['servers']['https']
            plain = V.configuration(front)
            self.assertEqual(original['routes'], plain['apps']['http']['servers']['playtest']['routes'])
            self.assertEqual(['127.0.0.1:18500'], plain['apps']['http']['servers']['playtest']['listen'])
            self.assertNotIn('tls', plain['apps']); self.assertTrue(plain['admin']['disabled'])

    def test_port_conflict_refuses_without_killing_occupant(self):
        import playtest_runtime as V
        with socket.socket() as sentinel:
            sentinel.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            sentinel.bind(('127.0.0.1', 0)); port = sentinel.getsockname()[1]; sentinel.listen()
            with patch.object(V.V, 'FRONT', port), patch.object(V.V, 'BACK', port):
                with self.assertRaisesRegex(ValueError, 'PORT_IN_USE'): V.check_ports()
            self.assertEqual(port, sentinel.getsockname()[1])

    def test_stopped_server_time_wait_does_not_block_normal_restart(self):
        import playtest_runtime as V
        with socket.socket() as listener:
            listener.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            listener.bind(('127.0.0.1', 0)); port = listener.getsockname()[1]; listener.listen()
            with socket.socket() as client:
                client.connect(('127.0.0.1', port)); accepted, _ = listener.accept()
                accepted.close()  # Server initiates FIN; its port enters TIME_WAIT after the client closes.
                self.assertEqual(b'', client.recv(1))
        with patch.object(V.V, 'FRONT', port), patch.object(V.V, 'BACK', port):
            V.check_ports()

    def test_stale_owner_pid_never_causes_signal(self):
        import playtest_runtime as V
        with tempfile.TemporaryDirectory() as d:
            root = Path(d); (root / 'owner.json').write_text(json.dumps({'pid': os.getpid(), 'token': 'old'}))
            with patch.object(V, 'runtime_directory', return_value=root), patch.object(V.os, 'kill') as kill:
                value = V.control('stop'); self.assertFalse(value['liveCoordinator']); kill.assert_not_called()

    def test_owned_signal_refuses_nonchild_and_zombie(self):
        import playtest_runtime as V
        p = type('Process', (), {'pid': 123})()
        with patch.object(V.os, 'waitid', side_effect=ChildProcessError), patch.object(V.os, 'kill') as kill:
            V.owned_signal(p, signal.SIGTERM); kill.assert_not_called()
        with patch.object(V.os, 'waitid', return_value=object()), patch.object(V.os, 'kill') as kill:
            V.owned_signal(p, signal.SIGTERM); kill.assert_not_called()

    def test_readiness_probe_is_only_read_only_404_and_rejects_html_or_false_200(self):
        import playtest_service as V
        for status, raw, expected in [(404, b'{"code":"HTTP_ERROR"}', True), (200, b'{"code":"HTTP_ERROR"}', False),
                                       (404, b'<html>fallback</html>', False), (404, b'{"code":"OTHER"}', False)]:
            response = type('Response', (), {'status': status, 'read': lambda self, n: raw})()
            with patch.object(V.http.client, 'HTTPConnection') as factory:
                connection = factory.return_value; connection.getresponse.return_value = response
                self.assertEqual(expected, V.read_only_ready(17075))
                connection.request.assert_called_once_with('GET', '/api/v1/__playtest_ready__')

    def test_portable_bundle_contains_no_runtime_data_and_can_show_cli_help(self):
        import playtest_bundle as B
        with tempfile.TemporaryDirectory() as d:
            root = Path(d); archive = root / 'tools.zip'; report = B.bundle(ROOT, archive)
            self.assertEqual(R.hash_file(archive), report['sha256'])
            files = R.read_archive(archive)
            self.assertFalse(any(n.endswith(('.jar', '.key', '.pem')) or 'profile' in n for n in files))
            output = root / 'installed'
            for n, b in files.items():
                p = output / n; p.parent.mkdir(parents=True, exist_ok=True); p.write_bytes(b)
            result = subprocess.run([sys.executable, '-B', str(output / 'tools/playtest.py'), '--help'], capture_output=True)
            self.assertEqual(0, result.returncode, result.stderr)
            self.assertIn(b'feedback', result.stdout)



if __name__ == '__main__': unittest.main()
