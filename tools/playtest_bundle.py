"""Portable stdlib-only playtest tool bundle. No runtime binaries or user data."""
from pathlib import Path
import web_release as R

FILES = ('playtest.py', 'playtest.sh', 'playtest_candidate.py', 'playtest_store.py',
         'playtest_runtime.py', 'playtest_service.py', 'guarded-process.py',
         'web_release.py', 'web_hosting.py', 'hosting-toolchain.json', 'host-web.py',
         'ci/download.py', 'ci/evidence.py', 'ci/bootstrap.py', 'ci/toolchain.json')


def bundle(root, output):
    root = Path(root); output = R.no_links(output)
    files = {'tools/' + n: R.no_links(root / 'tools' / n).read_bytes() for n in FILES}
    files['playtest.sh'] = b'#!/usr/bin/env bash\nset -euo pipefail\ncd -- "$(dirname -- "${BASH_SOURCE[0]}")"\nexec python3 -B tools/playtest.py "$@"\n'
    files['README.txt'] = ('Internal Linux complete-game playtest tool. Run bash playtest.sh.\n'
        'First configure Java21+, Chromium and the hashed Caddy named in tools/hosting-toolchain.json.\n'
        'No system installation, token collection, automatic updates or uploads.\n'
        'Import candidate with first-party run/artifact identity + evidence + receipt.\n'
        'Ctrl+C stops only owned services; active game expires. Close browser normally to finish local data writes.\n'
        'This HTTP tool does not validate HTTPS/SAN/public hosting or release manual gates.\n').encode()
    files['SHA256SUMS'] = ''.join(R.sha(b) + '  ' + n + '\n' for n, b in sorted(files.items())).encode()
    R.write_archive(files, output)
    R.require(R.read_archive(output) == files, 'TOOL_BUNDLE_CHANGED')
    return {'bytes': output.stat().st_size, 'sha256': R.hash_file(output)}
