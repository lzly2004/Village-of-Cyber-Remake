"""Harmless bounded workload for supervisor regression; never launches a JVM."""
import json
import os
import signal
import subprocess
import sys
import time

role = sys.argv[1]
if role != "root":
    signal.signal(signal.SIGTERM, signal.SIG_IGN)
if role in ("root", "detached"):
    subprocess.Popen([sys.executable, "-B", __file__, "detached" if role == "root" else "leaf"],
                     start_new_session=True)
print(json.dumps({"role": role, "pid": os.getpid(), "ppid": os.getppid(), "sid": os.getsid(0)}), flush=True)
if role == "root" and len(sys.argv) > 2 and sys.argv[2] == "exit":
    time.sleep(0.3)
else:
    time.sleep(15)  # Bounded even if the supervisor unexpectedly dies.
