"""Bounded, fail-closed Web release assembly/checking. No installation or deployment."""
import gzip
import hashlib
import importlib.util
import io
import json
import os
from pathlib import Path, PurePosixPath
import re
import shutil
import stat
import subprocess
import tempfile
import zipfile

MIB=1024*1024
MAX_FILE=32*MIB
MAX_ARCHIVE=192*MIB
MAX_FILES=20000
BUDGET={'static':100*MIB,'code':MIB,'gzip':512*1024,'images':32*MIB,'singleImage':512*1024,
        'bgm':64*MIB,'singleBgm':5*MIB,'sounds':MIB,'text':MIB}
REQUIRED_SUITES=('diff-check','guarded-tests','audit-tests','verify-tests','ci-tests','release-tests',
                 'image-tool-tests','audio-tool-tests','exchange-contract','frontend-unit','web-build',
                 'assets','audio','files-repeat','default-e2e','production-smoke','browser-file',
                 'java-full','java-results','oracle-audit','gate','desktop-exchange')
INTERNAL={'browser-file','java-results','oracle-audit'}
HEX=re.compile(r'[0-9a-f]{64}\Z')


def require(value,code):
    if not value:raise ValueError(code)


def sha(data):return hashlib.sha256(data).hexdigest()


def hash_file(path):
    h=hashlib.sha256()
    with Path(path).open('rb') as stream:
        for chunk in iter(lambda:stream.read(MIB),b''):h.update(chunk)
    return h.hexdigest()


def load_json(data):
    def pairs(values):
        out={}
        for k,v in values:
            require(k not in out,'DUPLICATE_JSON_KEY');out[k]=v
        return out
    try:return json.loads(data.decode('utf-8'),object_pairs_hook=pairs,parse_constant=lambda _:require(False,'INVALID_JSON_NUMBER'))
    except (UnicodeError,json.JSONDecodeError) as error:raise ValueError('INVALID_JSON') from error


def json_bytes(value):return (json.dumps(value,ensure_ascii=False,sort_keys=True,indent=2)+'\n').encode()


def safe_name(name):
    require(isinstance(name,str) and name and '\\' not in name and ':' not in name and not name.startswith('/')
            and all(ord(c)>=32 for c in name) and all(x not in ('','.','..') for x in name.split('/')),'UNSAFE_PATH')
    return name


def no_links(path):
    path=Path(path).absolute()
    for part in (path,*path.parents):require(not part.is_symlink(),'SYMLINK_FORBIDDEN')
    return Path(os.path.abspath(path))


def scan(directory):
    directory=no_links(directory);require(directory.is_dir(),'MISSING_DIRECTORY')
    files={};seen=set()
    for p in sorted(directory.rglob('*')):
        require(not p.is_symlink(),'SYMLINK_FORBIDDEN')
        if p.is_dir():continue
        require(stat.S_ISREG(p.stat().st_mode),'NONREGULAR_FILE')
        name=safe_name(p.relative_to(directory).as_posix())
        require(name.casefold() not in seen,'CASE_COLLISION');seen.add(name.casefold())
        require(p.stat().st_size<=MAX_FILE,'FILE_TOO_LARGE');files[name]=p
    require(files and len(files)<=MAX_FILES,'INVALID_FILE_COUNT')
    return files


def runtime_sources(root):
    root=Path(root)
    names=[p.relative_to(root).as_posix() for base in ('src/main/java','src/web/java') for p in (root/base).rglob('*.java')]
    return sorted(names+['build.gradle','settings.gradle','gradle.properties','config/probability.json'])


def current_audit(root):
    spec=importlib.util.spec_from_file_location('release_input_audit',Path(root)/'tools/audit-ci-inputs.py')
    value=importlib.util.module_from_spec(spec);spec.loader.exec_module(value)
    return value.audit(root,'HEAD',Path(root)/'build/web017')


def audit_summary(value):
    return {'sha':value['sha'],'fingerprints':value['fingerprints'],'worktree':value['worktree']}


