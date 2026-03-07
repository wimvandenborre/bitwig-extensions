# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

<!-- Populated by gig:plan. Cleared and archived by gig:verify on completion. -->

### Phase 2 — Track Filtering (v0.2.x)

> Exclude FX return tracks and master from the clip launcher grid and session ring so the controller only shows regular tracks.

**Decisions:** D-2.1, D-2.2, D-2.3

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 2.1 | `0.2.1` | Switch to createMainTrackBank | in-session | done |

### Batch 2.1 — Switch to createMainTrackBank

**Delegation:** in-session
**Decisions:** D-2.1, D-2.3
**Files:** `src/main/java/com/gregross/bitwig/launchpadmk2/LaunchpadMk2Extension.java`
**Work:** Change `host.createTrackBank(GRID_SIZE, 0, GRID_SIZE)` to `host.createMainTrackBank(GRID_SIZE, 0, GRID_SIZE)` in `init()`.
**Test criteria:** Build succeeds. In Bitwig, session ring only highlights regular tracks — FX returns and master are excluded from the grid.
**Acceptance:** Grid shows only audio/instrument/group tracks. Session ring rectangle does not cover FX or master channels.

**Phase Acceptance Criteria:**
- [ ] Session ring excludes FX returns and master track
- [ ] Grid only shows regular tracks
- [ ] Navigation scrolls through regular tracks only

---

## Plan Amendments

<!-- Log any changes to the plan after creation -->

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| — | — | — | — |
