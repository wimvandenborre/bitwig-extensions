# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

<!-- Populated by gig:plan. Cleared and archived by gig:verify on completion. -->

_No active phase. Run `gig:decide` then `gig:plan` to start._

### Batch 5.1 — Mode state + toggle + CursorTrack properties

**Delegation:** in-session
**Decisions:** D-1.1, D-1.2a, D-1.6
**Files:** LaunchpadMk2Extension.java
**Work:**
- Add `private int utilityMode = 0;` field (0=Global, 1=Track, 2=Utility)
- Add mode constants: `MODE_GLOBAL = 0`, `MODE_TRACK = 1`, `MODE_UTILITY = 2`
- In init(): mark `cursorTrack.solo()`, `cursorTrack.mute()`, `cursorTrack.arm()` as interested with value observers
- Change CC_SESSION handler from Capture Scene to mode cycle: `utilityMode = (utilityMode + 1) % 3; markDirty();`
**Test criteria:** Build succeeds. Session button press cycles through modes (verify via host.println logging the mode value).
**Acceptance:** Mode field toggles 0→1→2→0 on Session press. CursorTrack properties observed.

### Batch 5.2 — Modal button actions (all 3 modes)

**Delegation:** in-session (depends on 5.1)
**Decisions:** D-1.3, D-1.4, D-1.7, D-1.8
**Files:** LaunchpadMk2Extension.java
**Work:**
- Replace fixed CC_USER1/CC_USER2/CC_MIXER handlers with mode-switched logic:
  - Global (0): User1 → `transport.isArrangerRecordEnabled().toggle()`, User2 → `transport.stop()`, Mixer → `transport.play()`
  - Track (1): User1 → `cursorTrack.arm().toggle()`, User2 → `cursorTrack.solo().toggle()`, Mixer → `cursorTrack.mute().toggle()`
  - Utility (2): User1 → `application.getAction("Create Scene From Playing Launcher Clips").invoke()`, User2 → `application.undo()`, Mixer → `application.redo()`
**Test criteria:** Build succeeds. Each mode's buttons trigger correct actions in Bitwig.
**Acceptance:** All 9 button functions work correctly across 3 modes.

### Batch 5.3 — Modal LED feedback (all 3 modes)

**Delegation:** in-session (depends on 5.1)
**Decisions:** D-1.5
**Files:** LaunchpadMk2Extension.java
**Work:**
- Rewrite flushTopRow() utility section to be mode-aware:
  - Session button (idx 4): green (21) for Global, purple (49) for Track, yellow (13) for Utility
  - Global mode: User1 = red pulse (5) when recording / dim red (7) idle; User2 = static red (5); Mixer = green pulse (21) when playing / dim green (23) stopped
  - Track mode: User1 = red pulse (5) when armed / dim red (7); User2 = yellow pulse (13) when soloed / dim yellow (15); Mixer = orange pulse (9) when muted / dim orange (11)
  - Utility mode: User1 = static yellow (13); User2 = static white (3); Mixer = static white (3)
**Test criteria:** Build succeeds. LEDs update correctly when switching modes and when state changes.
**Acceptance:** Mode toggle LED changes color per mode. All action button LEDs reflect current state.

**Phase Acceptance Criteria:**
- [ ] Session button cycles through 3 modes with distinct LED colors
- [ ] Global mode: Play, Stop, Record all function with state LEDs
- [ ] Track mode: Arm, Solo, Mute toggle on cursor track with state LEDs
- [ ] Utility mode: Capture Scene, Undo, Redo all function with static LEDs
- [ ] Build succeeds with `./gradlew build`

**Completion triggers Phase 6 → version `0.6.0`**

---

## Plan Amendments

<!-- Log any changes to the plan after creation -->

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| — | — | — | — |
