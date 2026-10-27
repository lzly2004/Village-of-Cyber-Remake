#!/usr/bin/env python3
"""Verify once, package, check and smoke the actual Web release. Never deploy or upload."""
import argparse
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import uuid
import web_release as R

ROOT=Path(__file__).resolve().parents[1]


def build(result_file,output):
    output=R.no_links(output)
    R.require((ROOT/'build') in output.parents and not output.exists(),'INVALID_OR_EXISTING_RELEASE_OUTPUT')
    R.require('VOCR_WEB_SERVER_COMMAND' not in os.environ,'RELEASE_SERVER_OVERRIDE_FORBIDDEN')
    attempt=ROOT/'build/release-attempts'/uuid.uuid4().hex;attempt.mkdir(parents=True)
    candidate=attempt/'candidate';archive=R.build_candidate(ROOT,result_file,candidate)
    manifest=R.check_release(archive);tested_hash=R.hash_file(archive)
    extracted=R.extract_checked(archive,attempt/'extracted')
    smoke=ROOT/'build/verify'/('release-smoke-'+attempt.name);smoke.mkdir(parents=True)
    log=smoke/'packaged-smoke.log'
    env={**os.environ,'VOCR_RELEASE_DIR':str(extracted),'VOCR_RELEASE_TEST_OUTPUT':str(smoke/'browser')}
    argv=['npm','--prefix','web-client','run','test:e2e','--','--config=../tools/playwright.release.config.mjs']
    with log.open('wb') as stream:code=subprocess.run(argv,cwd=ROOT,env=env,stdout=stream,stderr=subprocess.STDOUT).returncode
    raw=log.read_text(errors='replace')
    R.require(code==0 and re.search(r'\b1 passed\b',raw) and not re.search(r'\b\d+ (?:failed|skipped|flaky|interrupted)\b',raw),'PACKAGED_SMOKE_FAILED')
    # The browser cannot make source/config changes disappear behind an old successful result.
    R.verify_inputs(ROOT,result_file);R.check_release(archive)
    R.require(R.hash_file(archive)==tested_hash,'ARCHIVE_CHANGED_DURING_SMOKE')
    R.require({n:p.read_bytes() for n,p in R.scan(extracted).items()}==R.read_archive(archive),'EXTRACTED_FILES_CHANGED_DURING_SMOKE')
    report={'schema':1,'status':'passed','source_sha':manifest['source']['sha'],
            'archive':{'name':archive.name,'bytes':archive.stat().st_size,'sha256':R.hash_file(archive)},
            'manifest_sha256':R.sha(R.read_archive(archive)['release-manifest.json']),
            'verification':manifest['verification'],'budgets':manifest['budgets'],
            'packaged_smoke':{'status':'passed','tests':1,'log':log.relative_to(ROOT).as_posix(),'sha256':R.hash_file(log),
                              'argv':argv,'scope':'unseeded game, restore, settlement, browser-local records/replay from extracted files'},
            'not_covered':manifest['notCovered']}
    (candidate/'release-report.json').write_bytes(R.json_bytes(report))
    (candidate/'release-manifest.json').write_bytes(R.read_archive(archive)['release-manifest.json'])
    output.parent.mkdir(parents=True,exist_ok=True);candidate.rename(output)
    return report


def main():
    parser=argparse.ArgumentParser(description=__doc__);sub=parser.add_subparsers(dest='command',required=True)
    p=sub.add_parser('build');p.add_argument('--base');p.add_argument('--verified-result',type=Path);p.add_argument('--output',type=Path,default=ROOT/'build/releases/web')
    p=sub.add_parser('check');p.add_argument('--archive',type=Path,required=True)
    args=parser.parse_args()
    try:
        if args.command=='check':
            value=R.check_release(args.archive);print(json.dumps({'status':'passed','source_sha':value['source']['sha'],'budgets':value['budgets']}));return 0
        result=args.verified_result
        if result is None:
            R.require(os.environ.get('VOCR_APPROVED_FULL_CHECK')=='1','FULL_CHECK_APPROVAL_REQUIRED')
            R.require(args.base is not None,'BASE_SHA_REQUIRED')
            d=ROOT/'build/release-attempts'/uuid.uuid4().hex;d.mkdir(parents=True)
            with (d/'verify.stdout').open('wb') as out,(d/'verify.stderr').open('wb') as err:
                code=subprocess.run(['bash','tools/verify.sh','--base',args.base,'--full','--json'],cwd=ROOT,stdout=out,stderr=err).returncode
            R.require(code==0,'FULL_VERIFICATION_FAILED')
            result=ROOT/R.load_json((d/'verify.stdout').read_bytes())['run_dir']/'result.json'
        else:
            # Packaging/smoke starts JVMs even when the full result is reused.
            R.require(os.environ.get('VOCR_APPROVED_FULL_CHECK')=='1','PACKAGE_APPROVAL_REQUIRED')
        report=build(result.absolute(),args.output.absolute());print(json.dumps(report,ensure_ascii=False));return 0
    except (OSError,ValueError,KeyError,TypeError,subprocess.SubprocessError) as error:
        # Never emit arbitrary exception/body/path content as a public diagnostic.
        code=str(error) if isinstance(error,ValueError) and re.fullmatch(r'[A-Z_]+(?::[a-z]+)?',str(error)) else 'RELEASE_BUILD_REFUSED'
        print(json.dumps({'status':'failed','error':code}),file=sys.stderr);return 1


if __name__=='__main__':sys.exit(main())
