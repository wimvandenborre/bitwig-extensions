# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

<!-- Populated by gig:plan. Cleared and archived by gig:verify on completion. -->

### Phase 4 — Utility Buttons (v0.4.x)

> Map the 4 unused top-row buttons (Session/User1/User2/Mixer) to Play/Stop, Stop All Clips, Undo, and Record toggle with LED feedback.

**Decisions:** D-4.1, D-4.2, D-4.3, D-4.4, D-4.5, D-4.6

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 4.1 | `0.4.1` | Transport, Stop All, Undo, Record buttons | in-session | done |

### Batch 4.1 — Transport, Stop All, Undo, Record buttons

**Delegation:** in-session
**Decisions:** D-4.1 through D-4.6
**Files:** `src/main/java/com/gregross/bitwig/launchpadmk2/LaunchpadMk2Extension.java`
**Work:**
1. Add `Transport` and `Application` fields + create in `init()`
2. Add observers: `transport.isPlaying()`, `transport.isArrangerRecordEnabled()` → markDirty
3. In `onCC()`: handle CC 108 (togglePlay), CC 109 (stop all clips), CC 110 (undo), CC 111 (record toggle)
4. In `flushTopRow()`: replace static MODE_ACTIVE/INACTIVE with dynamic LED states based on transport/record state
**Test criteria:** Build succeeds. Play/Stop toggles transport with green pulse LED. Stop All stops clips. Undo works. Record toggles with red pulse LED.
**Acceptance:** All 4 buttons functional with correct LED feedback.

**Phase Acceptance Criteria:**
- [ ] Session button toggles transport play/stop with green pulse when playing
- [ ] User1 button stops all clips without stopping transport
- [ ] User2 button triggers undo
- [ ] Mixer button toggles arranger record with red pulse when recording

---

## Plan Amendments

<!-- Log any changes to the plan after creation -->

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| — | — | — | — |
