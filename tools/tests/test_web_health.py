"""Operational probe transport contracts and isolated loopback deadline regressions."""
import contextlib
import importlib.util
import io
import os
from pathlib import Path
import socketserver
import ssl
import subprocess
import tempfile
import threading
import time
import unittest
from unittest.mock import patch
from http.server import BaseHTTPRequestHandler, HTTPServer

spec=importlib.util.spec_from_file_location('web_health',Path(__file__).resolve().parents[1]/'web_health.py')
H=importlib.util.module_from_spec(spec);spec.loader.exec_module(H)
URL='http://127.0.0.1:7070/api/v1/health'

class SocketFixture:
    def __init__(self,data):self.data=io.BytesIO(data);self.timeouts=[];self.sent=b''
    def recv_into(self,buffer):return self.data.readinto(buffer)
    def settimeout(self,value):self.timeouts.append(value)
    def sendall(self,value):self.sent+=value
    def __enter__(self):return self
    def __exit__(self,*args):pass

class HealthTest(unittest.TestCase):
    def test_rejects_unsafe_url_without_network(self):
        for url in ['https://example.com/api/v1/health','http://127.1/api/v1/health',
                    'http://localhost.evil/api/v1/health','file:///api/v1/health',
                    'http://secret@localhost/api/v1/health',URL+'?secret',URL+'?',URL+'#',
                    URL+'/extra',URL+'\n','http://localhost:99999/api/v1/health',
                    'http://localhost:/api/v1/health','http://localhost/other',
                    'http://localhost\\@evil/api/v1/health']:
            with self.subTest(url=url),patch.object(H,'fetch') as network:
                with self.assertRaises(ValueError):H.check(url)
                network.assert_not_called()

    def test_timeout_and_ca_input_refused(self):
        for timeout in [0,31,float('nan'),float('inf'),True,'5']:
            with self.subTest(timeout=timeout),self.assertRaises(ValueError):H.check(URL,timeout)
        with self.assertRaises(ValueError):H.check(URL,ca_file='secret')

    def test_response_contract_and_size_are_strict(self):
        for body,content_type,expected in [(b'{"status":"ok"}','application/json',True),
            (b'{"status":"unavailable"}','application/json',False),
            (b'{"status":"ok","secret":1}','application/json',False),
            (b'{"status":"ok"}','text/html',False),(b'null','application/json',False),
            (b'{"status":"ok"}'+b' '*256,'application/json',False),
            (b'\xff','application/json',False),(b'not json','application/json',False)]:
            data=b'HTTP/1.1 200 OK\r\nContent-Type: '+content_type.encode()+b'\r\nContent-Length: '+str(len(body)).encode()+b'\r\n\r\n'+body
            with self.subTest(body=body),patch.object(H.socket,'create_connection',return_value=SocketFixture(data)):
                self.assertEqual(expected,H.check(URL))

    def test_transport_errors_do_not_become_success(self):
        for error in [TimeoutError('PRIVATE'),ssl.SSLError('PRIVATE'),H.http.client.IncompleteRead(b'PRIVATE')]:
            with self.subTest(error=error),patch.object(H,'fetch',side_effect=error):
                self.assertFalse(H.check(URL))

    def test_tls_verification_is_preserved_for_every_scheme_case(self):
        for scheme in ['https','HTTPS','HtTpS']:
            with self.subTest(scheme=scheme),patch.object(H,'fetch',return_value=True) as fetch:
                self.assertTrue(H.check(scheme+'://localhost:18443/api/v1/health'))
                parts,timeout,context=fetch.call_args.args
                self.assertEqual('https',parts.scheme)
                self.assertTrue(context.check_hostname)
                self.assertEqual(ssl.CERT_REQUIRED,context.verify_mode)
                self.assertTrue(H.check(scheme+'://localhost:18443/api/v1/health',ca_file=None))

    def test_cli_never_echoes_private_arguments_or_transport_errors(self):
        for argv in [['--url','http://PRIVATE@localhost/api/v1/health'],['--PRIVATE'],['--timeout','PRIVATE'],['--ca-file','PRIVATE']]:
            output=io.StringIO()
            with contextlib.redirect_stdout(output),contextlib.redirect_stderr(output):
                self.assertEqual(2,H.main(argv))
            self.assertEqual('HEALTH_CHECK_REFUSED\n',output.getvalue())
        for ok in [True,False]:
            output=io.StringIO()
            with patch.object(H,'check',return_value=ok),contextlib.redirect_stdout(output):
                self.assertEqual(0 if ok else 1,H.main([]))
            self.assertEqual('HEALTH_OK\n' if ok else 'HEALTH_FAILED\n',output.getvalue())

    def test_actual_loopback_redirect_is_not_followed_and_environment_proxy_is_ignored(self):
        paths=[]
        class Handler(BaseHTTPRequestHandler):
            def log_message(self,*args):pass
            def do_GET(self):
                paths.append(self.path)
                if len(paths)==1:
                    self.send_response(302);self.send_header('Location','/PRIVATE');self.end_headers()
                else:
                    self.send_response(200);self.send_header('Content-Type','application/json');self.end_headers()
                    self.wfile.write(b'{"status":"ok"}')
        server=HTTPServer(('127.0.0.1',0),Handler)
        worker=threading.Thread(target=server.serve_forever,daemon=True);worker.start()
        try:
            with patch.dict(os.environ,{'http_proxy':'http://127.0.0.1:1','HTTP_PROXY':'http://127.0.0.1:1','no_proxy':'','NO_PROXY':''}):
                url='http://127.0.0.1:'+str(server.server_port)+'/api/v1/health'
                self.assertFalse(H.check(url));self.assertTrue(H.check(url))
            self.assertEqual(['/api/v1/health','/api/v1/health'],paths)
        finally:server.shutdown();server.server_close();worker.join()

    def test_real_tls_accepts_explicit_ca_rejects_untrusted_and_never_downgrades(self):
        class Handler(BaseHTTPRequestHandler):
            def log_message(self,*args):pass
            def do_GET(self):
                self.send_response(200);self.send_header('Content-Type','application/json');self.end_headers()
                self.wfile.write(b'{"status":"ok"}')
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);cert=root/'test.pem';key=root/'test.key';conf=root/'req.cnf'
            conf.write_text('[req]\ndistinguished_name=dn\nx509_extensions=ext\nprompt=no\n[dn]\nCN=localhost\n[ext]\nsubjectAltName=DNS:localhost,IP:127.0.0.1\nbasicConstraints=critical,CA:TRUE\nkeyUsage=digitalSignature,keyCertSign\nextendedKeyUsage=serverAuth\n')
            subprocess.run(['openssl','req','-x509','-newkey','rsa:2048','-nodes','-days','1','-config',str(conf),
                            '-keyout',str(key),'-out',str(cert)],check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
            context=ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER);context.load_cert_chain(str(cert),str(key))
            server=HTTPServer(('127.0.0.1',0),Handler)
            server.socket=context.wrap_socket(server.socket,server_side=True)
            worker=threading.Thread(target=server.serve_forever,daemon=True);worker.start()
            try:
                url='HTTPS://localhost:'+str(server.server_port)+'/api/v1/health'
                self.assertTrue(H.check(url,ca_file=str(cert)))
                self.assertFalse(H.check(url))
            finally:server.shutdown();server.server_close();worker.join()
            server=HTTPServer(('127.0.0.1',0),Handler)
            worker=threading.Thread(target=server.serve_forever,daemon=True);worker.start()
            try:
                self.assertFalse(H.check('HTTPS://localhost:'+str(server.server_port)+'/api/v1/health',1,ca_file=str(cert)))
            finally:server.shutdown();server.server_close();worker.join()

    def test_total_deadline_covers_continuous_100_headers_and_slow_body(self):
        for kind in ['interim','body']:
            stop=threading.Event()
            class Handler(socketserver.BaseRequestHandler):
                def handle(self):
                    self.request.settimeout(2)
                    self.request.recv(4096)
                    try:
                        if kind=='body':self.request.sendall(b'HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: 15\r\n\r\n')
                        while not stop.is_set():
                            self.request.sendall(b'HTTP/1.1 100 Continue\r\n\r\n' if kind=='interim' else b' ')
                            stop.wait(.1)
                    except OSError:pass
            server=socketserver.TCPServer(('127.0.0.1',0),Handler)
            worker=threading.Thread(target=server.serve_forever,daemon=True);worker.start()
            try:
                start=time.monotonic()
                self.assertFalse(H.check('http://127.0.0.1:'+str(server.server_address[1])+'/api/v1/health',1))
                elapsed=time.monotonic()-start
                self.assertLess(elapsed,2,kind)
                self.assertGreater(elapsed,.8,kind)
            finally:stop.set();server.shutdown();server.server_close();worker.join()

if __name__=='__main__':unittest.main()
