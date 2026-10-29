"""Terminal coordinator, socket control and browser isolation. Linux only."""
import fcntl
import json
import os
from pathlib import Path
import re
import signal
import socket
import subprocess
import sys
import time
import uuid
import playtest_store as S
import playtest_service as V
import web_hosting as H
import web_release as R

TOOLS = Path(__file__).resolve().parent
URL = 'http://localhost:18500/'
MESSAGES = {'PORT_IN_USE': '端口18500或17075已占用；043托管或其他服务可能正在运行。请自行确认并停止，不会自动结束任何程序。',
            'PROFILE_IN_USE': '此前的试玩浏览器仍在使用或留有锁；请正常关闭后再启动或切版。不会强杀浏览器或删除锁。',
            'LOW_MEMORY': '可用内存低于2GiB，服务拒绝启动或已安全停止；活局失效，本地历史未删除。',
            'STOPPED': '服务已停止。活局不能续玩；录像/战绩保留。请正常关闭试玩浏览器。',
            'START_FAILED': '服务未就绪，未打开游戏；查看本次本地运行记录。',
            'INTERRUPTED': '收到中断，只停止本次服务；浏览器未被强杀。'}


def runtime_directory():
    base = os.environ.get('XDG_RUNTIME_DIR')
    path = Path(base) / 'vocr-playtest' if base else Path('/tmp') / ('vocr-playtest-' + str(os.getuid()))
    directory = S.private_directory(path)
    R.require(len(os.fsencode(directory / 'control.sock')) < 104, 'RUNTIME_PATH_TOO_LONG')
    return directory


def check_ports():
    for port in (V.FRONT, V.BACK):
        with socket.socket() as s:
            # Match the servers' restart semantics; an active listener still refuses the bind.
            # This is only preflight: each actual server must bind successfully itself.
            s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            try: s.bind(('127.0.0.1', port))
            except OSError: raise ValueError('PORT_IN_USE') from None


def tool_version(path, java=False):
    path = R.no_links(path); R.require(path.is_file() and os.access(path, os.X_OK), 'TOOL_MISSING')
    raw = subprocess.check_output([str(path), '-version' if java else '--version'], stderr=subprocess.STDOUT, timeout=10).decode(errors='replace')
    if java:
        match = re.search(r'version "(\d+)', raw)
        R.require(match and int(match[1]) >= 21, 'JAVA_21_REQUIRED')
    else:
        match = re.search(r'^(?:Google Chrome|Chromium|Chrome)(?: for Testing)? \d[^\r\n]*', raw, re.M)
        R.require(match, 'CHROMIUM_REQUIRED')
        return match[0][:160]
    return raw.splitlines()[0][:160]


def owned_signal(process, sig):
    # A stale disk PID never reaches this function. The unreaped direct child is checked in-kernel.
    try:
        status = os.waitid(os.P_PID, process.pid, os.WEXITED | os.WNOHANG | os.WNOWAIT)
        if status is None: os.kill(process.pid, sig)
    except (ChildProcessError, ProcessLookupError): pass


def control(command):
    R.require(command in ('status', 'stop'), 'INVALID_CONTROL_COMMAND')
    d = runtime_directory()
    try:
        state = R.load_json((d / 'owner.json').read_bytes())
        with socket.socket(socket.AF_UNIX) as s:
            s.settimeout(5); s.connect(str(d / 'control.sock'))
            s.sendall(R.json_bytes({'command': command, 'token': state['token']}))
            data = s.recv(16384)
        return R.load_json(data)
    except (OSError, KeyError):
        last = R.load_json((d / 'last.json').read_bytes()) if (d / 'last.json').is_file() else {'state': 'STOPPED'}
        return {**last, 'liveCoordinator': False, 'note': 'No process was signalled; stale records do not prove services are absent.'}


