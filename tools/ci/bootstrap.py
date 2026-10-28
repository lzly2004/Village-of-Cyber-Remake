#!/usr/bin/env python3
"""CI-only user-directory tools. Cached archives must pass hashes again before use."""
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import shutil
import subprocess
import sys
import tarfile
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[2]


def digest(path):
    value = hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
            value.update(chunk)
    return value.hexdigest()


def archive(spec, target, opener=urllib.request.urlopen):
    target.parent.mkdir(parents=True, exist_ok=True)
    if target.is_symlink():
        raise ValueError('ARCHIVE_SYMLINK_REFUSED')
    if not target.exists():
        temporary = target.with_suffix('.partial')
        if temporary.exists() or temporary.is_symlink():
            raise ValueError('PARTIAL_ARCHIVE_ALREADY_EXISTS')
        try:
            with opener(spec['url'], timeout=90) as response, temporary.open('xb') as out:
                total = 0
                while True:
                    chunk = response.read(1024 * 1024)
                    if not chunk:
                        break
                    total += len(chunk)
                    if total > 256 * 1024 * 1024:
                        raise ValueError('ARCHIVE_EXCEEDS_BOUND')
                    out.write(chunk)
            if digest(temporary) != spec['sha256']:
                raise ValueError('ARCHIVE_HASH_MISMATCH')
            temporary.rename(target)
        finally:
            if temporary.is_file() and not temporary.is_symlink():
                temporary.unlink()
    if digest(target) != spec['sha256']:
        raise ValueError('CACHED_ARCHIVE_HASH_MISMATCH')
    return target


def gradle(zip_path, directory, version):
    prefix = 'gradle-' + version
    destination = directory / prefix
    if destination.exists() or destination.is_symlink():
        raise ValueError('GRADLE_OUTPUT_ALREADY_EXISTS')
    with zipfile.ZipFile(zip_path) as package:
        for entry in package.infolist():
            parts = PurePosixPath(entry.filename).parts
            if (not parts or parts[0] != prefix or '..' in parts or
                    '\\' in entry.filename or entry.filename.startswith('/') or
                    (entry.external_attr >> 16) & 0o170000 == 0o120000):
                raise ValueError('INVALID_GRADLE_ARCHIVE_PATH')
        package.extractall(directory)
    binary = destination / 'bin/gradle'
    if not binary.is_file():
        raise ValueError('GRADLE_LAUNCHER_MISSING')
    binary.chmod(0o755)
    return binary


def lfs(tar_path, binary, spec):
    with tarfile.open(tar_path) as package:
        member = package.getmember('git-lfs-' + spec['version'] + '/git-lfs')
        if not member.isfile():
            raise ValueError('LFS_BINARY_NOT_REGULAR')
        data = package.extractfile(member).read()
    if hashlib.sha256(data).hexdigest() != spec['binary_sha256']:
        raise ValueError('LFS_BINARY_HASH_MISMATCH')
    binary.parent.mkdir(parents=True, exist_ok=True)
    if binary.exists() or binary.is_symlink():
        raise ValueError('LFS_OUTPUT_ALREADY_EXISTS')
    binary.write_bytes(data)
    binary.chmod(0o755)
    return binary


def append_environment(file, values):
    if any('\n' in v or '\r' in v for v in values.values()):
        raise ValueError('INVALID_ENVIRONMENT_PATH')
    with file.open('a', encoding='utf-8') as stream:
        for key, value in values.items():
            stream.write(key + '=' + value + '\n')


