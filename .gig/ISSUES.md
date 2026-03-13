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
**Status:** DEFERRED
**Description:** The smoke test script (1992 lines) has: (1) `assert_contains` SIGPIPE bug with large strings under `pipefail`, (2) `gradlew` path wrong after monorepo migration, (3) snapshot path `['tracks'][0]` should be `['tracks']['tracks'][0]`, (4) clip slot index out of visible bank range, (5) stale field name `cursorTrackName` vs `trackName`, (6) wrong bank width assertion (64 vs 8), (7) `getNotes` returns empty after `setNotes` — possible cursor clip data loading issue. Partial fixes applied during investigation but script needs comprehensive overhaul and splitting into per-flow scripts.
**Evidence:** 44 offline failures before fix, 4 online failures remaining after partial fixes.
**Batch:** — (targeted for Phase 19)
