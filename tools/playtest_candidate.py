"""Internal preview contract. Full releases retain their independent strict checker."""
from pathlib import Path
import re
import web_release as R

PREVIEW = 'vocr-playtest-preview'
REQUIRED_FRONTEND = {'diff-check', 'audit-tests', 'verify-tests', 'frontend-unit',
                     'web-build', 'assets', 'audio', 'files-repeat', 'default-e2e'}


def check_preview_files(files):
    R.require('release-manifest.json' not in files and 'preview-manifest.json' in files,
              'PREVIEW_KIND_REQUIRED')
    m = R.load_json(files['preview-manifest.json'])
    R.require(set(m) == {'schema', 'kind', 'source', 'runtime', 'verification', 'budgets',
                        'budgetLimits', 'files', 'notCovered'} and type(m['schema']) is int and m['schema'] == 1
              and m['kind'] == PREVIEW, 'INVALID_PREVIEW_SCHEMA')
    R.require(re.fullmatch(r'[0-9a-f]{40}', m['source']['sha']) and
              set(m['source']['fingerprints']) == {'java_ref', 'java_worktree', 'java_files', 'frozen', 'inputs', 'build_policy'},
              'INVALID_PREVIEW_SOURCE')
    fingerprints = m['source']['fingerprints']
    R.require(R.HEX.fullmatch(fingerprints['java_ref']) and fingerprints['java_ref'] == fingerprints['java_worktree']
              and type(fingerprints['java_files']) is int and fingerprints['java_files'] >= 0, 'INVALID_PREVIEW_FINGERPRINT')
    for group in ('frozen', 'inputs', 'build_policy'):
        fingerprint = fingerprints[group]
        R.require(isinstance(fingerprint, dict) and R.HEX.fullmatch(fingerprint.get('sha256', ''))
                  and type(fingerprint.get('files')) is int and fingerprint['files'] >= 0, 'INVALID_PREVIEW_FINGERPRINT')
    v = m['verification']
    R.require(set(v) == {'tier', 'status', 'resultSha256', 'suites', 'javaFull'} and
              v['tier'] == 'frontend' and v['status'] == 'passed' and v['javaFull'] is False
              and R.HEX.fullmatch(v['resultSha256']), 'PREVIEW_VERIFICATION_REQUIRED')
    ids = [s['id'] for s in v['suites']]
    R.require(len(ids) == len(set(ids)) and REQUIRED_FRONTEND <= set(ids)
              and 'java-full' not in ids and all(s == {'id': s['id'], 'status': 'passed'}
              for s in v['suites']), 'PREVIEW_SUITE_MISMATCH')
    entries = m['files']
    R.require(isinstance(entries, dict) and set(entries) | {'preview-manifest.json', 'SHA256SUMS'}
              == set(files), 'PREVIEW_FILE_SET_MISMATCH')
    for n, row in entries.items():
        R.require(row == {'bytes': len(files[n]), 'sha256': R.sha(files[n])}, 'PREVIEW_HASH_MISMATCH')
    sums = ''.join(R.sha(b) + '  ' + n + '\n' for n, b in sorted(files.items()) if n != 'SHA256SUMS')
    R.require(files.get('SHA256SUMS') == sums.encode(), 'CHECKSUM_LIST_MISMATCH')
    required = {'backend/server.jar', 'backend/run.sh', 'backend/run.cmd', 'config/probability.json',
                'metadata/runtime-build.json', 'LICENSE', 'PREVIEW-README.txt'}
    R.require(required <= set(files) and any(n.startswith('backend/lib/') for n in files)
              and all(n in required or n in {'preview-manifest.json', 'SHA256SUMS'}
                      or n.startswith('frontend/') or re.fullmatch(r'backend/lib/[A-Za-z0-9_.-]+\.jar', n)
                      for n in files), 'PREVIEW_CONTENT_BOUNDARY')
    static = {n[9:]: b for n, b in files.items() if n.startswith('frontend/')}
    R.require(m['budgetLimits'] == R.BUDGET and m['budgets'] == R.budgets(static), 'PREVIEW_BUDGET_MISMATCH')
    R.require(sum(len(b) for n, b in files.items() if n.startswith('backend/')) <= 64 * R.MIB,
              'BACKEND_SAFETY_LIMIT')
    R.backend_contract(files['backend/server.jar'])
    stamp = R.load_json(files['metadata/runtime-build.json'])
    jars = {n[8:]: R.sha(b) for n, b in files.items() if n == 'backend/server.jar' or n.startswith('backend/lib/')}
    R.require(stamp['schema'] == 1 and stamp['sourceSha'] == m['source']['sha'] and stamp['files'] == jars
              and stamp['sourceFiles']['config/probability.json'] == R.sha(files['config/probability.json'])
              and m['runtime'] == {'javaMinimum': 21, 'stampSha256': R.sha(files['metadata/runtime-build.json'])},
              'PREVIEW_RUNTIME_MISMATCH')
    sh, cmd = R.launchers(R.sha(files['config/probability.json']))
    R.require(files['backend/run.sh'] == sh and files['backend/run.cmd'] == cmd, 'LAUNCHER_MISMATCH')
    R.require(isinstance(m['notCovered'], list) and 'java-full' in m['notCovered'], 'PREVIEW_SCOPE_MISSING')
    return m


def check_preview(archive):
    return check_preview_files(R.read_archive(archive))


def check_candidate(archive):
    files = R.read_archive(archive)
    if 'release-manifest.json' in files:
        R.require('preview-manifest.json' not in files, 'AMBIGUOUS_CANDIDATE_KIND')
        return 'full', R.check_release(archive), files
    return 'preview', check_preview_files(files), files


