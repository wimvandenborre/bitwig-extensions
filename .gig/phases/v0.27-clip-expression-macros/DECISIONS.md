# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## D-27.1 — Inline expression properties on note objects

**Status:** ACTIVE
**Date:** 2026-03-14

Extend note format in macros with optional: `chance`, `expressions` ({pan, timbre, pressure, gain, transpose, releaseVelocity, velocitySpread, mute}), `repeat` ({count, curve, velocityEnd, velocityCurve}), `occurrence` (string), `recurrence` ({length, mask}).

---

## D-27.2 — Deferred expression calls grouped by property type

**Status:** ACTIVE
**Date:** 2026-03-14

After `clip/setNotes`, schedule expression calls in next flush cycle(s). One `clip/setNoteExpressions` call per property type, one `clip/setChance` call for all chance values, etc.

---

## D-27.3 — buildSong inherits via delegation

**Status:** ACTIVE
**Date:** 2026-03-14

No changes to `macro/buildSong` — expression support flows through `macro/buildSection` → `writeNotesToCursor` automatically.
