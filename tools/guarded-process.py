#!/usr/bin/env python3
"""Linux-only child supervisor. No process-table traversal or group signals.

PR_SET_CHILD_SUBREAPER adopts descendants when their parents exit, including
detached Gradle daemons. Before EVERY signal, waitid(WNOWAIT) verifies that the
positive PID is our unreaped child. This single-threaded process never reaps
between that verification and kill: the PID cannot be recycled in that window.
SIGKILL of the supervisor itself cannot be handled; this is not a cgroup.
"""
import ctypes
import os
from pathlib import Path
import signal
import subprocess
import sys
import time


MIN_AVAILABLE_KIB = 2 * 1024 * 1024


def enable_subreaper():
    if sys.platform != "linux" or not hasattr(os, "WNOWAIT"):
        raise RuntimeError("Linux child adoption and waitid(WNOWAIT) are required")
    libc = ctypes.CDLL(None, use_errno=True)
    # Match prctl(int, unsigned long, ...) explicitly, including pointer width.
    prctl = libc.prctl
    prctl.argtypes = [ctypes.c_int] + [ctypes.c_ulong] * 4
    prctl.restype = ctypes.c_int
    if prctl(36, 1, 0, 0, 0) != 0:  # PR_SET_CHILD_SUBREAPER
        raise OSError(ctypes.get_errno(), "Cannot enable child adoption")
    enabled = ctypes.c_int()
    if prctl(37, ctypes.addressof(enabled), 0, 0, 0) != 0 or enabled.value != 1:
        raise RuntimeError("Child adoption could not be verified")


def available_kib():
    for line in Path("/proc/meminfo").read_text().splitlines():
        if line.startswith("MemAvailable:"):
            return int(line.split()[1])
    raise RuntimeError("MemAvailable is unavailable")


def direct_children():
    # Kernel-provided direct children only, NOT the global process table.
    value = Path("/proc/self/task/{}/children".format(os.getpid())).read_text()
    return [int(pid) for pid in value.split()]


def signal_child(pid, sig):
    if pid <= 1 or pid in (os.getpid(), os.getppid()):
        return False
    try:
        exited = os.waitid(os.P_PID, pid, os.WEXITED | os.WNOHANG | os.WNOWAIT)
    except (ChildProcessError, ProcessLookupError):
        return False  # Not ours, stale PID, or already reaped: NEVER signal it.
    if exited is not None:
        return False  # Zombie: leave reaping to the cleanup loop.
    os.kill(pid, sig)  # One positive, verified, unreaped child PID. Never killpg.
    return True


def reap_children(process):
    while True:
        try:
            pid, status = os.waitpid(-1, os.WNOHANG)
        except ChildProcessError:
            return
        if pid == 0:
            return
        if pid == process.pid:
            process.returncode = (os.WEXITSTATUS(status) if os.WIFEXITED(status)
                                  else -os.WTERMSIG(status))


def cleanup_children(process):
    started = time.monotonic()
    # Killing a parent makes its detached descendants our direct children.
    # Discover them only through the kernel's child list, then verify via waitid.
    while time.monotonic() - started < 4:
        reap_children(process)
        children = direct_children()
        if not children:
            return True
        sig = signal.SIGTERM if time.monotonic() - started < 1 else signal.SIGKILL
        for pid in children:
            try:
                signal_child(pid, sig)
            except (OSError, RuntimeError) as error:
                print("Child cleanup failed: {}".format(error), file=sys.stderr)
        time.sleep(0.05)
    reap_children(process)
    return not direct_children()


def run(command, memory_reader=available_kib):
    process = None
    requested_signal = [0]
    old_handlers = {}
    result = 78
    cleanup_ok = True

    def request_stop(signum, _frame):
        requested_signal[0] = signum  # No async signalling/reaping in the handler.

    try:
        enable_subreaper()  # Fail closed BEFORE starting the command.
        if memory_reader() < MIN_AVAILABLE_KIB:
            print("REFUSED: MemAvailable is below 2 GiB.", file=sys.stderr)
            return 75
        for sig in (signal.SIGTERM, signal.SIGINT):
            old_handlers[sig] = signal.signal(sig, request_stop)
        process = subprocess.Popen(command, start_new_session=True, close_fds=True)
        while True:
            if requested_signal[0]:
                result = 128 + requested_signal[0]
                break
            status = process.poll()
            if status is not None:
                result = status if status >= 0 else 128 - status
                break
            if memory_reader() < MIN_AVAILABLE_KIB:
                print("ABORT: MemAvailable fell below 2 GiB.", file=sys.stderr)
                result = 75
                break
            time.sleep(0.1)
    except (OSError, RuntimeError, ValueError) as error:
        print("REFUSED/ABORTED: {}".format(error), file=sys.stderr)
        result = 78
    finally:
        if process is not None:
            try:
                cleanup_ok = cleanup_children(process)
            except (OSError, RuntimeError, ValueError) as error:
                print("Cannot verify child cleanup: {}".format(error), file=sys.stderr)
                cleanup_ok = False  # Never fall back to process-table killing.
        for sig, handler in old_handlers.items():
            signal.signal(sig, handler)
    if not cleanup_ok:
        print("Owned children remain; inspect them before another build.", file=sys.stderr)
        return 70
    return result


if __name__ == "__main__":
    args = sys.argv[1:]
    if args[:1] == ["--"]:
        args = args[1:]
    if not args:
        print("Supply a command after --", file=sys.stderr)
        sys.exit(64)
    sys.exit(run(args))
