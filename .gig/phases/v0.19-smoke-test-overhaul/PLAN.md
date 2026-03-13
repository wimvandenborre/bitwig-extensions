# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 19 — Smoke Test Overhaul (v0.19.x)

> Split the 2040-line monolithic smoke test into a runner + per-flow test scripts, fix all remaining online failures, and consolidate offline schema checks into a data-driven approach. Resolves ISS-2.

**Decisions:** D-19.1, D-19.2, D-19.3, D-19.4, D-19.5

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 19.1 | `0.19.1` | Helpers + runner scaffold | in-session | done |
| 19.2 | `0.19.2` | Offline test scripts | in-session | done |
| 19.3 | `0.19.3` | Online test scripts (transport, tracks, clips, notes) | team | done |
| 19.4 | `0.19.4` | Online test scripts (devices, arranger, mixer, browser, project, clip-launcher) | team | done |
| 19.5 | `0.19.5` | Error tests + cleanup script | in-session | done |
| 19.6 | `0.19.6` | Fix remaining online failures | in-session | done |
| 19.7 | `0.19.7` | Delete old script + verify full suite | in-session | done |
| 19.8 | `0.19.8` | Manual verification scripts + workflow [UNPLANNED] | in-session | done |

### Batch 19.8 — Manual verification scripts + workflow [UNPLANNED]

**Delegation:** in-session
**Files:**
- `gig-maestro/scripts/manual/_helpers.sh` (new) — shared infrastructure: verify(), reset_project(), volume/dB conversion, bar math
- `gig-maestro/scripts/manual/transport.sh` (new) — 9 steps
- `gig-maestro/scripts/manual/arranger.sh` (new) — 6 steps
- `gig-maestro/scripts/manual/tracks.sh` (new) — 10 steps
- `gig-maestro/scripts/manual/clips.sh` (new) — 8 steps
- `gig-maestro/scripts/manual/devices.sh` (new) — 12 steps
- `gig-maestro/scripts/manual/mixer.sh` (new) — 7 steps
- `gig-maestro/scripts/manual/project.sh` (new) — 6 steps
- `gig-maestro/scripts/manual/notes.sh` (new) — 7 steps
- `gig-maestro/scripts/manual/all.sh` (new) — runner with --only/--list
- `gig-maestro/scripts/workflows/create-track-with-synth-and-melody.sh` (new) — 20-step e2e workflow
- `.gig/ISSUES.md` (modified) — added ISS-3

**Work:**
1. Interactive manual verification scripts with y/n/s/r prompts for visual verification in Bitwig
2. Shared helpers: reset_project (clean slate), verify (interactive), volume dB conversion, bar math from time signature
3. Each script auto-prepares view context (EDIT for notes, MIX for mixer, etc.) and restores ARRANGE on cleanup
4. End-to-end workflow: create track → add synth + FX → create clip → write melody → transpose duplicate → scene launch → loop
5. Discovered ISS-3 (device/remove cursor loss) during manual testing

**Test criteria:** All manual scripts pass when run interactively against Bitwig. Workflow completes 20/20 steps.

**Results:** All 8 domain scripts passed (transport: 9/9, arranger: 6/6, tracks: 10/10, clips: 8/8, devices: 11/11+1 known ISS-3, mixer: 7/7, project: 6/6, notes: 7/7). Workflow: 17/17 verified steps passed.

---

### Batch 19.1 — Helpers + runner scaffold

**Delegation:** in-session
**Decisions:** D-19.1, D-19.2, D-19.5
**Files:**
- `gig-maestro/scripts/tests/_helpers.sh` (new)
- `gig-maestro/scripts/smoke-test.sh` (rewrite)

**Work:**
1. Create `scripts/tests/_helpers.sh` with:
   - `rpc()` function
   - `assert_contains()` (using bash pattern matching, not echo|grep)
   - `assert_equals()`
   - `snapshot_field()` (using python3 for JSON path extraction)
   - Counter variables: `PASS`, `FAIL`, `TOTAL`
   - `BASE`, `PORT` from environment or defaults
   - `PROJECT_ROOT`, `REPO_ROOT` resolution
   - `TOOLS_FILE`, `TOOLS_LIST`, `PROMPT` loading for offline use
2. Create new `scripts/smoke-test.sh` runner that:
   - Parses `--offline`, `--online`, `--only <names>`, `--list`
   - Sources `_helpers.sh`
   - Discovers test scripts in `scripts/tests/` (excluding `_`-prefixed)
   - Runs offline scripts, then online scripts (unless filtered)
   - Prints per-script and final summary
   - Exits 1 if any failures

**Test criteria:** `./scripts/smoke-test.sh --list` shows all test scripts. Runner sources helpers without error.

---

### Batch 19.2 — Offline test scripts

**Delegation:** in-session
**Decisions:** D-19.4
**Depends on:** Batch 19.1
**Files:**
- `gig-maestro/scripts/tests/offline-schemas.sh` (new)
- `gig-maestro/scripts/tests/offline-builds.sh` (new)

**Work:**
1. Create `offline-schemas.sh`:
   - Data-driven tool existence checks: array of tool names, loop + `grep -qF` on tools JSON file
   - Data-driven tool schema spot-checks: array of `(tool, jq_path, expected)` tuples
   - Data-driven system prompt checks: array of `(label, expected_text)` tuples
   - Consolidates all Phase 9–25 + Gig Phase 16/18 offline assertions
2. Create `offline-builds.sh`:
   - CLI JAR build + help validation (current O3)
   - Extension JAR build validation (current O4)

**Test criteria:** `./scripts/smoke-test.sh --offline` passes with 0 failures. Offline assertion count matches or exceeds the old script.

