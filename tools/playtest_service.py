#!/usr/bin/env python3
"""Owned service worker. Must run below guarded-process; never launches a browser."""
import argparse
import http.client
import json
import os
from pathlib import Path
import signal
import shutil
import subprocess
import sys
import time
import web_hosting as H
import web_release as R
import playtest_candidate as P

FRONT, BACK = 18500, 17075


def configuration(frontend):
    root, _ = H.frontend(frontend)
    headers = {'Cache-Control': ['no-store'], 'Referrer-Policy': ['no-referrer'], 'X-Content-Type-Options': ['nosniff'],
               'X-Frame-Options': ['DENY'], 'Content-Security-Policy': ["default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; media-src 'self' blob:; connect-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'"]}
    def response(status, body, extra=None):
        return {'handler': 'static_response', 'status_code': status, 'body': body, 'headers': {**headers, **(extra or {})}}
    secure = {'handler': 'headers', 'response': {'set': headers, 'delete': ['Server']}}
    routes = [
        {'match': [{'not': [{'host': ['localhost', '127.0.0.1']}]}], 'handle': [response(421, 'Host refused.')], 'terminal': True},
        {'match': [{'path': ['/api', '/api/*']}], 'handle': [secure, {'handler': 'request_body', 'max_size': 16384},
            {'handler': 'reverse_proxy', 'upstreams': [{'dial': '127.0.0.1:' + str(BACK)}],
             'transport': {'protocol': 'http', 'dial_timeout': '3s', 'response_header_timeout': '15s', 'keep_alive': {'enabled': False}},
             'headers': {'request': {'delete': ['Forwarded', 'X-Real-Ip', 'X-Forwarded-For', 'X-Forwarded-Host', 'X-Forwarded-Proto'],
                                     'set': {'Host': ['127.0.0.1:' + str(BACK)]}}}}], 'terminal': True},
        {'match': [{'path': ['/', '/index.html', '/assets/*'], 'method': ['GET', 'HEAD']}],
         'handle': [secure, {'handler': 'file_server', 'root': str(root)}], 'terminal': True},
        {'match': [{'path': ['/', '/index.html', '/assets/*']}],
         'handle': [response(405, 'Method refused.', {'Allow': ['GET, HEAD']})], 'terminal': True},
        {'handle': [response(404, 'Not found.')], 'terminal': True}]
    return {'admin': {'disabled': True, 'config': {'persist': False}},
            'logging': {'logs': {'default': {'writer': {'output': 'discard'}}}},
            'apps': {'http': {'grace_period': '2s', 'servers': {'playtest': {
                'protocols': ['h1'], 'listen': ['127.0.0.1:' + str(FRONT)],
                'automatic_https': {'disable': True}, 'read_header_timeout': '5s', 'read_timeout': '15s',
                'write_timeout': '30s', 'idle_timeout': '30s', 'max_header_bytes': 16384, 'routes': routes,
                'errors': {'routes': [{'handle': [response('{http.error.status_code}', 'Proxy request failed.')]}]}}}}}}


def read_only_ready(port):
    connection = http.client.HTTPConnection('127.0.0.1', port, timeout=1)
    try:
        connection.request('GET', '/api/v1/__playtest_ready__')
        response = connection.getresponse(); raw = response.read(4097)
        return response.status == 404 and len(raw) <= 4096 and json.loads(raw).get('code') == 'HTTP_ERROR'
    except (OSError, ValueError, http.client.HTTPException): return False
    finally: connection.close()


def static_ready(payload):
    connection = http.client.HTTPConnection('127.0.0.1', FRONT, timeout=1)
    try:
        connection.request('GET', '/index.html', headers={'Host': 'localhost:' + str(FRONT)})
        response = connection.getresponse(); raw = response.read(R.MIB + 1)
        return response.status == 200 and len(raw) <= R.MIB and R.sha(raw) == R.hash_file(payload / 'frontend/index.html')
    except (OSError, http.client.HTTPException): return False
    finally: connection.close()


