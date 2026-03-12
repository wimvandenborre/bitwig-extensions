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
