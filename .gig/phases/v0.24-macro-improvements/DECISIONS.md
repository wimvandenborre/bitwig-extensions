# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## D-24.1 — Extend `macro/createTrack` with `pages` parameter

**Status:** ACTIVE
**Date:** 2026-03-13

Add an optional `pages` parameter to `macro/createTrack` rather than creating a new method. When both `device` and `pages` are provided: create track → rename → insert device → (flush) → set parameters. Collapses 3 AI calls into 1. `pages` requires `device` — return `-32602` if `pages` provided without `device`.

---

## D-24.2 — Reuse `macro/createSound` internally via scheduled delegation

**Status:** ACTIVE
**Date:** 2026-03-13

When `macro/createTrack` receives `pages`, schedule a call to `macro/createSound` (without `device` param) after `FLUSH_DELAY_MS` so the just-inserted device has time to initialize. Reuses existing two-task-per-page timing logic in `device/setParameters` rather than reimplementing it.

---

## D-24.3 — Return shape includes sound metadata

**Status:** ACTIVE
**Date:** 2026-03-13

When `pages` is provided, the return object includes `paramCount` and `pageCount` alongside the existing `ok: true`.
