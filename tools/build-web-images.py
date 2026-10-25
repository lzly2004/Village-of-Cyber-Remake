#!/usr/bin/env python3
"""WEB-022: deterministic selection, sequential conversion and fail-closed verification.

Requires Pillow >= 10 with WebP; no Java, network, original edits or runtime UI.
Build refuses an existing output directory. Check never writes.
"""
import argparse
from collections import Counter
import hashlib
import gzip
import importlib.util
import io
import json
import math
from pathlib import Path
import shutil
import sys
import tempfile

import PIL
from PIL import Image, ImageChops, ImageStat, features

SPEC = importlib.util.spec_from_file_location('source_audit', Path(__file__).with_name('audit-web-source-assets.py'))
AUDIT = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(AUDIT)
INPUTS = ('src/main/java/com/vocr/resources/UiImage.java',
          'src/main/resources/config/event_image_mapping.json')
BOUNDARIES = ('14day.png', '50day.png', 'yaku1_6.png', 'yaku2_5.png', 'yaku3_5.png',
              'result1_6white.png', 'result2_5white.png', 'result3_5.png', 'uranai6.png', 'goei5.png')
POLICY = {'version': 1, 'format': 'webp', 'method': 6, 'lossyQualities': [90, 96],
          'minCompositePsnr': 35, 'imageBudget': 32 * 1024**2,
          'singleBudget': 512 * 1024, 'manifestBudget': 1024**2,
          'largeUiLossyExceptions': ['images/title_base_resized.png']}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def input_hashes(root):
    # Source PNG/output hashes remain raw bytes. Only text inputs tolerate checkout newlines.
    return {name: sha((root / name).read_text(encoding='utf-8').encode('utf-8')) for name in INPUTS}


def source_catalog(root):
    refs, *_ = AUDIT.image_reference_catalog(root)
    require(refs, 'Empty image selection')
    # Never normalize case or accept a symlink outside the canonical directory.
    base = (root / 'src/main/resources/images').resolve()
    exact_names = {p.name for p in base.iterdir()}
    for name in refs:
        path = root / 'src/main/resources' / name
        require(path.is_file() and not path.is_symlink() and path.resolve().parent == base,
                'Missing or unsafe source: ' + name)
        require(path.name in exact_names, 'Source case mismatch: ' + name)
    return refs


def boundary_state(root):
    return {name: (root / 'src/main/resources/images' / name).is_file() for name in BOUNDARIES}


def lossless_required(name, origins):
    # Preserve small text/UI and portraits exactly; only the audited large background is exempt.
    return name not in POLICY['largeUiLossyExceptions'] and any(
        origin != 'event-image mapping' for origin in origins)


def decode(data, expected_format):
    with Image.open(io.BytesIO(data)) as image:
        require(image.format == expected_format and getattr(image, 'n_frames', 1) == 1,
                'Wrong format or animated input')
        image.load()  # Full decode, not just IHDR/header acceptance.
        return image.convert('RGBA')


def composite_psnr(original, converted):
    values = []
    for background in ('black', 'white'):
        a = Image.new('RGBA', original.size, background)
        b = a.copy()
        a.alpha_composite(original)
        b.alpha_composite(converted)
        rms = ImageStat.Stat(ImageChops.difference(a.convert('RGB'), b.convert('RGB'))).rms
        mse = sum(x*x for x in rms) / 3
        values.append(99.0 if not mse else 10 * math.log10(255**2 / mse))
    return min(values)


def validate_pixels(original, converted, lossless):
    require(original.size == converted.size, 'Dimension mismatch')
    require(original.getchannel('A').tobytes() == converted.getchannel('A').tobytes(),
            'Alpha channel mismatch')
    if lossless:
        require(original.tobytes() == converted.tobytes(), 'Lossless RGBA mismatch')
    score = composite_psnr(original, converted)
    require(score >= POLICY['minCompositePsnr'], 'Composite PSNR below quality floor')
    return round(score, 4)


def encode(original, lossless, quality):
    output = io.BytesIO()
    original.save(output, format='WEBP', lossless=lossless, quality=quality,
                  method=POLICY['method'], exact=True, alpha_quality=100)
    return output.getvalue()


