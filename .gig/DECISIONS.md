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

## 2026-03-08 — Scene LEDs: Color source

**Decision:** Use `scene.color()` for base color via SysEx RGB with hue correction.
**Rationale:** Scenes have a settable color in Bitwig; user wants scene hue reflected on pads.
**Alternatives considered:** Fixed color (current approach) — no visual distinction between scenes.
**Status:** ACTIVE
**ID:** D-3.1

## 2026-03-08 — Scene LEDs: Playing state detection

**Decision:** Infer playing state by checking if any clip in the scene row is playing.
**Rationale:** Scene API has no `isPlaying()` method; this pattern already exists in `onNoteOn`.
**Alternatives considered:** None — API limitation.
**Status:** ACTIVE
**ID:** D-3.2

## 2026-03-08 — Scene LEDs: Queued state detection

**Decision:** Infer queued state by checking `isPlaybackQueued()` across all tracks in the scene.
**Rationale:** Same API limitation as playing state.
**Alternatives considered:** None.
**Status:** ACTIVE
**ID:** D-3.3

## 2026-03-08 — Scene LEDs: Playing feedback

**Decision:** Pulse red (velocity 5) — same as playing clips.
**Rationale:** Consistent visual language across grid and scene buttons.
**Alternatives considered:** Pulse with scene's own color — less contrast.
**Status:** ACTIVE
**ID:** D-3.4

## 2026-03-08 — Scene LEDs: Queued feedback

**Decision:** Pulse amber (velocity 61) — same as queued clips.
**Rationale:** Consistent with `CLIP_QUEUED` already used.
**Alternatives considered:** Flash mode — less consistent.
**Status:** ACTIVE
**ID:** D-3.5

## 2026-03-08 — Scene LEDs: Idle feedback

**Decision:** Scene's own color via SysEx RGB with hue correction.
**Rationale:** Uses existing `packRgb()` with hue-aware correction for pad visibility.
**Alternatives considered:** Fixed dim cyan (current) — no visual info about scene identity.
**Status:** ACTIVE
**ID:** D-3.6