def verify_inputs(root,result_file):
    root=Path(root);result_file=no_links(result_file);require(result_file.is_file(),'VERIFICATION_RESULT_MISSING')
    result=load_json(result_file.read_bytes());require(isinstance(result,dict),'INVALID_VERIFICATION_RESULT')
    require(result.get('status')=='passed' and result.get('dry_run') is False and not result.get('first_failure'),'VERIFICATION_NOT_PASSED')
    run=result.get('run_dir');require(isinstance(run,str) and re.fullmatch(r'build/verify/[A-Za-z0-9_.-]+',run),'INVALID_VERIFICATION_DIRECTORY')
    require(result_file==root.absolute()/run/'result.json','NONCANONICAL_VERIFICATION_RESULT')
    now=current_audit(root);summary=audit_summary(now)
    require(summary['worktree']['git_status_clean'] and summary['worktree']['matches_ref_bytes'],'SOURCE_NOT_CLEAN')
    require(re.fullmatch(r'[0-9a-f]{40}',result.get('tested_sha','')) and result['tested_sha']==summary['sha'],'SOURCE_SHA_MISMATCH')
    require(audit_summary(result['input_audit'])==summary and audit_summary(result['final_audit'])==summary,'VERIFICATION_INPUT_DRIFT')
    planned=result['plan'];suites=result['suites']
    require(planned['tier']=='full' and [s['id'] for s in planned['suites']]==list(REQUIRED_SUITES)
            and [s['id'] for s in suites]==list(REQUIRED_SUITES) and all(s['status']=='passed' for s in suites),'INCOMPLETE_VERIFICATION')
    for s in suites:
        if s['id'] in INTERNAL:continue
        require(s.get('log')==s['id']+'.log','INVALID_SUITE_LOG')
        log=no_links(root/run/s['log']);require(log.is_file() and hash_file(log)==s['log_sha256'],'VERIFICATION_LOG_CHANGED')
    runtime=root/'build/web-runtime';files=scan(runtime);require('runtime-build.json' in files,'RUNTIME_STAMP_MISSING')
    stamp=load_json(files['runtime-build.json'].read_bytes())
    built=next(s for s in suites if s['id']=='java-full')
    require(built.get('runtime_build_sha256')==hash_file(files['runtime-build.json']),'RUNTIME_STAMP_NOT_VERIFIED')
    require(stamp.get('schema')==1 and stamp.get('sourceSha')==summary['sha'],'RUNTIME_SOURCE_MISMATCH')
    sources={n:hash_file(no_links(root/n)) for n in runtime_sources(root)}
    require(stamp.get('sourceFiles')==sources,'RUNTIME_INPUT_DRIFT')
    jars={n:hash_file(p) for n,p in files.items() if n!='runtime-build.json'}
    require(stamp.get('files')==jars and 'server.jar' in jars and any(n.startswith('lib/') for n in jars)
            and all(n=='server.jar' or re.fullmatch(r'lib/[A-Za-z0-9_.-]+\.jar',n) for n in jars),'RUNTIME_FILES_DRIFT')
    backend_contract(files['server.jar'].read_bytes())
    return result,now,stamp,files


def resource_gates(root):
    # Same fixed image/audio distribution checks as npm postbuild; no recoding or regeneration.
    for module_name,file,function in [('release_images','build-web-images.py','check_distribution'),('release_audio','build-web-audio.py','distribution')]:
        spec=importlib.util.spec_from_file_location(module_name,Path(root)/'tools'/file)
        value=importlib.util.module_from_spec(spec)
        import sys
        sys.modules[module_name]=value;spec.loader.exec_module(value)
        if function=='distribution':value.distribution(Path(root),Path(root)/'web-client/dist',os.environ.get('FFMPEG','ffmpeg'))
        else:value.check_distribution(Path(root),Path(root)/'web-client/dist')


