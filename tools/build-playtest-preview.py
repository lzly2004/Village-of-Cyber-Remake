#!/usr/bin/env python3
"""Same-head frontend preview, with real packaged game smoke; never a full release."""
import argparse
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import uuid
import playtest_candidate as P
import web_release as R

ROOT = Path(__file__).resolve().parents[1]


def build(result_file, output):
    R.require(os.environ.get('VOCR_APPROVED_FULL_CHECK') == '1', 'PACKAGE_APPROVAL_REQUIRED')
    output = R.no_links(output)
    R.require(ROOT / 'build' in output.parents and not output.exists(), 'INVALID_OR_EXISTING_PREVIEW_OUTPUT')
    P.preview_verification(ROOT, result_file)
    attempt = ROOT / 'build/preview-attempts' / uuid.uuid4().hex; attempt.mkdir(parents=True)
    log_dir = ROOT / 'build/verify' / ('preview-smoke-' + attempt.name); log_dir.mkdir(parents=True)
    with (log_dir / 'prepare-runtime.log').open('wb') as log:
        code = subprocess.run(['bash', 'tools/run-gradle-safe.sh', 'prepareWebRuntime', '--offline', '--rerun-tasks'], cwd=ROOT,
                              stdout=log, stderr=subprocess.STDOUT).returncode
    R.require(code == 0, 'PREVIEW_RUNTIME_BUILD_FAILED')
    files = P.preview_payload(ROOT, result_file)
    archive = attempt / 'vocr-preview.zip'; R.write_archive(files, archive); P.check_preview(archive)
    original_hash = R.hash_file(archive); extracted = attempt / 'extracted'; extracted.mkdir()
    for n, data in files.items():
        path = extracted / n; path.parent.mkdir(parents=True, exist_ok=True); path.write_bytes(data)
    smoke = log_dir / 'packaged-smoke.log'
    command = ['npm', '--prefix', 'web-client', 'run', 'test:e2e', '--', '--config=../tools/playwright.playtest.config.mjs']
    env = {**os.environ, 'VOCR_PLAYTEST_PAYLOAD': str(extracted), 'VOCR_PLAYTEST_RUN': str(log_dir / 'service'),
           'VOCR_PLAYTEST_OUTPUT': str(log_dir / 'browser')}
    with smoke.open('wb') as log: code = subprocess.run(command, cwd=ROOT, env=env, stdout=log, stderr=subprocess.STDOUT).returncode
    text = smoke.read_text(errors='replace')
    R.require(code == 0 and re.search(r'\b1 passed\b', text) and not re.search(r'\b\d+ (failed|skipped|flaky|interrupted)\b', text),
              'PREVIEW_SMOKE_FAILED')
    P.preview_inputs(ROOT, result_file); m = P.check_preview(archive)
    R.require(R.hash_file(archive) == original_hash and {n: p.read_bytes() for n, p in R.scan(extracted).items()} == files,
              'PREVIEW_CHANGED_DURING_SMOKE')
    report = {'schema': 1, 'status': 'passed', 'source_sha': m['source']['sha'],
              'archive': {'name': archive.name, 'bytes': archive.stat().st_size, 'sha256': original_hash},
              'manifest_sha256': R.sha(files['preview-manifest.json']), 'verification': m['verification'],
              'budgets': m['budgets'], 'packaged_smoke': {'status': 'passed', 'tests': 1,
              'log': smoke.relative_to(ROOT).as_posix(), 'sha256': R.hash_file(smoke)}, 'not_covered': m['notCovered']}
    publish = attempt / 'publish'; publish.mkdir(); shutil.copyfile(archive, publish / archive.name)
    (publish / 'preview-manifest.json').write_bytes(files['preview-manifest.json'])
    (publish / 'preview-report.json').write_bytes(R.json_bytes(report))
    output.parent.mkdir(parents=True, exist_ok=True); publish.rename(output)
    return report


def main():
    p = argparse.ArgumentParser(); p.add_argument('--verified-result', type=Path, required=True)
    p.add_argument('--output', type=Path, required=True); a = p.parse_args()
    try:
        print(R.json_bytes(build(a.verified_result.absolute(), a.output.absolute())).decode()); return 0
    except (OSError, ValueError, KeyError, TypeError, subprocess.SubprocessError):
        print('PREVIEW_BUILD_REFUSED', file=sys.stderr); return 1


if __name__ == '__main__': sys.exit(main())
