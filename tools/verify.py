#!/usr/bin/env python3
"""Serial verification planner/runner. No installation, fetch, push or golden updates."""
import argparse
import datetime
import fnmatch
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys
import time
import uuid
import xml.etree.ElementTree as ET

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('verify_input_audit', HERE / 'audit-ci-inputs.py')
AUDIT = importlib.util.module_from_spec(spec)
spec.loader.exec_module(AUDIT)
ORDER = ('docs', 'tooling', 'frontend', 'full')
# Commands are argument arrays, never shell snippets derived from changed paths.
COMMANDS = {
    'diff-check': ['git', 'diff', '--check'],
    'guarded-tests': ['python3', '-B', 'tools/tests/test_guarded_process.py', '-v'],
    'audit-tests': ['python3', '-B', 'tools/tests/test_ci_inputs.py', '-v'],
    'verify-tests': ['python3', '-B', 'tools/tests/test_verify.py', '-v'],
    'ci-tests': ['python3', '-B', 'tools/tests/test_ci_workflow.py', '-v'],
    'release-tests': ['python3', '-B', 'tools/tests/test_web_release.py', '-v'],
    'image-tool-tests': ['python3', '-B', 'tools/tests/test_web_images.py', '-v'],
    'audio-tool-tests': ['python3', '-B', 'tools/tests/test_web_audio.py', '-v'],
    'exchange-contract': ['node', '--test', 'tools/tests/replay-exchange.test.mjs'],
    'frontend-unit': ['npm', '--prefix', 'web-client', 'run', 'test:unit'],
    'web-build': ['npm', '--prefix', 'web-client', 'run', 'build'],
    'assets': ['npm', '--prefix', 'web-client', 'run', 'test:assets'],
    'audio': ['npm', '--prefix', 'web-client', 'run', 'test:audio'],
    'files-repeat': ['npm', '--prefix', 'web-client', 'run', 'test:e2e', '--', 'tests/replay-files.spec.js', '--repeat-each=3'],
    'production-smoke': ['npm', '--prefix', 'web-client', 'run', 'test:e2e', '--', '--config=playwright.production.config.js'],
    'default-e2e': ['npm', '--prefix', 'web-client', 'run', 'test:e2e'],
    'oracle-generate': ['bash', 'tools/run-gradle-safe.sh', 'test', '--tests', 'com.vocr.web.Web017JourneyTest', '--offline', '--rerun-tasks'],
    'java-full': ['bash', 'tools/run-gradle-safe.sh', 'fatJar', 'prepareWebRuntime', '--offline', '--rerun-tasks'],
    'gate': ['npm', '--prefix', 'web-client', 'run', 'test:gate'],
    'desktop-exchange': ['npm', '--prefix', 'web-client', 'run', 'test:e2e', '--', '--config=../tools/playwright.exchange.config.mjs'],
}
SEQUENCE = ['diff-check', 'guarded-tests', 'audit-tests', 'verify-tests', 'ci-tests', 'release-tests', 'image-tool-tests',
            'audio-tool-tests', 'exchange-contract', 'frontend-unit', 'web-build', 'assets',
            'audio', 'files-repeat', 'default-e2e', 'production-smoke', 'browser-file', 'java-full', 'java-results',
            'oracle-generate', 'oracle-audit', 'gate', 'desktop-exchange']
INTERNAL = {'browser-file', 'java-results', 'oracle-audit'}
MANUAL = ['A06', 'A07', 'A08', '046 browser/DPI/accessibility/listening/devices/loop seams',
          '049 pre-release manual review', '047 capacity/long game']


class Refused(ValueError):
    def __init__(self, code):
        super().__init__(code)
        self.code = code