def budgets(files):
    out={k:0 for k in ['totalStaticBytes','codeBytes','codeGzipBytes','imageBytes','bgmBytes','soundsBytes','textBytes']}
    for name,data in files.items():
        safe_name(name);size=len(data);out['totalStaticBytes']+=size;suffix=PurePosixPath(name).suffix.lower()
        if suffix in ('.html','.js','.mjs','.css'):
            out['codeBytes']+=size;out['codeGzipBytes']+=len(gzip.compress(data,mtime=0))
        elif name.startswith('assets/images/') and suffix in ('.webp','.avif','.png'):
            require(size<=BUDGET['singleImage'],'SINGLE_IMAGE_BUDGET');out['imageBytes']+=size
        elif name.startswith('assets/audio/bgm/') and suffix in ('.mp3','.ogg','.opus','.m4a','.aac'):
            require(size<=BUDGET['singleBgm'],'SINGLE_BGM_BUDGET');out['bgmBytes']+=size
        elif name.startswith('assets/audio/sounds/') and suffix in ('.mp3','.ogg','.opus','.m4a','.aac'):out['soundsBytes']+=size
        elif suffix in ('.json','.txt'):out['textBytes']+=size
        else:raise ValueError('UNCLASSIFIED_STATIC_FILE')
    for field,key in [('totalStaticBytes','static'),('codeBytes','code'),('codeGzipBytes','gzip'),('imageBytes','images'),('bgmBytes','bgm'),('soundsBytes','sounds'),('textBytes','text')]:
        require(out[field]<=BUDGET[key],'STATIC_BUDGET_EXCEEDED:'+key)
    require('index.html' in files and any(n.endswith('.js') for n in files),'FRONTEND_BUILD_MISSING')
    return out


def backend_contract(data):
    try:
        with zipfile.ZipFile(io.BytesIO(data)) as z:
            names=z.namelist()
            require(len(names)==len(set(names)) and len(names)<=MAX_FILES,'INVALID_SERVER_JAR')
            require({'com/vocr/web/GuestGameServer.class','com/vocr/application/GameSession.class','com/vocr/logging/WebDiagnostics.class'}<=set(names),'SERVER_ENTRY_MISSING')
            require(not any(n.startswith(('org/junit/','org/gradle/','BGM/','sounds/','images/','data/')) or
                            'SeededGuestGameServer' in n or n.lower().endswith(('.wav','.png','.webp')) for n in names),'SERVER_JAR_BOUNDARY')
    except zipfile.BadZipFile as error:raise ValueError('INVALID_SERVER_JAR') from error


def launchers(config_hash):
    sh='''#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
[[ $# -le 1 ]] || { echo 'Supply at most one port.' >&2; exit 64; }
port=${1:-7070}
[[ "$port" =~ ^[0-9]{1,5}$ ]] && (( 10#$port >= 1 && 10#$port <= 65535 )) || { echo 'Invalid port.' >&2; exit 64; }
[[ -f config/probability.json && ! -L config/probability.json ]] || { echo 'Probability config missing.' >&2; exit 1; }
actual=$(sha256sum -- config/probability.json)
[[ "${actual%% *}" == "CONFIG_HASH" ]] || { echo 'Probability config changed; rebuild the release.' >&2; exit 1; }
java_bin=java
if [[ -n ${JAVA_HOME:-} ]]; then java_bin="$JAVA_HOME/bin/java"; fi
exec "$java_bin" -Xms32m -Xmx256m -XX:MaxMetaspaceSize=192m -XX:ActiveProcessorCount=2 -Djava.awt.headless=true -cp 'backend/server.jar:backend/lib/*' com.vocr.web.GuestGameServer "$port"
'''.replace('CONFIG_HASH',config_hash)
    cmd='''@echo off
setlocal
cd /d "%~dp0.."
if not "%~2"=="" exit /b 64
set "PORT=%~1"
if "%PORT%"=="" set "PORT=7070"
if not exist "config\\probability.json" exit /b 1
powershell -NoProfile -NonInteractive -Command "if ((Get-FileHash -LiteralPath 'config/probability.json' -Algorithm SHA256).Hash -ne 'CONFIG_HASH') { exit 1 }"
if errorlevel 1 exit /b 1
set "JAVA_BIN=java"
if defined JAVA_HOME set "JAVA_BIN=%JAVA_HOME%\\bin\\java.exe"
"%JAVA_BIN%" -Xms32m -Xmx256m -XX:MaxMetaspaceSize=192m -XX:ActiveProcessorCount=2 -Djava.awt.headless=true -cp "backend\\server.jar;backend\\lib\\*" com.vocr.web.GuestGameServer "%PORT%"
exit /b %errorlevel%
'''.replace('CONFIG_HASH',config_hash)
    return sh.encode(),cmd.replace('\n','\r\n').encode()