def git_environment():
    version = subprocess.check_output(['git', '--version'], cwd=ROOT, text=True).strip()
    config = subprocess.run(['git', 'config', '--get', 'diff.renameLimit'],
                            cwd=ROOT, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if config.returncode not in (0, 1):
        raise ValueError('GIT_RENAME_LIMIT_QUERY_FAILED')
    limit = int(config.stdout.strip()) if config.returncode == 0 else None
    if limit is not None and limit < 0:
        raise ValueError('INVALID_GIT_RENAME_LIMIT')
    return {'git_version': version, 'git_diff_rename_limit': limit}


def main():
    try:
        if os.environ.get('GITHUB_ACTIONS') != 'true':
            raise ValueError('GITHUB_ACTIONS_CONTEXT_REQUIRED')
        lock = json.loads((ROOT / 'tools/ci/toolchain.json').read_text())
        directory = ROOT / 'build/ci'
        downloads, tools = directory / 'downloads', directory / 'toolchain'
        tools.mkdir(parents=True, exist_ok=True)
        env, report = {}, {'python': sys.version, 'runner_image_version': os.environ.get('ImageVersion'),
                           'toolchain_lock_sha256': digest(ROOT / 'tools/ci/toolchain.json')}
        report.update(git_environment())
        if '.'.join(map(str, sys.version_info[:3])) != lock['python']:
            raise ValueError('PYTHON_VERSION_MISMATCH')
        if os.environ.get('CI_NEEDS_LFS') == 'true':
            spec = lock['git_lfs']
            package = archive(spec, downloads / ('git-lfs-' + spec['version'] + '.tar.gz'))
            binary = lfs(package, tools / 'bin/git-lfs', spec)
            with open(os.environ['GITHUB_PATH'], 'a') as out:
                out.write(str(binary.parent) + '\n')
            report['git_lfs_sha256'] = digest(binary)
        if os.environ.get('CI_NEEDS_NODE') == 'true':
            actual = subprocess.check_output(['node', '--version'], text=True).strip()
            if actual != 'v' + lock['node']:
                raise ValueError('NODE_VERSION_MISMATCH')
            report['node'] = actual
            package = json.loads((ROOT / 'web-client/package-lock.json').read_text())
            if package['packages']['node_modules/@playwright/test']['version'] != lock['playwright']:
                raise ValueError('PLAYWRIGHT_VERSION_MISMATCH')
        if os.environ.get('CI_NEEDS_JAVA') == 'true':
            release = Path(os.environ['JAVA_HOME']) / 'release'
            text = release.read_text()
            actual = re.search(r'^JAVA_VERSION="([^"]+)"', text, re.M).group(1)
            if actual != lock['java']:
                raise ValueError('JAVA_VERSION_MISMATCH')
            report['java_release'] = text
            spec = lock['gradle']
            package = archive(spec, downloads / ('gradle-' + spec['version'] + '-bin.zip'))
            binary = gradle(package, tools, spec['version'])
            env['VOCR_GRADLE'] = str(binary)
            report['gradle_archive_sha256'] = digest(package)
        if os.environ.get('CI_NEEDS_PIP') == 'true':
            import PIL
            import imageio_ffmpeg
            import yaml
            if (PIL.__version__ != lock['pillow'] or imageio_ffmpeg.__version__ != lock['imageio_ffmpeg'] or
                    yaml.__version__ != '6.0.3'):
                raise ValueError('PINNED_PYTHON_TOOL_VERSION_MISMATCH')
            ffmpeg = Path(imageio_ffmpeg.get_ffmpeg_exe())
            if digest(ffmpeg) != lock['ffmpeg_sha256']:
                raise ValueError('FFMPEG_BINARY_HASH_MISMATCH')
            env['FFMPEG'] = str(ffmpeg)
            report['ffmpeg_sha256'] = digest(ffmpeg)
            report['pillow'] = PIL.__version__
        if os.environ.get('CI_NEEDS_HOSTING') == 'true':
            import importlib.util
            spec = importlib.util.spec_from_file_location('ci_hosting', ROOT / 'tools/web_hosting.py')
            hosting = importlib.util.module_from_spec(spec); spec.loader.exec_module(hosting)
            binary = hosting.install(downloads / ('caddy-' + json.loads(hosting.LOCK.read_text())['version']))
            env['VOCR_CADDY'] = str(binary)
            report['caddy_version'] = json.loads(hosting.LOCK.read_text())['version']
            report['caddy_sha256'] = digest(binary)
            report['hosting_toolchain_sha256'] = digest(hosting.LOCK)
        append_environment(Path(os.environ['GITHUB_ENV']), env)
        (directory / 'tools.json').write_text(json.dumps(report, indent=2) + '\n')
        return 0
    except (OSError, ValueError, KeyError, AttributeError, ImportError, tarfile.TarError, subprocess.SubprocessError) as error:
        print(json.dumps({'error': 'CI_TOOLCHAIN_REFUSED', 'reason': str(error)}), file=sys.stderr)
        return 2


if __name__ == '__main__':
    sys.exit(main())