def policy():
    value = json.loads((HERE / 'verify-tiers.json').read_text(encoding='utf-8'))
    if (not isinstance(value, dict) or value.get('schema') != 1 or
            not isinstance(value.get('tiers'), dict) or set(value['tiers']) != set(ORDER) or
            value.get('unknown') != 'full' or not isinstance(value.get('rules'), list)):
        raise Refused('INVALID_TIER_POLICY')
    for suites in value['tiers'].values():
        if not isinstance(suites, list) or not all(isinstance(s, str) for s in suites) or not set(suites) <= set(SEQUENCE):
            raise Refused('UNKNOWN_SUITE_IN_POLICY')
    required = {'docs': {'diff-check'}, 'tooling': {'diff-check', 'audit-tests', 'verify-tests'},
                'frontend': {'frontend-unit', 'web-build', 'default-e2e'},
                'full': {'production-smoke', 'ci-tests', 'browser-file', 'java-full', 'java-results', 'oracle-audit', 'gate', 'desktop-exchange'}}
    if any(not suites <= set(value['tiers'][tier]) for tier, suites in required.items()):
        raise Refused('REQUIRED_SUITE_REMOVED_FROM_POLICY')
    for rule in value['rules']:
        if (not isinstance(rule, dict) or rule.get('tier') not in ORDER or
                not isinstance(rule.get('patterns'), list) or not rule['patterns'] or
                not all(isinstance(p, str) and p for p in rule['patterns']) or
                not isinstance(rule.get('extra', []), list) or
                not all(isinstance(s, str) and s in SEQUENCE for s in rule.get('extra', []))):
            raise Refused('INVALID_TIER_RULE')
    return value


def valid_path(path):
    if not path or path.startswith('/') or '\\' in path or any(ord(c) < 32 for c in path) or any(p in ('', '.', '..') for p in path.split('/')):
        raise Refused('INVALID_CHANGED_PATH')
    return path


def plan(paths, full=False):
    if not paths and not full:
        raise Refused('NO_CHANGES_TO_VERIFY')
    config = policy()
    matches, unknown, rank, extras = [], [], 0, set()
    for path in sorted(set(map(valid_path, paths))):
        rules = [r for r in config['rules'] if any(fnmatch.fnmatchcase(path, pattern) for pattern in r['patterns'])]
        tier = max((r['tier'] for r in rules), key=ORDER.index) if rules else config['unknown']
        rank = max(rank, ORDER.index(tier))
        for r in rules:
            extras.update(r.get('extra', []))
        if not rules:
            unknown.append(path)
        matches.append({'path': path, 'tier': tier, 'reason': 'matched_policy' if rules else 'unknown_path_conservative_full'})
    tier = 'full' if full else ORDER[rank]
    selected = set(config['tiers'][tier]) | extras
    if 'java-full' in selected:
        selected.discard('oracle-generate')  # Full test already regenerates these artifacts.
    return {'tier': tier, 'paths': matches, 'unknown_paths': unknown,
            'requires_full_java_approval': 'java-full' in selected,
            'requires_lfs': bool(selected - {'diff-check', 'audit-tests', 'verify-tests', 'ci-tests', 'release-tests', 'guarded-tests', 'exchange-contract', 'frontend-unit'}),
            'sensitive_paths_requiring_review': [p for p in sorted(set(paths)) if any(g in ('frozen', 'inputs') for g in AUDIT.groups(p))],
            'suites': [{'id': name, 'argv': COMMANDS.get(name), 'internal': name in INTERNAL} for name in SEQUENCE if name in selected],
            'skipped': [{'id': name, 'reason': 'not_triggered_by_changed_paths'} for name in SEQUENCE if name not in selected]}


def inventory(root, ref):
    return AUDIT.audit(root, ref, root / 'build/web017')


def require_inputs(report, lfs=False, oracle=False):
    w, f = report['worktree'], report['fingerprints']
    if not (w['git_status_clean'] and w['matches_ref_bytes'] and f['java_ref'] == f['java_worktree']):
        raise Refused('DIRTY_OR_DIFFERENT_WORKTREE')
    if lfs and any(not e['worktree_hydrated'] for e in report['lfs']['entries']):
        raise Refused('LFS_PAYLOAD_NOT_READY')
    if oracle and ({e['file'] for e in report['oracle']['entries']} != AUDIT.ORACLE_FILES or
                   any(e['state'] != 'matches_ref' for e in report['oracle']['entries'])):
        raise Refused('ORACLE_STALE_OR_MISSING')


