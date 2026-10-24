#!/usr/bin/env python3
"""Read-only Git/tree/LFS/oracle inventory. Never normalizes, stages or fetches files."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys

JAVA_ROOTS = ('src/main/java/', 'src/test/java/', 'src/web/java/')
ORACLE_FILES = {c + '-' + s + '.json' for c in
                ('jianyi', 'tongchang', 'yaoohu', 'kuangxin', 'beide', 'maoyou', 'daxing')
                for s in ('designated', 'mixed')} | {'tongchang-interruption.json'}
LFS = re.compile(rb'version https://git-lfs.github.com/spec/v1\noid sha256:([0-9a-f]{64})\nsize ([0-9]+)\n?\Z')


def git(root, *args):
    return subprocess.check_output(['git', '--no-optional-locks', '-C', str(root), *args], stderr=subprocess.PIPE)


def file_hash(path):
    digest = hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(chunk)
    return digest.hexdigest()


def groups(path):
    result = []
    if path.startswith('src/test/resources/') or path in (
            'web-client/public/assets/images/manifest.json', 'web-client/public/assets/audio/manifest.json'):
        result.append('frozen')
    if path.startswith('src/main/resources/') or path == 'config/probability.json':
        result.append('inputs')
    if path in ('.gitattributes', '.gitignore', 'build.gradle', 'settings.gradle', 'gradle.properties',
                'web-client/package.json', 'web-client/package-lock.json') or path.startswith(('gradle/', 'tools/', '.github/')) or (path.startswith('web-client/playwright') and path.endswith('.js')):
        result.append('build_policy')
    return result


def aggregate(items):
    digest = hashlib.sha256()
    for path, content_hash in sorted(items):
        digest.update(path.encode('utf-8') + b'\0' + content_hash.encode('ascii') + b'\n')
    return digest.hexdigest()


def audit(root, ref, oracle_dir):
    root = Path(git(root, 'rev-parse', '--show-toplevel').decode().strip())
    sha = git(root, 'rev-parse', '--verify', ref + '^{commit}').decode().strip()
    status = git(root, 'status', '--porcelain=v1', '-z', '--untracked-files=normal')
    common = Path(git(root, 'rev-parse', '--git-common-dir').decode().strip())
    if not common.is_absolute():
        common = root / common
    tree = []
    for item in git(root, 'ls-tree', '-r', '-z', sha).split(b'\0'):
        if item:
            meta, path = item.split(b'\t', 1)
            mode, kind, oid = meta.decode().split()
            if kind != 'blob':
                raise ValueError('Unsupported non-blob tree entry')
            tree.append((path.decode('utf-8'), mode, oid))
    records, differences, lfs = {}, [], []
    fingerprints = {name: [] for name in ('frozen', 'inputs', 'build_policy')}
    batch = subprocess.Popen(['git', '-C', str(root), 'cat-file', '--batch'],
                             stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL)
    try:
        for path, mode, oid in tree:
            batch.stdin.write(oid.encode() + b'\n')
            batch.stdin.flush()
            header = batch.stdout.readline().split()
            if len(header) != 3 or header[1] != b'blob':
                raise ValueError('Git object unavailable')
            size = int(header[2])
            content_hash, retained, remaining = hashlib.sha256(), bytearray(), size
            keep = size <= 1024 or (path.endswith('.java') and path.startswith(JAVA_ROOTS))
            if keep and size > 8 * 1024 * 1024:
                raise ValueError('Java source exceeds audit bound')
            while remaining:
                chunk = batch.stdout.read(min(remaining, 1024 * 1024))
                if not chunk:
                    raise ValueError('Truncated Git object')
                content_hash.update(chunk)
                if keep:
                    retained.extend(chunk)
                remaining -= len(chunk)
            if batch.stdout.read(1) != b'\n':
                raise ValueError('Invalid Git object separator')
            data = bytes(retained) if keep else None
            digest = content_hash.hexdigest()
            for name in groups(path):
                fingerprints[name].append((path, digest))
            work = root / path
            regular = mode != '120000' and work.is_file() and not work.is_symlink()
            working_hash = file_hash(work) if regular else None
            match = LFS.fullmatch(data) if data is not None else None
            if data is not None and data.startswith(b'version https://git-lfs.github.com/spec/') and not match:
                raise ValueError('Invalid LFS pointer: ' + path)
            if match:
                payload, length = match[1].decode(), int(match[2])
                obj = common / 'lfs/objects' / payload[:2] / payload[2:4] / payload
                local_valid = obj.is_file() and not obj.is_symlink() and obj.stat().st_size == length and file_hash(obj) == payload
                hydrated = regular and work.stat().st_size == length and working_hash == payload
                lfs.append({'path': path, 'sha256': payload, 'bytes': length,
                            'worktree_hydrated': hydrated, 'local_object_valid': local_valid,
                            'remote_verified': False})
                if not hydrated and working_hash != digest:
                    differences.append({'path': path, 'kind': 'missing_or_corrupt_lfs_payload'})
            elif working_hash != digest:
                kind = 'missing_or_nonregular' if not regular else 'different_bytes'
                # This is byte classification, never a semantic-equivalence claim.
                if regular and size <= 8 * 1024 * 1024 and work.stat().st_size <= 8 * 1024 * 1024:
                    if data is None:
                        data = git(root, 'cat-file', 'blob', oid)
                    if work.read_bytes().replace(b'\r\n', b'\n') == data.replace(b'\r\n', b'\n'):
                        kind = 'crlf_only'
                differences.append({'path': path, 'kind': kind})
            if path.endswith('.java') and path.startswith(JAVA_ROOTS):
                records[path] = data
    finally:
        batch.stdin.close()
        batch.stdout.close()
        if batch.wait() != 0:
            raise ValueError('Git object reader failed')
    committed, working, java_complete = hashlib.sha256(), hashlib.sha256(), True
    for prefix in JAVA_ROOTS:
        for path in sorted(p for p in records if p.startswith(prefix)):
            committed.update(path.encode() + b'\0' + records[path])
            work = root / path
            if not work.is_file() or work.is_symlink():
                java_complete = False
                continue
            working.update(path.encode() + b'\0' + work.read_bytes())
    expected = committed.hexdigest()
    actual = working.hexdigest() if java_complete else None
    oracles = []
    if oracle_dir is not None:
        for file in sorted(Path(oracle_dir).glob('*.json')):
            try:
                value = json.loads(file.read_text(encoding='utf-8')).get('sourceFingerprint')
                state = 'matches_ref' if value == expected else 'stale_for_ref'
            except (OSError, ValueError, AttributeError):
                value, state = None, 'invalid'
            oracles.append({'file': file.name, 'fingerprint': value, 'state': state})
    return {'schema': 1, 'sha': sha, 'tracked_files': len(tree),
            'worktree': {'git_status_clean': not status, 'matches_ref_bytes': not differences,
                         'differences': differences},
            'fingerprints': {'java_ref': expected, 'java_worktree': actual, 'java_files': len(records),
                             **{name: {'files': len(items), 'sha256': aggregate(items)} for name, items in fingerprints.items()}},
            'lfs': {'files': len(lfs), 'bytes': sum(x['bytes'] for x in lfs), 'entries': lfs},
            'oracle': {'files': len(oracles), 'entries': oracles},
            'not_covered': ['remote LFS availability', 'tests/build/runtime behavior',
                            'desktop/DPI/audio/manual release acceptance']}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repo', type=Path, default=Path('.'))
    parser.add_argument('--ref', default='HEAD')
    parser.add_argument('--oracle-dir', type=Path)
    parser.add_argument('--require-clean', action='store_true')
    parser.add_argument('--require-lfs', action='store_true')
    parser.add_argument('--require-oracle', action='store_true')
    args = parser.parse_args()
    try:
        result = audit(args.repo, args.ref, args.oracle_dir)
        failures = []
        if args.require_clean and not (result['worktree']['git_status_clean'] and result['worktree']['matches_ref_bytes']):
            failures.append('DIRTY_OR_DIFFERENT_WORKTREE')
        if args.require_lfs and any(not e['worktree_hydrated'] for e in result['lfs']['entries']):
            failures.append('LFS_PAYLOAD_NOT_READY')
        if args.require_oracle and ({e['file'] for e in result['oracle']['entries']} != ORACLE_FILES or any(e['state'] != 'matches_ref' for e in result['oracle']['entries'])):
            failures.append('ORACLE_STALE_OR_MISSING: regenerate with final source; never weaken fingerprints or refresh golden expectations')
        result['failures'] = failures
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return 1 if failures else 0
    except (OSError, ValueError, subprocess.SubprocessError) as error:
        reason = str(error) if isinstance(error, ValueError) else ('GIT_READ_FAILED' if isinstance(error, subprocess.SubprocessError) else 'FILESYSTEM_READ_FAILED')
        print(json.dumps({'error': 'INPUT_AUDIT_FAILED', 'kind': type(error).__name__, 'reason': reason}), file=sys.stderr)
        return 2


if __name__ == '__main__':
    sys.exit(main())
