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

## 2026-03-08 — Utility Buttons: Session (CC 108)

**Decision:** Play/Stop transport toggle via `transport.togglePlay()`. LED green pulse when playing, off when stopped.
**Rationale:** Most essential transport control for any live or composing workflow.
**Alternatives considered:** Dedicated play + stop on separate buttons — wastes a button.
**Status:** ACTIVE
**ID:** D-4.1

## 2026-03-08 — Utility Buttons: User1 (CC 109)

**Decision:** Stop All Clips — iterate all tracks and call `track.stop()`. LED always red.
**Rationale:** Panic button that stops clips without stopping transport. Essential for live performance.
**Alternatives considered:** Global stop method — none exists in API.
**Status:** ACTIVE
**ID:** D-4.2

## 2026-03-08 — Utility Buttons: User2 (CC 110)

**Decision:** Undo via `application.undo()`. LED always white.
**Rationale:** Safety net for live performance and composing. Undo last clip launch or edit.
**Alternatives considered:** Redo — less commonly needed; can add later.
**Status:** ACTIVE
**ID:** D-4.3

## 2026-03-08 — Utility Buttons: Mixer (CC 111)

**Decision:** Record toggle via `transport.isArrangerRecordEnabled().toggle()`. LED red pulse when recording, off otherwise.
**Rationale:** Enables capturing jams to arranger for song composition.
**Alternatives considered:** Capture new scene — not confirmed available in API.
**Status:** ACTIVE
**ID:** D-4.4

## 2026-03-08 — Utility Buttons: LED colors

**Decision:** Play=green (vel 21) pulse when playing, Stop All=red (vel 5) static, Undo=white (vel 3) static, Record=red (vel 5) pulse when recording.
**Rationale:** Intuitive color coding; distinct from cyan/orange nav colors.
**Alternatives considered:** All same color — no visual differentiation.
**Status:** ACTIVE
**ID:** D-4.5

## 2026-03-08 — Utility Buttons: API objects

**Decision:** Add `host.createTransport()` and `host.createApplication()` in `init()` as fields.
**Rationale:** Required for transport control and undo functionality.
**Alternatives considered:** None.
**Status:** ACTIVE
**ID:** D-4.6


