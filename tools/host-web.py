#!/usr/bin/env python3
"""Explicit localhost setup/serve; never installs system trust or starts Java."""
import argparse,json,os,subprocess,sys,tarfile
from pathlib import Path
import web_hosting as H

def main():
 parser=argparse.ArgumentParser();sub=parser.add_subparsers(dest='command',required=True)
 a=sub.add_parser('install-tool');a.add_argument('--directory',required=True)
 a=sub.add_parser('prepare');source=a.add_mutually_exclusive_group(required=True);source.add_argument('--frontend');source.add_argument('--archive');a.add_argument('--output',required=True)
 a=sub.add_parser('serve');a.add_argument('--caddy',required=True);a.add_argument('--directory',required=True)
 args=parser.parse_args()
 try:
  if args.command=='install-tool':print(json.dumps({'caddy':str(H.install(args.directory))}));return 0
  if args.command=='prepare':
   p=H.prepare_archive(args.archive,args.output) if args.archive else H.prepare(args.frontend,args.output);print(json.dumps({k:p[k] for k in ('private','certificate_sha256','ca_sha256','system_trust_modified')}));return 0
  binary=H.checked_caddy(args.caddy);directory=H.regular(args.directory,True);plan=json.loads(H.regular(directory/'plan.json').read_text())
  root,actual=H.frontend(plan['frontend']);H.require(actual==plan['frontend_files'],'HOSTED_FRONTEND_DRIFT')
  H.require(H.digest(directory/'site.pem')==plan['certificate_sha256'],'CERTIFICATE_DRIFT')
  config=H.configuration(root,directory/'site.pem',directory/'site.key');expected=json.dumps(config,indent=2)+'\n'
  H.require(H.regular(directory/'caddy.json').read_text()==expected,'HOSTING_CONFIG_DRIFT')
  subprocess.run([str(binary),'validate','--config',str(directory/'caddy.json')],check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
  os.execv(str(binary),[str(binary),'run','--config',str(directory/'caddy.json')])
 except (OSError,ValueError,KeyError,tarfile.TarError,subprocess.SubprocessError):
  print(json.dumps({'error':'HOSTING_REFUSED'}),file=sys.stderr);return 2
if __name__=='__main__':sys.exit(main())
