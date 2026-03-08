# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

<!-- Decision statuses:
  PROPOSED  — Claude's recommendation, awaiting user approval
  ACTIVE    — Approved and in effect
  AMENDED   — Overridden by user (original preserved, new entry appended)
  REVISED   — Claude revised based on new information (original preserved)
-->

<!-- Entry format:
## YYYY-MM-DD — Domain: Question

**Decision:** What was decided.
**Rationale:** Why this choice was made.
**Alternatives considered:** What else was evaluated.
**Status:** PROPOSED | ACTIVE | AMENDED | REVISED
**ID:** D-{batch}.{num}
-->

## 2026-03-08 — Modes: Toggle mechanism

**Decision:** Session button (CC 108) acts as mode toggle. Press cycles through modes. The other 3 buttons (User1/User2/Mixer) change function based on active mode.
**Rationale:** Session is the first of the 4 utility buttons (topmost after CCW rotation), making it a natural "mode selector." Using a dedicated toggle keeps the interaction simple — no long-press timing or modifier combos.
**Alternatives considered:** Long-press for mode switch (complex timing logic, slower UX); using a nav button as modifier (nav buttons already assigned); double-press (unreliable timing).
**Status:** ACTIVE
**ID:** D-1.1

## 2026-03-08 — Modes: Number of modes

**Decision:** Two modes — Global Transport and Track Control. The current Capture Scene/Stop All/Undo functions are replaced.
**Rationale:** User explicitly asked for these two modes. Keeping it to 2 makes cycling fast (one press to swap). The old utilities can be restored as an optional 3rd mode in a future phase if desired.
**Alternatives considered:** Three modes (add Utility mode with Capture Scene/Stop All/Undo) — adds complexity and slower cycling; would be a good future addition.
**Status:** AMENDED
**ID:** D-1.2

## 2026-03-08 — Modes: Number of modes (revised)

**Decision:** Three modes — Global Transport, Track Control, and Utility. Cycle: Global → Track → Utility → Global.
**Rationale:** User wants undo/redo/capture scene preserved. 3 modes means max 2 presses to reach any mode — still fast. Overridden by user — original: two modes.
**Alternatives considered:** Two modes (original proposal) — loses useful utility functions.
**Status:** ACTIVE
**ID:** D-1.2a

## 2026-03-08 — Modes: Global Transport button mapping

**Decision:** In Global Transport mode: User1 (CC109) = Record toggle, User2 (CC110) = Stop, Mixer (CC111) = Play.
**Rationale:** Matches user's stated preference: "mixer was play / user2..stop / user1..record." Play at the bottom (Mixer) is the most frequently used and easiest to reach.
**Alternatives considered:** Different ordering — but user was explicit about the mapping.
**Status:** ACTIVE
**ID:** D-1.3

## 2026-03-08 — Modes: Track Control button mapping

**Decision:** In Track Control mode: User1 (CC109) = Record Arm, User2 (CC110) = Solo, Mixer (CC111) = Mute. All act on the cursor track (highlighted track).
**Rationale:** Mirrors the Global Transport order conceptually — Record/Arm at top, then channel strip functions. User said "record/solo/mute" in that order, which maps naturally to User1/User2/Mixer.
**Alternatives considered:** Different orderings — but user specified the order.
**Status:** ACTIVE
**ID:** D-1.4

## 2026-03-08 — Modes: LED feedback

**Decision:** Session button (mode toggle) lights up with a distinct color per mode — green for Global Transport, purple for Track Control, yellow for Utility. The 3 modal buttons show state-aware LEDs: Record = red pulse when active / dim red idle; Play = green pulse when playing / dim green stopped; Stop = static red; Arm = red pulse when armed / dim red; Solo = yellow pulse when soloed / dim yellow; Mute = orange pulse when muted / dim orange; Capture Scene = static yellow; Undo = static white; Redo = static white.
**Rationale:** Color-coding the mode toggle makes it instantly clear which mode is active. State-aware LEDs on action buttons provide feedback without looking at the screen.
**Alternatives considered:** Single color for all modes with only button colors changing — less discoverable which mode is active.
**Status:** ACTIVE
**ID:** D-1.5

## 2026-03-08 — Modes: CursorTrack API usage

**Decision:** Use the existing `cursorTrack` object's `solo()`, `mute()`, and `arm()` properties. Mark all three as interested during init and add value observers for LED updates.
**Rationale:** CursorTrack already exists and follows the highlighted track. Its solo/mute/arm are SettableBooleanValue properties with `.toggle()` for action and `.get()` for state — same pattern as `track.arm()` already used.
**Alternatives considered:** Using `trackBank.getItemAt(index)` with a tracked index — more complex, CursorTrack already handles track following.
**Status:** ACTIVE
**ID:** D-1.6

## 2026-03-08 — Modes: Transport Play vs Stop separation

**Decision:** Use `transport.play()` for the Play button and `transport.stop()` for the Stop button, replacing the current `transport.togglePlay()`.
**Rationale:** User wants separate Play and Stop buttons, not a single toggle. The Bitwig Transport API provides both `play()` and `stop()` as separate methods.
**Alternatives considered:** Keep `togglePlay()` on Play button — but then Stop button would also need `togglePlay()` or a conditional, which defeats the purpose of having separate buttons.
**Status:** ACTIVE
**ID:** D-1.7

## 2026-03-08 — Modes: Utility mode mapping

**Decision:** In Utility mode: User1 (CC109) = Capture Scene, User2 (CC110) = Undo, Mixer (CC111) = Redo. LEDs: Capture Scene = static yellow, Undo = static white, Redo = static white.
**Rationale:** Preserves the popular Capture Scene from Phase 4. Undo/Redo is a natural pair. Redo uses `application.redo()` which mirrors the existing `application.undo()` call.
**Alternatives considered:** Stop All instead of Redo — but Stop All is less useful alongside transport Stop in Global mode; Undo/Redo pair is more cohesive.
**Status:** ACTIVE
**ID:** D-1.8

