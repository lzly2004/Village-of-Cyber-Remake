# WEB-050 Desktop Voting Parity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restore the actual desktop voting scene, menu/selection workflow, vote-result interlude, public skill markers and calendar/message typography inside the game window.

**Architecture:** Vue presentation components consume existing audited public DTOs and existing command callbacks. A local vote-review state displays each authoritative round before any execution dialogue, without sending commands or advancing the event cursor. Desktop Java renderers and their original images define structure and menus; Web diagnostic/help content stays outside this window.

**Tech Stack:** Existing Vue/JavaScript/CSS, original PNG-to-WebP manifest, Node unit tests and Playwright. No dependencies or production Java/API/storage changes.

**Spec:** User-approved six-item correction in the 2026-10-31 conversation; `docs/web-desktop-visual-alignment.md`. The user requests direct implementation and one complete candidate, and explicitly waives summoning independent CR during this batch; this does not authorize merge/settings or local full Java execution.

## Global Constraints

- Baseline cd682cb; preserve original NTFS dirty tree and PR12 head117eda9; no checkouts of that tree.
- 1280×720 desktop logical coordinates; information board (0,198), text (40,228), calendar image (880,10), calendar text (890,25), message user-approved (260,490,760,230), body26px and information/vote24px at baseline.
- Preserve public-only information, FINISHED reveal gate, command ownership/revision and cursor semantics. Never infer private roles/skills or recalculate vote totals/leaders.
- Existing 1.5MiB/3MiB/100MiB budgets, single worker/JVM, ≥2GiB JVM admission and safe entry unchanged. Local full/package and external publication follow their own authorization requirements.
- All existing product capabilities remain accessible in the corresponding desktop menus; do not keep an extra all-controls form in the main information board.

## Review Focus

- Old event callbacks or resync must not dismiss/confirm vote review or disclose execution/ending early.
- Tie rounds show authoritative rows in order, local next/revote does not repeat POST votes.
- CO interruption is not a completed vote; it displays dialogue rather than a fabricated result.
- Target skill markers require public reports only; missing/order-out-of-art-range is diagnosed outside core rather than guessed.
- 44 players and long history stay keyboard/scroll accessible without hiding row two; responsive adaptation must not scale day-start text independently.

### Task 1: Public desktop formatting and markers
**Files:** create `web-client/src/desktopVoteText.js`, `DesktopInformation.vue`; modify existing unit/public-information-text.test.js and player-visual.test.js.
**Interfaces:** `desktopInformation(information, name):string`; `publicSkillMarkers(information, players, day):Map<number,Marker[]>`; `desktopVoteRound(vote, roundIndex, name):{title,lines,conclusion}`. Marker fields icon/actor/day/order/role/target. Names use existing character short names.
- [x] Pin five desktop section headings, arrow chains, exposed reports, empty information, guard reports and public-only access in unit tests.
- [x] Implement formatting from public skillReports/deathHistory and marker placement from ImageNames.skillResultIcon; reject unknown colors/order instead of infer.
- [x] Source-lock GameStrings constants for result/death formatting where applicable and document trailing-space display normalization.

### Task 2: Actual menu and selection scenes
**Files:** replace DesktopVoteWindow.vue; create DesktopTargetSelection.vue and DesktopVoteReview.vue; modify App.vue/desktop-dialogue.css.
**Interfaces:** DesktopVoteWindow receives update/players/bindings/playerName/toggleTarget plus slots for public top avatars/avoidance; state home/orders/claims/designations/vote/divination/guard/claimPlayers/records/suspicion/history/voteConfirm. All commands call existing unique useGuestGame callbacks. Mode switches are local only.
- [x] Main board displays only desktop five-section text. No notices, suspicion, command forms or history mounted there by default.
- [x] Restore original right-button image sets at desktop coordinates; original selection panel avatar/frame/result/drag/delete art, pointer drag/drop and keyboard equivalent; claimed actor vs hidden/all scopes remain distinct.
- [x] Restore records menu and read-only suspicion/history views; back navigation focuses its originating button.
- [x] Retain no-read/no-uncertain/no-busy command protections, existing legal-target and CO drafts semantics.

