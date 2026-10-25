#!/usr/bin/env python3
"""Resolve immutable event SHAs; scope preview is not a successful verification."""
import importlib.util
import json
import os
from pathlib import Path
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location('ci_verify', ROOT / 'tools/verify.py')
VERIFY = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(VERIFY)
SHA = re.compile(r'[0-9a-f]{40}\Z')
ZERO = '0' * 40


def sha(value):
    if not isinstance(value, str) or not SHA.fullmatch(value) or value == ZERO:
        raise ValueError('INVALID_EVENT_SHA')
    return value


def git(root, *args):
    return subprocess.check_output(['git', '--no-optional-locks', '-C', str(root), *args], stderr=subprocess.PIPE).decode().strip()


def resolve(event, name, environment, git_read):
    checked = sha(git_read('rev-parse', 'HEAD'))
    if name == 'pull_request':
        base = sha(event['pull_request']['base']['sha'])
        head = sha(event['pull_request']['head']['sha'])
        source, full = 'pull_request.base.sha', False
    elif name == 'push':
        if event.get('deleted'):
            raise ValueError('DELETED_PUSH_NOT_VERIFIED')
        head = sha(event['after'])
        before = event['before']
        full = False
        if before == ZERO:
            branch = event['repository']['default_branch']
            if not isinstance(branch, str) or branch.startswith('-') or any(ord(c) < 32 for c in branch):
                raise ValueError('INVALID_DEFAULT_BRANCH')
            git_read('check-ref-format', '--branch', branch)
            base = sha(git_read('rev-parse', '--verify', 'refs/remotes/origin/' + branch + '^{commit}'))
            source = 'new_branch:origin/default_branch'
        else:
            base, source = sha(before), 'push.before'
    elif name == 'workflow_dispatch':
        inputs = event.get('inputs', {})
        base, head = sha(inputs.get('base_sha')), sha(environment['GITHUB_SHA'])
        value = inputs.get('full', False)
        if not (isinstance(value, bool) or isinstance(value, str) and value in ('false', 'true')):
            raise ValueError('INVALID_FULL_INPUT')
        full, source = value in (True, 'true'), 'workflow_dispatch.inputs.base_sha'
    else:
        raise ValueError('UNSUPPORTED_EVENT')
    if checked != head:
        raise ValueError('CHECKOUT_IS_NOT_EVENT_HEAD')
    # Missing/rewritten base fails explicitly; never silently replace it with HEAD^.
    try:
        if git_read('rev-parse', '--verify', base + '^{commit}') != base:
            raise ValueError('BASE_COMMIT_UNAVAILABLE')
    except subprocess.CalledProcessError as error:
        raise ValueError('BASE_COMMIT_UNAVAILABLE') from error
    return {'schema': 1, 'trigger': name, 'base_sha': base, 'head_sha': head,
            'base_source': source, 'force_full': full, 'same_job_exchange': True}


def flags(plan):
    ids = {s['id'] for s in plan['suites']}
    return {'CI_NEEDS_LFS': plan['requires_lfs'],
            'CI_NEEDS_JAVA': bool(ids & {'java-full', 'oracle-generate', 'gate', 'files-repeat', 'default-e2e', 'desktop-exchange'}),
            'CI_NEEDS_NODE': bool(ids & {'frontend-unit', 'exchange-contract', 'web-build', 'assets', 'audio', 'files-repeat', 'default-e2e', 'gate', 'desktop-exchange'}),
            'CI_NEEDS_BROWSER': bool(ids & {'assets', 'audio', 'files-repeat', 'default-e2e', 'gate', 'desktop-exchange'}),
            'CI_NEEDS_PIP': bool(ids & {'ci-tests', 'image-tool-tests', 'audio-tool-tests', 'web-build'})}


def main():
    try:
        if os.environ.get('GITHUB_ACTIONS') != 'true':
            raise ValueError('GITHUB_ACTIONS_CONTEXT_REQUIRED')
        event = json.loads(Path(os.environ['GITHUB_EVENT_PATH']).read_text(encoding='utf-8'))
        report = resolve(event, os.environ['GITHUB_EVENT_NAME'], os.environ, lambda *a: git(ROOT, *a))
        _, paths = VERIFY.changes(ROOT, report['base_sha'], report['head_sha'])
        preview = VERIFY.plan(paths, report['force_full'])
        report['scope_preview'] = preview
        report['scope_only_not_verified'] = True
        directory = ROOT / 'build/ci'
        directory.mkdir(parents=True, exist_ok=True)
        (directory / 'context.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
        with open(os.environ['GITHUB_ENV'], 'a', encoding='utf-8') as out:
            out.write('CI_BASE_SHA=' + report['base_sha'] + '\n')
            out.write('CI_FORCE_FULL=' + str(report['force_full']).lower() + '\n')
            for key, value in flags(preview).items():
                out.write(key + '=' + str(value).lower() + '\n')
        print(json.dumps({'base_sha': report['base_sha'], 'head_sha': report['head_sha'], 'scope_tier': preview['tier']}))
        return 0
    except (OSError, ValueError, KeyError, subprocess.SubprocessError) as error:
        print(json.dumps({'error': 'CI_CONTEXT_REFUSED', 'reason': str(error)}), file=sys.stderr)
        return 2


if __name__ == '__main__':
    sys.exit(main())
