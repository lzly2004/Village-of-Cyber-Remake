"""Local, explicit import and feedback. No tokens, network, user-data migration or deletion."""
import datetime
import importlib.util
import os
from pathlib import Path
import re
import shutil
import stat
import sys
import tempfile
import uuid
import zipfile
import playtest_candidate as P
import web_release as R

TOOLS = Path(__file__).resolve().parent
REPOSITORY = 'lzly2004/Village-of-Cyber-Remake'
KEY = re.compile(r'(full|preview)-[0-9a-f]{40}-[0-9a-f]{64}\Z')


def private_directory(path):
    path = R.no_links(path)
    path.mkdir(parents=True, exist_ok=True, mode=0o700)
    R.require(path.is_dir() and path.stat().st_uid == os.getuid()
              and path.stat().st_mode & 0o077 == 0, 'PRIVATE_DIRECTORY_REQUIRED')
    return path


def home_path(value=None):
    return private_directory(value or Path.home() / '.local/share/vocr-playtest')


def atomic_json(path, value):
    path = R.no_links(path); private_directory(path.parent)
    fd, temp = tempfile.mkstemp(prefix='.state-', dir=path.parent)
    try:
        with os.fdopen(fd, 'wb') as stream: stream.write(R.json_bytes(value))
        os.replace(temp, path)
    finally:
        if os.path.exists(temp): os.unlink(temp)


def evidence_reader():
    # The trusted tool bundle carries these two existing, independently tested readers.
    for name in ('evidence', 'download'):
        spec = importlib.util.spec_from_file_location(name, TOOLS / 'ci' / (name + '.py'))
        module = importlib.util.module_from_spec(spec); sys.modules[name] = module; spec.loader.exec_module(module)
    return sys.modules['download']


def artifact_identity(path, row, run, prefix):
    R.require(type(row.get('id')) is int and row['id'] > 0 and row.get('expired') is False
              and row['workflow_run']['id'] == run['id'] and row['workflow_run']['head_sha'] == run['head_sha']
              and row['name'] == prefix + '-' + str(run['id']) + '-' + str(run['run_attempt'])
              and row['size_in_bytes'] == path.stat().st_size
              and row['digest'] == 'sha256:' + R.hash_file(path), 'ARTIFACT_IDENTITY_MISMATCH')


def outer_files(path, allowed, maximum=256 * R.MIB):
    R.require(path.stat().st_size <= maximum, 'ARTIFACT_TOO_LARGE')
    with zipfile.ZipFile(path) as z:
        infos = z.infolist(); names = [R.safe_name(i.filename) for i in infos]
        R.require(len(names) == len(set(n.casefold() for n in names)) and set(names) <= allowed
                  and sum(i.file_size for i in infos) <= maximum and all(not i.is_dir()
                  and stat.S_IFMT(i.external_attr >> 16) in (0, stat.S_IFREG) and not i.flag_bits & 1
                  and i.file_size <= maximum for i in infos), 'UNSAFE_ARTIFACT_ENTRIES')
        return {i.filename: z.read(i) for i in infos}