### Task 3: Vote-result interlude
**Files:** App.vue, useGuestGame.js only if required, new presentation helper/unit tests and tests/desktop-voting-parity.spec.js.
**Interfaces:** local vote-review key session/day, authoritative rounds, current round and continue. It is not an API phase and sends zero writes.
- [x] Successful completed vote displays all lines/counts/method/candidates and authoritative tie/leader summary before execution; interrupted CO bypasses vote review.
- [x] Local continue advances rounds, then releases execution; refresh before the execution cursor can reconstruct the review, stale responses cannot open another session's review.
- [x] During review do not mount execution art/dialogue or reveal roles/record settlement; closing review never calls nextEvent.

### Task 4: Exact resources, markers and text geometry
**Files:** DesktopVoteWindow.vue, PlayerPortrait.vue/App.vue, desktop-dialogue.css.
- [x] Use UiImage.CALENDAR hiduke.png; restore26px day-start and existing typewriter behavior with the same container-relative scale as normal dialogue.
- [x] Draw original result1/order black/white and result2/order black/white on public targets with desktop historical row ordering and source actor death cutoff; retain CO/death overlays.
- [x] Capture same-data desktop/Web reference pairs for home, menus, selection, vote outcome/tie, day-start and 44-person scenarios; inspect actual PNGs before delivery.

### Task 5: Final validation and complete-game candidate
**Files:** relevant old/new browser tests, shared presentation fixture where necessary, docs/web-desktop-core-window.md, review-cd682cb.md and task status.
- [x] Adapt old layout/selector expectations only when contradicted by approved desktop flow; preserve business/security/cursor assertions and explicitly navigate real menus.
- [x] Run unit, production build, targeted then final default/browser/assets/audio/gate serially through existing safe tools; reuse final same-source oracle only after matching fingerprints.
- [x] Explain exact remaining differences; no automatic golden refresh or threshold increases; no “perfect” claim based on CI alone.
- [ ] Commit validated semantic themes, prepare source-bound full candidate and one self-service complete-game entry after necessary publication/package authorization; no Ready/merge.


## Execution notes (2026-10-31)

- User explicitly authorized this six-item scope and waived calling independent CR for this batch; the missing executing-plans sub-skill is not available in the current catalog, so execution follows the approved checklist directly.
- Original Swing capture now covers13 scenes, with matching decision/settlement public DTOs exported for Web pairs. Same-data pairs cover menus/three selection types/CO/settlement; tie/44-person boundaries are synthetic public fixtures with distinct labels, and real full-game journeys remain independent.
- Actual capture showed EndAnimeHandler starts atx124 (one-based index), metadata210/460, row2portrait348; corrected together with removal of settlement CO corner badges. Original return-title artwork restored.
- Full-seer tool overlaps seer1 atx250 in Swing. Numbered tool coordinates retained; only the overlapping all-tool moved tox150 to preserve pointer access. This is an explicit delivery difference, not a hidden parity claim.
- Node sandbox wrapper did not execute individual test cases; all reported unit counts use the real unsandboxed Node runner. Final219/219 includes GameStrings fate wording/trimEnd source anchors.
- First default run212pass/2fail retained. Precise BGM locator corrected; two-page menu timeout requires frozen-source rerun. The next launch was refused beforeJVM because MemAvailable fell to~1.2GiB. Waiting for memory restoration; no threshold relaxation, no full Java/package invocation, no commit while verification incomplete.

- Records fidelity follow-up: original day-image chooser, per-day round buttons at right slots2/3, first10/remaining two-column suspicion/history, and original one-second translucent hints added. Relevant22-case run retained21pass/1 virtual-clock precondition failure, corrected before final rerun.
- Final cross-configuration gate15/15, assets5/5 (real wall time), native audio29/29 and unseeded production journey1/1 passed before this read-only menu follow-up. No production JVM/build changes or local full/package.

- Final related22/22 passed after frozen-clock precondition fix; unit219/219 and final production build passed. Protected NTFS136 hashes all match and default porcelain status bytes unchanged; expanded-untracked status is a different format, not compared as identical.
