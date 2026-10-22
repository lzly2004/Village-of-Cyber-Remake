import importlib.util
import json
import os
from pathlib import Path
import select
import signal
import subprocess
import sys
import tempfile
import time
import unittest
from unittest import mock

TOOLS = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location("guard", TOOLS / "guarded-process.py")
guard = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(guard)


class ChildIdentityTest(unittest.TestCase):
    def test_old_awk_bug_is_reproduced_without_signals(self):
        old = '''{ parent[$1]=$2 } END { owned[root]=1;
            do { changed=0; for(pid in parent)
                if(!owned[pid] && owned[parent[pid]]) {owned[pid]=1;changed=1}
            } while(changed); for(pid in owned) print pid }'''
        result = subprocess.run(["awk", "-v", "root=100", old],
                                input="100 1\n101 100\n200 1\n201 200\n",
                                text=True, capture_output=True, check=True)
        self.assertTrue({"200", "201", "1"}.issubset(result.stdout.split()))

    def test_special_pids_are_never_signalled(self):
        with mock.patch.object(guard.os, "kill") as kill, mock.patch.object(guard.os, "waitid") as wait:
            for pid in (-100, 0, 1, os.getpid(), os.getppid()):
                self.assertFalse(guard.signal_child(pid, signal.SIGTERM))
            kill.assert_not_called()
            wait.assert_not_called()

    def test_recycled_or_nonchild_pid_is_never_signalled(self):
        with mock.patch.object(guard.os, "waitid", side_effect=ChildProcessError), \
                mock.patch.object(guard.os, "kill") as kill:
            self.assertFalse(guard.signal_child(123456, signal.SIGTERM))
            kill.assert_not_called()

    def test_zombie_is_not_signalled_or_reaped_during_identity_check(self):
        with mock.patch.object(guard.os, "waitid", return_value=object()) as wait, \
                mock.patch.object(guard.os, "kill") as kill:
            self.assertFalse(guard.signal_child(123456, signal.SIGTERM))
            self.assertTrue(wait.call_args[0][2] & os.WNOWAIT)
            kill.assert_not_called()

    def test_only_verified_live_child_receives_positive_pid_signal(self):
        with mock.patch.object(guard.os, "waitid", return_value=None), \
                mock.patch.object(guard.os, "kill") as kill:
            self.assertTrue(guard.signal_child(123456, signal.SIGTERM))
            kill.assert_called_once_with(123456, signal.SIGTERM)

    def test_low_memory_refuses_before_launch_without_exhausting_memory(self):
        with mock.patch.object(guard, "enable_subreaper"), mock.patch.object(guard.subprocess, "Popen") as launch:
            self.assertEqual(75, guard.run(["must-not-run"], memory_reader=lambda: 0))
            launch.assert_not_called()

    def test_unsupported_adoption_refuses_before_launch(self):
        with mock.patch.object(guard, "enable_subreaper", side_effect=RuntimeError("unsupported")), \
                mock.patch.object(guard.subprocess, "Popen") as launch:
            self.assertEqual(78, guard.run(["must-not-run"]))
            launch.assert_not_called()

    def test_unreadable_child_list_fails_without_any_signal_fallback(self):
        process = mock.Mock(pid=123456)
        process.poll.return_value = 0
        with mock.patch.object(guard, "enable_subreaper"), \
                mock.patch.object(guard.subprocess, "Popen", return_value=process), \
                mock.patch.object(guard, "reap_children"), \
                mock.patch.object(guard, "direct_children", side_effect=OSError("unreadable")), \
                mock.patch.object(guard.os, "kill") as kill:
            self.assertEqual(70, guard.run(["fake"], memory_reader=lambda: 8388608))
            kill.assert_not_called()