def start(home, key, java, caddy, browser):
    R.require(sys.platform == 'linux', 'LINUX_REQUIRED')
    R.require(os.uname().machine in ('x86_64', 'amd64'), 'LINUX_AMD64_REQUIRED')
    home = S.home_path(home); d = runtime_directory()
    with R.no_links(d / 'run.lock').open('a') as lock:
        try: fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError: raise ValueError('ALREADY_RUNNING') from None
        R.require(S.KEY.fullmatch(key), 'INVALID_CANDIDATE_KEY')
        print('CHECKING ' + key, flush=True)
        meta, payload = S.candidate(home, key)
        java = R.no_links(java); browser = R.no_links(browser); caddy = H.checked_caddy(caddy)
        java_version = tool_version(java, java=True); browser_version = tool_version(browser)
        R.require(os.environ.get('DISPLAY') or os.environ.get('WAYLAND_DISPLAY'), 'VISIBLE_DISPLAY_REQUIRED')
        spec = __import__('importlib.util', fromlist=['util']).spec_from_file_location('play_guard', TOOLS / 'guarded-process.py')
        guard = __import__('importlib.util', fromlist=['util']).module_from_spec(spec); spec.loader.exec_module(guard)
        R.require(guard.available_kib() >= guard.MIN_AVAILABLE_KIB, 'LOW_MEMORY')
        profile, downloads = S.profiles(home, key); check_ports()
        run_dir = S.private_directory(S.private_directory(home / 'runs') / uuid.uuid4().hex)
        config = {'state': 'STARTING', 'candidate': key, 'sourceSha': meta['sourceSha'], 'url': URL,
                  'browserVersion': browser_version, 'javaVersion': java_version, 'runDirectory': str(run_dir), 'reason': None}
        S.atomic_json(run_dir / 'status.json', config)
        socket_path = d / 'control.sock'
        R.require(not socket_path.is_symlink(), 'SYMLINK_FORBIDDEN')
        if socket_path.exists(): socket_path.unlink()  # Only after exclusive lock; never signal a stored PID.
        stopping = [False]; interrupted = [False]; handlers = {}
        for sig in (signal.SIGINT, signal.SIGTERM):
            handlers[sig] = signal.signal(sig, lambda *_: (stopping.__setitem__(0, True), interrupted.__setitem__(0, True)))
        token = uuid.uuid4().hex; worker = None; browser_process = None; failure = None
        with socket.socket(socket.AF_UNIX) as server, (run_dir / 'guardian.log').open('wb') as log:
            server.bind(str(socket_path)); os.chmod(socket_path, 0o600); server.listen(4); server.settimeout(0.15)
            S.atomic_json(d / 'owner.json', {'token': token, 'runDirectory': str(run_dir)})
            try:
                argv = [sys.executable, '-B', str(TOOLS / 'guarded-process.py'), '--', sys.executable, '-B',
                        str(TOOLS / 'playtest_service.py'), '--payload', str(payload), '--run-dir', str(run_dir),
                        '--java', str(java), '--caddy', str(caddy)]
                worker = subprocess.Popen(argv, stdout=log, stderr=subprocess.STDOUT)
                deadline = time.monotonic() + 40
                print('STARTING ' + key + '\n' + URL + '\n停止服务会使活局失效；关闭浏览器不会自动停止服务。Ctrl+C停止自身服务。', flush=True)
                while not stopping[0]:
                    code = worker.poll()
                    if code is not None:
                        failure = 'LOW_MEMORY' if code == 75 else 'CLEANUP_FAILED' if code == 70 else 'START_FAILED'; break
                    if config['state'] == 'STARTING':
                        if (run_dir / 'ready.json').is_file():
                            ready = R.load_json((run_dir / 'ready.json').read_bytes())
                            R.require(ready['sourceSha'] == meta['sourceSha'], 'CANDIDATE_CHANGED')
                            # Browser is the coordinator's child, a sibling of guardian, never in its cleanup tree.
                            with (run_dir / 'browser-startup.log').open('wb') as browser_log:
                                browser_process = subprocess.Popen([str(browser), '--user-data-dir=' + str(profile),
                                    '--no-first-run', '--no-default-browser-check', '--disable-background-networking', '--disable-sync', URL],
                                    stdout=browser_log, stderr=subprocess.STDOUT, start_new_session=True)
                            config['state'] = 'READY'; S.atomic_json(run_dir / 'status.json', config)
                            print('READY ' + meta['sourceSha'] + '\n正常玩完整一局；终端控制不注入浏览器游戏页。', flush=True)
                        elif time.monotonic() >= deadline: failure = 'START_FAILED'; break
                    if browser_process is not None and browser_process.poll() is not None:
                        # May exit after delegating to an already-live profile; never assume it is safe to reuse.
                        config['browserProcessExited'] = True
                        if browser_process.returncode != 0:
                            failure = 'BROWSER_START_FAILED'; break
                    try:
                        connection, _ = server.accept()
                    except socket.timeout: continue
                    with connection:
                        connection.settimeout(1)
                        try:
                            request = R.load_json(connection.recv(4096))
                            R.require(request.get('token') == token and request.get('command') in ('status', 'stop'), 'CONTROL_REFUSED')
                            if request['command'] == 'stop': stopping[0] = True
                            connection.sendall(R.json_bytes({**config, 'state': 'STOPPING' if stopping[0] else config['state']}))
                        except (OSError, ValueError): pass
            finally:
                config['state'] = 'STOPPING'; S.atomic_json(run_dir / 'status.json', config)
                if worker is not None:
                    owned_signal(worker, signal.SIGTERM)
                    try: worker.wait(timeout=8)
                    except subprocess.TimeoutExpired: failure = 'CLEANUP_FAILED'  # No killpg or process-table fallback.
                config['state'] = 'FAILED' if failure else 'STOPPED'
                config['reason'] = failure or ('INTERRUPTED' if interrupted[0] else 'STOPPED')
                S.atomic_json(run_dir / 'status.json', config); S.atomic_json(d / 'last.json', config)
                # No browser signals: users close its window normally so IndexedDB can finish.
                if socket_path.exists(): socket_path.unlink()
                for sig, handler in handlers.items(): signal.signal(sig, handler)
                print(MESSAGES.get(config['reason'], config['reason']), flush=True)
            return 75 if failure == 'LOW_MEMORY' else 1 if failure else 130 if interrupted[0] else 0
