"""Immutable release transition preparation; reuses actual release checker fixtures."""
import copy
import importlib.util
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('release_fixture',ROOT/'tools/tests/test_web_release.py')
F=importlib.util.module_from_spec(spec);spec.loader.exec_module(F)
with patch.dict(sys.modules,{'web_release':F.R}):
    spec=importlib.util.spec_from_file_location('upgrade',ROOT/'tools/web_upgrade.py')
    U=importlib.util.module_from_spec(spec);spec.loader.exec_module(U)

class UpgradeTest(unittest.TestCase):
    def setUp(self):
        self.fixture=F.ReleaseTest('runTest');self.fixture.setUp();self.addCleanup(self.fixture.doCleanups)
        self.old=self.fixture.build('old');self.old_sha=U.R.hash_file(self.old)
        self.fixture.head='c'*40;self.fixture.audit['sha']=self.fixture.head
        self.fixture.result['tested_sha']=self.fixture.head
        self.fixture.result['input_audit']=copy.deepcopy(self.fixture.audit)
        self.fixture.result['final_audit']=copy.deepcopy(self.fixture.audit)
        self.fixture.stamp();self.new=self.fixture.build('new');self.new_sha=U.R.hash_file(self.new)
        self.out=self.fixture.root/'prepared'
    def prepare(self,**kw):
        args=dict(current=self.old,current_sha256=self.old_sha,candidate=self.new,candidate_sha256=self.new_sha,
                  current_origin='https://localhost:18443',candidate_origin='https://localhost:18443',output=self.out)
        args.update(kw);return U.prepare(**args)
    def test_exact_pair_prepared_in_separate_new_directories_without_claiming_compatibility(self):
        plan=self.prepare()
        self.assertEqual('a'*40,plan['current']['source_sha']);self.assertEqual('c'*40,plan['candidate']['source_sha'])
        self.assertNotEqual(plan['current']['directory'],plan['candidate']['directory'])
        self.assertTrue((self.out/'current/backend/run.sh').is_file());self.assertTrue((self.out/'candidate/frontend/index.html').is_file())
        self.assertFalse(plan['services_started_or_stopped']);self.assertFalse(plan['browser_data_touched'])
        self.assertFalse(plan['active_games_survive_restart']);self.assertFalse(plan['browser_data_automatically_migrated'])
        self.assertEqual('UNPROVEN_UNTIL_PAIR_ACCEPTANCE',plan['rollback_compatibility'])
        self.assertEqual(self.old_sha,U.R.hash_file(self.old));self.assertEqual(self.new_sha,U.R.hash_file(self.new))
    def test_bad_digest_or_corrupt_candidate_never_creates_output(self):
        for digest in ['0'*64,'bad']:
            with self.assertRaises(ValueError):self.prepare(candidate_sha256=digest)
            self.assertFalse(self.out.exists())
        self.new.write_bytes(b'broken')
        with self.assertRaises(ValueError):self.prepare(candidate_sha256=U.R.hash_file(self.new))
        self.assertFalse(self.out.exists())
    def test_existing_output_and_source_symlink_are_preserved(self):
        self.out.mkdir();sentinel=self.out/'user';sentinel.write_text('keep')
        with self.assertRaisesRegex(ValueError,'OUTPUT_EXISTS'):self.prepare()
        self.assertEqual('keep',sentinel.read_text())
        link=self.fixture.root/'archive-link.zip';link.symlink_to(self.old)
        with self.assertRaisesRegex(ValueError,'SYMLINK'):self.prepare(current=link,output=self.fixture.root/'other')
    def test_source_change_after_snapshot_does_not_change_prepared_release(self):
        original=U.snapshot
        def after_copy(archive,digest,target):
            result=original(archive,digest,target)
            if Path(archive)==self.new:self.new.write_bytes(b'changed after authenticated snapshot')
            return result
        with patch.object(U,'snapshot',side_effect=after_copy):plan=self.prepare()
        self.assertEqual('c'*40,plan['candidate']['source_sha'])
        self.assertTrue((self.out/'candidate/backend/server.jar').is_file())
    def test_origin_normalization_and_each_origin_change(self):
        self.assertEqual('https://example.com',U.origin('HTTPS://EXAMPLE.COM:443/'))
        for changed in ['https://localhost:18444','https://127.0.0.1:18443','http://localhost:18443']:
            with tempfile.TemporaryDirectory() as directory:
                plan=self.prepare(candidate_origin=changed,output=Path(directory)/'new')
                self.assertTrue(plan['origin_changed']);self.assertFalse(plan['browser_data_automatically_migrated'])
    def test_acceptance_output_normalizes_before_containment_and_refuses_links(self):
        root=self.fixture.root
        for value in [root/'build/../outside',root/'build',root/'outside']:
            with self.subTest(value=value),self.assertRaises(ValueError):U.acceptance_output(root,value)
        self.assertEqual(root/'build/allowed',U.acceptance_output(root,root/'build/nested/../allowed'))
        (root/'build/link').symlink_to(root)
        with self.assertRaises(ValueError):U.acceptance_output(root,root/'build/link/new')
        self.assertFalse((root/'outside').exists())

    def test_unsafe_origin_and_identical_pair_refused(self):
        for value in ['https://user:secret@localhost','http://public.example','https://localhost?secret','https://localhost#x',
                      'https://127.1','https://2130706433','https://0x7f000001','https://example.123',
                      'https://localhost/api','https://localhost:','https://localhost:0','https://localhost:99999','https://localhost\n']:
            with self.subTest(value=value),self.assertRaises(ValueError):self.prepare(candidate_origin=value)
        with self.assertRaisesRegex(ValueError,'SAME_RELEASE'):self.prepare(candidate=self.old,candidate_sha256=self.old_sha)
        self.assertFalse(self.out.exists())

if __name__=='__main__':unittest.main()