def payload(root,result_file):
    result,now,stamp,runtime=verify_inputs(root,result_file);resource_gates(root)
    static={n:p.read_bytes() for n,p in scan(Path(root)/'web-client/dist').items()};budget=budgets(static)
    built=next(s for s in result['suites'] if s['id']=='web-build')
    require(built.get('frontend_files')=={n:{'bytes':len(b),'sha256':sha(b)} for n,b in static.items()},'FRONTEND_OUTPUT_DRIFT')
    files={'frontend/'+n:b for n,b in static.items()}
    files['LICENSE']=no_links(Path(root)/'LICENSE').read_bytes()
    for n,p in runtime.items():files[('metadata/runtime-build.json' if n=='runtime-build.json' else 'backend/'+n)]=p.read_bytes()
    config=no_links(Path(root)/'config/probability.json').read_bytes();files['config/probability.json']=config
    files['backend/run.sh'],files['backend/run.cmd']=launchers(sha(config))
    files['RELEASE-README.txt']=('Web build candidate. Not production deployment or release approval.\n'
        'Linux: Java 21+, bash and sha256sum; backend/run.sh [port] (127.0.0.1 only).\n'
        'Windows launcher requires PowerShell; native Windows execution remains unverified.\n'
        'Use sha256sum -c SHA256SUMS before running. Do not change probability.json in place.\n'
        'Serve frontend separately through the trusted same-origin HTTPS proxy specified in WEB-043.\n'
        'No Vite, Gradle, source checkout or original WAV/PNG is required for the backend.\n'
        'Browser local replay/records are not stored in this package.\n').encode()
    package=load_json((Path(root)/'web-client/package.json').read_bytes())
    manifest={'schema':1,'kind':'vocr-web-runtime','source':{'sha':now['sha'],'fingerprints':now['fingerprints']},
              'runtime':{'javaMinimum':21,'javaBuildVersion':stamp['javaVersion'],'gradleVersion':stamp['gradleVersion'],
                         'frontendVersion':package['version'],'frontendDependencies':{**package['dependencies'],**package['devDependencies']}},
              'verification':{'result':result['run_dir']+'/result.json','resultSha256':hash_file(result_file),
                              'status':'passed','suites':[{'id':s['id'],'status':s['status']} for s in result['suites']],
                              'scope':'full verification precedes packaging; packaged smoke is attested by the external release report'},
              'budgets':budget,'budgetLimits':dict(BUDGET),
              'files':{n:{'bytes':len(b),'sha256':sha(b)} for n,b in sorted(files.items())},
              'notCovered':['production deployment/HTTPS/proxy','capacity/long game','manual A06/A07/A08/046/049','native Windows launch']}
    files['release-manifest.json']=json_bytes(manifest)
    files['SHA256SUMS']=''.join(sha(b)+'  '+n+'\n' for n,b in sorted(files.items())).encode()
    require(sum(map(len,files.values()))<=MAX_ARCHIVE,'RELEASE_TOO_LARGE')
    return files


