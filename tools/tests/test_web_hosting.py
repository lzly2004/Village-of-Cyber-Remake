"""Hosting boundary contracts; no JVM/browser/network/system trust changes."""
import hashlib,importlib.util,io,json,os,subprocess,tarfile,tempfile,unittest
from unittest.mock import patch
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('hosting',ROOT/'tools/web_hosting.py')
H=importlib.util.module_from_spec(spec);spec.loader.exec_module(H)

class HostingTest(unittest.TestCase):
 def setUp(self):
  self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup);self.root=Path(self.temp.name)
  self.front=self.root/'frontend';self.front.mkdir();(self.front/'index.html').write_text('<html>fixture</html>')
  self.cert=self.root/'cert.pem';self.key=self.root/'key.pem';self.cert.write_text('fixture');self.key.write_text('fixture')
 def config(self,**kw):return H.configuration(self.front,self.cert,self.key,**kw)
 def test_no_admin_automation_persistence_access_logs_or_external_listeners(self):
  c=self.config();self.assertTrue(c['admin']['disabled']);self.assertFalse(c['admin']['config']['persist'])
  self.assertEqual('discard',c['logging']['logs']['default']['writer']['output'])
  for s in c['apps']['http']['servers'].values():
   self.assertTrue(all(x.startswith('127.0.0.1:') for x in s['listen']));self.assertNotIn('logs',s)
  self.assertTrue(c['apps']['http']['servers']['https']['automatic_https']['disable'])
 def test_api_has_priority_no_uri_rewrite_retry_or_spa_fallback(self):
  routes=self.config()['apps']['http']['servers']['https']['routes'];api=routes[1]
  self.assertEqual(['/api','/api/*'],api['match'][0]['path']);self.assertTrue(api['terminal'])
  proxy=api['handle'][-1];self.assertEqual('reverse_proxy',proxy['handler']);self.assertEqual([{'dial':'127.0.0.1:17075'}],proxy['upstreams'])
  self.assertNotIn('rewrite',proxy);self.assertNotIn('load_balancing',proxy)
  self.assertEqual(False,proxy['transport']['keep_alive']['enabled']);self.assertIn('Forwarded',proxy['headers']['request']['delete'])
  self.assertEqual('static_response',routes[-1]['handle'][0]['handler']);self.assertEqual(404,routes[-1]['handle'][0]['status_code'])
 def test_static_is_get_head_only_and_never_serves_backend_or_config(self):
  routes=self.config()['apps']['http']['servers']['https']['routes'];static=routes[2]
  self.assertEqual(['GET','HEAD'],static['match'][0]['method']);self.assertEqual(['/','/index.html','/assets/*'],static['match'][0]['path'])
  fs=static['handle'][-1];self.assertEqual(str(self.front),fs['root']);self.assertFalse(fs.get('browse',False))
  self.assertEqual(421,routes[0]['handle'][0]['status_code'])
 def test_redirect_has_fixed_target_never_copies_sensitive_path_or_query(self):
  c=self.config();redirect=c['apps']['http']['servers']['http']['routes'][1]['handle'][0]
  self.assertEqual(['https://localhost:18443/'],redirect['headers']['Location']);self.assertEqual(308,redirect['status_code'])
  self.assertNotIn('{http.request',json.dumps(c))
 def test_invalid_ports_overlap_or_bool_refused(self):
  for n in [0,1,1023,65536,True,'18443','18443\ninject']:
   with self.subTest(n=n),self.assertRaises(ValueError):self.config(https_ports=[n])
  with self.assertRaises(ValueError):self.config(https_ports=[18443,18443])
  with self.assertRaises(ValueError):self.config(http_port=18443)
 def test_links_and_unknown_static_files_refused(self):
  (self.front/'link').symlink_to(self.cert)
  with self.assertRaisesRegex(ValueError,'SYMLINK'):self.config()
  (self.front/'link').unlink();(self.front/'private.java').write_text('secret')
  with self.assertRaisesRegex(ValueError,'STATIC_FILE'):self.config()
 def test_json_paths_are_data_not_config_injection(self):
  strange=self.root/'quote"\nfile.pem';strange.write_text('fixture')
  c=H.configuration(self.front,strange,self.key);self.assertEqual(str(strange),c['apps']['tls']['certificates']['load_files'][0]['certificate'])
  self.assertEqual(c,json.loads(json.dumps(c)))
 def test_caddy_binary_hash_and_symlink_fail_closed(self):
  p=self.root/'caddy';p.write_bytes(b'bad')
  with self.assertRaisesRegex(ValueError,'CADDY_BINARY'):H.checked_caddy(p)
  link=self.root/'alias';link.symlink_to(p)
  with self.assertRaisesRegex(ValueError,'SYMLINK'):H.checked_caddy(link)
 def test_private_output_cannot_be_under_public_frontend(self):
  with self.assertRaisesRegex(ValueError,'PRIVATE_DIRECTORY_OVERLAPS'):H.prepare(self.front,self.front/'assets/private')
 def test_certificate_private_files_not_under_artifact_directory(self):
  with self.assertRaisesRegex(ValueError,'PRIVATE_DIRECTORY'):H.prepare(self.front,self.root/'build/verify/private')

 def test_generated_chain_is_verified_without_system_openssl_config_or_trust(self):
  # An ambient req extension must not duplicate CA basicConstraints (OpenSSL 1.1 regression).
  ambient=self.root/'ambient.cnf';ambient.write_text('[req]\ndistinguished_name=dn\nx509_extensions=bad\n[dn]\n[bad]\nbasicConstraints=critical,CA:TRUE\n')
  with patch.dict(os.environ,{'OPENSSL_CONF':str(ambient)}):plan=H.prepare(self.front,self.root/'private')
  private=Path(plan['private'])
  subprocess.run(['openssl','verify','-CAfile',str(private/'ca.pem'),'-verify_hostname','localhost',str(private/'site.pem')],check=True,stdout=subprocess.DEVNULL)
  wrong=subprocess.run(['openssl','verify','-CAfile',str(private/'ca.pem'),'-verify_hostname','wrong.invalid',str(private/'site.pem')],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
  self.assertNotEqual(0,wrong.returncode);self.assertFalse(plan['system_trust_modified'])
  self.assertEqual(0o700,private.stat().st_mode&0o777)
  for key in private.glob('*.key'):self.assertEqual(0o600,key.stat().st_mode&0o777)
  self.assertEqual(['h1','h2'],json.loads((private/'caddy.json').read_text())['apps']['http']['servers']['https']['protocols'])
  with self.assertRaisesRegex(ValueError,'OUTPUT_EXISTS'):H.prepare(self.front,private)
 def test_installer_uses_hashed_archive_and_rechecks_existing_binary(self):
  directory=self.root/'tool';directory.mkdir();archive=directory/'caddy.tar.gz';binary=b'fixture binary'
  with tarfile.open(archive,'w:gz') as z:
   info=tarfile.TarInfo('caddy');info.size=len(binary);z.addfile(info,io.BytesIO(binary))
  lock=self.root/'lock.json';lock.write_text(json.dumps({'url':'https://invalid.example/never','sha256':H.digest(archive),'binary_sha256':hashlib.sha256(binary).hexdigest()}))
  with patch.object(H,'LOCK',lock):
   installed=H.install(directory);self.assertEqual(binary,installed.read_bytes());installed.write_bytes(b'tampered')
   with self.assertRaisesRegex(ValueError,'BINARY_HASH'):H.install(directory)
 def test_installer_rejects_symlink_archive_member_even_with_matching_archive_hash(self):
  directory=self.root/'tool';directory.mkdir();archive=directory/'caddy.tar.gz'
  with tarfile.open(archive,'w:gz') as z:
   info=tarfile.TarInfo('caddy');info.type=tarfile.SYMTYPE;info.linkname='/bin/sh';z.addfile(info)
  lock=self.root/'lock.json';lock.write_text(json.dumps({'url':'https://invalid.example/never','sha256':H.digest(archive),'binary_sha256':'0'*64}))
  with patch.object(H,'LOCK',lock),self.assertRaisesRegex(ValueError,'ARCHIVE_ENTRY'):H.install(directory)
  self.assertFalse((directory/'caddy').exists())
 def test_release_setup_refuses_bad_archive_before_creating_private_output(self):
  import sys
  spec=importlib.util.spec_from_file_location('web_release',ROOT/'tools/web_release.py');release=importlib.util.module_from_spec(spec);spec.loader.exec_module(release)
  archive=self.root/'broken.zip';archive.write_bytes(b'not zip')
  with patch.dict(sys.modules,{'web_release':release}),self.assertRaises(ValueError):H.prepare_archive(archive,self.root/'output')
  self.assertFalse((self.root/'output').exists())

if __name__=='__main__':unittest.main()
