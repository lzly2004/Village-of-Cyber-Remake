# WEB-044 Operations Implementation Plan

> **For agentic workers:** Follow these steps sequentially in the current session; do not spawn implementation agents or run concurrent JVMs.

**Goal:** Provide safe operational failures, a minimal health endpoint and a bounded local alert probe.

**Architecture:** Extend the existing fixed-event logger and GuestGameServer. Keep proxy raw logging disabled. A standard-library Python probe validates only a fixed loopback health response.

**Tech Stack:** Existing Java21/Javalin7, JUnit5, Python3 standard library.

**Spec:** ../specs/2026-11-20-web044-operations-design.md

## Global Constraints

- No game rules, dependencies, formats, public deployment, notifications or process cleanup changes.
- One worker/JVM, MemAvailable >= 2 GiB; targeted guarded tests first.
- Production Java cannot be committed before separately authorized full checks/package.
- Preserve Windows data and IDE edits; no writes to read-only mount.

## Review Focus

- No request/session/exception data can reach the log or health response.
- Health checks do not create, reap, refresh or borrow a game.
- Readiness degrades on reaper failure and recovers only after a successful cycle.
- Probe rejects redirects, proxy environment, unbounded responses and untrusted TLS.
- Normal completion/close stays quiet while Replay remains intact.

### Task 1: Fixed diagnostics and health contract

Files: src/web/java/com/vocr/logging/WebDiagnostics.java, src/web/java/com/vocr/web/GuestGameServer.java; tests under src/test/java/com/vocr/{logging,web}.

- [x] Add targeted failing assertions for fixed-event time/severity, health response and no game effects, reaper failure/recovery and terminal quietness.
- [x] Implement fixed metadata and a health handler behind existing admission; factor scheduled reap into a testable package-local operation without changing the registry algorithm.
- [x] Run guarded filtered JUnit tests and preserve output under build/web044.

### Task 2: Single-shot operational probe

Files: tools/web_health.py, tools/check-web-health.py, tools/tests/test_web_health.py.

- [x] Test input boundaries, fixed results, malformed/large responses, redirect/timeout/TLS errors and proxy isolation.
- [x] Implement direct loopback HTTP/TLS with a shared monotonic network deadline; use verified TLS, fixed path and capped reading, without environment proxies or redirects.
- [x] Run Python and existing hosting unit tests; add real loopback probe smoke.

### Task 3: Review and handoff

Files: docs/web-operations.md, task/status/acceptance documentation, build/web044 verification record.

- [x] Document operator commands, severity and limits; preserve failure evidence and list unexecuted gates.
- [x] Obtain independent security review of final diff and address supported findings.
- [x] User authorized final full validation after implementation/targeted checks; full693/check/probability/runtime/FatJAR passed with one isolated-display environment retry. No push or deployment.
