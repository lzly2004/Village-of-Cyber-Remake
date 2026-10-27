#!/usr/bin/env python3
"""Pure evidence assembly: never runs suites, posts reviews, or changes acceptance gates."""
import argparse
import hashlib
import html
import json
import os
from pathlib import Path, PurePosixPath
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
MANUAL = ['A06', 'A07', 'A08', '046 browser/DPI/accessibility/listening/devices/loop seams',
          '049 pre-release manual review', '047 capacity/long game']
ANSI = re.compile(r'\x1b\[[0-?]*[ -/]*[@-~]')
SHA = re.compile(r'[0-9a-f]{40}\Z')


def digest(data):
    return hashlib.sha256(data).hexdigest()


def load_json(data):
    def pairs(items):
        value = {}
        for key, entry in items:
            if key in value:
                raise ValueError('JSON_DUPLICATE_KEY')
            value[key] = entry
        return value
    value = json.loads(data, object_pairs_hook=pairs)
    if not isinstance(value, dict):
        raise ValueError('JSON_OBJECT_REQUIRED')
    return value


def safe_path(value):
    if (not isinstance(value, str) or not value or '\\' in value or
            any(ord(c) < 32 for c in value) or value.startswith('/') or
            any(p in ('', '.', '..') for p in value.split('/'))):
        raise ValueError('UNSAFE_EVIDENCE_PATH')
    return value


def local_reader(root):
    def read(name):
        name = safe_path(name)
        p = root / name
        # Also support Python3.8 without depending on Path.is_relative_to.
        try:
            p.resolve().relative_to(root.resolve())
        except ValueError:
            raise ValueError('EVIDENCE_PATH_ESCAPE')
        node = p
        while node != root:
            if node.is_symlink():
                raise ValueError('EVIDENCE_SYMLINK')
            node = node.parent
        return p.read_bytes()
    return read


def counts(text):
    text = ANSI.sub('', text)
    tap = {k: int(v) for k, v in re.findall(r'^# (tests|pass|fail|skipped|cancelled) (\d+)\s*$', text, re.M)}
    if set(tap) == {'tests', 'pass', 'fail', 'skipped', 'cancelled'}:
        if tap['tests'] != sum(tap[k] for k in ('pass', 'fail', 'skipped', 'cancelled')):
            raise ValueError('TAP_COUNT_MISMATCH')
        return {'tests': tap['tests'], 'passed': tap['pass'], 'failures': tap['fail'],
                'skipped': tap['skipped'], 'cancelled': tap['cancelled'], 'source': 'node-tap-log'}
    match = re.search(r'^Ran (\d+) tests? in .+\n\s*\n(OK(?: \([^\n]*\))?|FAILED \([^\n]*\))\s*$', text, re.M)
    if match:
        d = {k: int(v) for k, v in re.findall(r'(failures|errors|skipped)=(\d+)', match[2])}
        n = int(match[1]); f, e, s = (d.get(k, 0) for k in ('failures', 'errors', 'skipped'))
        if n < f + e + s:
            raise ValueError('UNITTEST_COUNT_MISMATCH')
        return {'tests': n, 'passed': n-f-e-s, 'failures': f, 'errors': e, 'skipped': s, 'source': 'python-unittest-log'}
    pw = {k: int(v) for v, k in re.findall(r'^\s*(\d+) (passed|failed|skipped|flaky|interrupted)(?:\s*\([^\n]*\))?\s*$', text, re.M)}
    if pw:
        return {'tests': sum(pw.values()), 'passed': pw.get('passed', 0), 'failures': pw.get('failed', 0),
                'skipped': pw.get('skipped', 0), 'flaky': pw.get('flaky', 0),
                'interrupted': pw.get('interrupted', 0), 'source': 'playwright-log'}
    return None


def short(value):
    value = ANSI.sub('', str(value)).replace('\r', ' ').replace('\n', ' ')
    value = re.sub(r'https?://\S+', '[URL omitted]', value)
    return ''.join(c for c in value if ord(c) >= 32)[:400]


