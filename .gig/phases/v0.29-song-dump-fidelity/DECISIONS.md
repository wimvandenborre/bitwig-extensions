# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## D-29.1 — Capture device parameters via discovery during dump

**Status:** ACTIVE
**Date:** 2026-03-14

**Decision:** During the instrument dump step, after selecting each track's device, call `device/discoverAll` then `device/getDiscoveryResult` with `format: "preset"` to capture all parameter pages and values. Store as a `pages` array on each instrument entry (same format as `macro/createSound`).

**Rationale:** The preset format `[{pageIndex, params: [{index, value}]}]` is already the input format for `macro/createSound` and `device/setParameters`, making round-trip seamless. Discovery reads all pages automatically. The full format (with names) is not needed for rebuild.

**Alternatives considered:** Using `session/snapshot` to read parameters — only captures the current page, not all pages. Using full discovery format — includes unnecessary metadata (names, displayed values) that inflates the dump file.

---

## D-29.2 — Add clip launch and playback settings to dump

**Status:** ACTIVE
**Date:** 2026-03-14

**Decision:** After reading each clip's notes, call `clip/getLaunchSettings` and `clip/getPlaybackSettings` to capture launch quantization, launch mode, shuffle, accent, loop settings, and play range. Store as `launchSettings` and `playbackSettings` objects on each clip entry. Only include non-default values to keep dumps compact.

**Rationale:** These settings affect playback behavior. Without them, rebuilt clips use Bitwig defaults which may differ from the original. The RPC methods already exist and return clean JSON.

**Alternatives considered:** Storing all settings always — wasteful since most clips use defaults. Storing in a separate top-level array — breaks the per-clip locality principle.

---

## D-29.3 — Rebuild restores device parameters after insertion

**Status:** ACTIVE
**Date:** 2026-03-14

**Decision:** During rebuild, if an instrument entry has a `pages` array, call `device/setParameters` after the device has been inserted and initialized (with appropriate flush delay). This reuses the existing page-based parameter setter.

**Rationale:** `device/setParameters` already handles multi-page writes with proper timing. No new extension code needed — only CLI scheduling logic.

**Alternatives considered:** Using `macro/createSound` — would re-insert the device which is already inserted during the track creation step.

---

## D-29.4 — Rebuild restores clip settings after clip creation

**Status:** ACTIVE
**Date:** 2026-03-14

**Decision:** During rebuild, after each clip's notes are written, apply launch and playback settings by calling `clip/setLaunchQuantization`, `clip/setLaunchMode`, `clip/setShuffle`, `clip/setAccent`, `clip/setPlayStart`, `clip/setPlayStop`, `clip/setLoopStart`, `clip/setLoopLength`, `clip/setLoopEnabled` as needed. Only call setters for properties present in the dump (backward compat).

**Rationale:** Settings must be applied after the clip exists. Calling only for present properties ensures old dump files without these fields rebuild without errors.

**Alternatives considered:** Batching all settings into a single macro — overengineering for a sequential CLI workflow.

---

## D-29.5 — Backward compatible with existing dump files

**Status:** ACTIVE
**Date:** 2026-03-14

**Decision:** All new fields (`pages` on instruments, `launchSettings`/`playbackSettings` on clips) are optional. Rebuild skips them if absent. No format version bump needed — the existing `formatVersion: "1"` remains valid.

**Rationale:** Users have existing dump files. Breaking them would require a migration step. Optional fields are the simplest backward-compat approach.

**Alternatives considered:** Bumping format version to "2" — unnecessary complexity since the structure is additive, not breaking.
