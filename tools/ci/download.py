#!/usr/bin/env python3
"""First-party read-only evidence download or offline verification. Never extracts or executes ZIP entries."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import stat
import sys
import urllib.error
import urllib.parse
import urllib.request
import zipfile
import xml.etree.ElementTree as ET

import evidence

MAX_ZIP = 256 * 1024 * 1024
MAX_EXPANDED = 512 * 1024 * 1024
MAX_ENTRY = 32 * 1024 * 1024


class Redirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        value = urllib.parse.urlsplit(newurl)
        host = value.hostname or ''
        if value.scheme != 'https' or value.username or value.password or value.port not in (None, 443) or not (
                host in ('api.github.com', 'github.com') or host.endswith(('.blob.core.windows.net', '.githubusercontent.com'))):
            raise ValueError('UNTRUSTED_ARTIFACT_REDIRECT')
        redirected = super().redirect_request(req, fp, code, msg, headers, newurl)
        # Never forward GitHub API authorization to blob storage, even for trusted HTTPS redirects.
        redirected.remove_header('Authorization')
        return redirected


def get(path, token=None, limit=MAX_ZIP):
    headers = {'Accept': 'application/vnd.github+json', 'X-GitHub-Api-Version': '2026-03-10'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    req = urllib.request.Request('https://api.github.com/' + path, headers=headers)
    try:
        with urllib.request.build_opener(Redirect()).open(req, timeout=60) as response:
            data = response.read(limit+1)
        if len(data) > limit:
            raise ValueError('DOWNLOAD_EXCEEDS_BOUND')
        return data
    except urllib.error.HTTPError as error:
        if error.code == 429 or error.code == 403 and error.headers.get('X-RateLimit-Remaining') == '0':
            raise ValueError('GITHUB_API_RATE_LIMITED') from None
        if error.code in (401, 403):
            raise ValueError('AUTHENTICATION_OR_ACTIONS_READ_REQUIRED') from None
        if error.code == 410:
            raise ValueError('ARTIFACT_EXPIRED') from None
        raise ValueError('GITHUB_HTTP_' + str(error.code)) from None
    except urllib.error.URLError:
        raise ValueError('GITHUB_NETWORK_FAILED') from None


def validate(zip_path, run, artifact, expected_head, repository):
    if not evidence.SHA.fullmatch(expected_head):
        raise ValueError('INVALID_EXPECTED_HEAD')
    if type(run.get('id')) is not int or type(artifact.get('id')) is not int or artifact['id'] <= 0:
        raise ValueError('INVALID_RUN_OR_ARTIFACT_ID')
    if (run['status'] != 'completed' or run['head_sha'] != expected_head or
            run['repository']['full_name'] != repository or artifact['workflow_run']['id'] != run['id'] or
            artifact['workflow_run']['head_sha'] != expected_head or artifact.get('expired') or
            artifact['name'] != 'verify-' + str(run['id']) + '-' + str(run['run_attempt'])):
        raise ValueError('RUN_ARTIFACT_HEAD_OR_ATTEMPT_MISMATCH')
    if zip_path.stat().st_size > MAX_ZIP or zip_path.stat().st_size != artifact['size_in_bytes']:
        raise ValueError('ZIP_SIZE_MISMATCH')
    with zip_path.open('rb') as stream:
        h = hashlib.sha256()
        for data in iter(lambda: stream.read(1024*1024), b''):h.update(data)
    if artifact.get('digest') != 'sha256:' + h.hexdigest():
        raise ValueError('ARTIFACT_ZIP_DIGEST_MISMATCH')
    with zipfile.ZipFile(zip_path) as archive:
        entries = archive.infolist()
        if len(entries) > 20000 or sum(i.file_size for i in entries) > MAX_EXPANDED:
            raise ValueError('EXPANDED_ZIP_EXCEEDS_BOUND')
        names, seen = [], set()
        for i in entries:
            name = evidence.safe_path(i.filename.rstrip('/') if i.is_dir() else i.filename)
            if name.casefold() in seen or stat.S_ISLNK(i.external_attr >> 16) or i.flag_bits & 1 or i.file_size > MAX_ENTRY:
                raise ValueError('ZIP_DUPLICATE_SYMLINK_ENCRYPTED_OR_OVERSIZED')
            seen.add(name.casefold())
            if not i.is_dir():
                if not name.startswith(('build/ci/', 'build/verify/', 'build/test-results/', 'build/web035/',
                                        'web-client/test-results/', 'tools/test-results/')):
                    raise ValueError('UNEXPECTED_ARTIFACT_CONTENT')
                names.append(name)
        stored = (evidence.load_json(archive.read('build/ci/evidence.json'))
                  if 'build/ci/evidence.json' in names else None)
        # Absent revision identifies the original, published WF-005 assembly rule.
        revision = stored.get('assembly_revision', 1) if stored is not None else 2
        report = evidence.assemble(archive.read, names, {'CI_EXPECTED_HEAD': expected_head,
                  'GITHUB_REPOSITORY': repository, 'GITHUB_RUN_ID': str(run['id']),
                  'GITHUB_RUN_ATTEMPT': str(run['run_attempt']),
                  'CI_JOB_STATUS': 'success' if run['conclusion'] == 'success' else 'failure'},
                  assembly_revision=revision)
        if stored is not None:
            if stored != report:
                raise ValueError('STORED_EVIDENCE_DOES_NOT_RECOMPUTE')
        # Known WF-004 bundles without evidence.json can also be independently recomputed.
        report['download_proof'] = {'repository': repository, 'run_id': run['id'], 'attempt': run['run_attempt'],
                                  'artifact_id': artifact['id'], 'artifact_digest': artifact['digest'],
                                  'source': 'supplied run/artifact metadata + SHA-256 verified ZIP',
                                  'archive_executed_or_extracted': False}
        return report


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repository', required=True); parser.add_argument('--run-id', required=True, type=int)
    parser.add_argument('--expected-head', required=True); parser.add_argument('--output', required=True, type=Path)
    parser.add_argument('--zip', type=Path); parser.add_argument('--run-json', type=Path); parser.add_argument('--artifact-json', type=Path)
    args = parser.parse_args(argv)
    try:
        if not re.fullmatch(r'[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+', args.repository) or args.run_id <= 0 or not evidence.SHA.fullmatch(args.expected_head):
            raise ValueError('INVALID_REPOSITORY_RUN_OR_HEAD')
        offline = [args.zip, args.run_json, args.artifact_json]
        if (args.run_json or args.artifact_json) and not all(offline):
            raise ValueError('OFFLINE_PROOF_REQUIRES_ALL_THREE_FILES')
        if args.output.exists() or args.output.is_symlink():
            raise ValueError('OUTPUT_ALREADY_EXISTS')
        args.output.mkdir(parents=True)
        token = os.environ.get('GH_TOKEN') or os.environ.get('GITHUB_TOKEN')
        if all(offline):
            run = evidence.load_json(args.run_json.read_text());artifact = evidence.load_json(args.artifact_json.read_text())
            zip_path = args.zip
            freshness = 'not_checked_offline; compare current remote head before accepting a review'
        else:
            base = 'repos/' + args.repository
            run = evidence.load_json(get(base+'/actions/runs/'+str(args.run_id), token, 4*1024*1024))
            if run['head_sha'] != args.expected_head or run['status'] != 'completed':
                raise ValueError('RUN_HEAD_CHANGED_OR_NOT_COMPLETED')
            head_repo = run['head_repository']['full_name']
            if not re.fullmatch(r'[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+', head_repo):
                raise ValueError('INVALID_HEAD_REPOSITORY')
            branch = urllib.parse.quote(run['head_branch'], safe='')
            ref = evidence.load_json(get('repos/'+head_repo+'/git/ref/heads/'+branch, token, 4*1024*1024))
            if ref['object']['sha'] != args.expected_head:
                raise ValueError('REMOTE_HEAD_CHANGED')
            freshness = 'current branch head matched at download time; recheck before merge'
            values = evidence.load_json(get(base+'/actions/runs/'+str(args.run_id)+'/artifacts?per_page=100', token, 4*1024*1024))
            target = 'verify-'+str(args.run_id)+'-'+str(run['run_attempt'])
            found = [a for a in values['artifacts'] if a['name']==target]
            if len(found)!=1 or not isinstance(found[0]['id'],int):
                raise ValueError('EXACT_ATTEMPT_ARTIFACT_MISSING_OR_AMBIGUOUS')
            artifact = found[0]
            if args.zip:
                zip_path = args.zip
            else:
                zip_path = args.output/'artifact.zip'
                zip_path.write_bytes(get(base+'/actions/artifacts/'+str(artifact['id'])+'/zip', token))
        if run['id'] != args.run_id:
            raise ValueError('REQUESTED_RUN_MISMATCH')
        report = validate(zip_path, run, artifact, args.expected_head, args.repository)
        report['download_proof']['head_freshness'] = freshness
        report['download_proof']['metadata_origin'] = 'supplied files, not authenticated in this invocation' if all(offline) else 'GitHub REST read in this invocation'
        report['download_proof']['archive_origin'] = 'local supplied ZIP' if args.zip else 'GitHub REST artifact download'
        (args.output/'run.json').write_text(json.dumps(run,indent=2)+'\n')
        (args.output/'artifact.json').write_text(json.dumps(artifact,indent=2)+'\n')
        (args.output/'evidence.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
        (args.output/'handoff.md').write_text(evidence.markdown(report))
        print(json.dumps({'status':report['status'],'head_sha':report['head_sha'],'output':str(args.output)}))
        return 0 if report['status']=='passed' else 1
    except (OSError, ValueError, KeyError, TypeError, AttributeError, IndexError, zipfile.BadZipFile, ET.ParseError) as error:
        # Fixed diagnostic code only; a URL exception may contain a signed query string.
        code = str(error) if isinstance(error,ValueError) and re.fullmatch(r'[A-Z0-9_]+',str(error)) else 'INVALID_OR_UNREADABLE_EVIDENCE'
        print('EVIDENCE_DOWNLOAD_REFUSED: '+code, file=sys.stderr)
        return 2


if __name__=='__main__':sys.exit(main())
