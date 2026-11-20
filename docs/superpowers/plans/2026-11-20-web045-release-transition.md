# WEB-045 Release Transition Implementation Plan

> **For agentic workers:** Execute sequentially in this session; only bounded read-only exploration/security review may be delegated. One JVM/worker.

**Goal:** Prove and document same-origin upgrade/restart/rollback without losing browser history.

**Architecture:** Reuse release ZIP integrity and extraction, add an explicit transition preparation CLI and packaged-browser acceptance. Existing frontend and storage formats stay unchanged.

**Tech Stack:** Existing Python standard library, Playwright/Vite, guarded Java release launcher.

**Spec:** ../specs/2026-11-20-web045-release-transition-design.md

## Global Constraints

- No auto deployment, service termination, profile manipulation, storage migrations, network settings, Ready/merge or fees.
- SHA verification is tied to external official identities, not self-authentication.
- Both versions live in new isolated directories; only directly owned test child processes are normally stopped.
- Keep 2 GiB threshold and one JVM. Do not repeat local full Java after tooling/docs-only changes.

## Review Focus

- Bad SHA or mutated archives never reach a runnable prepared release.
- Origin changes never claim automatic data migration.
- Old session IDs never become valid in a fresh process and no command is silently replayed.
- Replay payload, slots, ledger and preferences remain byte/field identical across all transitions.
- Rollback is proven only for the exact tested pair, not future schema changes.

### Task 1: Transition preparation
Files: tools/web_upgrade.py, tools/prepare-web-upgrade.py, tools/tests/test_web_upgrade.py; existing release-tests integration.
- [x] Add negative and successful preparation tests using existing release fixtures.
- [x] Implement checked origins, trusted SHA verification/snapshots and new-directory extraction with a fixed plan.
- [x] Run related tool unit suites and syntax checks.

### Task 2: Browser acceptance
Files: tools/upgrade-smoke.py, tools/playwright.upgrade.config.mjs, web-client/upgrade-tests/upgrade.spec.js; existing hosting health test.
- [x] Implement serialized packaged A/B backend+frontend lifecycle via existing guarded-process supervision and checked backend/run.sh.
- [x] Seed valid records/replay/slot/preferences once; check exact persistence, real 404/UI invalidation/no auto-create and explicit new-game creation on B/restart/A.
- [x] Run initial acceptance with existing real releases and preserve source identities/screenshots.
- [x] Verify 044 health through actual Caddy proxy in existing hosting suite.

### Task 3: Unified delivery
Files: docs/web-upgrade-rollback.md, current status/task/acceptance, build/web045/publication.
- [x] Write operator runbook and user acceptance instructions; get independent safety review.
- [ ] Commit locally after scoped checks; user has authorized pushing/updating draft PR13/cloud CI/artifact delivery in this round.
- [ ] Run full24 on exact head, verify official artifacts/receipt/package; preserve failures and exact validation boundaries.
- [ ] Run packaged A→official B/restart/rollback acceptance, prepare self-service candidate and give explicit test commands/expected results.