def failure_detail(text, *, diff_check=False):
    text = ANSI.sub('', text)
    if diff_check:
        issues = re.findall(r'^([^\r\n]+?):([1-9][0-9]*): (trailing whitespace(?:, space before tab in indent)?\.|space before tab in indent\.|new blank line at EOF\.)\r?$', text, re.M)
        if issues:
            first = issues[0][0] + ':' + issues[0][1]
            reasons = [reason for item in issues for reason in item[2][:-1].split(', ')]
            labels = '; '.join(kind + ': ' + str(reasons.count(kind)) for kind in dict.fromkeys(reasons))
            return {'case': short(first), 'first_error': short('git diff --check: ' + str(len(issues)) +
                    ' issues / ' + str(len({item[0] for item in issues})) + ' files (' + labels + '); first ' + first)}
    case = re.search(r'^(?:FAIL|ERROR): (.+)$|^not ok \d+ - (.+)$|^\s*\d+\) (.+ › .+)$', text, re.M)
    error = re.search(r'^\s*((?:AssertionError|Error|TypeError|ValueError|FAILED|REFUSED)[^\n]*)', text, re.M)
    error = error or re.search(r'^\s*(error:[^\n]*)', text, re.M)
    return {'case': short(next((g for g in case.groups() if g), '')) if case else None,
            'first_error': short(error[1]) if error else None}


def xml_counts(read, names):
    total = {k: 0 for k in ('tests', 'failures', 'errors', 'skipped')}
    failure = None
    for name in sorted(names):
        data = read(name)
        if b'<!DOCTYPE' in data or b'<!ENTITY' in data:
            raise ValueError('XML_ENTITY_REFUSED')
        root = ET.fromstring(data)
        if root.tag != 'testsuite':
            raise ValueError('INVALID_TEST_XML')
        for k in total:
            total[k] += int(root.attrib[k])
        cases = root.findall('testcase')
        for key, tag in [('failures', 'failure'), ('errors', 'error'), ('skipped', 'skipped')]:
            if sum(case.find(tag) is not None for case in cases) != int(root.attrib[key]):
                raise ValueError('XML_BODY_COUNT_MISMATCH')
        if len(cases) != int(root.attrib['tests']):
            raise ValueError('XML_CASE_COUNT_MISMATCH')
        if not failure:
            for case in cases:
                err = case.find('failure')
                if err is None:
                    err = case.find('error')
                if err is not None:
                    failure = {'case': short(case.get('classname', '') + '.' + case.get('name', '')),
                               'first_error': short(err.get('message') or err.text or '')}
                    break
    return {**total, 'passed': total['tests']-sum(total[k] for k in ('failures', 'errors', 'skipped')),
            'xml_files': len(names), 'source': 'build/test-results/test/TEST-*.xml'}, failure


def release_evidence(read, get, files, outcome, result, environment):
    """Bind a small report to a separately uploaded binary, without reading/executing that binary."""
    if result['plan'].get('tier') != 'full':
        if (outcome or {}).get('release_required') or (outcome or {}).get('web_release'):
            raise ValueError('UNEXPECTED_RELEASE_FOR_NONFULL_PLAN')
        return {'status': 'not_built', 'reason': 'not_full_tier'}
    package = get('build/ci/web-release.json')
    upload = get('build/ci/release-upload.json')
    if (outcome or {}).get('status') != 'passed':
        return {'status': 'failed', 'reason': 'release_build_or_smoke_not_completed'}
    if not upload and environment.get('CI_JOB_STATUS') == 'failure':
        return {'status': 'failed', 'reason': 'release_upload_not_completed'}
    pointer = (outcome or {}).get('web_release') or {}
    if (outcome.get('release_required') is not True or not package or not upload or
            pointer.get('report') != 'build/ci/web-release.json' or
            pointer.get('sha256') != files['build/ci/web-release.json']['sha256']):
        raise ValueError('RELEASE_EVIDENCE_MISSING_OR_CHANGED')
    if (package.get('status') != 'passed' or package.get('source_sha') != result['tested_sha'] or
            package.get('verification', {}).get('resultSha256') != outcome.get('result_sha256') or
            package.get('packaged_smoke', {}).get('status') != 'passed' or
            package.get('packaged_smoke', {}).get('tests') != 1):
        raise ValueError('RELEASE_VERIFICATION_MISMATCH')
    smoke = package['packaged_smoke']
    log_name = smoke.get('log', '')
    if not re.fullmatch(r'build/verify/release-smoke-[0-9a-f]{32}/packaged-smoke\.log', log_name):
        raise ValueError('RELEASE_SMOKE_LOG_INVALID')
    try:
        raw = read(log_name)
    except (KeyError, OSError):
        raise ValueError('RELEASE_SMOKE_LOG_MISSING') from None
    files[log_name] = {'sha256': digest(raw), 'bytes': len(raw)}
    text = raw.decode('utf-8', errors='replace')
    if (digest(raw) != smoke.get('sha256') or not re.search(r'\b1 passed\b', text) or
            re.search(r'\b\d+ (?:failed|skipped|flaky|interrupted)\b', text)):
        raise ValueError('RELEASE_SMOKE_LOG_MISMATCH')
    archive = package.get('archive') or {}
    if (archive.get('name') != 'vocr-web.zip' or type(archive.get('bytes')) is not int or archive['bytes'] <= 0 or
            not re.fullmatch(r'[0-9a-f]{64}', archive.get('sha256', '')) or
            not re.fullmatch(r'[0-9a-f]{64}', package.get('manifest_sha256', ''))):
        raise ValueError('RELEASE_ARCHIVE_IDENTITY_INVALID')
    repo, run, attempt = (environment.get(k) for k in ('GITHUB_REPOSITORY', 'GITHUB_RUN_ID', 'GITHUB_RUN_ATTEMPT'))
    identity = upload.get('id')
    if (upload.get('repository') != repo or upload.get('run_id') != run or upload.get('attempt') != attempt or
            upload.get('tested_head') != result['tested_sha'] or
            upload.get('artifact_name') != 'release-' + str(run) + '-' + str(attempt) or
            not isinstance(identity, str) or not identity.isdigit() or int(identity) <= 0 or
            not re.fullmatch(r'[0-9a-f]{64}', upload.get('digest', '')) or
            upload.get('url') != 'https://github.com/' + str(repo) + '/actions/runs/' + str(run) + '/artifacts/' + identity):
        raise ValueError('RELEASE_UPLOAD_IDENTITY_MISMATCH')
    return {'status': 'passed', 'report': package, 'artifact': upload,
            'scope': 'report and upload identity bound here; download/check the separate binary before use'}


