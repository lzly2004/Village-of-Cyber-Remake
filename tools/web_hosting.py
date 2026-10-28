"""Localhost-only Caddy hosting. No system trust, privileged ports or public bind."""
import base64,copy,hashlib,importlib.util,json,os,re,subprocess,tarfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
LOCK=ROOT/'tools/hosting-toolchain.json'

def require(value,code):
 if not value:raise ValueError(code)

def digest(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()

def regular(path,directory=False):
 p=Path(path).absolute()
 require(not any(x.is_symlink() for x in (p,*p.parents)),'SYMLINK_FORBIDDEN')
 require(p.is_dir() if directory else p.is_file(),'MISSING_PATH')
 return p

def checked_caddy(path):
 p=regular(path);require(digest(p)==json.loads(LOCK.read_text())['binary_sha256'],'CADDY_BINARY_HASH_MISMATCH');return p

def install(directory):
 """Explicit opt-in fixed user-directory tool installation; cache is always rehashed."""
 spec=json.loads(LOCK.read_text());directory=Path(directory).absolute()
 require(not any(p.is_symlink() for p in (directory,*directory.parents)),'SYMLINK_FORBIDDEN')
 directory.mkdir(parents=True,exist_ok=True);binary=directory/'caddy'
 if binary.exists():return checked_caddy(binary)
 loader=importlib.util.spec_from_file_location('hosting_archive_reader',ROOT/'tools/ci/bootstrap.py')
 module=importlib.util.module_from_spec(loader);loader.loader.exec_module(module)
 archive=module.archive(spec,directory/'caddy.tar.gz')
 with tarfile.open(archive) as z:
  matches=[m for m in z.getmembers() if m.name=='caddy'];require(len(matches)==1 and matches[0].isfile() and 0<matches[0].size<=64*1024*1024,'CADDY_ARCHIVE_ENTRY')
  data=z.extractfile(matches[0]).read();require(hashlib.sha256(data).hexdigest()==spec['binary_sha256'],'CADDY_BINARY_HASH_MISMATCH')
 with binary.open('xb') as f:f.write(data)
 binary.chmod(0o755);return checked_caddy(binary)

def frontend(path):
 root=regular(path,True);require((root/'index.html').is_file(),'INDEX_MISSING')
 files={}
 for f in root.rglob('*'):
  require(not f.is_symlink(),'SYMLINK_FORBIDDEN')
  if f.is_dir():continue
  n=f.relative_to(root).as_posix()
  require((n=='index.html' or n.startswith('assets/')) and all(not p.startswith('.') for p in n.split('/'))
          and f.suffix.lower() in ('.html','.js','.css','.json','.txt','.webp','.mp3','.svg','.ico','.woff2'),'UNEXPECTED_STATIC_FILE')
  files[n]=digest(f)
 return root,files

def port(n):require(type(n) is int and 1024<=n<=65535,'INVALID_LOCAL_PORT');return n

def configuration(front,cert,key,https_ports=(18443,18444),http_port=18080,upstream_port=17075):
 root,_=frontend(front);cert=regular(cert);key=regular(key)
 ports=list(https_ports);require(1<=len(ports)<=2,'INVALID_HTTPS_PORTS')
 for n in [*ports,http_port,upstream_port]:port(n)
 require(len(set([*ports,http_port,upstream_port]))==len(ports)+2,'PORT_COLLISION')
 headers={'Cache-Control':['no-store'],'Referrer-Policy':['no-referrer'],'X-Content-Type-Options':['nosniff'],
          'X-Frame-Options':['DENY'],'Content-Security-Policy':["default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; media-src 'self' blob:; connect-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'"]}
 def response(status,body,extra=None):return {'handler':'static_response','status_code':status,'body':body,'headers':{**headers,**(extra or {})}}
 secure={'handler':'headers','response':{'set':headers,'delete':['Server']}}
 host={'match':[{'not':[{'host':['localhost','127.0.0.1']}]}],'handle':[response(421,'Host refused.')],'terminal':True}
 proxy={'handler':'reverse_proxy','upstreams':[{'dial':'127.0.0.1:'+str(upstream_port)}],
        'transport':{'protocol':'http','dial_timeout':'3s','response_header_timeout':'15s','keep_alive':{'enabled':False}},
        'headers':{'request':{'delete':['Forwarded','X-Real-Ip','X-Forwarded-For','X-Forwarded-Host','X-Forwarded-Proto'],
                              'set':{'Host':['127.0.0.1:'+str(upstream_port)]}}}}
 routes=[host,
  {'match':[{'path':['/api','/api/*']}],'handle':[secure,{'handler':'request_body','max_size':16384},proxy],'terminal':True},
  {'match':[{'path':['/','/index.html','/assets/*'],'method':['GET','HEAD']}],'handle':[secure,{'handler':'file_server','root':str(root)}],'terminal':True},
  {'match':[{'path':['/','/index.html','/assets/*']}],'handle':[response(405,'Method refused.',{'Allow':['GET, HEAD']})],'terminal':True},
  {'handle':[response(404,'Not found.')],'terminal':True}]
 server={'protocols':['h1','h2'],'listen':['127.0.0.1:'+str(n) for n in ports],'automatic_https':{'disable':True},'tls_connection_policies':[{}],
         'read_header_timeout':'5s','read_timeout':'15s','write_timeout':'30s','idle_timeout':'30s','max_header_bytes':16384,'routes':routes,
         'errors':{'routes':[{'handle':[response('{http.error.status_code}','Proxy request failed.')]}]}}
 redirect={'protocols':['h1'],'listen':['127.0.0.1:'+str(http_port)],'automatic_https':{'disable':True},'routes':[copy.deepcopy(host),
  {'match':[{'method':['GET','HEAD']}],'handle':[response(308,'Use HTTPS.',{'Location':['https://localhost:'+str(ports[0])+'/']})],'terminal':True},
  {'handle':[response(426,'HTTPS required.')],'terminal':True}]}
 return {'admin':{'disabled':True,'config':{'persist':False}},
         'logging':{'logs':{'default':{'writer':{'output':'discard'}}}},
         'apps':{'http':{'grace_period':'2s','servers':{'https':server,'http':redirect}},
                 'tls':{'certificates':{'load_files':[{'certificate':str(cert),'key':str(key)}]}}}}

def private_output(output):
 output=Path(output).absolute()
 require(not any(x.is_symlink() for x in (output,*output.parents)),'SYMLINK_FORBIDDEN')
 require(not any(output.parts[i:i+2]==('build','verify') for i in range(len(output.parts)-1)),'PRIVATE_DIRECTORY_REQUIRED')
 require(not output.exists(),'OUTPUT_EXISTS')
 return output

def prepare_archive(archive,output):
 import web_release as R
 archive=regular(archive);output=private_output(output)
 manifest=R.check_release(archive)
 output.mkdir(parents=True,mode=0o700)
 release=R.extract_checked(archive,output/'release')
 plan=prepare(release/'frontend',output/'host')
 plan['archive']={'sha256':digest(archive),'source_sha':manifest['source']['sha']}
 (output/'host/plan.json').write_text(json.dumps(plan,indent=2)+'\n')
 return plan

def prepare(front,output):
 root,inventory=frontend(front);output=private_output(output)
 require(output!=root and root not in output.parents and output not in root.parents,'PRIVATE_DIRECTORY_OVERLAPS_FRONTEND')
 require(not output.exists(),'OUTPUT_EXISTS');output.mkdir(parents=True,mode=0o700)
 def openssl(*args):return subprocess.check_output(['openssl',*args],stderr=subprocess.DEVNULL)
 try:
  request_config=output/'request.cnf'
  request_config.write_text('[req]\nprompt=no\ndistinguished_name=dn\n[dn]\nCN=VOCR isolated localhost test CA\n[ca_ext]\nbasicConstraints=critical,CA:TRUE\nkeyUsage=critical,keyCertSign,cRLSign\nsubjectKeyIdentifier=hash\nauthorityKeyIdentifier=keyid:always\n')
  openssl('req','-config',str(request_config),'-extensions','ca_ext','-x509','-newkey','rsa:2048','-nodes','-keyout',str(output/'ca.key'),'-out',str(output/'ca.pem'),'-days','2','-subj','/CN=VOCR isolated localhost test CA')
  openssl('req','-config',str(request_config),'-new','-newkey','rsa:2048','-nodes','-keyout',str(output/'site.key'),'-out',str(output/'site.csr'),'-subj','/CN=localhost')
  ext=output/'extensions.cnf';ext.write_text('basicConstraints=critical,CA:FALSE\nkeyUsage=critical,digitalSignature,keyEncipherment\nextendedKeyUsage=serverAuth\nsubjectAltName=DNS:localhost,IP:127.0.0.1\n')
  openssl('x509','-req','-in',str(output/'site.csr'),'-CA',str(output/'ca.pem'),'-CAkey',str(output/'ca.key'),'-CAcreateserial','-out',str(output/'site.pem'),'-days','2','-extfile',str(ext))
  openssl('verify','-CAfile',str(output/'ca.pem'),'-verify_hostname','localhost',str(output/'site.pem'))
  for key in output.glob('*.key'):key.chmod(0o600)
  public=openssl('x509','-in',str(output/'site.pem'),'-pubkey','-noout')
  der=subprocess.run(['openssl','pkey','-pubin','-outform','DER'],input=public,stdout=subprocess.PIPE,stderr=subprocess.DEVNULL,check=True).stdout
  pin=base64.b64encode(hashlib.sha256(der).digest()).decode()
  config=configuration(root,output/'site.pem',output/'site.key')
  (output/'caddy.json').write_text(json.dumps(config,indent=2)+'\n')
  # Same-port protocol-only test: these are never loaded by the ordinary hosting command.
  alternate=configuration(root,output/'site.pem',output/'site.key',https_ports=[18500],http_port=18501)
  alternate['apps']['http']['servers']={'https':alternate['apps']['http']['servers']['https']}
  (output/'tls-origin.json').write_text(json.dumps(alternate,indent=2)+'\n')
  plain=copy.deepcopy(alternate);s=plain['apps']['http']['servers']['https'];s.pop('tls_connection_policies');s['protocols']=['h1']
  (output/'plain-origin.json').write_text(json.dumps(plain,indent=2)+'\n')
  probe=configuration(root,output/'site.pem',output/'site.key',https_ports=[18510],http_port=18511,upstream_port=17101)
  probe['apps']['http']['servers']={'https':probe['apps']['http']['servers']['https']}
  (output/'probe.json').write_text(json.dumps(probe,indent=2)+'\n')
  plan={'schema':1,'frontend':str(root),'frontend_files':inventory,'private':str(output),'spki':pin,
        'certificate_sha256':digest(output/'site.pem'),'ca_sha256':digest(output/'ca.pem'),'system_trust_modified':False}
  (output/'plan.json').write_text(json.dumps(plan,indent=2)+'\n');return plan
 except BaseException:
  # Retain only this new private directory for explicit diagnosis; never remove preexisting paths.
  raise