class IsolatedProcessTest(unittest.TestCase):
    def setUp(self):
        # This sibling is outside the supervisor's subtree; it must survive ALL cases.
        self.sentinel = subprocess.Popen([sys.executable, "-c", "import time; time.sleep(20)"])

    def tearDown(self):
        self.assertIsNone(self.sentinel.poll(), "unrelated sentinel was terminated")
        self.sentinel.terminate()
        self.sentinel.wait(timeout=3)

    def launch(self, *command):
        return subprocess.Popen([sys.executable, "-B", str(TOOLS / "guarded-process.py"), "--", *command],
                                stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, bufsize=1)

    def test_success_and_failure_exit_codes_preserved(self):
        for code in (0, 7):
            process = self.launch(sys.executable, "-c", "raise SystemExit({})".format(code))
            out, err = process.communicate(timeout=6)
            self.assertEqual(code, process.returncode, (out, err))

    def test_actual_sibling_pid_is_rejected_by_kernel_child_check(self):
        code = ("import runpy,signal,sys; g=runpy.run_path(sys.argv[1]); "
                "assert g['signal_child'](int(sys.argv[2]),signal.SIGTERM) is False")
        process = self.launch(sys.executable, "-B", "-c", code,
                              str(TOOLS / "guarded-process.py"), str(self.sentinel.pid))
        out, err = process.communicate(timeout=6)
        self.assertEqual(0, process.returncode, (out, err))
        self.assertIsNone(self.sentinel.poll())

    def tree_case(self, mode, stop_signal=None, low_memory=False):
        command = [sys.executable, "-B", str(TOOLS / "tests/fake-workload.py"), "root", mode]
        if low_memory:
            code = ("import runpy,sys,time\ng=runpy.run_path(sys.argv[1])\ncalls=[0]\n"
                    "def memory():\n calls[0]+=1\n"
                    " if calls[0]==1: return 8388608\n"
                    " time.sleep(0.5)\n return 0\n"
                    "sys.exit(g['run'](sys.argv[2:],memory_reader=memory))\n")
            process = subprocess.Popen([sys.executable, "-B", "-c", code,
                                        str(TOOLS / "guarded-process.py"), *command],
                                       stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, bufsize=1)
        else:
            process = self.launch(*command)
        pids = []
        try:
            deadline = time.monotonic() + 4
            # Read one byte at a time: avoid TextIOWrapper prefetch hiding ready lines.
            line = b""
            while len(pids) < 3 and time.monotonic() < deadline:
                ready, _, _ = select.select([process.stdout.fileno()], [], [], 0.2)
                if not ready:
                    continue
                chunk = os.read(process.stdout.fileno(), 1)
                if not chunk:
                    break
                line += chunk
                if chunk == b"\n":
                    entry = json.loads(line)
                    self.assertEqual(entry["pid"], entry["sid"], "fixture must really create separate sessions")
                    pids.append(entry["pid"])
                    line = b""
            self.assertEqual(3, len(pids), "all detached fixture processes must actually start")
            if stop_signal:
                process.send_signal(stop_signal)  # Our own unreaped Popen child only.
            out, err = process.communicate(timeout=7)
            expected = 75 if low_memory else 128 + stop_signal if stop_signal else 0
            self.assertEqual(expected, process.returncode, (out, err))
            for pid in pids:
                self.assertFalse(Path("/proc/{}".format(pid)).exists(), "owned fixture not reaped: {}".format(pid))
            self.assertIsNone(self.sentinel.poll())
        finally:
            if process.poll() is None:
                process.terminate()
                process.communicate(timeout=7)

    def test_normal_exit_adopts_and_cleans_detached_descendants(self):
        self.tree_case("exit")

    def test_sigterm_cleans_detached_descendants_without_touching_sibling(self):
        self.tree_case("wait", signal.SIGTERM)

    def test_sigint_cleans_detached_descendants_without_touching_sibling(self):
        self.tree_case("wait", signal.SIGINT)

    def test_simulated_memory_drop_cleans_only_owned_descendants(self):
        self.tree_case("wait", low_memory=True)

    def test_shell_entry_with_noop_command_releases_lock_and_preserves_sibling(self):
        with tempfile.TemporaryDirectory(prefix="vocr-guard-test-") as directory:
            env = dict(os.environ, VOCR_GRADLE="/bin/true", XDG_RUNTIME_DIR=directory)
            for _ in range(2):
                result = subprocess.run(["bash", str(TOOLS / "run-gradle-safe.sh"), "test", "--tests", "fake"],
                                        env=env, capture_output=True, text=True, timeout=6)
                self.assertEqual(0, result.returncode, result.stderr)
                self.assertIsNone(self.sentinel.poll())
            rejected = subprocess.run(["bash", str(TOOLS / "run-gradle-safe.sh"), "test"],
                                      env=env, capture_output=True, text=True, timeout=6)
            self.assertEqual(64, rejected.returncode)


if __name__ == "__main__":
    unittest.main()