def assemble(read, names, environment, *, assembly_revision=5):
    """read(names) can be filesystem or a bounded ZIP reader; no extraction/execution."""
    # Reproduce published reports exactly: revision 1 duplicates; revision 2 null diff diagnostics.
    if type(assembly_revision) is not int or assembly_revision not in (1, 2, 3, 4, 5):
        raise ValueError("UNSUPPORTED_ASSEMBLY_REVISION")
    names = set(map(safe_path, names))
    files = {}
    def get(name):
        if name not in names:
            return None
        b = read(name); files[name] = {'sha256': digest(b), 'bytes': len(b)}
        return load_json(b)
    context, plan, outcome = (get('build/ci/' + n + '.json') for n in ('context', 'plan', 'outcome'))
    tools = get('build/ci/tools.json')
    for name in ('verify.stdout', 'verify.stderr', 'plan.stderr') + (('release.stdout', 'release.stderr') if assembly_revision >= 5 else ()):
        path = 'build/ci/' + name
        if path in names:
            b = read(path); files[path] = {'sha256': digest(b), 'bytes': len(b)}
    expected = environment.get('CI_EXPECTED_HEAD')
    if expected and not SHA.fullmatch(expected):
        raise ValueError('INVALID_EXPECTED_HEAD')
    report = {'schema': 1, 'status': 'not_run', 'base_sha': (context or {}).get('base_sha'),
              'head_sha': (context or {}).get('head_sha') or expected, 'tested_sha': None,
              'trigger': (context or {}).get('trigger'), 'base_source': (context or {}).get('base_source'),
              'ci': {'repository': environment.get('GITHUB_REPOSITORY'), 'run_id': environment.get('GITHUB_RUN_ID'),
                     'attempt': environment.get('GITHUB_RUN_ATTEMPT'),
                     'run_url': None, 'artifact_name': (outcome or {}).get('artifact_name') or ('verify-'+str(environment.get('GITHUB_RUN_ID'))+'-'+str(environment.get('GITHUB_RUN_ATTEMPT')))},
              'tools': tools, 'suites': [], 'manual_gates': {n: 'not_covered' for n in MANUAL},
              'not_covered': ['real PR/fork event unless separately evidenced', 'release approval',
                              'physical output/listening/DPI', 'capacity/long game'],
              'files': files, 'first_failure': None, 'fat_jar': (outcome or {}).get('fat_jar')}
    if assembly_revision >= 5:
        report['web_release'] = {'status': 'not_built', 'reason': 'verification_not_completed'}
    if assembly_revision >= 2:
        report['assembly_revision'] = assembly_revision
    repo, run = report['ci']['repository'], report['ci']['run_id']
    if repo and re.fullmatch(r'[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+', repo) and run and str(run).isdigit():
        report['ci']['run_url'] = 'https://github.com/' + repo + '/actions/runs/' + str(run)
    if context and (not isinstance(context.get('base_sha'), str) or not SHA.fullmatch(context['base_sha']) or context['base_sha'] == '0'*40):
        raise ValueError('INVALID_EVIDENCE_BASE_SHA')
    if expected and report['head_sha'] != expected:
        raise ValueError('EVIDENCE_HEAD_MISMATCH')
    result_name = (outcome or {}).get('result')
    # A failed verification may have a real result before run.py can attach it.
    if not result_name:
        candidates = [n for n in names if re.fullmatch(r'build/verify/[^/]+/result\.json', n)]
        if len(candidates) > 1:
            raise ValueError('AMBIGUOUS_CURRENT_RESULT')
        result_name = candidates[0] if candidates else None
    result = get(safe_path(result_name)) if result_name else None
    if result is not None and 'build/ci/verify.stdout' in names:
        if load_json(read('build/ci/verify.stdout')) != result:
            raise ValueError('STDOUT_RESULT_MISMATCH')
    if result is None:
        if assembly_revision >= 4:
            report['unexecuted_suites'] = []
            report['policy_skipped_suites'] = []
        report['status'] = 'failed' if environment.get('CI_JOB_STATUS') in ('failure','cancelled') or outcome else 'not_run'
        report['first_failure'] = {'suite': 'preparation-or-entry', 'case': None,
                                   'first_error': short((outcome or {}).get('reason', 'No verification result; inspect job preparation logs.'))}
        report['plan'] = (plan or {}).get('plan') or (context or {}).get('scope_preview')
        return report
    if (not context or not plan or result.get('tested_sha') != context.get('head_sha') or
            result.get('base_sha') != context.get('base_sha') or plan.get('tested_sha') != context.get('head_sha') or
            plan.get('base_sha') != context.get('base_sha') or plan.get('status') != 'planned' or
            plan.get('plan') != result.get('plan') or plan.get('tier_policy_sha256') != result.get('tier_policy_sha256')):
        raise ValueError('EVIDENCE_PLAN_HEAD_MISMATCH')
    if outcome and outcome.get('tested_sha') and outcome['tested_sha'] != result['tested_sha']:
        raise ValueError('OUTCOME_HEAD_MISMATCH')
    if outcome and outcome.get('status') == 'passed' and not outcome.get('result_sha256'):
        raise ValueError('RESULT_HASH_MISSING')
    if outcome and outcome.get('result_sha256') and files[result_name]['sha256'] != outcome['result_sha256']:
        raise ValueError('RESULT_HASH_MISMATCH')
    report.update({'tested_sha': result['tested_sha'], 'plan': result['plan'], 'eol_policy': result.get('eol_policy'),
                   'fingerprints': result.get('input_audit', {}).get('fingerprints'),
                   'lfs': result.get('input_audit', {}).get('lfs'), 'not_covered': report['not_covered'] + result.get('not_covered', [])})
    if assembly_revision >= 2:
        report['not_covered'] = list(dict.fromkeys(report['not_covered']))
    planned = [s['id'] for s in result['plan']['suites']]
    actual = [s['id'] for s in result.get('suites', [])]
    if actual != planned[:len(actual)] or len(actual) != len(set(actual)):
        raise ValueError('SUITE_ORDER_OR_ID_MISMATCH')
    prefix = str(PurePosixPath(result_name).parent) + '/'
    xml_names = [n for n in names if re.fullmatch(r'build/test-results/test/TEST-[^/]+\.xml', n)]
    for s in result.get('suites', []):
        item = {k: s.get(k) for k in ('id', 'status', 'argv', 'seconds', 'exit_code', 'log', 'log_sha256')}
        item['counts'] = None
        if s.get('log'):
            name = prefix + safe_path(s['log']); b = read(name)
            if digest(b) != s['log_sha256']:
                raise ValueError('SUITE_LOG_HASH_MISMATCH')
            files[name] = {'sha256': digest(b), 'bytes': len(b)}
            text = b.decode('utf-8', errors='replace'); item['counts'] = counts(text)
            if s['status'] == 'failed':
                report['first_failure'] = {'suite': s['id'], **failure_detail(text, diff_check=assembly_revision >= 3 and s['id'] == 'diff-check')}
        if s['id'] == 'java-full' and xml_names:
            item['counts'], failure = xml_counts(read, xml_names)
            for name in xml_names:
                b = read(name); files[name] = {'sha256': digest(b), 'bytes': len(b)}
            if failure and s['status'] == 'failed':
                report['first_failure'] = {'suite': s['id'], **failure}
        report['suites'].append(item)
    lfs = report.get('lfs')
    if lfs:
        pairs = {}
        for item in lfs['entries']:
            oid, size = item['sha256'], item['bytes']
            if not re.fullmatch(r'[0-9a-f]{64}', oid) or type(size) is not int or size < 0 or oid in pairs and pairs[oid] != size:
                raise ValueError('INVALID_LFS_IDENTITY')
            pairs[oid] = size
        rows = [{'oid_sha256': oid, 'bytes': size} for oid, size in sorted(pairs.items())]
        report['lfs_summary'] = {'declared_pointer_paths': len(lfs['entries']), 'unique_payloads': len(rows),
                                'unique_payload_bytes': sum(pairs.values()), 'oid_sizes': rows,
                                'oid_size_aggregate_sha256': digest(''.join(oid+'\t'+str(size)+'\n' for oid,size in sorted(pairs.items())).encode('ascii')),
                                'remote_fetch_http_status_counts': None, 'account_usage': None,
                                'storage_relative_path': '.git/lfs/objects',
                                'remote_claim': 'input inventory does not independently prove remote availability'}
    report['unexecuted_suites'] = planned[len(actual):]
    report['policy_skipped_suites'] = result['plan'].get('skipped', [])
    if result.get('status') == 'passed':
        if tools is None:
            raise ValueError('TOOLCHAIN_EVIDENCE_MISSING')
        if assembly_revision >= 5:
            report['web_release'] = release_evidence(read, get, files, outcome, result, environment)
            if report['web_release']['status'] == 'failed':
                report['status'] = 'failed'
                report['first_failure'] = {'suite': 'web-release', 'case': None,
                                          'first_error': short((outcome or {}).get('reason', 'Release build or smoke failed.'))}
                return report
        if (not outcome or outcome.get('status') != 'passed' or actual != planned or not actual or
                any(s['status'] != 'passed' for s in result['suites']) or
                environment.get('CI_JOB_STATUS') == 'failure'):
            raise ValueError('INCOMPLETE_PASSED_EVIDENCE')
        report['status'] = 'passed'
    else:
        report['status'] = 'failed'
        report['first_failure'] = report['first_failure'] or {**result.get('first_failure', {}), 'case': None, 'first_error': None}
    return report


