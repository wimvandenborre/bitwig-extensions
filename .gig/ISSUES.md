# Issues

> Tracked during governance. Resolved issues are archived with their phase.
> Deferred issues persist here and carry forward to future phases.

<!-- Issue statuses:
  OPEN      — Discovered, not yet addressed
  FIXING    — Unplanned batch created, fix in progress
  RESOLVED  — Fixed and verified
  DEFERRED  — Severity allows deferral to a future phase
-->

<!-- Entry format:
## ISS-{N}: {Title}

**Severity:** Blocker | Major | Minor | Cosmetic
**Source:** UAT-{N} | Decision Audit | Automated Tests | Lint
**Phase:** {phase number where discovered}
**Status:** OPEN | FIXING | RESOLVED | DEFERRED
**Description:** What's wrong.
**Evidence:** Error output, failing test, mismatched behavior.
**Batch:** — (assigned when fix starts)
-->

## ISS-1: Lost launchpad-mk2/v0.1.6 tag during import

**Severity:** Cosmetic
**Source:** Decision Audit (D-2.2)
**Phase:** 2
**Status:** DEFERRED
**Description:** The `v0.1.6` tag from launchpad-mk2 was overwritten during `git fetch` because gig-maestro had an identically named tag. Only 4 of 5 launchpad-mk2 tags were preserved.
**Evidence:** `git tag -l 'launchpad-mk2/*'` shows 4 tags instead of 5. The commit itself is in the history — only the tag pointer was lost.
**Batch:** —

## ISS-2: Smoke test monolithic script has multiple stale/broken assertions

**Severity:** Minor
**Source:** Governance — Phase 18
**Phase:** 18
**Status:** RESOLVED
**Description:** Monolithic 2040-line smoke test replaced by runner + 14 per-flow test scripts with shared helpers. All offline assertions pass (507). Online getNotes issue handled with defensive SKIP. Legacy script deleted.
**Evidence:** `./scripts/smoke-test.sh --offline` → 507 passed, 0 failed.
**Batch:** v0.19.7

## ISS-3: device/remove loses cursor after removing current device

**Severity:** Minor
**Source:** Manual Verification — Phase 19
**Phase:** 19
**Status:** OPEN
**Description:** After `device/remove` removes the cursor device, the CursorDevice drops to position -1 (no target). Subsequent `device/selectNext`, `device/selectPrevious`, and `device/remove` calls return `ok` but have no effect. The remaining devices on the track are unreachable via the cursor until the user manually clicks a device in the UI.
**Evidence:** Manual test: insert EQ-5 + Compressor → remove Compressor (works) → remove EQ-5 (returns ok but EQ-5 remains). Snapshot shows `position: -1, name: ""` after first remove.
**Batch:** —
