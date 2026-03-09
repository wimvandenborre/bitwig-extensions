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

## 2026-03-07 — Track Bank: Should the grid include FX returns and master?

**Decision:** Use `createMainTrackBank()` instead of `createTrackBank()` to exclude FX return tracks and master track from the grid and session ring.
**Rationale:** The session ring should only highlight regular tracks. FX returns and master are not clip-launchable and clutter the grid.
**Alternatives considered:** Filtering tracks manually after creation — unnecessary complexity when the API provides a dedicated method.
**Status:** ACTIVE
**ID:** D-2.1

## 2026-03-07 — Navigation: Track scrolling via rotated arrows

**Decision:** Keep current CC_LEFT/CC_RIGHT → track scroll behavior (already implemented in v0.1.7).
**Rationale:** After CCW rotation, left/right arrows physically point up/down. Already wired to scroll tracks.
**Alternatives considered:** None — already working as intended.
**Status:** ACTIVE
**ID:** D-2.2

## 2026-03-07 — Scope: Minimal change

**Decision:** Single-line change in `init()` — swap `createTrackBank` for `createMainTrackBank`. No other code changes needed.
**Rationale:** Grid, session ring, and nav all derive from the track bank object. Changing the source fixes everything downstream.
**Alternatives considered:** None.
**Status:** ACTIVE
**ID:** D-2.3
