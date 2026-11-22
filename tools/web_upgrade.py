"""Explicit immutable release preparation; never deploys, stops services or edits browser data."""
import hashlib
import json
from pathlib import Path
import re
import shutil
import tempfile
import urllib.parse
import web_release as R


def origin(value):
    R.require(isinstance(value, str) and all(32 < ord(c) < 127 for c in value), 'INVALID_ORIGIN')
    parts = urllib.parse.urlsplit(value)
    R.require(parts.scheme in ('http', 'https') and parts.hostname is not None
              and parts.username is None and parts.password is None
              and parts.path in ('', '/') and not parts.query and not parts.fragment
              and '?' not in value and '#' not in value and '\\' not in value
              and not parts.netloc.endswith(':'), 'INVALID_ORIGIN')
    host = parts.hostname
    # Reject non-canonical numeric IPv4 aliases (127.1, decimal/hex integers),
    # which browsers normalize differently. Public hosts require ordinary DNS labels.
    label = r'[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?'
    dns = re.fullmatch(r'(?:' + label + r'\.)+[a-z](?:[a-z0-9-]{0,61}[a-z0-9])?', host)
    R.require(host in ('localhost', '127.0.0.1') or dns, 'NONCANONICAL_HOST')
    R.require(parts.scheme == 'https' or host in ('localhost', '127.0.0.1'), 'PUBLIC_HTTP_FORBIDDEN')
    port = parts.port if parts.port is not None else (443 if parts.scheme == 'https' else 80)
    R.require(1 <= port <= 65535, 'INVALID_ORIGIN')
    return parts.scheme + '://' + host + (':' + str(port) if port != (443 if parts.scheme == 'https' else 80) else '')


def acceptance_output(root, value):
    root = R.no_links(root)
    output = R.no_links(value)
    try:
        relative = output.relative_to(root/'build')
    except ValueError:
        raise ValueError('ACCEPTANCE_OUTPUT_REFUSED')
    R.require(str(relative) != '.', 'ACCEPTANCE_OUTPUT_REFUSED')
    return output


def snapshot(archive, expected_sha256, target):
    R.require(isinstance(expected_sha256, str) and R.HEX.fullmatch(expected_sha256), 'OFFICIAL_DIGEST_REQUIRED')
    archive = R.no_links(archive)
    R.require(archive.is_file() and archive.stat().st_size <= R.MAX_ARCHIVE, 'INVALID_ARCHIVE_SIZE')
    digest = hashlib.sha256(); size = 0
    with archive.open('rb') as source, target.open('xb') as output:
        while True:
            block = source.read(R.MIB)
            if not block: break
            size += len(block); R.require(size <= R.MAX_ARCHIVE, 'INVALID_ARCHIVE_SIZE')
            digest.update(block); output.write(block)
    R.require(digest.hexdigest() == expected_sha256, 'ARCHIVE_DIGEST_MISMATCH')
    return R.check_release(target)


def prepare(current, current_sha256, candidate, candidate_sha256, current_origin, candidate_origin, output):
    old_origin, new_origin = origin(current_origin), origin(candidate_origin)
    output = R.no_links(output)
    R.require(not output.exists(), 'OUTPUT_EXISTS')
    R.require(current_sha256 != candidate_sha256, 'SAME_RELEASE')
    # Only authenticated private snapshots are checked/extracted; source replacement cannot bypass the digest.
    with tempfile.TemporaryDirectory(prefix='vocr-upgrade-snapshots-') as temporary:
        temporary = Path(temporary)
        old = snapshot(current, current_sha256, temporary/'current.zip')
        new = snapshot(candidate, candidate_sha256, temporary/'candidate.zip')
        output.mkdir(parents=True, mode=0o700)
        old_dir = R.extract_checked(temporary/'current.zip', output/'current')
        new_dir = R.extract_checked(temporary/'candidate.zip', output/'candidate')
        plan = {'schema': 1, 'state': 'PREPARED_NOT_STARTED',
                'current': {'source_sha': old['source']['sha'], 'archive_sha256': current_sha256, 'directory': str(old_dir)},
                'candidate': {'source_sha': new['source']['sha'], 'archive_sha256': candidate_sha256, 'directory': str(new_dir)},
                'current_origin': old_origin, 'candidate_origin': new_origin, 'origin_changed': old_origin != new_origin,
                'active_games_survive_restart': False, 'browser_data_automatically_migrated': False,
                'same_origin_preservation_requires_pair_acceptance': True,
                'rollback_compatibility': 'UNPROVEN_UNTIL_PAIR_ACCEPTANCE',
                'services_started_or_stopped': False, 'browser_data_touched': False}
        (output/'transition-plan.json').write_bytes(R.json_bytes(plan))
        return plan