def check_extracted(payload):
    """Check the complete immutable imported tree before starting either service."""
    files = {n: p.read_bytes() for n, p in R.scan(payload).items()}
    name = 'release-manifest.json' if 'release-manifest.json' in files else 'preview-manifest.json'
    if name == 'preview-manifest.json':
        return check_preview_files(files)
    m = R.load_json(files[name])
    R.require(m['kind'] == 'vocr-web-runtime' and 'preview-manifest.json' not in files,
              'CANDIDATE_CHANGED')
    R.require(set(m['files']) | {'release-manifest.json', 'SHA256SUMS'} == set(files), 'CANDIDATE_CHANGED')
    for n, row in m['files'].items():
        R.require(row == {'bytes': len(files[n]), 'sha256': R.sha(files[n])}, 'CANDIDATE_CHANGED')
    sums = ''.join(R.sha(b) + '  ' + n + '\n' for n, b in sorted(files.items()) if n != 'SHA256SUMS')
    R.require(files['SHA256SUMS'] == sums.encode(), 'CANDIDATE_CHANGED')
    return m


def preview_verification(root, result_file):
    root = Path(root).absolute()
    result_file = R.no_links(result_file)
    result = R.load_json(result_file.read_bytes())
    now = R.current_audit(root)
    R.require(result_file == root / result['run_dir'] / 'result.json'
              and re.fullmatch(r'build/verify/[A-Za-z0-9_.-]+', result['run_dir']), 'INVALID_PREVIEW_RESULT')
    R.require(now['worktree']['git_status_clean'] and now['worktree']['matches_ref_bytes']
              and result['status'] == 'passed' and result['dry_run'] is False
              and not result.get('first_failure') and result['tested_sha'] == now['sha']
              and R.audit_summary(result['input_audit']) == R.audit_summary(now)
              and R.audit_summary(result['final_audit']) == R.audit_summary(now), 'PREVIEW_INPUT_DRIFT')
    ids = [s['id'] for s in result['suites']]
    R.require(result['plan']['tier'] == 'frontend' and ids == [s['id'] for s in result['plan']['suites']]
              and REQUIRED_FRONTEND <= set(ids) and 'java-full' not in ids
              and all(s['status'] == 'passed' for s in result['suites']), 'PREVIEW_VERIFICATION_REQUIRED')
    for s in result['suites']:
        if s.get('log'):
            R.require(s['log'] == s['id'] + '.log' and R.hash_file(R.no_links(root / result['run_dir'] / s['log']))
                      == s['log_sha256'], 'PREVIEW_LOG_DRIFT')
    return result, now


def preview_inputs(root, result_file):
    root = Path(root).absolute()
    result, now = preview_verification(root, result_file)
    runtime = R.scan(root / 'build/web-runtime')
    stamp = R.load_json(runtime['runtime-build.json'].read_bytes())
    R.require(stamp['schema'] == 1 and stamp['sourceSha'] == now['sha']
              and stamp['sourceFiles'] == {n: R.hash_file(R.no_links(root / n)) for n in R.runtime_sources(root)}
              and stamp['files'] == {n: R.hash_file(p) for n, p in runtime.items() if n != 'runtime-build.json'},
              'PREVIEW_RUNTIME_INPUT_DRIFT')
    R.backend_contract(runtime['server.jar'].read_bytes())
    static = {n: p.read_bytes() for n, p in R.scan(root / 'web-client/dist').items()}
    built = next(s for s in result['suites'] if s['id'] == 'web-build')
    R.require(built['frontend_files'] == {n: {'bytes': len(b), 'sha256': R.sha(b)} for n, b in static.items()},
              'FRONTEND_OUTPUT_DRIFT')
    return result, now, runtime, static


def preview_payload(root, result_file):
    result, now, runtime, static = preview_inputs(root, result_file)
    R.resource_gates(root)
    files = {'frontend/' + n: b for n, b in static.items()}
    files.update({('metadata/runtime-build.json' if n == 'runtime-build.json' else 'backend/' + n): p.read_bytes()
                  for n, p in runtime.items()})
    files['config/probability.json'] = (Path(root) / 'config/probability.json').read_bytes()
    files['LICENSE'] = (Path(root) / 'LICENSE').read_bytes()
    files['backend/run.sh'], files['backend/run.cmd'] = R.launchers(R.sha(files['config/probability.json']))
    files['PREVIEW-README.txt'] = b'Internal complete playable preview. Not a full release. Java full tests were not run.\n'
    manifest = {'schema': 1, 'kind': PREVIEW, 'source': {'sha': now['sha'], 'fingerprints': now['fingerprints']},
                'runtime': {'javaMinimum': 21, 'stampSha256': R.sha(files['metadata/runtime-build.json'])},
                'verification': {'tier': 'frontend', 'status': 'passed', 'resultSha256': R.hash_file(result_file),
                                 'suites': [{'id': s['id'], 'status': s['status']} for s in result['suites']], 'javaFull': False},
                'budgets': R.budgets(static), 'budgetLimits': R.BUDGET,
                'files': {n: {'bytes': len(b), 'sha256': R.sha(b)} for n, b in sorted(files.items())},
                'notCovered': ['java-full', 'HTTPS/SAN/public deployment', 'capacity', 'manual A06/A07/A08/046/049']}
    files['preview-manifest.json'] = R.json_bytes(manifest)
    files['SHA256SUMS'] = ''.join(R.sha(b) + '  ' + n + '\n' for n, b in sorted(files.items())).encode()
    check_preview_files(files)
    return files
