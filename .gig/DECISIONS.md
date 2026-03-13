# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-12 — Structure: How to split the monolithic smoke test

**Decision:** Split the 2040-line `smoke-test.sh` into a runner script + individual test scripts per domain flow. Structure: `scripts/smoke-test.sh` becomes the runner (parses args, sources helpers, orchestrates execution). `scripts/tests/` contains one file per flow: `offline-schemas.sh`, `offline-builds.sh`, `transport.sh`, `tracks.sh`, `clips.sh`, `notes.sh`, `devices.sh`, `arranger.sh`, `mixer.sh`, `browser.sh`, `project.sh`, `clip-launcher.sh`, `errors.sh`. Each script is independently runnable (`./scripts/tests/transport.sh`) and also callable by the runner. The runner supports `--offline`, `--only <name>`, and `--list` flags.
**Rationale:** The user explicitly requested "separate dedicated scripts to test each integration or flow." Independent scripts let you test one flow without waiting for the full suite. The runner provides the same single-command convenience. Grouping by domain (transport, tracks, clips, etc.) maps naturally to the existing test sections and to the Bitwig API handlers.
**Alternatives considered:** (1) Split by test type (api-list vs behavioral vs error) — cross-cuts domains, harder to reason about. (2) One file per test section (50+ files) — too granular, most sections are 10-20 lines. (3) Keep monolithic, just fix bugs — doesn't address maintainability.
**Status:** ACTIVE
**ID:** D-19.1

## 2026-03-12 — Helpers: Shared test infrastructure

**Decision:** Extract helpers into `scripts/tests/_helpers.sh` (underscore prefix = not a test). Contains `rpc()`, `assert_contains()`, `assert_equals()`, `snapshot_field()`, counter management (`PASS`/`FAIL`/`TOTAL`), and connection variables (`BASE`, `PORT`). Each test script sources this file. The runner sources it once and passes the environment.
**Rationale:** All test scripts need the same assertion functions and RPC helper. A shared file avoids duplication. The underscore prefix convention makes it clear this isn't a standalone test.
**Alternatives considered:** (1) Inline helpers in each file — massive duplication. (2) Put helpers in the runner — individual scripts couldn't run standalone.
**Status:** ACTIVE
**ID:** D-19.2

## 2026-03-12 — Fixes: Address all known online test failures

**Decision:** Fix the 4 remaining online failures: (1) Test 16 cursor track — add a guard that skips if only 1 track exists. (2) Test 21 getNotes — investigate the cursor clip data loading issue; add longer sleep after clip/select and verify it works. If the Bitwig cursor clip API genuinely doesn't populate step data reliably, mark the test as a known limitation with a `SKIP` assertion. (3) Increase sleep from 0.3s to 0.5s for all state-change verifications (track volume/mute, clip create). (4) All snapshot paths already fixed (`['tracks']['tracks'][0]`).
**Rationale:** The test suite must pass reliably. Flaky tests are worse than no tests — they erode trust. Known Bitwig API limitations (async cursor clip data loading) should be documented as SKIPs rather than false failures.
**Alternatives considered:** (1) Remove flaky tests entirely — loses coverage. (2) Retry loops — complex, masks real failures.
**Status:** ACTIVE
**ID:** D-19.3

## 2026-03-12 — Offline tests: Consolidate schema validation

**Decision:** Merge all per-phase offline schema checks (Phase 9 through Phase 25, plus Gig Phase 16/18) into a single `offline-schemas.sh` that uses a data-driven approach: a list of `(tool_name, field_path, expected_type)` tuples checked in a loop rather than 200+ individual assertions. System prompt checks use a similar list of `(label, expected_text)` tuples. This replaces the fragile phase-by-phase approach where every new phase required new handwritten assertions.
**Rationale:** The current approach has 200+ near-identical assertions that differ only in tool name and field path. A data-driven approach is more maintainable — adding a new tool requires one line, not 5-10 assertion calls. It also reduces the script size dramatically.
**Alternatives considered:** (1) Keep per-phase assertions — proven to go stale (ISS-2). (2) Generate assertions from claude-tools.json — over-engineered, the JSON is already the source of truth.
**Status:** ACTIVE
**ID:** D-19.4

## 2026-03-12 — Runner: Execution modes and output

**Decision:** The runner script supports: `--offline` (schemas + builds only), `--online` (skip offline, run all online), `--only transport,clips` (run specific flow scripts), `--list` (list available test scripts). Output includes per-script pass/fail counts and a final summary. Exit code 1 if any failures.
**Rationale:** Different use cases need different scopes: quick offline validation during development, targeted flow testing when working on a specific handler, full suite for governance. The `--only` flag directly addresses the user's request for testing individual flows.
**Alternatives considered:** (1) No runner, just individual scripts — loses the convenience of a single command. (2) Make the runner optional — individual scripts should always be runnable standalone.
**Status:** ACTIVE
**ID:** D-19.5