def import_candidate(home, artifact, verify, receipt, metadata, confirmed_page=False):
    home = home_path(home)
    paths = [R.no_links(p) for p in (artifact, verify, receipt, metadata)]
    artifact, verify, receipt, metadata = paths
    m = R.load_json(metadata.read_bytes())
    R.require(m.get('schema') == 1 and m.get('repository') == REPOSITORY and confirmed_page is True,
              'SOURCE_UNCONFIRMED')
    R.require(m.get('confirmation') == 'user-confirmed-page', 'SOURCE_UNCONFIRMED')
    run = m['run']
    R.require(run['repository']['full_name'] == REPOSITORY and run['status'] == 'completed'
              and run['conclusion'] == 'success' and type(run['run_attempt']) is int and run['run_attempt'] > 0,
              'SUCCESSFUL_RUN_REQUIRED')
    artifact_identity(verify, m['verify'], run, 'verify')
    artifact_identity(receipt, m['receipt'], run, 'receipt')
    report = evidence_reader().validate(verify, run, m['verify'], run['head_sha'], REPOSITORY)
    R.require(report['status'] == 'passed' and report['tested_sha'] == run['head_sha'], 'EVIDENCE_NOT_PASSED')
    rows = outer_files(receipt, {'receipt.json'}, R.MIB)
    r = R.load_json(rows['receipt.json'])
    R.require(r['repository'] == REPOSITORY and r['tested_head'] == run['head_sha'] and str(r['run_id']) == str(run['id'])
              and str(r['attempt']) == str(run['run_attempt']) and str(r['id']) == str(m['verify']['id'])
              and r['digest'] == R.hash_file(verify), 'RECEIPT_MISMATCH')
    kind = m['kind']; R.require(kind in ('full', 'preview'), 'INVALID_CANDIDATE_KIND')
    prefix = 'release' if kind == 'full' else 'preview'
    artifact_identity(artifact, m['package'], run, prefix)
    base = 'release' if kind == 'full' else 'preview'
    archive_name = 'vocr-web.zip' if kind == 'full' else 'vocr-preview.zip'
    data = outer_files(artifact, {archive_name, base + '-report.json', base + '-manifest.json', 'playtest-tools.zip'})
    R.require({archive_name, base + '-report.json', base + '-manifest.json'} <= set(data), 'PACKAGE_SIDECAR_MISSING')
    package_report = R.load_json(data[base + '-report.json'])
    R.require(package_report['status'] == 'passed' and package_report['source_sha'] == run['head_sha']
              and package_report['archive'] == {'name': archive_name, 'bytes': len(data[archive_name]), 'sha256': R.sha(data[archive_name])},
              'PACKAGE_REPORT_MISMATCH')
    bound = report.get('web_release' if kind == 'full' else 'web_preview') or {}
    R.require(bound.get('status') == 'passed' and bound.get('report') == package_report
              and str(bound['artifact']['id']) == str(m['package']['id'])
              and bound['artifact']['digest'] == R.hash_file(artifact), 'PACKAGE_NOT_BOUND_TO_EVIDENCE')
    key = kind + '-' + run['head_sha'] + '-' + R.sha(data[archive_name])
    parent = private_directory(home / 'candidates'); target = parent / key
    R.require(not target.exists(), 'CANDIDATE_ALREADY_IMPORTED')
    temporary = Path(tempfile.mkdtemp(prefix='.import-', dir=parent))
    try:
        archive = temporary / archive_name; archive.write_bytes(data[archive_name])
        actual_kind, manifest, files = P.check_candidate(archive)
        R.require(actual_kind == kind and manifest['source']['sha'] == run['head_sha']
                  and manifest['source']['fingerprints'] == report['fingerprints']
                  and manifest['verification']['suites'] == [{'id': s['id'], 'status': s['status']} for s in report['suites']]
                  and files[base + '-manifest.json'] == data[base + '-manifest.json']
                  and package_report['manifest_sha256'] == R.sha(files[base + '-manifest.json'])
                  and package_report['verification'] == manifest['verification'], 'PACKAGE_SOURCE_MISMATCH')
        payload = temporary / 'payload'; payload.mkdir(mode=0o700)
        for name, content in files.items():
            path = payload / name; path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(content); path.chmod(0o500 if name == 'backend/run.sh' else 0o400)
        provenance = {'schema': 1, 'key': key, 'kind': kind, 'sourceSha': run['head_sha'],
                      'run': run['id'], 'attempt': run['run_attempt'], 'repository': REPOSITORY,
                      'archive': {'name': archive_name, 'sha256': R.sha(data[archive_name])},
                      'metadataOrigin': 'user-confirmed-page', 'confirmedAt': datetime.datetime.now(datetime.timezone.utc).isoformat(),
                      'metadataSha256': R.hash_file(metadata), 'verifySha256': R.hash_file(verify),
                      'receiptSha256': R.hash_file(receipt), 'artifactSha256': R.hash_file(artifact),
                      'freshness': 'import-time confirmation; cached integrity is not a live GitHub check'}
        atomic_json(temporary / 'candidate.json', provenance)
        (temporary / 'source-metadata.json').write_bytes(metadata.read_bytes())
        archive.chmod(0o400)
        temporary.rename(target)
        return key
    finally:
        if temporary.exists(): shutil.rmtree(temporary)


