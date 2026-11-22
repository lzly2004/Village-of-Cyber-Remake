#!/usr/bin/env python3
"""Self-service complete local game. No deployment, background updates or uploads."""
import argparse
import json
import os
import re
from pathlib import Path
import sys
import time
import zipfile
import playtest_runtime as V
import playtest_store as S
import web_release as R


def listing(home):
    parent = S.home_path(home) / 'candidates'
    return [R.load_json((p / 'candidate.json').read_bytes()) for p in sorted(parent.iterdir())
            if p.is_dir() and S.KEY.fullmatch(p.name)] if parent.exists() else []


def settings(home):
    path = S.home_path(home) / 'tools.json'
    R.require(path.is_file(), 'TOOLS_NOT_CONFIGURED')
    data = R.load_json(R.no_links(path).read_bytes())
    return {k: Path(data[k]) for k in ('java', 'caddy', 'browser')}


def choose(home):
    rows = listing(home)
    R.require(rows, 'NO_CANDIDATES')
    for i, row in enumerate(rows, 1): print(str(i) + '. ' + row['key'])
    raw = input('选择候选序号：').strip()
    R.require(raw.isdigit() and 1 <= int(raw) <= len(rows), 'INVALID_SELECTION')
    return rows[int(raw) - 1]['key']


def import_menu(home):
    print('从本轮交接下载package、verify、receipt三个ZIP与source-metadata.json。')
    metadata = Path(input('source-metadata.json路径：').strip()).expanduser()
    m = R.load_json(R.no_links(metadata).read_bytes())
    R.require(m.get('repository') == S.REPOSITORY and re.fullmatch(r'[0-9a-f]{40}', m['run']['head_sha'])
              and type(m['run']['id']) is int and type(m['run']['run_attempt']) is int, 'SOURCE_UNCONFIRMED')
    for name in ('verify', 'receipt', 'package'):
        R.require(type(m[name]['id']) is int and re.fullmatch(r'sha256:[0-9a-f]{64}', m[name]['digest']), 'SOURCE_UNCONFIRMED')
    print('请在官方GitHub运行页核对head/run/attempt及三个artifact的id/digest：')
    for name in ('verify', 'receipt', 'package'):
        print(name + ': ' + str(m[name]['id']) + ' ' + m[name]['digest'])
    print('head=' + m['run']['head_sha'] + ' run=' + str(m['run']['id']) + ' attempt=' + str(m['run']['run_attempt']))
    R.require(input('已从官方页面核对上述身份？输入CONFIRM（任意JSON本身不是来源证明）：') == 'CONFIRM', 'SOURCE_UNCONFIRMED')
    files = {n: Path(input(n + ' ZIP路径：').strip()).expanduser() for n in ('package', 'verify', 'receipt')}
    print(S.import_candidate(home, files['package'], files['verify'], files['receipt'], metadata, True))


def menu(home):
    while True:
        print('\n完整本地试玩 / http://localhost:18500/\n1 导入候选  2 候选列表  3 开始完整试玩\n4 运行状态  5 停止服务（活局失效）  6 创建反馈  7 首次配置工具  0 退出菜单')
        action = input('选择：').strip()
        try:
            if action == '0': return 0
            if action == '1': import_menu(home)
            elif action == '2': print(json.dumps(listing(home), ensure_ascii=False, indent=2))
            elif action == '3': V.start(home, choose(home), **settings(home))
            elif action == '4': print(json.dumps(V.control('status'), ensure_ascii=False, indent=2))
            elif action == '5': print(json.dumps(V.control('stop'), ensure_ascii=False, indent=2))
            elif action == '6': print(S.feedback(home, choose(home)))
            elif action == '7':
                data = {k: str(Path(input(k + ' 可执行文件绝对路径：').strip()).expanduser().absolute()) for k in ('java', 'caddy', 'browser')}
                V.tool_version(Path(data['java']), java=True); V.tool_version(Path(data['browser'])); V.H.checked_caddy(data['caddy'])
                S.atomic_json(S.home_path(home) / 'tools.json', data); print('已保存；启动不自动下载或安装。')
        except (OSError, ValueError, KeyError, TypeError, zipfile.BadZipFile) as error:
            reason = str(error) if isinstance(error, ValueError) and str(error).isupper() else 'PLAYTEST_REFUSED'
            print(V.MESSAGES.get(reason, reason), file=sys.stderr)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--home', type=Path)
    sub = parser.add_subparsers(dest='command')
    p = sub.add_parser('import'); p.add_argument('--artifact', type=Path, required=True)
    p.add_argument('--verify', type=Path, required=True); p.add_argument('--receipt', type=Path, required=True)
    p.add_argument('--metadata', type=Path, required=True); p.add_argument('--confirm-page', action='store_true')
    sub.add_parser('list'); sub.add_parser('status'); sub.add_parser('stop')
    p = sub.add_parser('start'); p.add_argument('key')
    for name in ('java', 'caddy', 'browser'): p.add_argument('--' + name, type=Path)
    p = sub.add_parser('configure')
    for name in ('java', 'caddy', 'browser'): p.add_argument('--' + name, type=Path, required=True)
    p = sub.add_parser('feedback'); p.add_argument('key'); p.add_argument('--attach', action='append', default=[], type=Path)
    args = parser.parse_args(argv)
    try:
        home = S.home_path(args.home)
        if args.command is None: return menu(home)
        if args.command == 'import': print(S.import_candidate(home, args.artifact, args.verify, args.receipt, args.metadata, args.confirm_page))
        elif args.command == 'list': print(json.dumps(listing(home), ensure_ascii=False, indent=2))
        elif args.command in ('status', 'stop'):
            value = V.control(args.command)
            if args.command == 'stop' and value['state'] == 'STOPPING':
                deadline = time.monotonic() + 10
                while value['state'] == 'STOPPING' and time.monotonic() < deadline:
                    time.sleep(0.1); value = V.control('status')
            print(json.dumps(value, ensure_ascii=False, indent=2))
            return 0 if value['state'] not in ('FAILED', 'STOPPING') else 1
        elif args.command == 'configure':
            V.tool_version(args.java, java=True); V.tool_version(args.browser); V.H.checked_caddy(args.caddy)
            S.atomic_json(home / 'tools.json', {k: str(getattr(args, k).absolute()) for k in ('java', 'caddy', 'browser')})
            print('Configured. No system installation or automatic updates.')
        elif args.command == 'start':
            values = {n: getattr(args, n) for n in ('java', 'caddy', 'browser')}
            if not all(values.values()): values = {**settings(home), **{k: v for k, v in values.items() if v is not None}}
            return V.start(home, args.key, **values)
        elif args.command == 'feedback': print(S.feedback(home, args.key, args.attach))
        return 0
    except (OSError, ValueError, KeyError, TypeError, zipfile.BadZipFile) as error:
        reason = str(error) if isinstance(error, ValueError) and str(error).isupper() else 'PLAYTEST_REFUSED'
        print(json.dumps({'state': 'FAILED', 'reason': reason, 'message': V.MESSAGES.get(reason, reason)}, ensure_ascii=False), file=sys.stderr)
        return 75 if reason == 'LOW_MEMORY' else 1
    except (EOFError, KeyboardInterrupt): return 130


if __name__ == '__main__': sys.exit(main())