def convert(original, exact):
    if exact:
        data = encode(original, True, 100)
        return data, True, 100, validate_pixels(original, decode(data, 'WEBP'), True)
    for quality in POLICY['lossyQualities']:
        data = encode(original, False, quality)
        converted = decode(data, 'WEBP')
        if composite_psnr(original, converted) >= POLICY['minCompositePsnr']:
            return data, False, quality, validate_pixels(original, converted, False)
    data = encode(original, True, 100)
    return data, True, 100, validate_pixels(original, decode(data, 'WEBP'), True)


def manifest_bytes(manifest):
    return (json.dumps(manifest, ensure_ascii=False, sort_keys=True, separators=(',', ':')) + '\n').encode()


def check(root, output):
    require(output.is_dir() and not output.is_symlink(), 'Missing or unsafe output directory')
    raw = (output / 'manifest.json').read_bytes()
    manifest = json.loads(raw)
    refs = source_catalog(root)
    require(manifest['schemaVersion'] == 1 and manifest['policy'] == POLICY, 'Manifest schema/policy mismatch')
    require(manifest.get('selectionInputNormalization') == 'utf8-lf' and manifest['selectionInputs'] == input_hashes(root),
            'Selection input changed; regenerate images')
    require(manifest['boundarySamplesExist'] == boundary_state(root), 'Boundary source state changed')
    entries = manifest['images']
    require(set(entries) == set(refs), 'Selected image set mismatch')
    expected_files = {'manifest.json'}
    sizes = {}
    modes = Counter()
    for name, entry in entries.items():
        source = (root / 'src/main/resources' / name).read_bytes()
        require(entry['sourceSha256'] == sha(source) and entry['sourceBytes'] == len(source), 'Source drift: ' + name)
        require(entry['origins'] == sorted(refs[name]), 'Origin mismatch: ' + name)
        digest = entry['sha256']
        require(isinstance(digest, str) and len(digest) == 64 and all(c in '0123456789abcdef' for c in digest),
                'Invalid output digest: ' + name)
        filename = digest + '.webp'
        require(entry['file'] == filename, 'Unsafe or non-content-addressed filename: ' + name)
        path = output / filename
        require(path.is_file() and not path.is_symlink(), 'Missing/unsafe output: ' + name)
        data = path.read_bytes()
        require(sha(data) == digest and len(data) == entry['bytes'], 'Output hash/size mismatch: ' + name)
        original, converted = decode(source, 'PNG'), decode(data, 'WEBP')
        require([original.width, original.height] == entry['size'], 'Manifest dimension mismatch: ' + name)
        require(type(entry['lossless']) is bool and (entry['lossless'] or not lossless_required(name, refs[name])),
                'Exact UI/portrait policy violated: ' + name)
        require(entry['quality'] in ([100] if entry['lossless'] else POLICY['lossyQualities']), 'Invalid encoding quality')
        score = validate_pixels(original, converted, entry['lossless'])
        require(entry['compositePsnr'] == score, 'Quality measurement mismatch: ' + name)
        require(len(data) <= POLICY['singleBudget'], 'Single image budget exceeded: ' + name)
        expected_files.add(filename)
        sizes[filename] = len(data)
        modes['lossless' if entry['lossless'] else 'lossy'] += 1
    actual = {p.name for p in output.iterdir()}
    require(actual == expected_files, 'Unexpected/stale output files')
    require(sum(sizes.values()) <= POLICY['imageBudget'], 'Image bundle budget exceeded')
    require(len(raw) <= POLICY['manifestBudget'], 'Manifest budget exceeded')
    report = {'images': len(entries), 'uniqueFiles': len(sizes), 'sourceBytes': sum(e['sourceBytes'] for e in entries.values()),
              'imageBytes': sum(sizes.values()), 'manifestBytes': len(raw), 'largestImageBytes': max(sizes.values()),
              'encodingCounts': dict(modes), 'minCompositePsnr': min(e['compositePsnr'] for e in entries.values())}
    require(manifest['summary'] == report, 'Summary mismatch')
    return report


