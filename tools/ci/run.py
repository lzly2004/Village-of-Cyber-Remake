#!/usr/bin/env python3
"""Run the existing verification entry once; attach CI identity without changing its format."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[2]


def digest(file):
    h = hashlib.sha256()
    with file.open('rb') as stream:
        for data in iter(lambda: stream.read(1024 * 1024), b''):
            h.update(data)
    return h.hexdigest()


def compare(plan, result, context):
    if (plan['status'] != 'planned' or result['status'] != 'passed' or
            plan['tested_sha'] != context['head_sha'] or result['tested_sha'] != context['head_sha'] or
            plan['base_sha'] != context['base_sha'] or result['base_sha'] != context['base_sha'] or
            plan['plan'] != result['plan'] or plan['tier_policy_sha256'] != result['tier_policy_sha256']):
        raise ValueError('CI_PLAN_HEAD_OR_EXECUTION_MISMATCH')


def jar_report(file):
    if not file.is_file() or file.is_symlink():
        raise ValueError('FAT_JAR_MISSING')
    with zipfile.ZipFile(file) as package:
        names = package.namelist()
        if 'com/vocr/app/Game.class' not in names or any(n.startswith(('com/vocr/web/', 'io/javalin/', 'org/eclipse/jetty/')) for n in names):
            raise ValueError('FAT_JAR_CONTENT_BOUNDARY_FAILED')
    return {'path': file.relative_to(ROOT).as_posix(), 'bytes': file.stat().st_size, 'sha256': digest(file)}


def main():
    directory = ROOT / 'build/ci'
    outcome = {'schema': 1, 'status': 'failed', 'run_id': os.environ.get('GITHUB_RUN_ID'),
               'run_attempt': os.environ.get('GITHUB_RUN_ATTEMPT'),
               'artifact_name': 'verify-' + os.environ.get('GITHUB_RUN_ID', 'unknown') + '-' + os.environ.get('GITHUB_RUN_ATTEMPT', 'unknown'),
               'same_job_exchange': True}
    try:
        if os.environ.get('GITHUB_ACTIONS') != 'true':
            raise ValueError('GITHUB_ACTIONS_CONTEXT_REQUIRED')
        context = json.loads((directory / 'context.json').read_text())
        plan = json.loads((directory / 'plan.json').read_text())
        argv = ['bash', 'tools/verify.sh', '--base', context['base_sha'], '--json']
        if context['force_full']:
            argv.append('--full')
        with (directory / 'verify.stdout').open('wb') as out, (directory / 'verify.stderr').open('wb') as err:
            code = subprocess.run(argv, cwd=ROOT, stdout=out, stderr=err).returncode
        outcome['verification_exit_code'] = code
        if code:
            raise ValueError('VERIFICATION_FAILED: see verify.stdout/stderr and first_failure in result.json')
        result = json.loads((directory / 'verify.stdout').read_text())
        compare(plan, result, context)
        result_path = ROOT / result['run_dir'] / 'result.json'
        outcome['result'] = result_path.relative_to(ROOT).as_posix()
        outcome['result_sha256'] = digest(result_path)
        outcome['tested_sha'] = result['tested_sha']
        if any(s['id'] == 'java-full' for s in result['plan']['suites']):
            outcome['fat_jar'] = jar_report(ROOT / 'build/libs/vocr-1.0.3.4-all.jar')
        outcome['status'] = 'passed'
        return 0
    except (OSError, ValueError, KeyError, subprocess.SubprocessError, zipfile.BadZipFile) as error:
        outcome['reason'] = str(error)
        print(json.dumps(outcome), file=sys.stderr)
        return 1
    finally:
        if directory.is_dir():
            (directory / 'outcome.json').write_text(json.dumps(outcome, indent=2) + '\n')
        if os.environ.get('GITHUB_STEP_SUMMARY'):
            with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as summary:
                summary.write('Verification: **' + outcome['status'] + '**\n\nArtifact: `' + outcome['artifact_name'] + '`\n')


if __name__ == '__main__':
    sys.exit(main())
