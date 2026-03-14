# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## D-26.1 — Add `plugin` parameter to macros

**Status:** ACTIVE
**Date:** 2026-03-14

Add optional `plugin` object (`{type, id}`) to `macro/createTrack`, `macro/createSound`, and `macro/buildSong` track definitions. Mutually exclusive with `device`. Calls `device/insertPluginDevice` instead of `device/insertBitwigDevice`.

---

## D-26.2 — `pages` works with plugins (no changes needed)

**Status:** ACTIVE
**Date:** 2026-03-14

`device/setParameters` operates on cursor device regardless of type. `pages` works with plugins out of the box. Document only.

---

## D-26.3 — Validation rules for `device` vs `plugin`

**Status:** ACTIVE
**Date:** 2026-03-14

`device` + `plugin` together → error. `pages` without either → error (existing). `plugin` without valid `type` → delegated to `insertPluginDevice` validation.
