#!/usr/bin/env python3
"""Actual Caddy/TLS + unseeded Java integration; private keys never in CI evidence."""
import json,os,subprocess,sys,uuid
from pathlib import Path
import web_hosting as H
ROOT=Path(__file__).resolve().parents[1]
def main():
 private=ROOT/'build/hosting-private'/uuid.uuid4().hex
 try:
  binary=H.checked_caddy(os.environ.get('VOCR_CADDY',ROOT/'build/ci/toolchain/caddy/caddy'))
  plan=H.prepare(ROOT/'web-client/dist',private)
  for config in ('caddy.json','plain-origin.json','tls-origin.json','probe.json'):
   subprocess.run([str(binary),'validate','--config',str(private/config)],check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
  env={**os.environ,'VOCR_HOSTING_PLAN':str(private/'plan.json'),'VOCR_CADDY':str(binary),'NODE_EXTRA_CA_CERTS':str(private/'ca.pem')}
  command=['npm','--prefix','web-client','run','test:e2e','--','--config=../tools/playwright.hosting.config.mjs']
  code=subprocess.run(command,cwd=ROOT,env=env).returncode
  H.require(code==0,'HOSTING_SMOKE_FAILED')
  # Certificate/keys are outside evidence; only public fingerprints and tool identity are recorded.
  report={'status':'passed','caddy_version':json.loads(H.LOCK.read_text())['version'],'caddy_sha256':H.digest(binary),
          'certificate_sha256':plan['certificate_sha256'],'ca_sha256':plan['ca_sha256'],
          'system_trust_modified':False,'browser_trust':'isolated Chromium SPKI allowlist for this generated key only; no ignoreHTTPSErrors',
          'tls_verified':'Node/curl-style CA+hostname checks; wrong CA and hostname rejected',
          'public_deployment':False,'private_material_uploaded':False}
  output=ROOT/'build/verify/hosting-browser';output.mkdir(parents=True,exist_ok=True);(output/'hosting-proof.json').write_text(json.dumps(report,indent=2)+'\n')
  return 0
 except (OSError,ValueError,KeyError,subprocess.SubprocessError):print('HOSTING_SMOKE_REFUSED',file=sys.stderr);return 1
if __name__=='__main__':sys.exit(main())
