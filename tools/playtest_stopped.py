"""Loopback-only, read-only explanation after owned playtest services stop.

No payload files, game APIs, credentials, telemetry or restart actions are served.
The terminal coordinator owns this socket; status/stop and Ctrl+C remain usable.
"""
from http.server import BaseHTTPRequestHandler, HTTPServer
import json

REASONS = {
    'LOW_MEMORY': '可用内存低于 2 GiB，试玩安全监护已停止服务。',
    'START_FAILED': '试玩后端或代理已退出，服务未能继续运行。',
    'BROWSER_START_FAILED': '试玩浏览器未能正常启动，服务已停止。',
}


class StoppedSite:
    def __init__(self, reason, port=18500):
        if reason not in REASONS:
            raise ValueError('INVALID_STOP_REASON')
        outer = self

        class Handler(BaseHTTPRequestHandler):
            def log_message(self, *_args): pass  # Never log capability URLs/queries.

            def answer(self):
                port = outer.server.server_address[1]
                if self.headers.get('Host') not in ('localhost:'+str(port), '127.0.0.1:'+str(port)):
                    status, body, kind = 421, b'Invalid host', 'text/plain; charset=utf-8'
                elif self.path.startswith('/api/'):
                    status, kind = 503, 'application/json'
                    body = json.dumps({'code':'PLAYTEST_SERVICE_STOPPED','reason':reason,
                                       'message':REASONS[reason]+' 当前活局不能继续；本地战绩和Replay保留。请查看试玩终端。'},ensure_ascii=False).encode()
                else:
                    status, kind = 503, 'text/html; charset=utf-8'
                    body = ('<!doctype html><html lang="zh-CN"><meta charset="utf-8">'
                            '<title>试玩服务已停止</title><h1>试玩服务已停止</h1><p>'+REASONS[reason]+
                            '</p><p>原因码：'+reason+'</p><p>当前活局不能续玩；本地战绩、Replay和音量没有删除。</p>'
                            '<p>请回到启动终端查看记录；释放内存后，正常关闭试玩浏览器，'
                            '在终端按 Ctrl+C 停止此说明页，再运行同一启动脚本。不会自动重开或重发操作。</p></html>').encode()
                self.send_response(status)
                for name,value in {'Content-Type':kind,'Content-Length':str(len(body)),
                                   'Cache-Control':'no-store','X-Content-Type-Options':'nosniff',
                                   'Referrer-Policy':'no-referrer','Content-Security-Policy':"default-src 'none'; frame-ancestors 'none'",'Connection':'close'}.items():
                    self.send_header(name,value)
                self.end_headers()
                if self.command!='HEAD': self.wfile.write(body)

            do_GET = answer
            do_HEAD = answer
            do_POST = answer
            do_DELETE = answer
            do_PUT = answer
            do_OPTIONS = answer

        class BoundedServer(HTTPServer):
            def get_request(self):
                connection, address = super().get_request()
                connection.settimeout(.5)  # A partial request must not freeze terminal stop/status.
                return connection, address
        self.server = BoundedServer(('127.0.0.1',port),Handler)
        self.server.timeout = .05

    def poll(self): self.server.handle_request()
    def close(self): self.server.server_close()
    def __enter__(self): return self
    def __exit__(self,*_args): self.close()
