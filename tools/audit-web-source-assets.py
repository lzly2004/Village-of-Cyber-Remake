#!/usr/bin/env python3
"""WEB-020 read-only source inventory; no conversion, hashes, or runtime manifest.

The dynamic image ranges below describe the audited source set, not a proof
that all possible rule/replay inputs are covered. Missing boundary samples
are reported separately and do not count as established runtime defects.
"""
import argparse
from collections import Counter, defaultdict
import json
from pathlib import Path
import re
import struct
import wave


def audit(root):
    resources = root / 'src/main/resources'
    java = root / 'src/main/java/com/vocr/resources'
    files = sorted(path for path in resources.rglob('*') if path.is_file())
    if not files:
        raise ValueError('No source resources found')
    groups = defaultdict(list)
    for path in files:
        parts = path.relative_to(resources).parts
        groups[parts[0] if len(parts) > 1 else '[root]'].append(path)
    group_report = {key: {'files': len(paths), 'bytes': sum(p.stat().st_size for p in paths)}
                    for key, paths in sorted(groups.items())}
    references = defaultdict(set)

    def ref(name, origin, folder='images'):
        if not isinstance(name, str) or not name or Path(name).name != name:
            raise ValueError('Invalid source filename in ' + origin)
        references[folder + '/' + name].add(origin)

    fixed = re.findall(r'^\s+([A-Z_0-9]+)\("([^"\n]+)"\)',
                       (java / 'UiImage.java').read_text(), re.M)
    for key, name in fixed:
        ref(name, 'UiImage.' + key)
    if not fixed:
        raise ValueError('No UiImage entries parsed')
    mapping = json.loads((resources / 'config/event_image_mapping.json').read_text())
    branches = Counter()
    shapes = Counter()
    event_images = set()
    for character, events in mapping.items():
        for event, entry in events.items():
            if event == 'default':
                ref(entry['all'], 'event-image metadata')
                branches['metadata'] += 1
                continue
            for branch, value in [('default', entry['default']),
                                  *[('special', v) for v in entry.get('special', {}).values()]]:
                branches[branch] += 1
                shapes['array' if isinstance(value, list) else 'string'] += 1
                names = value if isinstance(value, list) else [value]
                if not names:
                    raise ValueError('Empty image branch: ' + character + '/' + event)
                for name in names:
                    ref(name, 'event-image mapping')
                    event_images.add(name)
    # Frozen WEB-020 audit domains, explicitly not an exhaustive reachability proof.
    for number in range(1, 45):
        for suffix in ('s', 'gs', 'cs', 'fs', 'hs', 'ws', 'ks'):
            ref(f'{number:02}{suffix}.png', 'ImageNames.portraits')
        ref(f'{number}job.png', 'ImageNames.characterText')
    for number in range(1, 8):
        ref(f'game{number}.png', 'ImageNames.gameSetupButton')
        ref(f'game{number}-1.png', 'ImageNames.gameSetupTitle')
    for number in range(8):
        ref(f'gameTitle{number}.png', 'ImageNames.recordTitle')
    for day in range(1, 14):
        ref(f'{day}day.png', 'ImageNames.voteDay audited days 1-13')
    for role, maximum in ((1, 5), (2, 4), (3, 4)):
        for order in range(1, maximum + 1):
            ref(f'yaku{role}_{order}.png', 'ImageNames.claimedRoleIcon')
            for color in ('white', 'black') if role != 3 else ('',):
                ref(f'result{role}_{order}{color}.png', 'ImageNames.skillResultIcon')
            if role != 2:
                ref(f'{"uranai" if role == 1 else "goei"}{order}.png', 'ImageNames.claimedRoleActionIcon')
    for name in ('yaku4.png', 'yaku5.png', 'turi.png', 'noroi.png', 'atooi.png', 'kami.png'):
        ref(name, 'ImageNames.static suffix')
    audio_catalogs = {}
    for catalog, folder in (('BgmTrack', 'BGM'), ('SoundEffect', 'sounds')):
        source = (java / (catalog + '.java')).read_text()
        names = re.findall(r'"([^"\n]+\.wav)"', source)
        if not names:
            raise ValueError('No audio entries parsed: ' + catalog)
        for name in names:
            ref(name, catalog, folder)
        descriptions = re.findall(r'"([^"\n]+\.txt)"', source)
        for name in descriptions:
            ref(name, catalog + '.description', 'help')
        audio_catalogs[catalog] = {'tracks': len(names), 'descriptions': len(descriptions)}
    invalid = []
    image_count = 0
    max_image = (0, '')
    audio_seconds = defaultdict(float)
    for path in files:
        relative = path.relative_to(resources).as_posix()
        if path.suffix.lower() == '.png':
            image_count += 1
            with path.open('rb') as stream:
                header = stream.read(24)
            if len(header) != 24 or header[:8] != b'\x89PNG\r\n\x1a\n' or header[12:16] != b'IHDR':
                invalid.append(relative)
            else:
                width, height = struct.unpack('>II', header[16:24])
                if width <= 0 or height <= 0:
                    invalid.append(relative)
            max_image = max(max_image, (path.stat().st_size, relative))
        elif path.suffix.lower() == '.wav':
            try:
                with wave.open(str(path), 'rb') as audio:
                    audio_seconds[path.parent.name] += audio.getnframes() / audio.getframerate()
            except (wave.Error, EOFError, ZeroDivisionError):
                invalid.append(relative)
    missing = {name: sorted(origins) for name, origins in sorted(references.items())
               if not (resources / name).is_file()}
    images = {name for name in references if name.startswith('images/')}
    outside = [p for p in groups['images'] if p.relative_to(resources).as_posix() not in images]
    boundary = ('14day.png', '50day.png', 'yaku1_6.png', 'yaku2_5.png', 'yaku3_5.png',
                'result1_6white.png', 'result2_5white.png', 'result3_5.png', 'uranai6.png', 'goei5.png')
    return {
        'groups': group_report,
        'total_files': len(files), 'total_bytes': sum(p.stat().st_size for p in files),
        'fixed_images': {'identifiers': len(fixed), 'unique_paths': len(set(n for _, n in fixed))},
        'event_images': {'characters': len(mapping), 'branches': dict(branches),
                         'shapes': dict(shapes), 'unique_normal_paths': len(event_images)},
        'image_candidates': {'unique_paths': len(images),
                             'bytes': sum((resources / n).stat().st_size for n in images if (resources / n).is_file())},
        'images_outside_audited_references': {'files': len(outside), 'bytes': sum(p.stat().st_size for p in outside)},
        'audio_catalogs': audio_catalogs, 'audio_seconds': dict(audio_seconds),
        'png_headers_checked': image_count, 'largest_png': {'bytes': max_image[0], 'path': max_image[1]},
        'missing_audited_references': missing, 'invalid_headers': invalid,
        'boundary_samples_exist': {name: (resources / 'images' / name).is_file() for name in boundary},
    }


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[1])
    args = parser.parse_args()
    report = audit(args.root)
    print(json.dumps(report, ensure_ascii=False, indent=2))
    raise SystemExit(1 if report['missing_audited_references'] or report['invalid_headers'] else 0)
