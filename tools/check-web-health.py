#!/usr/bin/env python3
"""Single-shot local health probe; does not restart services or install a monitor."""
from web_health import main

if __name__ == '__main__':
    raise SystemExit(main())