---

### Batch 19.3 — Online test scripts (transport, tracks, clips, notes)

**Delegation:** team
**Decisions:** D-19.1
**Depends on:** Batch 19.1
**Files:**
- `gig-maestro/scripts/tests/transport.sh` (new)
- `gig-maestro/scripts/tests/tracks.sh` (new)
- `gig-maestro/scripts/tests/clips.sh` (new)
- `gig-maestro/scripts/tests/notes.sh` (new)

**Work:**
1. `transport.sh` — old sections 4, 10 (HTTP 405), 8 (batch request): transport play/stop/tempo/position/loop/metronome + state restore
2. `tracks.sh` — old sections 5, 28, 29: track volume/mute, create/rename/duplicate/delete, select errors
3. `clips.sh` — old sections 11, 12, 13: clip snapshot, create/launch/stop/delete, scene launch
4. `notes.sh` — old sections 21, 22: note editing workflow (create clip, select, write/read/clear notes, step size/scroll)

Each script: sources `_helpers.sh`, runs standalone, saves/restores state where possible.

**Test criteria:** Each script runs standalone against live Bitwig and passes.

---

### Batch 19.4 — Online test scripts (devices, arranger, mixer, browser, project, clip-launcher)

**Delegation:** team
**Decisions:** D-19.1
**Depends on:** Batch 19.1
**Files:**
- `gig-maestro/scripts/tests/devices.sh` (new)
- `gig-maestro/scripts/tests/arranger.sh` (new)
- `gig-maestro/scripts/tests/mixer.sh` (new)
- `gig-maestro/scripts/tests/browser.sh` (new)
- `gig-maestro/scripts/tests/project.sh` (new)
- `gig-maestro/scripts/tests/clip-launcher.sh` (new)

**Work:**
1. `devices.sh` — old sections 15, 16, 24, 25, 26, 44, 45: device snapshot, cursor navigation, list/insert/remove, master device, chain navigation
2. `arranger.sh` — old sections 31–37: arranger/arrangement snapshot, visibility toggle, loop range, automation write mode, cue marker errors
3. `mixer.sh` — old sections 40–43: snapshot sends, track color, master mute/solo, send errors
4. `browser.sh` — old sections 47, 48: browser API, filter navigation
5. `project.sh` — old section 46: project state, unsoloAll/unmuteAll/unarmAll, panel layout, pre-roll, metronome volume
6. `clip-launcher.sh` — old section 50: clip launcher automation, transport settings, fill mode

Each script: sources `_helpers.sh`, runs standalone, saves/restores state.

**Test criteria:** Each script runs standalone against live Bitwig and passes.

---

### Batch 19.5 — Error tests + cleanup script

**Delegation:** in-session
**Decisions:** D-19.1
**Depends on:** Batch 19.1
**Files:**
- `gig-maestro/scripts/tests/errors.sh` (new)
- `gig-maestro/scripts/tests/health.sh` (new)

**Work:**
1. `errors.sh` — old sections 9, 18: malformed JSON, unknown method, invalid params across domains
2. `health.sh` — old sections 1, 2, 3, 7, 19: health check, session snapshot structure, api/list, app/getState, method count check
   - This is the "connectivity + introspection" script that verifies the extension is running

**Test criteria:** Each script runs standalone and passes.

---

### Batch 19.6 — Fix remaining online failures

**Delegation:** in-session
**Decisions:** D-19.3
**Depends on:** Batches 19.3, 19.4, 19.5
**Files:**
- `gig-maestro/scripts/tests/devices.sh` (fix cursor track test)
- `gig-maestro/scripts/tests/notes.sh` (fix getNotes test)

**Work:**
1. `devices.sh` cursor track test: add guard — if project has only 1 track, skip the "cursor changed" assertion
2. `notes.sh` getNotes issue: investigate with longer sleeps (2s+); if cursor clip data never populates, add SKIP with explanation
3. Ensure all sleeps after state-change RPCs are ≥ 0.5s
4. Run full suite, confirm 0 failures

**Test criteria:** `./scripts/smoke-test.sh` passes with 0 failures (or only documented SKIPs).

---

### Batch 19.7 — Delete old script + verify full suite

**Delegation:** in-session
**Depends on:** Batch 19.6
**Files:**
- `gig-maestro/scripts/smoke-test.sh` (finalize)
- Delete old monolithic script content (already replaced by runner in 19.1)

**Work:**
1. Verify `./scripts/smoke-test.sh` (runner) executes all test scripts correctly
2. Verify `./scripts/smoke-test.sh --offline` passes
3. Verify `./scripts/smoke-test.sh --online` passes (Bitwig required)
4. Verify `./scripts/smoke-test.sh --only transport` runs just transport tests
5. Verify individual scripts: `./scripts/tests/transport.sh` works standalone
6. Update `.claude/CLAUDE.md` smoke test documentation
7. Mark ISS-2 as RESOLVED

**Test criteria:** Full suite green. Individual scripts runnable. CLAUDE.md updated.

---

**Phase Acceptance Criteria:**
- [ ] Monolithic smoke test replaced by runner + per-flow scripts
- [ ] `_helpers.sh` shared by all test scripts
- [ ] Offline schemas consolidated into data-driven approach
- [ ] All online tests pass (or documented SKIPs for known Bitwig API limitations)
- [ ] Runner supports `--offline`, `--online`, `--only`, `--list`
- [ ] Each test script runnable standalone
- [ ] ISS-2 resolved
- [ ] Manual verification scripts for all domains (unplanned)
- [ ] End-to-end workflow script (unplanned)

**Completion triggers Phase 20 → version `0.20.0`**