def eol_policy(root):
    values = {}
    for key in ('core.autocrlf', 'core.eol'):
        result = subprocess.run(['git', '--no-optional-locks', '-C', str(root), 'config', '--get', key], capture_output=True, text=True)
        if result.returncode not in (0, 1):
            raise Refused('GIT_CONFIG_READ_FAILED')
        values[key] = result.stdout.strip() if result.returncode == 0 else None
    if values['core.autocrlf'] not in (None, 'false') or values['core.eol'] not in (None, 'lf'):
        raise Refused('LF_CHECKOUT_POLICY_REQUIRED')
    # Raw equality alone could accept a committed CRLF Java tree: refuse that case too.
    for path in AUDIT.git(root, 'ls-files', '-z', '--', '*.java').decode().split('\0'):
        if path and path.startswith(AUDIT.JAVA_ROOTS) and b'\r\n' in (root / path).read_bytes():
            raise Refused('LF_JAVA_SOURCE_REQUIRED')
    return {'autocrlf': False, 'eol': 'lf', 'tree': 'clean-clone', 'git_values': values,
            'enforcement': 'no conversion; require raw ref/worktree equality and LF Java'}


def changes(root, base, head):
    if not base or base.startswith('-'):
        raise Refused('INVALID_BASE_REF')
    try:
        base_sha = AUDIT.git(root, 'rev-parse', '--verify', base + '^{commit}').decode().strip()
    except subprocess.CalledProcessError as error:
        raise Refused('INVALID_BASE_REF') from error
    paths = AUDIT.git(root, 'diff', '--name-only', '--no-renames', '-z', base_sha, head, '--').decode().split('\0')
    return base_sha, [p for p in paths if p]


def contained(path, root):
    try:
        path.resolve().relative_to(root.resolve())
        return True
    except ValueError:
        return False


def require_file(root, relative, after_ns):
    path = root / relative
    if path.is_symlink() or not path.is_file() or not contained(path, root):
        raise Refused('CURRENT_RUN_FILE_MISSING:' + relative)
    if path.stat().st_mtime_ns < after_ns:
        raise Refused('CURRENT_RUN_FILE_STALE:' + relative)
    value = json.loads(path.read_text(encoding='utf-8'))
    if not isinstance(value, list) or len(value) != 9 or not any(value):
        raise Refused('INVALID_EXCHANGE_FILE:' + relative)
    return {'path': relative, 'sha256': AUDIT.file_hash(path), 'bytes': path.stat().st_size}


def java_results(root, after_ns):
    files = sorted((root / 'build/test-results/test').glob('TEST-*.xml'))
    if not files or any(p.is_symlink() or p.stat().st_mtime_ns < after_ns for p in files):
        raise Refused('JAVA_RESULTS_MISSING_OR_STALE')
    total = {k: 0 for k in ('tests', 'failures', 'errors', 'skipped')}
    for path in files:
        suite = ET.parse(path).getroot()
        for key in total:
            total[key] += int(suite.get(key, '0'))
    if total['tests'] <= 0 or any(total[k] for k in ('failures', 'errors', 'skipped')):
        raise Refused('JAVA_RESULTS_NOT_ALL_EXECUTED_AND_PASSED')
    return {**total, 'xml_files': len(files), 'source': 'build/test-results/test/TEST-*.xml'}


def execute(argv, root, env, log):
    return subprocess.run(argv, cwd=root, env=env, stdout=log, stderr=subprocess.STDOUT).returncode


def output_inventory(directory):
    """Bind ignored frontend outputs to this successful build, not merely the current source tree."""
    if not directory.is_dir() or directory.is_symlink():
        raise Refused('BUILD_OUTPUT_MISSING')
    rows = {}
    seen = set()
    for file in sorted(directory.rglob('*')):
        if file.is_symlink():
            raise Refused('BUILD_OUTPUT_SYMLINK')
        if file.is_dir():
            continue
        path = valid_path(file.relative_to(directory).as_posix())
        if not file.is_file() or path.casefold() in seen:
            raise Refused('BUILD_OUTPUT_INVALID_OR_DUPLICATE')
        seen.add(path.casefold())
        rows[path] = {'bytes': file.stat().st_size, 'sha256': AUDIT.file_hash(file)}
    if not rows:
        raise Refused('BUILD_OUTPUT_EMPTY')
    return rows


