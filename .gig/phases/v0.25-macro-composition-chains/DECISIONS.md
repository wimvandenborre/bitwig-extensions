# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## D-25.1 — Create `macro/buildSong` method

**Status:** ACTIVE
**Date:** 2026-03-13

New macro accepting `tracks[]` and `sections[]`, chaining `macro/createTrack` and `macro/buildSection` internally with proper flush delays. Collapses N+M calls to 1.

---

## D-25.2 — Sequential track creation with calculated delays

**Status:** ACTIVE
**Date:** 2026-03-13

Each track created sequentially. Track N+1 starts after Track N's sound params finish. Delay per track: `FLUSH_DELAY_MS * (1 + 2 * pageCount)`. Tracks without pages need only `FLUSH_DELAY_MS` after device insertion (or 0 if no device).

---

## D-25.3 — Track definition includes `color`

**Status:** ACTIVE
**Date:** 2026-03-13

Add optional `color` (`{r, g, b}` floats 0.0-1.0) to track definitions in `macro/buildSong` and `macro/createTrack`. Applied after rename, before device insertion.

---

## D-25.4 — Section building starts after all tracks complete

**Status:** ACTIVE
**Date:** 2026-03-13

Sections scheduled after last track's sound params complete. Sections processed sequentially via `macro/buildSection` delegation. Delay per section: `FLUSH_DELAY_MS * (1 + 2 * clipCount)`.