def run(payload, run_dir, java, caddy):
    parent = Path('/proc') / str(os.getppid()) / 'cmdline'
    command = parent.read_bytes().split(b'\0')
    R.require(os.fsencode(Path(__file__).resolve().parent / 'guarded-process.py') in command,
              'GUARDED_ENTRY_REQUIRED')
    R.require(shutil.which('choom') is not None, 'CHOOM_REQUIRED')
    payload = R.no_links(payload); run_dir = R.no_links(run_dir)
    P.check_extracted(payload); binary = H.checked_caddy(caddy)
    R.require(run_dir.is_dir() and run_dir.stat().st_uid == os.getuid(), 'RUN_DIRECTORY_REQUIRED')
    config = run_dir / 'caddy.json'; config.write_bytes(R.json_bytes(configuration(payload / 'frontend')))
    subprocess.run([str(binary), 'validate', '--config', str(config)], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    java_log = run_dir / 'backend.log'; processes = []; stop = [False]
    for sig in (signal.SIGINT, signal.SIGTERM): signal.signal(sig, lambda *_: stop.__setitem__(0, True))
    try:
        with java_log.open('wb') as out, (run_dir / 'proxy.log').open('wb') as proxy:
            processes.append(subprocess.Popen(['choom', '-n', '500', '--', str(java), '-Xms32m', '-Xmx256m', '-XX:MaxMetaspaceSize=192m',
                '-XX:ActiveProcessorCount=2', '-Djava.awt.headless=true', '-cp', 'backend/server.jar:backend/lib/*',
                'com.vocr.web.GuestGameServer', str(BACK)], cwd=payload, stdout=out, stderr=subprocess.STDOUT))
            java_deadline = time.monotonic() + 30
            listening = ('[HTTP] 游客 API: http://127.0.0.1:' + str(BACK) + '/api/v1/games').encode()
            while not stop[0] and not (listening in java_log.read_bytes() and read_only_ready(BACK)):
                if processes[0].poll() is not None or time.monotonic() >= java_deadline: return 71
                time.sleep(0.1)
            if stop[0]: return 0
            processes.append(subprocess.Popen([str(binary), 'run', '--config', str(config)], stdout=proxy, stderr=subprocess.STDOUT))
            deadline = time.monotonic() + 30
            ready = False
            while not stop[0]:
                if any(p.poll() is not None for p in processes): return 71
                if not ready:
                    listening = ('[HTTP] 游客 API: http://127.0.0.1:' + str(BACK) + '/api/v1/games').encode()
                    if listening in java_log.read_bytes() and read_only_ready(BACK) and read_only_ready(FRONT) and static_ready(payload):
                        oom = int((Path('/proc') / str(processes[0].pid) / 'oom_score_adj').read_text())
                        R.require(oom == 500, 'JVM_OOM_SCORE_REQUIRED')
                        (run_dir / 'ready.json').write_bytes(R.json_bytes({'status': 'ready', 'sourceSha': P.check_extracted(payload)['source']['sha'],
                            'probe': 'GET /api/v1/__playtest_ready__ -> 404 HTTP_ERROR', 'gameWrites': 0,
                            'backendPid': processes[0].pid, 'backendOomScore': oom}))
                        ready = True
                    elif time.monotonic() >= deadline: return 71
                time.sleep(0.1)
            return 0
    finally:
        # Do not signal/reap children here: the outer existing guardian verifies and cleans its owned tree.
        # Even a Java bind failure must not cause a PID-file/process-table fallback.
        pass


def main():
    p = argparse.ArgumentParser(); p.add_argument('--payload', required=True, type=Path)
    p.add_argument('--run-dir', required=True, type=Path); p.add_argument('--java', required=True, type=Path)
    p.add_argument('--caddy', required=True, type=Path); a = p.parse_args()
    try: return run(a.payload, a.run_dir, a.java, a.caddy)
    except (OSError, ValueError, KeyError, subprocess.SubprocessError):
        print('START_FAILED', file=sys.stderr); return 71


if __name__ == '__main__': sys.exit(main())
