"""Isolated Git fixtures; no project mutation, downloads, JVM or global git configuration."""
import hashlib
import importlib.util
import json
import os
import sys
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / 'audit-ci-inputs.py'
spec = importlib.util.spec_from_file_location('audit_ci', SCRIPT)
audit_ci = importlib.util.module_from_spec(spec)
spec.loader.exec_module(audit_ci)


class InputAuditTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name) / 'repo'
        self.root.mkdir()
        self.git('init', '-q')
        self.git('config', 'user.name', 'Fixture')
        self.git('config', 'user.email', 'fixture@example.invalid')
        self.git('config', 'core.autocrlf', 'false')
        self.write('src/main/java/A.java', b'class A {}\n')
        self.write('src/test/java/T.java', b'class T {}\n')
        self.write('src/web/java/W.java', b'class W {}\n')
        self.write('src/test/resources/frozen.json', b'{"fixed":true}\n')
        self.write('config/probability.json', b'{"input":true}\n')
        self.write('.gitignore', b'build/\n')
        self.commit()

    def git(self, *args):
        return subprocess.check_output(['git', '-C', str(self.root), *args], stderr=subprocess.PIPE)

    def write(self, path, data):
        p = self.root / path
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_bytes(data)
        return p

    def commit(self):
        self.git('add', '.')
        self.git('commit', '-qm', 'fixture')

    def audit(self, ref='HEAD', oracle=None):
        return audit_ci.audit(self.root, ref, oracle)

    def cli(self, *args):
        return subprocess.run(['python3', '-B', str(SCRIPT), '--repo', str(self.root), *args],
                              capture_output=True, text=True)

    def pointer(self, data=b'RIFF fixture payload'):
        digest = hashlib.sha256(data).hexdigest()
        pointer = ('version https://git-lfs.github.com/spec/v1\noid sha256:' + digest + '\nsize ' + str(len(data)) + '\n').encode()
        return data, digest, pointer

    def test_clean_fingerprints_match_existing_java_algorithm(self):
        result = self.audit()
        expected = hashlib.sha256()
        for path in ['src/main/java/A.java', 'src/test/java/T.java', 'src/web/java/W.java']:
            expected.update(path.encode() + b'\0' + (self.root / path).read_bytes())
        self.assertEqual(expected.hexdigest(), result['fingerprints']['java_ref'])
        self.assertEqual(expected.hexdigest(), result['fingerprints']['java_worktree'])
        self.assertTrue(result['worktree']['git_status_clean'])
        self.assertTrue(result['worktree']['matches_ref_bytes'])
        self.assertEqual(0, self.cli('--require-clean').returncode)

    def test_crlf_is_reported_without_normalization_or_accepting_clean(self):
        p = self.write('src/main/java/A.java', b'class A {}\r\n')
        before, index = p.read_bytes(), (self.root / '.git/index').read_bytes()
        result = self.audit()
        self.assertEqual([{'path': 'src/main/java/A.java', 'kind': 'crlf_only'}], result['worktree']['differences'])
        self.assertNotEqual(result['fingerprints']['java_ref'], result['fingerprints']['java_worktree'])
        self.assertEqual(1, self.cli('--require-clean').returncode)
        self.assertEqual(before, p.read_bytes())
        self.assertEqual(index, (self.root / '.git/index').read_bytes())

    def test_non_eol_and_missing_source_never_claim_semantic_equivalence(self):
        self.write('src/main/java/A.java', b'class A { }\n')
        (self.root / 'src/test/java/T.java').unlink()
        result = self.audit()
        kinds = {x['path']: x['kind'] for x in result['worktree']['differences']}
        self.assertEqual('different_bytes', kinds['src/main/java/A.java'])
        self.assertEqual('missing_or_nonregular', kinds['src/test/java/T.java'])
        self.assertIsNone(result['fingerprints']['java_worktree'])

    def test_untracked_file_fails_clean_even_if_committed_bytes_match(self):
        self.write('private-note.txt', b'do not stage')
        result = self.audit()
        self.assertTrue(result['worktree']['matches_ref_bytes'])
        self.assertFalse(result['worktree']['git_status_clean'])
        self.assertEqual(1, self.cli('--require-clean').returncode)

    def test_pointer_is_not_hydrated_and_remote_is_never_assumed(self):
        data, digest, pointer = self.pointer()
        self.write('src/main/resources/music.wav', pointer)
        self.commit()
        result = self.audit()
        self.assertEqual(len(data), result['lfs']['bytes'])
        entry = result['lfs']['entries'][0]
        self.assertFalse(entry['worktree_hydrated'])
        self.assertFalse(entry['local_object_valid'])
        self.assertFalse(entry['remote_verified'])
        self.assertEqual(1, self.cli('--require-lfs').returncode)
        self.assertEqual(0, self.cli().returncode)  # Inventory alone is not a gate pass.

    def test_hydrated_payload_and_object_need_exact_hash_and_size(self):
        data, digest, pointer = self.pointer()
        self.write('src/main/resources/music.wav', pointer)
        self.commit()
        self.write('src/main/resources/music.wav', data)
        obj = self.write('.git/lfs/objects/' + digest[:2] + '/' + digest[2:4] + '/' + digest, data)
        self.assertTrue(self.audit()['lfs']['entries'][0]['worktree_hydrated'])
        self.assertTrue(self.audit()['lfs']['entries'][0]['local_object_valid'])
        self.assertEqual(0, self.cli('--require-lfs').returncode)
        obj.write_bytes(b'X' * len(data))
        self.write('src/main/resources/music.wav', b'Y' * len(data))
        entry = self.audit()['lfs']['entries'][0]
        self.assertFalse(entry['local_object_valid'])
        self.assertFalse(entry['worktree_hydrated'])
        self.assertEqual(1, self.cli('--require-lfs').returncode)

    def test_symlink_cannot_supply_source_or_lfs_payload(self):
        p = self.root / 'src/main/java/A.java'
        p.unlink()
        p.symlink_to('missing')
        self.assertIsNone(self.audit()['fingerprints']['java_worktree'])

    def test_frozen_and_input_fingerprints_are_independent(self):
        before = self.audit()['fingerprints']
        self.write('src/test/resources/frozen.json', b'{"fixed":false}\n')
        self.commit()
        after = self.audit()['fingerprints']
        self.assertNotEqual(before['frozen'], after['frozen'])
        self.assertEqual(before['inputs'], after['inputs'])
        self.assertEqual(before['java_ref'], after['java_ref'])

    def test_oracle_matches_ref_not_dirty_worktree_and_requires_exact_names(self):
        self.write('src/main/java/A.java', b'class A {}\r\n')
        result = self.audit()
        directory = self.root / 'build/oracle'
        directory.mkdir(parents=True)
        for name in audit_ci.ORACLE_FILES:
            (directory / name).write_text(json.dumps({'sourceFingerprint': result['fingerprints']['java_worktree']}))
        self.assertEqual(1, self.cli('--oracle-dir', str(directory), '--require-oracle').returncode)
        for path in directory.iterdir():
            path.write_text(json.dumps({'sourceFingerprint': result['fingerprints']['java_ref']}))
        self.assertEqual(0, self.cli('--oracle-dir', str(directory), '--require-oracle').returncode)
        first = directory / 'jianyi-mixed.json'
        first.rename(directory / 'wrong-name.json')
        self.assertEqual(1, self.cli('--oracle-dir', str(directory), '--require-oracle').returncode)

    def test_missing_invalid_oracle_and_unknown_ref_fail_explicitly(self):
        self.assertEqual(1, self.cli('--require-oracle').returncode)
        d = self.root / 'build/oracle'
        d.mkdir(parents=True)
        (d / 'broken.json').write_text('not JSON')
        self.assertEqual('invalid', self.audit(oracle=d)['oracle']['entries'][0]['state'])
        self.assertEqual(2, self.cli('--ref', 'does-not-exist').returncode)

    def test_ref_selects_committed_tree_not_index(self):
        first = self.audit()
        self.write('src/main/java/A.java', b'class A { int n; }\n')
        self.git('add', '.')
        self.assertEqual(first['fingerprints']['java_ref'], self.audit()['fingerprints']['java_ref'])
        self.commit()
        self.assertNotEqual(first['fingerprints']['java_ref'], self.audit()['fingerprints']['java_ref'])
        previous = self.audit(ref='HEAD^')
        self.assertEqual('HEAD^', previous['ref'])
        self.assertEqual(first['fingerprints']['java_ref'], previous['fingerprints']['java_ref'])

    def test_missing_git_is_tool_error_not_filesystem_failure(self):
        empty = Path(self.temp.name) / 'empty-path'
        empty.mkdir()
        result = subprocess.run([sys.executable, '-B', str(SCRIPT), '--repo', str(self.root)],
                                env={**os.environ, 'PATH': str(empty)}, capture_output=True, text=True)
        self.assertEqual(2, result.returncode)
        self.assertEqual('', result.stdout)
        problem = json.loads(result.stderr)
        self.assertEqual('TOOL_MISSING', problem['error'])
        self.assertEqual('git', problem['tool'])
        self.assertIn('git', problem['reason'])

    def test_unique_payload_count_is_not_pointer_or_object_directory_count(self):
        data, digest, pointer = self.pointer()
        self.write('src/main/resources/one.wav', pointer)
        self.write('src/main/resources/two.wav', pointer)
        self.commit()
        result = self.audit()['lfs']
        self.assertEqual(2, result['files'])
        self.assertEqual(1, result['unique_payloads'])
        self.assertEqual(len(data), result['unique_payload_bytes'])
        self.assertEqual(2 * len(data), result['bytes'])

    def test_invalid_pointer_fails_without_repair(self):
        p = self.write('src/main/resources/music.wav', b'version https://git-lfs.github.com/spec/v1\ninvalid\n')
        self.commit()
        original = p.read_bytes()
        result = self.cli()
        self.assertEqual(2, result.returncode)
        self.assertIn('Invalid LFS pointer: src/main/resources/music.wav', json.loads(result.stderr)['reason'])
        self.assertEqual(original, p.read_bytes())


if __name__ == '__main__':
    unittest.main()