def run_suites(root, result, directory, runner=execute, env=None):
    env = dict(os.environ if env is None else env)
    def progress(event, **values):
        if env.get('VOCR_VERIFY_PROGRESS') == '1':
            print(json.dumps({'event': event, **values}), file=sys.stderr, flush=True)
    start = time.time_ns()
    result['suites'] = []
    result['status'] = 'running'
    def save():
        (directory / 'result.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    save()
    for suite in result['plan']['suites']:
        name = suite['id']
        entry = {'id': name, 'status': 'running'}
        result['suites'].append(entry)
        save()
        progress('suite_started', suite=name)
        tick = time.monotonic()
        try:
            if name == 'oracle-audit':
                report = inventory(root, result['tested_sha'])
                require_inputs(report, lfs=True, oracle=True)
                result['oracle_audit'] = report
            elif name == 'browser-file':
                result['browser_to_desktop'] = require_file(root, 'build/web035/browser-export.json', start)
            elif name == 'java-results':
                entry['xml_summary'] = java_results(root, start)
                result['desktop_to_browser'] = require_file(root, 'build/web035/desktop-actual.json', start)
            else:
                argv = list(suite['argv'])
                if name == 'diff-check':
                    argv = ['git', 'diff', '--check', result['base_sha'], result['tested_sha'], '--']
                if name == 'java-full':
                    env['VOCR_WEB035_BROWSER_EXPORT'] = str(root / 'build/web035/browser-export.json')
                if name == 'desktop-exchange':
                    env['VOCR_VERIFY_DESKTOP_EXPORT'] = str(root / 'build/web035/desktop-actual.json')
                log = directory / (name + '.log')
                entry['argv'], entry['log'] = argv, log.name
                with log.open('wb') as stream:
                    code = runner(argv, root, env.copy(), stream)
                entry['exit_code'] = code
                entry['log_sha256'] = AUDIT.file_hash(log)
                entry['tail'] = log.read_bytes()[-3000:].decode('utf-8', errors='replace')
                if code:
                    raise Refused('SUITE_FAILED:' + name)
                if name == 'web-build':
                    entry['frontend_files'] = output_inventory(root / 'web-client/dist')
                if name == 'java-full':
                    stamp = root / 'build/web-runtime/runtime-build.json'
                    if not stamp.is_file() or stamp.is_symlink():
                        raise Refused('WEB_RUNTIME_STAMP_MISSING')
                    entry['runtime_build_sha256'] = AUDIT.file_hash(stamp)
            entry['status'] = 'passed'
        except (Refused, OSError, ValueError, ET.ParseError) as error:
            entry['status'], entry['error'] = 'failed', str(error)
            result['status'], result['first_failure'] = 'failed', {'suite': name, 'reason': str(error)}
            entry['seconds'] = round(time.monotonic() - tick, 3)
            save()
            progress('suite_finished', suite=name, status=entry['status'], seconds=entry['seconds'])
            return 1
        entry['seconds'] = round(time.monotonic() - tick, 3)
        save()
        progress('suite_finished', suite=name, status=entry['status'], seconds=entry['seconds'])
    # Catch writes/input changes and HEAD drift even if all child exit codes were zero.
    try:
        final = inventory(root, 'HEAD')
        require_inputs(final, lfs=result['plan']['requires_lfs'])
        if final['sha'] != result['tested_sha'] or final['fingerprints'] != result['input_audit']['fingerprints']:
            raise Refused('HEAD_OR_INPUT_CHANGED_DURING_RUN')
        result['final_audit'] = final
        result['status'] = 'passed'
    except (Refused, OSError, ValueError, subprocess.SubprocessError) as error:
        result['status'], result['first_failure'] = 'failed', {'suite': 'final-input-audit', 'reason': str(error)}
    save()
    progress('verification_finished', status=result['status'])
    return 0 if result['status'] == 'passed' else 1


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base', default='HEAD^', help='committed diff base; renames inspect both old/new paths')
    parser.add_argument('--paths', action='append', default=[], help='add conservative triggers; never excludes actual Git diff paths')
    parser.add_argument('--full', action='store_true', help='upgrade to full; does not grant Java/package authorization')
    parser.add_argument('--dry-run', action='store_true')
    parser.add_argument('--json', action='store_true', help='JSON on stdout; child output always goes to run logs')
    args = parser.parse_args(argv)
    result, report = None, None
    try:
        root = Path(AUDIT.git(HERE.parent, 'rev-parse', '--show-toplevel').decode().strip()).resolve()
        # First gate; no suite and no run directory exists before this succeeds.
        report = inventory(root, 'HEAD')
        require_inputs(report)
        if sys.platform != 'linux':
            raise Refused('LINUX_SAFE_ENTRY_REQUIRED')
        eol = eol_policy(root)
        base_sha, paths = changes(root, args.base, report['sha'])
        if not paths and not args.full:
            raise Refused('NO_CHANGES_TO_VERIFY')
        selected = plan(paths + args.paths, args.full)
        require_inputs(report, lfs=selected['requires_lfs'])
        result = {'schema': 1, 'base_ref': args.base, 'base_sha': base_sha, 'tested_sha': report['sha'],
                  'input_audit': report, 'plan': selected, 'eol_policy': eol,
                  'tier_policy_sha256': AUDIT.file_hash(HERE / 'verify-tiers.json'),
                  'manual_gates': {m: 'not_covered' for m in MANUAL},
                  'not_covered': ['remote LFS availability', 'fresh OS installation', 'release approval'],
                  'status': 'planned' if args.dry_run else 'preflight', 'dry_run': args.dry_run}
        if args.dry_run:
            code = 0
        else:
            if selected['requires_full_java_approval'] and os.environ.get('VOCR_APPROVED_FULL_CHECK') != '1':
                raise Refused('FULL_JAVA_PACKAGE_APPROVAL_REQUIRED')
            if any(s['id'] in ('gate', 'default-e2e', 'files-repeat', 'production-smoke') for s in selected['suites']) and 'VOCR_WEB_SERVER_COMMAND' in os.environ:
                raise Refused('SERVER_OVERRIDE_NOT_ACCEPTED')
            if selected['requires_full_java_approval'] and not os.environ.get('DISPLAY'):
                raise Refused('DISPLAY_REQUIRED_FOR_FULL_JAVA')
            directory = root / 'build/verify' / (datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ') + '-' + uuid.uuid4().hex[:8])
            # Output must be ignored and contained; no alternate path that could overwrite source/user files.
            parent = directory.parent
            if not contained(parent, root) or any(p.is_symlink() for p in (root / 'build', parent)):
                raise Refused('UNSAFE_RUN_DIRECTORY')
            ignored = subprocess.run(['git', '-C', str(root), 'check-ignore', '-q', '--', str(directory / 'result.json')])
            if ignored.returncode != 0:
                raise Refused('RUN_DIRECTORY_MUST_BE_IGNORED')
            directory.mkdir(parents=True, exist_ok=False)
            result['run_dir'] = directory.relative_to(root).as_posix()
            code = run_suites(root, result, directory)
        if args.json:
            print(json.dumps(result, ensure_ascii=False, indent=2))
        else:
            print('{}: {} ({})'.format(result['status'], selected['tier'], ', '.join(s['id'] for s in selected['suites'])))
            if 'run_dir' in result:
                print('Evidence: ' + result['run_dir'] + '/result.json')
            if 'first_failure' in result:
                print(json.dumps(result['first_failure'], ensure_ascii=False), file=sys.stderr)
        return code
    except AUDIT.ToolMissingError as error:
        print(json.dumps({'status': 'refused', 'error': 'TOOL_MISSING', 'tool': 'git', 'reason': str(error)}), file=sys.stderr)
        return 2
    except (Refused, OSError, ValueError, KeyError, subprocess.SubprocessError) as error:
        problem = {'status': 'refused', 'error': str(error) if isinstance(error, (ValueError, KeyError)) else type(error).__name__}
        if report:
            problem['input_audit'] = report
        if result:
            problem['plan'] = result['plan']
        print(json.dumps(problem, ensure_ascii=False), file=sys.stderr)
        return 2


if __name__ == '__main__':
    sys.exit(main())
