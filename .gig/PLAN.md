# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

<!-- Populated by gig:plan. Cleared and archived by gig:verify on completion. -->

### Phase 3 — Scene Launch Feedback (v0.3.x)

> Add dynamic LED feedback to scene launch buttons: display scene color when idle, pulse red when playing, pulse amber when queued.

**Decisions:** D-3.1, D-3.2, D-3.3, D-3.4, D-3.5, D-3.6

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 3.1 | `0.3.1` | Scene color observers + stateful LED feedback | in-session | done |

### Batch 3.1 — Scene color observers + stateful LED feedback

**Delegation:** in-session
**Decisions:** D-3.1 through D-3.6
**Files:** `src/main/java/com/gregross/bitwig/launchpadmk2/LaunchpadMk2Extension.java`
**Work:**
1. In `init()`: add `scene.color().markInterested()` and observer for each scene
2. In `flushSceneLaunch()`: for each scene, check clip states across all tracks to determine playing/queued, then send appropriate LED mode:
   - Queued → pulse amber (0x92, velocity 61)
   - Playing → pulse red (0x92, velocity 5)
   - Idle → SysEx RGB with scene color via `packRgb()`
3. Update scene LED cache to track both color and mode (like the grid cache)
**Test criteria:** Build succeeds. Scene buttons show scene colors when idle, pulse red when playing, pulse amber when queued.
**Acceptance:** Scene launch buttons dynamically reflect scene state and color.

**Phase Acceptance Criteria:**
- [ ] Scene buttons display scene's own color when idle
- [ ] Scene buttons pulse red when any clip in the scene is playing
- [ ] Scene buttons pulse amber when any clip in the scene is queued
- [ ] Colors match through pads with hue correction

---

## Plan Amendments

<!-- Log any changes to the plan after creation -->

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| — | — | — | — |
