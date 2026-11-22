#!/usr/bin/env python3
"""Verify two official releases and prepare isolated directories; no automatic deployment."""
import argparse
import json
import sys
import zipfile
from web_upgrade import prepare


def main(argv=None):
    parser=argparse.ArgumentParser(description=__doc__)
    for name in ('current','current-sha256','candidate','candidate-sha256','current-origin','output'):
        parser.add_argument('--'+name,required=True)
    parser.add_argument('--candidate-origin')
    args=parser.parse_args(argv)
    try:
        plan=prepare(args.current,args.current_sha256,args.candidate,args.candidate_sha256,
                     args.current_origin,args.candidate_origin or args.current_origin,args.output)
        print(json.dumps(plan,ensure_ascii=False,indent=2));return 0
    except (OSError,ValueError,KeyError,TypeError,zipfile.BadZipFile):
        print('UPGRADE_PREPARATION_REFUSED',file=sys.stderr);return 2

if __name__=='__main__':raise SystemExit(main())
