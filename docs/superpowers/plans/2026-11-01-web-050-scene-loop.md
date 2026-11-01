# WEB-050 Desktop Scene Loop Implementation Plan

> **For agentic workers:** Execute natively in this session, task by task; the user approved the five-step plan and CR acceptance refinements. No additional design question round.

**Goal:** Restore the desktop title → graphical configuration → game → settlement → title → second game loop inside 01, and successful-but-empty CO feedback.

**Architecture:** Keep authoritative guest state separate from local title/select/game presentation. Reuse audited images and existing Replay/record/help components; keep active game components mounted while browsing title to avoid losing local reading/vote state. New-game selection explicitly releases an existing capability before creating exactly one replacement; failed or uncertain cleanup blocks creation.

**Tech Stack:** Existing Vue 3, CSS logical 1280×720, Playwright and Node tests; no dependencies or API/schema changes.

**Spec:** User-approved five-step plan and CR refinements (2026-11-01); `docs/web-desktop-scene-matrix.md` is the executable source/action inventory.

## Global Constraints

- Pure frontend risk tier: unit, production build, related/default browsers, real gate, assets/audio/production smoke, sequential single worker and ≥2 GiB JVM threshold. Local full/FatJAR requires separate approval and is not part of these commands.
- No budget increases: 512 KiB/image, 32 MiB images, 1.5 MiB cold title and 3 MiB incremental first game; existing audio/package limits unchanged.
- Title/select and local entry/return navigation issue no game command; selection starts once. No private-field inference, storage-format change, automatic retry or cursor acknowledgement.
- PR #13 remains draft; no merge, main/settings/secrets changes. Deliver a single final candidate after verification, user full playtest remains required.

## Review Focus

- Empty successful CO versus failed/uncertain request: only confirmed empty replies show source notice; cursor stays unchanged.
- Title opened during live game: browsing local libraries cannot release or advance it; guarded replacement handles delete failure without creating a second game.
- Saving result/Replay: menu browsing keeps components and database contents, no duplicate record or implicit Replay save/delete.
- Restoring/uncertain/in-flight requests: local actions never race create/delete, preserved credentials are recoverable.
- Audio and layout: help/title/game scenes select exclusive BGM, original button order/geometry, no hidden legacy start flow or hidden profile mutation.

### Task 1: Source inventory and lifecycle
**Files:** docs/web-desktop-scene-matrix.md; web-client/src/useGuestGame.js; web-client/src/DesktopVoteWindow.vue; web-client/tests/desktop-scene-loop.spec.js.
**Interfaces:** claim requests return `{kind:'empty'|'events'}` only for an accepted, same-session/same-revision reply; error/blocked returns undefined. New `startReplacing(configuration)` uses existing release+start only after confirmed cleanup.
- [x] Add failing cases for roles/players empty/events/error; check no extra command and unchanged confirmed cursor.
- [x] Return confirmed claim outcome from the common run wrapper; source one-second notice via existing tip, without swallowing network diagnostics.
- [x] Confirm title actions retain capability and start replacement never proceeds after failed cleanup.

### Task 2: Original title and configuration scenes
**Files:** web-client/src/DesktopStartScene.vue; web-client/src/desktop-start.css; web-client/src/App.vue; web-client/src/BgmControl.vue; web-client/src/bgmScene.js.
**Interfaces:** DesktopStartScene consumes `scene: title|select`, `blocked`, emits navigate/start; App owns local scene, BgmControl consumes an explicit local menu music hint.
- [x] Add tests for source title logo (600,200,554,138), six buttons in source insertion/focus order at (660,400), 309×68, gaps300/80; setup images435×138 at (30,20), offsets465/168.
- [x] Build title/select inside journal, original assets already in audited manifest; no API on navigation. Configuration button starts directly, no old radio-card/start screen.
- [x] Wire source no-op buttons without fabricated behavior; add explicit resume-current-game browser control outside core where needed.
- [x] Test keyboard/geometry at1440/1024/800; missing images remain diagnosed, budgets unchanged.

### Task 3: Entry/return and complete verification
**Files:** web-client/src/App.vue; web-client/src/ReplayEntry.vue; web-client/test-fixtures/desktop-navigation.js; existing journeys and asset/audio tests; docs/web-desktop-scene-matrix.md; docs/web-desktop-core-window.md.
**Interfaces:** ReplayEntry exposes explicit open/close, menu return exits only the local panel; original components remain authoritative for library/read/error behavior.
- [x] Bind Replay/records/help to existing implementations, explicit return to title; local feature entry changes music only, not guest state.
- [x] Replace legacy launch preconditions with explicit graphical navigation in tests; preserve business and recovery assertions. Update image allowlist for newly visible original title/setup art, retain byte budgets and lazy private/player loading checks.
- [x] Add synthetic full-loop and delete-error cases plus extend real seeded gate to title/second start and cleanup; retain true random production smoke.
- [x] Run unit/build/targeted → default/gate/assets/audio/production sequentially on final code; preserve failure logs and record actual counts.
- [ ] Commit verified implementation and publish the final candidate within existing PR #13 authorization; verify exact-head CI/artifact/receipt before user playtest. Matrix and screenshots reviewed; do not claim user acceptance or full visual parity.