def check_distribution(root, dist):
    source_output = root / 'web-client/public/assets/images'
    output = dist / 'assets/images'
    report = check(root, output)
    require((source_output / 'manifest.json').read_bytes() == (output / 'manifest.json').read_bytes(),
            'Distribution manifest differs from public source')
    files = [p for p in dist.rglob('*') if p.is_file()]
    code = [p for p in files if p.suffix in ('.html', '.js', '.css')]
    require(any(p.suffix == '.js' for p in code) and (dist / 'index.html').is_file(), 'Missing frontend build')
    code_bytes = sum(p.stat().st_size for p in code)
    gzip_bytes = sum(len(gzip.compress(p.read_bytes(), mtime=0)) for p in code)
    total = sum(p.stat().st_size for p in files)
    require(code_bytes <= 1024**2 and gzip_bytes <= 512 * 1024, 'Code budget exceeded (includes dialogue)')
    require(total <= 100 * 1024**2, 'Total static budget exceeded')
    return {**report, 'codeBytes': code_bytes, 'codeGzipBytes': gzip_bytes, 'totalStaticBytes': total}


def build(root, output):
    source_root = (root / 'src/main/resources').resolve()
    require(output.resolve() != source_root and source_root not in output.resolve().parents, 'Output must not be inside original resources')
    require(features.check('webp') and tuple(map(int, PIL.__version__.split('.')[:2])) >= (10, 0),
            'Pillow >=10 and WebP codec required')
    require(not output.exists() and not output.is_symlink(), 'Output already exists; build into a fresh directory')
    refs = source_catalog(root)
    output.parent.mkdir(parents=True, exist_ok=True)
    temporary = Path(tempfile.mkdtemp(prefix='.web-images-', dir=output.parent))
    try:
        entries, sizes, modes = {}, {}, Counter()
        for index, (name, origins) in enumerate(sorted(refs.items()), 1):
            source = (root / 'src/main/resources' / name).read_bytes()
            original = decode(source, 'PNG')
            data, lossless, quality, score = convert(original, lossless_required(name, origins))
            require(len(data) <= POLICY['singleBudget'], 'Single image budget exceeded: ' + name)
            filename = sha(data) + '.webp'
            (temporary / filename).write_bytes(data)
            sizes[filename] = len(data)
            modes['lossless' if lossless else 'lossy'] += 1
            entries[name] = {'sourceSha256': sha(source), 'sourceBytes': len(source), 'origins': sorted(origins),
                             'sha256': sha(data), 'file': filename, 'bytes': len(data),
                             'size': list(original.size), 'lossless': lossless, 'quality': quality, 'compositePsnr': score}
            if index % 100 == 0:
                print(f'Converted {index}/{len(refs)}', file=sys.stderr, flush=True)
        summary = {'images': len(entries), 'uniqueFiles': len(sizes), 'sourceBytes': sum(e['sourceBytes'] for e in entries.values()),
                   'imageBytes': sum(sizes.values()), 'manifestBytes': 0, 'largestImageBytes': max(sizes.values()),
                   'encodingCounts': dict(modes), 'minCompositePsnr': min(e['compositePsnr'] for e in entries.values())}
        manifest = {'schemaVersion': 1, 'policy': POLICY, 'encoder': {'pillow': PIL.__version__, 'libwebp': features.version('webp')},
                    'selectionInputNormalization': 'utf8-lf', 'selectionInputs': input_hashes(root),
                    'boundarySamplesExist': boundary_state(root), 'images': entries, 'summary': summary}
        # The byte count includes itself; converge before hashing/publishing the manifest.
        while summary['manifestBytes'] != len(manifest_bytes(manifest)):
            summary['manifestBytes'] = len(manifest_bytes(manifest))
        (temporary / 'manifest.json').write_bytes(manifest_bytes(manifest))
        report = check(root, temporary)
        temporary.rename(output)
        return report
    finally:
        if temporary.exists():
            shutil.rmtree(temporary)  # Only the directory made by this invocation.


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=('build', 'check', 'distribution'))
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument('--output', type=Path)
    parser.add_argument('--dist', type=Path)
    args = parser.parse_args()
    root = args.root.resolve()
    output = args.output or root / 'web-client/public/assets/images'
    try:
        report = (check_distribution(root, args.dist or root / 'web-client/dist') if args.command == 'distribution'
                  else (build if args.command == 'build' else check)(root, output))
        print(json.dumps(report, ensure_ascii=False, sort_keys=True))
    except (ValueError, OSError, KeyError, TypeError) as error:
        print('WEB_IMAGES_ERROR: ' + str(error), file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
