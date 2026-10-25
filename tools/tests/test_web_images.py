"""WEB-022 contracts: source selection, immutable publication, corruption and pixel fidelity."""
import contextlib
import importlib.util
import io
import json
from pathlib import Path
import shutil
import tempfile
import unittest
from unittest.mock import patch

from PIL import Image

SPEC = importlib.util.spec_from_file_location('web_images', Path(__file__).resolve().parents[1] / 'build-web-images.py')
IMAGES = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(IMAGES)


class ImagePipelineTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name) / 'repo'
        self.output = Path(self.temp.name) / 'output'
        self.java = self.root / 'src/main/java/com/vocr/resources/UiImage.java'
        self.java.parent.mkdir(parents=True)
        self.java.write_text('    FRAME("frame.png"),\n')
        self.mapping = self.root / 'src/main/resources/config/event_image_mapping.json'
        self.mapping.parent.mkdir(parents=True)
        self.mapping.write_text(json.dumps({'Abel': {'default': {'all': 'frame.png'},
            'crsl': {'default': ['one.png', 'two.png'], 'special': {'Erich': '特別.png'}}}}, ensure_ascii=False, indent=2))
        self.base = self.root / 'src/main/resources/images'
        self.base.mkdir()
        refs, *_ = IMAGES.AUDIT.image_reference_catalog(self.root)
        image = Image.new('RGBA', (8, 8), (55, 120, 180, 0))
        image.putpixel((3, 3), (240, 88, 31, 127))
        image.putpixel((5, 2), (15, 33, 99, 255))
        for name in refs:
            image.save(self.root / 'src/main/resources' / name)

    def build(self):
        with contextlib.redirect_stderr(io.StringIO()):
            return IMAGES.build(self.root, self.output)

    def test_roundtrip_deduplicates_content_not_logical_paths_and_preserves_sources(self):
        original = {p.name: p.read_bytes() for p in self.base.iterdir()}
        report = self.build()
        self.assertGreater(report['images'], 400)
        self.assertLess(report['uniqueFiles'], report['images'])
        self.assertEqual(report, IMAGES.check(self.root, self.output))
        self.assertEqual(original, {p.name: p.read_bytes() for p in self.base.iterdir()})
        manifest = json.loads((self.output / 'manifest.json').read_text())
        self.assertIn('images/特別.png', manifest['images'])
        self.assertFalse(manifest['boundarySamplesExist']['14day.png'])
        self.assertEqual(manifest['images']['images/01s.png']['size'], [8, 8])
        self.assertTrue(manifest['images']['images/01s.png']['lossless'])

    def test_build_refuses_existing_directory_without_touching_it(self):
        self.output.mkdir()
        marker = self.output / 'keep'
        marker.write_text('untouched')
        with self.assertRaisesRegex(ValueError, 'already exists'):
            self.build()
        self.assertEqual(marker.read_text(), 'untouched')

    def test_missing_and_wrong_case_sources_fail_before_publication(self):
        (self.base / '特別.png').rename(self.base / 'Special.png')
        with self.assertRaisesRegex(ValueError, 'Missing'):
            self.build()
        self.assertFalse(self.output.exists())

    def test_truncated_png_and_lfs_pointer_fail_without_partial_output(self):
        for data in (b'version https://git-lfs.github.com/spec/v1\n', b'\x89PNG\r\n\x1a\n'):
            with self.subTest(data=data):
                (self.base / 'frame.png').write_bytes(data)
                with self.assertRaises(OSError):
                    self.build()
                self.assertFalse(self.output.exists())
                self.assertFalse(list(self.output.parent.glob('.web-images-*')))

    def test_selection_rejects_paths_and_empty_event_branches(self):
        for name in ('../outside.png', 'folder/file.png', 'folder\\file.png'):
            self.java.write_text('    BAD("' + name + '"),\n')
            with self.assertRaisesRegex(ValueError, 'Invalid source filename'):
                IMAGES.source_catalog(self.root)
        self.java.write_text('    FRAME("frame.png"),\n')
        data = json.loads(self.mapping.read_text())
        data['Abel']['crsl']['special']['Erich'] = []
        self.mapping.write_text(json.dumps(data))
        with self.assertRaisesRegex(ValueError, 'Empty image branch'):
            self.build()

    def test_source_and_selection_drift_fail_check(self):
        self.build()
        (self.base / '01s.png').write_bytes(b'changed source')
        with self.assertRaisesRegex(ValueError, 'Source drift'):
            IMAGES.check(self.root, self.output)
        self.java.write_text(self.java.read_text() + '// changed\n')
        with self.assertRaisesRegex(ValueError, 'Selection input changed'):
            IMAGES.check(self.root, self.output)

    def test_output_corruption_and_stale_files_fail_check(self):
        self.build()
        stale = self.output / 'old.webp'
        stale.write_bytes(b'stale')
        with self.assertRaisesRegex(ValueError, 'Unexpected/stale'):
            IMAGES.check(self.root, self.output)
        stale.unlink()
        path = next(self.output.glob('*.webp'))
        data = path.read_bytes()
        path.write_bytes(data[:-1])
        with self.assertRaisesRegex(ValueError, 'Output hash/size'):
            IMAGES.check(self.root, self.output)

    def test_manifest_cannot_escape_output(self):
        self.build()
        path = self.output / 'manifest.json'
        data = json.loads(path.read_text())
        next(iter(data['images'].values()))['file'] = '../outside.webp'
        path.write_text(json.dumps(data))
        with self.assertRaisesRegex(ValueError, 'Unsafe or non-content-addressed'):
            IMAGES.check(self.root, self.output)

    def test_symlink_sources_and_outputs_rejected(self):
        path = self.base / 'frame.png'
        path.unlink()
        path.symlink_to(self.base / '01s.png')
        with self.assertRaisesRegex(ValueError, 'unsafe source'):
            self.build()
        path.unlink()
        shutil.copy(self.base / '01s.png', path)
        self.build()
        image = next(self.output.glob('*.webp'))
        saved = self.output.parent / 'external.webp'
        image.rename(saved)
        image.symlink_to(saved)
        with self.assertRaisesRegex(ValueError, 'Missing/unsafe output'):
            IMAGES.check(self.root, self.output)

    def test_size_alpha_and_quality_are_independent_gates(self):
        a = Image.new('RGBA', (10, 10), (255, 255, 255, 255))
        with self.assertRaisesRegex(ValueError, 'Dimension'):
            IMAGES.validate_pixels(a, a.resize((5, 5)), False)
        with self.assertRaisesRegex(ValueError, 'Alpha'):
            IMAGES.validate_pixels(a, Image.new('RGBA', a.size, (255, 255, 255, 254)), False)
        with self.assertRaisesRegex(ValueError, 'PSNR'):
            IMAGES.validate_pixels(a, Image.new('RGBA', a.size, (0, 0, 0, 255)), False)
        with self.assertRaisesRegex(ValueError, 'Lossless RGBA'):
            IMAGES.validate_pixels(a, Image.new('RGBA', a.size, (254, 255, 255, 255)), True)

    def test_text_checkout_newlines_do_not_invalidate_image_manifest(self):
        self.build()
        before = self.mapping.read_bytes()
        self.mapping.write_bytes(before.replace(b'\n', b'\r\n'))
        self.java.write_bytes(self.java.read_bytes().replace(b'\n', b'\r\n'))
        self.assertEqual(IMAGES.check(self.root, self.output)['images'],
                         len(IMAGES.source_catalog(self.root)))

    def test_same_input_encoder_and_policy_produce_identical_manifest_and_files(self):
        self.build()
        second = self.output.parent / 'second'
        with contextlib.redirect_stderr(io.StringIO()):
            IMAGES.build(self.root, second)
        self.assertEqual({p.name: p.read_bytes() for p in self.output.iterdir()},
                         {p.name: p.read_bytes() for p in second.iterdir()})

    def test_cli_failure_is_nonzero_and_explicit(self):
        (self.base / '特別.png').unlink()
        stderr = io.StringIO()
        stdout = io.StringIO()
        with patch('sys.argv', ['build-web-images.py', 'build', '--root', str(self.root),
                                '--output', str(self.output)]):
            with contextlib.redirect_stderr(stderr), contextlib.redirect_stdout(stdout):
                self.assertEqual(IMAGES.main(), 1)
        self.assertIn('WEB_IMAGES_ERROR: Missing or unsafe source:', stderr.getvalue())
        self.assertEqual(stdout.getvalue(), '')
        self.assertFalse(self.output.exists())

    def test_bundle_and_manifest_budgets_are_not_silently_expanded(self):
        for budget, diagnostic in [('imageBudget', 'Image bundle'), ('manifestBudget', 'Manifest budget')]:
            with self.subTest(budget=budget), patch.dict(IMAGES.POLICY, {budget: 1}):
                with self.assertRaisesRegex(ValueError, diagnostic):
                    self.build()
                self.assertFalse(self.output.exists())

    def test_original_resource_directory_is_never_an_output(self):
        with self.assertRaisesRegex(ValueError, 'inside original resources'):
            IMAGES.build(self.root, self.base / 'new')
        self.assertFalse((self.base / 'new').exists())

    def test_distribution_checks_copy_and_includes_dialogue_code_budget(self):
        self.build()
        public = self.root / 'web-client/public/assets/images'
        public.parent.mkdir(parents=True)
        shutil.copytree(self.output, public)
        dist = self.root / 'web-client/dist'
        (dist / 'assets').mkdir(parents=True)
        shutil.copytree(self.output, dist / 'assets/images')
        (dist / 'index.html').write_text('<html>static</html>')
        (dist / 'assets/app.js').write_text('const dialogue = "台詞";')
        report = IMAGES.check_distribution(self.root, dist)
        self.assertGreater(report['codeBytes'], 0)
        self.assertGreater(report['codeGzipBytes'], 0)
        self.assertGreater(report['totalStaticBytes'], report['imageBytes'])
        (dist / 'assets/app.js').write_bytes(b'a' * (1024**2 + 1))
        with self.assertRaisesRegex(ValueError, 'Code budget exceeded'):
            IMAGES.check_distribution(self.root, dist)

    def test_budget_excess_does_not_publish_or_delete_existing_source(self):
        with patch.dict(IMAGES.POLICY, singleBudget=1):
            with self.assertRaisesRegex(ValueError, 'Single image budget'):
                self.build()
        self.assertFalse(self.output.exists())
        self.assertTrue((self.base / '01s.png').is_file())
        self.assertFalse(list(self.output.parent.glob('.web-images-*')))


if __name__ == '__main__':
    unittest.main()