def md(value):
    return html.escape(short(value), quote=False).replace('`', '\\`').replace('|', '\\|').replace('[', '\\[').replace(']', '\\]')


def markdown(report):
    lines = ['# Verification evidence', '', 'Status: **' + md(report['status']) + '**', '',
             'Base: `' + md(report.get('base_sha')) + '`  ', 'Tested: `' + md(report.get('tested_sha')) + '`', '',
             '| Suite | Status | Tests | Source | Seconds |', '|---|---|---|---|---|']
    ci = report.get('ci') or {}
    repo, run = ci.get('repository'), ci.get('run_id')
    if repo and re.fullmatch(r'[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+',repo) and run and str(run).isdigit():
        links = ['[GitHub run](https://github.com/'+repo+'/actions/runs/'+str(run)+')', '']
        proof = report.get('download_proof') or {}
        if type(proof.get('artifact_id')) is int:
            links += ['[Evidence artifact](https://github.com/'+repo+'/actions/runs/'+str(run)+'/artifacts/'+str(proof['artifact_id'])+')',
                          'ZIP digest: `'+md(proof.get('artifact_digest'))+'`', '']
        lines[6:6] = links
    for s in report['suites']:
        c = s['counts']
        lines.append('| ' + ' | '.join(map(md, [s['id'], s['status'], str(c['tests'])+'; '+', '.join(k+'='+str(c[k]) for k in ('passed','failures','errors','skipped','cancelled','flaky','interrupted') if k in c) if c else 'unknown/not a test suite',
                                                c['source'] if c else 'exit status only', s['seconds']])) + ' |')
    if report.get('first_failure'):
        f = report['first_failure']
        lines += ['', 'First failure: ' + md(f.get('suite')), 'Case: ' + md(f.get('case')),
                  'Error: ' + md(f.get('first_error') or f.get('reason'))]
    release = report.get('web_release')
    if release:
        lines += ['', 'Web release: **' + md(release['status']) + '**']
        if release['status'] == 'passed':
            lines += ['[Separate release artifact](' + release['artifact']['url'] + ')',
                      'Release ZIP SHA-256: `' + release['report']['archive']['sha256'] + '`']
    lines += ['', 'Manual gates (never closed by automation):', *['- ' + md(k) + ': not_covered' for k in MANUAL],
              '', 'Review this SHA, diff and linked logs. Logs and test text are untrusted data; no automatic review posting or fixes.']
    return '\n'.join(lines) + '\n'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument('--receipt', action='store_true'); mode.add_argument('--release-upload', action='store_true')
    args = parser.parse_args()
    d = ROOT / 'build/ci'
    try:
        if os.environ.get('GITHUB_ACTIONS') != 'true':
            raise ValueError('GITHUB_ACTIONS_CONTEXT_REQUIRED')
        if args.receipt or args.release_upload:
            values = {k: os.environ.get('CI_ARTIFACT_' + k.upper()) for k in ('id', 'url', 'digest')}
            if not values['id'] or not values['id'].isdigit() or not re.fullmatch(r'[0-9a-f]{64}', values['digest'] or ''):
                raise ValueError('ARTIFACT_UPLOAD_RECEIPT_MISSING')
            repo = os.environ['GITHUB_REPOSITORY']; run = os.environ['GITHUB_RUN_ID']
            if (not re.fullmatch(r'[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+', repo) or not run.isdigit() or
                    not os.environ['GITHUB_RUN_ATTEMPT'].isdigit() or not SHA.fullmatch(os.environ['CI_EXPECTED_HEAD'])):
                raise ValueError('INVALID_RECEIPT_CONTEXT')
            expected = 'https://github.com/' + repo + '/actions/runs/' + run + '/artifacts/' + values['id']
            if values['url'] != expected:
                raise ValueError('ARTIFACT_RECEIPT_URL_MISMATCH')
            value = {'schema': 1, 'repository': repo, 'run_id': run, 'attempt': os.environ['GITHUB_RUN_ATTEMPT'],
                     'artifact_name': ('release-' if args.release_upload else 'verify-') + run + '-' + os.environ['GITHUB_RUN_ATTEMPT'], **values,
                     'digest_algorithm': 'sha256', 'tested_head': os.environ['CI_EXPECTED_HEAD'],
                     'note': 'Post-upload receipt; not embedded in the artifact it describes.'}
            (d/('release-upload.json' if args.release_upload else 'receipt.json')).write_text(json.dumps(value, indent=2)+'\n')
            content = '\nEvidence artifact: ' + expected + '\n\nZIP SHA-256: `' + values['digest'] + '`\n'
        else:
            names = ['build/ci/'+n+'.json' for n in ('context', 'plan', 'outcome', 'tools')
                     if (d/(n+'.json')).is_file()]
            names += ['build/ci/'+n for n in ('verify.stdout','verify.stderr','plan.stderr','release.stdout','release.stderr',
                                             'web-release.json','release-upload.json') if (d/n).is_file()]
            names += [p.relative_to(ROOT).as_posix() for p in (ROOT/'build/verify').rglob('*')
                      if p.is_file() and not p.is_symlink()]
            names += [p.relative_to(ROOT).as_posix() for p in (ROOT/'build/test-results/test').glob('TEST-*.xml')]
            report = assemble(local_reader(ROOT), names, os.environ)
            (d/'evidence.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')
            content = markdown(report); (d/'summary.md').write_text(content)
        if os.environ.get('GITHUB_STEP_SUMMARY'):
            with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as out: out.write(content)
        return 0
    except (OSError, ValueError, KeyError, TypeError, AttributeError, IndexError, ET.ParseError) as error:
        # No exception URLs, environment contents or authentication details in stdout/stderr.
        reason = short(error)
        print('EVIDENCE_REFUSED: ' + reason, file=sys.stderr)
        if d.is_dir() and not args.receipt:
            fallback = {'schema': 1, 'status': 'failed', 'base_sha': None, 'head_sha': None,
                        'tested_sha': None, 'suites': [], 'manual_gates': {n:'not_covered' for n in MANUAL},
                        'first_failure': {'suite':'evidence-validation','case':None,'first_error':reason}}
            (d/'evidence.json').write_text(json.dumps(fallback,indent=2)+'\n')
            (d/'summary.md').write_text(markdown(fallback))
        return 1


if __name__ == '__main__':
    sys.exit(main())
