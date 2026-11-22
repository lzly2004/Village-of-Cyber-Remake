#!/usr/bin/env python3
"""Prepare two authenticated releases, then verify one-context same-origin transitions."""
import argparse
import json
import os
from pathlib import Path
import subprocess
import sys
import zipfile
from web_upgrade import prepare, acceptance_output
ROOT=Path(__file__).resolve().parents[1]


def main():
    p=argparse.ArgumentParser(description=__doc__)
    for name in ('current','current-sha256','candidate','candidate-sha256','output'):p.add_argument('--'+name,required=True)
    a=p.parse_args()
    try:
        # Normalize before containment checks, including any dot-dot components.
        out=acceptance_output(ROOT,a.output)
        plan=prepare(a.current,a.current_sha256,a.candidate,a.candidate_sha256,
                     'http://127.0.0.1:5180','http://127.0.0.1:5180',out)
        env={**os.environ,'VOCR_UPGRADE_PLAN':str(out/'transition-plan.json')}
        result=subprocess.run(['npm','--prefix','web-client','run','test:e2e','--',
                               '--config=../tools/playwright.upgrade.config.mjs'],cwd=ROOT,env=env)
        if result.returncode:return result.returncode
        (out/'pair-acceptance.json').write_text(json.dumps({'status':'passed','current':plan['current'],
            'candidate':plan['candidate'],'origin':plan['current_origin'],'scope':'same-context packaged A/B/restart/A; not public TLS deployment'},indent=2)+'\n')
        return 0
    except (OSError,ValueError,KeyError,TypeError,zipfile.BadZipFile):
        print('UPGRADE_ACCEPTANCE_REFUSED',file=sys.stderr);return 2
if __name__=='__main__':raise SystemExit(main())