def write_archive(files,target):
    with zipfile.ZipFile(target,'x',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as z:
        for name,data in sorted(files.items()):
            entry=zipfile.ZipInfo(safe_name(name),(1980,1,1,0,0,0));entry.create_system=3
            entry.external_attr=(stat.S_IFREG|(0o755 if name=='backend/run.sh' else 0o644))<<16
            entry.compress_type=zipfile.ZIP_DEFLATED;z.writestr(entry,data,compresslevel=9)


def build_candidate(root,result_file,output):
    root=Path(root).absolute();output=no_links(output)
    require(output.is_relative_to(root/'build') if hasattr(output,'is_relative_to') else (root/'build') in output.parents,'OUTPUT_MUST_BE_IN_BUILD')
    require(not output.exists(),'OUTPUT_ALREADY_EXISTS')
    files=payload(root,result_file)
    output.parent.mkdir(parents=True,exist_ok=True)
    temporary=Path(tempfile.mkdtemp(prefix='.web-release-',dir=output.parent))
    try:
        target=temporary/'vocr-web.zip';write_archive(files,target);check_release(target)
        temporary.rename(output);return output/target.name
    finally:
        if temporary.exists():shutil.rmtree(temporary)


def read_archive(archive):
    archive=no_links(archive);require(archive.is_file() and archive.stat().st_size<=MAX_ARCHIVE,'INVALID_ARCHIVE_SIZE')
    try:
        with zipfile.ZipFile(archive) as z:
            entries=z.infolist();require(0<len(entries)<=MAX_FILES and sum(i.file_size for i in entries)<=MAX_ARCHIVE,'ARCHIVE_EXPANSION_LIMIT')
            files={};seen=set()
            for i in entries:
                n=safe_name(i.filename);require(n.casefold() not in seen,'DUPLICATE_ARCHIVE_PATH');seen.add(n.casefold())
                mode=i.external_attr>>16
                require(not i.is_dir() and stat.S_IFMT(mode) in (0,stat.S_IFREG) and not (i.flag_bits&1) and i.file_size<=MAX_FILE,'UNSAFE_ARCHIVE_ENTRY')
                files[n]=z.read(i)
            return files
    except (zipfile.BadZipFile,RuntimeError) as error:raise ValueError('INVALID_ARCHIVE') from error


def check_release(archive):
    files=read_archive(archive);require({'release-manifest.json','SHA256SUMS'}<=set(files),'MANIFEST_MISSING')
    manifest=load_json(files['release-manifest.json']);require(manifest.get('schema')==1 and manifest.get('kind')=='vocr-web-runtime','INVALID_MANIFEST')
    require(re.fullmatch(r'[0-9a-f]{40}',manifest['source']['sha']) and HEX.fullmatch(manifest['verification']['resultSha256']),'INVALID_MANIFEST_SOURCE')
    entries=manifest['files'];require(set(entries)|{'release-manifest.json','SHA256SUMS'}==set(files),'RELEASE_FILE_SET_MISMATCH')
    for n,row in entries.items():require(row=={'bytes':len(files[n]),'sha256':sha(files[n])},'RELEASE_HASH_MISMATCH')
    sums=''.join(sha(b)+'  '+n+'\n' for n,b in sorted(files.items()) if n!='SHA256SUMS').encode()
    require(files['SHA256SUMS']==sums,'CHECKSUM_LIST_MISMATCH')
    require(manifest['budgetLimits']==BUDGET,'BUDGET_POLICY_MISMATCH')
    static={n[len('frontend/'):]:b for n,b in files.items() if n.startswith('frontend/')}
    require(manifest['budgets']==budgets(static),'BUDGET_REPORT_MISMATCH')
    require(sum(len(b) for n,b in files.items() if n.startswith('backend/'))<=64*MIB,'BACKEND_SAFETY_LIMIT')
    allowed={'backend/server.jar','backend/run.sh','backend/run.cmd','config/probability.json','metadata/runtime-build.json','LICENSE','RELEASE-README.txt','release-manifest.json','SHA256SUMS'}
    require(all(n in allowed or n.startswith('frontend/') or re.fullmatch(r'backend/lib/[A-Za-z0-9_.-]+\.jar',n) for n in files),'UNEXPECTED_RELEASE_FILE')
    require(allowed<=set(files) and any(n.startswith('backend/lib/') for n in files),'REQUIRED_RELEASE_FILE_MISSING');backend_contract(files['backend/server.jar'])
    sh,cmd=launchers(sha(files['config/probability.json']));require(files['backend/run.sh']==sh and files['backend/run.cmd']==cmd,'LAUNCHER_MISMATCH')
    stamp=load_json(files['metadata/runtime-build.json']);require(stamp['sourceSha']==manifest['source']['sha'],'RUNTIME_MANIFEST_SOURCE_MISMATCH')
    actual={n[len('backend/'):]:sha(b) for n,b in files.items() if n=='backend/server.jar' or n.startswith('backend/lib/')}
    require(stamp['files']==actual and stamp['sourceFiles']['config/probability.json']==sha(files['config/probability.json']),'RUNTIME_MANIFEST_FILES_MISMATCH')
    require(manifest['verification']['status']=='passed' and manifest['verification']['suites']==[{'id':n,'status':'passed'} for n in REQUIRED_SUITES],'VERIFICATION_DECLARATION_MISMATCH')
    return manifest


def extract_checked(archive,destination):
    check_release(archive);files=read_archive(archive);destination=no_links(destination)
    require(not destination.exists(),'EXTRACTION_DESTINATION_EXISTS')
    destination.mkdir(parents=True)
    try:
        for name,data in files.items():
            p=destination/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(data)
            if name=='backend/run.sh':p.chmod(0o755)
    except BaseException:
        shutil.rmtree(destination);raise
    return destination