def candidate(home, key):
    R.require(isinstance(key, str) and KEY.fullmatch(key), 'INVALID_CANDIDATE_KEY')
    folder = R.no_links(home_path(home) / 'candidates' / key)
    meta = R.load_json(R.no_links(folder / 'candidate.json').read_bytes())
    expected_name = 'vocr-web.zip' if key.startswith('full-') else 'vocr-preview.zip'
    R.require(meta['archive']['name'] == expected_name, 'CANDIDATE_CHANGED')
    archive = folder / meta['archive']['name']
    R.require(meta['key'] == key and R.hash_file(archive) == meta['archive']['sha256'], 'CANDIDATE_CHANGED')
    kind, manifest, _ = P.check_candidate(archive)
    R.require(key == kind + '-' + manifest['source']['sha'] + '-' + R.hash_file(archive)
              and P.check_extracted(folder / 'payload') == manifest, 'CANDIDATE_CHANGED')
    return meta, folder / 'payload'


def profiles(home, key):
    R.require(KEY.fullmatch(key), 'INVALID_CANDIDATE_KEY')
    parent = private_directory(home_path(home) / 'profiles')
    # A stopped service may leave its browser open at the same fixed origin.
    # Refuse switching candidates until every managed browser has closed normally.
    for existing in parent.iterdir():
        if KEY.fullmatch(existing.name):
            chromium = R.no_links(R.no_links(existing) / 'chromium')
            R.require(not os.path.lexists(chromium / 'SingletonLock'), 'PROFILE_IN_USE')
    root = private_directory(parent / key)
    profile = private_directory(root / 'chromium'); downloads = private_directory(root / 'downloads')
    # Chromium's SingletonLock is a symlink, including when its target is absent.
    R.require(not os.path.lexists(profile / 'SingletonLock'), 'PROFILE_IN_USE')
    default = profile / 'Default'
    if not default.exists():
        private_directory(default)
        atomic_json(default / 'Preferences', {'download': {'default_directory': str(downloads), 'prompt_for_download': True}})
    return profile, downloads


def feedback(home, key, attachments=(), browser_version='not recorded'):
    meta, _ = candidate(home, key)
    statuses = sorted((home_path(home) / 'runs').glob('*/status.json'), key=lambda p: p.stat().st_mtime, reverse=True)
    for status in statuses:
        recorded = R.load_json(R.no_links(status).read_bytes())
        if recorded.get('candidate') == key:
            browser_version = recorded.get('browserVersion', browser_version); break
    parent = private_directory(home_path(home) / 'feedback')
    dest = private_directory(parent / uuid.uuid4().hex)
    attached = private_directory(dest / 'attachments'); rows = []; total = 0
    for value in attachments:
        source = R.no_links(value); size = source.stat().st_size; total += size
        R.require(source.is_file() and size <= 32 * R.MIB and total <= 64 * R.MIB, 'ATTACHMENT_LIMIT')
        name = str(len(rows) + 1) + '-' + R.safe_name(source.name)
        shutil.copyfile(source, attached / name)
        rows.append({'name': 'attachments/' + name, 'bytes': size, 'sha256': R.hash_file(attached / name)})
    value = {'schema': 1, 'kind': 'vocr-playtest-feedback', 'candidate': {k: meta[k] for k in ('key', 'kind', 'sourceSha', 'run', 'attempt')},
             'environment': {'os': sys.platform, 'architecture': os.uname().machine, 'browserVersion': browser_version},
             'play': {'configuration': '', 'completion': 'not_started'},
             **{n: {'status': 'not_checked', 'comment': ''} for n in ('visual', 'sound', 'controls')},
             'steps': [], 'attachments': rows,
             'limitations': ['User fills conclusions; not auto-approved.', 'Attachments may contain hidden roles or private file information; review before sharing.',
                             'No recording, microphone collection, credential collection or automatic upload.', 'Backend restart invalidates active games; local history is separate.']}
    path = dest / 'feedback.json'; atomic_json(path, value); return path
