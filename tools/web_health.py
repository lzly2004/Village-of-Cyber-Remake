"""Bounded loopback readiness probe; no request data or exceptions in diagnostics."""
import argparse
import http.client
import json
import math
import io
import socket
import time
import ssl
import sys
import urllib.parse

MAX_BODY = 256


class DeadlineReader(io.RawIOBase):
    """Every underlying read shares one deadline, including HTTP 100/header loops."""
    def __init__(self, sock, deadline):
        self.sock, self.deadline = sock, deadline

    def readable(self):
        return True

    def readinto(self, buffer):
        self.sock.settimeout(remaining(self.deadline))
        return self.sock.recv_into(buffer)


class DeadlineSocket:
    def __init__(self, sock, deadline):
        self.sock, self.deadline = sock, deadline

    def makefile(self, mode):
        assert mode == 'rb'
        return io.BufferedReader(DeadlineReader(self.sock, self.deadline))


def remaining(deadline):
    value = deadline - time.monotonic()
    if value <= 0:
        raise TimeoutError('HEALTH_DEADLINE')
    return value


def fetch(parts, timeout, context):
    deadline = time.monotonic() + timeout
    port = parts.port or (443 if parts.scheme == 'https' else 80)
    # Fixed numeric loopback avoids resolver/DNS delays. localhost is still the TLS identity.
    with socket.create_connection(('127.0.0.1', port), remaining(deadline)) as raw:
        raw.settimeout(remaining(deadline))
        sock = context.wrap_socket(raw, server_hostname=parts.hostname) if context else raw
        try:
            sock.settimeout(remaining(deadline))
            request = ('GET /api/v1/health HTTP/1.1\r\nHost: ' + parts.hostname + ':' + str(port)
                       + '\r\nAccept: application/json\r\nConnection: close\r\n\r\n')
            sock.sendall(request.encode('ascii'))
            response = http.client.HTTPResponse(DeadlineSocket(sock, deadline))
            try:
                response.begin()
                # Oversized bodies and unexpected status/types never enter JSON parsing.
                if response.status != 200 or response.headers.get_content_type() != 'application/json':
                    return False
                body = response.read(MAX_BODY + 1)
                remaining(deadline)
                return len(body) <= MAX_BODY and json.loads(body.decode('utf-8')) == {'status': 'ok'}
            finally:
                response.close()
        finally:
            if sock is not raw:
                sock.close()


def checked_url(value):
    if not isinstance(value, str) or any(ord(c) <= 32 or ord(c) >= 127 for c in value):
        raise ValueError('URL_REFUSED')
    parts = urllib.parse.urlsplit(value)
    if (parts.scheme not in ('http', 'https') or parts.hostname not in ('127.0.0.1', 'localhost')
            or parts.username is not None or parts.password is not None
            or parts.path != '/api/v1/health' or parts.query or parts.fragment
            or '?' in value or '#' in value or '\\' in value or parts.netloc.endswith(':')):
        raise ValueError('URL_REFUSED')
    # Evaluate even if a malformed port would otherwise never be used.
    port = parts.port
    if port is not None and not 1 <= port <= 65535:
        raise ValueError('PORT_REFUSED')
    return parts


def check(url, timeout=5, ca_file=None):
    parts = checked_url(url)
    if isinstance(timeout, bool) or not isinstance(timeout, (int, float)) or not math.isfinite(timeout) or not 1 <= timeout <= 30:
        raise ValueError('TIMEOUT_REFUSED')
    if ca_file is not None and parts.scheme != 'https':
        raise ValueError('CA_REQUIRES_TLS')
    context = ssl.create_default_context(cafile=ca_file) if parts.scheme == 'https' else None
    try:
        return fetch(parts, timeout, context)
    except (OSError, ValueError, http.client.HTTPException):
        return False


class SafeParser(argparse.ArgumentParser):
    def error(self, message):
        raise ValueError('ARGUMENTS_REFUSED')


def main(argv=None):
    parser = SafeParser(description='Check one local Web readiness endpoint; exit 0/1/2.')
    parser.add_argument('--url', default='http://127.0.0.1:7070/api/v1/health')
    parser.add_argument('--timeout', type=float, default=5)
    parser.add_argument('--ca-file')
    try:
        args = parser.parse_args(argv)
        healthy = check(args.url, args.timeout, args.ca_file)
    except (OSError, ValueError):
        print('HEALTH_CHECK_REFUSED', file=sys.stderr)
        return 2
    print('HEALTH_OK' if healthy else 'HEALTH_FAILED')
    return 0 if healthy else 1
