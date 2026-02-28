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

## 2026-02-28 — Scope: What does Phase 8 cover?

**Decision:** Phase 8 adds arrangement and automation control RPC methods across four domains: (1) arranger visibility/layout, (2) loop range + punch range, (3) cue markers (create/navigate/list/delete), (4) automation write mode controls. No envelope editing — just the transport-level automation state toggles. Requires creating `Arranger` and `CueMarkerBank` objects in the extension entry point.
**Rationale:** These are the "production glue" features missing between clip-level editing (Phases 4-6) and a complete arrangement workflow. All four areas are well-supported by API v25 with `SettableBooleanValue`/`SettableEnumValue`/`SettableBeatTimeValue` patterns we've used throughout. This unlocks the agent's ability to set up loop regions, drop markers, configure automation recording, and control arranger panel layout — all essential for moving from "clips in a grid" to "arranged song."
**Alternatives considered:** (a) Mixer routing (sends, aux buses) — useful but less impactful for arrangement workflow. (b) Automation envelope editing (read/write individual automation points) — not exposed by Controller API. (c) Arranger clip operations (move/copy/split) — not available in API v25.
**Status:** ACTIVE
**ID:** D-8.1

## 2026-02-28 — Architecture: How to organize new API objects?

**Decision:** Create `Arranger` via `host.createArranger()` and `CueMarkerBank` via `host.createCueMarkerBank(16)` in `GigMaestroExtension.init()`. Pass both to a new `ArrangerHandler` class. Loop/punch/automation methods live on `Transport`, so they go in a new `ArrangementHandler` that receives the existing `Transport` object. Two new handler classes total: `ArrangerHandler` (visibility + cue markers) and `ArrangementHandler` (loop, punch, automation).
**Rationale:** Separating arranger-panel concerns (visibility, cue markers) from transport-level arrangement concerns (loop, punch, automation) follows the existing handler pattern where each handler wraps a distinct Bitwig API object. The `TransportHandler` already exists but handles playback controls — arrangement-specific transport features are conceptually different and warrant their own handler to keep classes focused.
**Alternatives considered:** (a) Cram everything into existing `TransportHandler` — it already has 10 methods, adding 15+ more makes it unwieldy. (b) One mega `ArrangementHandler` — mixes Arranger and Transport API objects, muddies responsibility. (c) Three handlers (arranger, cue, arrangement) — over-split; cue markers come from the arranger object so they belong together.
**Status:** AMENDED
**ID:** D-8.2

## 2026-02-28 — Arranger: Which visibility/layout controls to expose?

**Decision:** Expose 7 arranger visibility toggles as RPC methods: `arranger/setPlaybackFollow`, `arranger/setClipLauncherVisible`, `arranger/setTimelineVisible`, `arranger/setCueMarkersVisible`, `arranger/setEffectTracksVisible`, `arranger/setIoSectionVisible`, `arranger/setDoubleRowTrackHeight`. Each takes a boolean `enabled` param. Add arranger state to `session/snapshot` under an `arranger` section.
**Rationale:** All 7 are `SettableBooleanValue` — trivial to implement with the same pattern as `transport/setLoop`. Including all 7 is low-effort and gives the agent full control over the arranger panel layout. Snapshot integration lets the agent read current state before toggling.
**Alternatives considered:** (a) Only expose 3-4 "most useful" toggles — arbitrary; implementing all 7 is the same effort. (b) Add zoom controls (`zoomIn`/`zoomOut`/`zoomToFit`) — these are visual-only and less useful for an agent that doesn't see the screen. Could add later.
**Status:** ACTIVE
**ID:** D-8.3

## 2026-02-28 — Loop/Punch: What RPC methods to add?

**Decision:** Add 4 RPC methods: `arrangement/setLoopRange` (params: `start` in beats, `duration` in beats, `enabled` boolean), `arrangement/setPunchIn` (params: `position` in beats, `enabled` boolean), `arrangement/setPunchOut` (params: `position` in beats, `enabled` boolean), `arrangement/getLoopRange` (returns start, duration, enabled, punchIn, punchOut state). Combine setter with enable in a single call to reduce round-trips.
**Rationale:** The existing `transport/setLoop` only toggles the clip launcher loop. Arranger loop is separate (`isArrangerLoopEnabled`, `arrangerLoopStart`, `arrangerLoopDuration`). Combining position + enable in one method follows the same ergonomic pattern as `clip_create` (which sets length and creates in one call). The getter returns everything in one snapshot-like response.
**Alternatives considered:** (a) Separate enable/disable from position setting (6 methods) — more granular but more round-trips. (b) Put loop in snapshot only, no dedicated getter — snapshot is heavy; a lightweight getter is useful for loop-specific checks.
**Status:** ACTIVE
**ID:** D-8.4

## 2026-02-28 — Cue Markers: What operations to support?

**Decision:** Add 5 RPC methods: `cueMarker/add` (creates marker at current playback position or specified beat position), `cueMarker/list` (returns all markers in the bank — name, position, color), `cueMarker/jump` (params: `direction` "next"/"previous"), `cueMarker/launch` (params: `index`, `quantized` boolean — jumps to marker and starts playback), `cueMarker/delete` (params: `index` — deletes marker at bank index). Bank size: 16 markers.
**Rationale:** These cover the core marker workflow: add markers at key positions (intro, verse, chorus), list them for reference, jump between them during playback, launch from a marker for rehearsal, delete unwanted ones. 16-marker bank is generous for most songs (typical 8-section structure). `CueMarker` extends `DeleteableObject` so deletion is supported.
**Alternatives considered:** (a) Add rename/recolor — nice-to-have but low priority; `CueMarker.name()` is `SettableStringValue` so it's possible later. (b) Scroll-based navigation instead of bank — banks are simpler and consistent with track/clip pattern. (c) 8-marker bank — too few for complex arrangements.
**Status:** AMENDED
**ID:** D-8.5

## 2026-02-28 — Automation: What controls to expose?

**Decision:** Add 4 RPC methods: `automation/setWriteMode` (params: `mode` enum "latch"/"touch"/"write"), `automation/setArrangerWrite` (params: `enabled` boolean), `automation/setClipLauncherWrite` (params: `enabled` boolean), `automation/resetOverrides` (no params — clears all automation overrides). Add automation state to snapshot: `writeMode`, `arrangerWriteEnabled`, `clipLauncherWriteEnabled`, `overrideActive`.
**Rationale:** These are the four automation controls a producer uses: pick a write mode, enable writing for arranger or clip launcher, and reset overrides when parameters get stuck. The `resetAutomationOverrides()` method is critical for the agent — without it, parameter changes during playback permanently override automation.
**Alternatives considered:** (a) Skip automation entirely — leaves a gap; the agent can't set up automation recording for the user. (b) Add per-track automation arm — not available in Controller API at the global level. (c) Include `toggleLatchAutomationWriteMode` — redundant since `setWriteMode("latch")` covers it.
**Status:** ACTIVE
**ID:** D-8.6

## 2026-02-28 — Snapshot: How to integrate new state?

**Decision:** Add two new top-level sections to `session/snapshot`: (1) `arranger` — 7 boolean visibility fields. (2) `arrangement` — loop range (start, duration, enabled), punch in/out (position, enabled), automation (writeMode, arrangerWrite, clipLauncherWrite, overrideActive), cue markers (array of {index, name, position, color}). Keep existing sections unchanged.
**Rationale:** Two sections mirrors the two-handler architecture. `arranger` is UI panel state; `arrangement` is musical structure state. Cue markers in the snapshot means the agent doesn't need a separate `cueMarker/list` call in most cases — but the dedicated method is still useful for lightweight queries.
**Alternatives considered:** (a) One combined section — too many mixed concerns. (b) Three sections (arranger, loop, automation) — over-fragmented. (c) Skip snapshot integration, require dedicated getters — breaks the perception-action loop pattern.
**Status:** ACTIVE
**ID:** D-8.7

## 2026-02-28 — Architecture: How to organize new API objects? (amended)

**Decision:** Create `Arranger` via `host.createArranger()` and `CueMarkerBank` via `host.createCueMarkerBank(16)` in `GigMaestroExtension.init()`. Pass both to a new `ArrangerHandler` class (visibility + cue markers). Loop/punch/automation methods live on `Transport` — add them to the existing `TransportHandler`. One new handler class: `ArrangerHandler`. Transport grows ~8 methods for loop range, punch range, and automation write controls.
**Rationale:** Loop, punch, and automation write are all Transport-owned API calls (`transport.isArrangerLoopEnabled()`, `transport.arrangerLoopStart()`, `transport.automationWriteMode()`, etc.). Putting them in a separate handler that just wraps the same Transport object adds indirection for no gain. TransportHandler is the natural home. Overridden by user — original: two new handlers (ArrangerHandler + ArrangementHandler).
**Alternatives considered:** Same as D-8.2.
**Status:** ACTIVE
**ID:** D-8.2a

## 2026-02-28 — Cue Markers: What operations to support? (amended)

**Decision:** Add 4 RPC methods: `cueMarker/addAtPlayhead` (calls `transport.addCueMarkerAtPlaybackPosition()` — creates marker at current playback position), `cueMarker/list` (returns all markers in the 16-slot bank — name, position, color), `cueMarker/launch` (params: `index`, `quantized` boolean — calls `CueMarker.launch(quantized)` which jumps to marker and starts playback), `cueMarker/delete` (params: `index` — deletes marker at bank index). No separate "jump" method — `launch(quantized=true)` covers that. No "add at arbitrary position" — the API only provides `addCueMarkerAtPlaybackPosition()`, so to place a marker at a specific beat the agent should `transport/setPosition` first, then `cueMarker/addAtPlayhead`.
**Rationale:** `Transport.addCueMarkerAtPlaybackPosition()` is the only creation API — there's no `createCueMarker(position)`. Wrapping a "set position → add → restore position" sequence in a single RPC method hides complexity but also hides the side effect (transport position changes). Better to let the agent compose the two calls explicitly. `CueMarker.launch(quantized)` already handles both "jump to" and "start playback from" — a separate `jump` method using `Transport.jumpToNextCueMarker()` is redundant and less precise (no index targeting). Overridden by user — original: 5 methods including separate jump and add-at-position.
**Alternatives considered:** Same as D-8.5, plus: (a) `cueMarker/addAtPosition` that internally sets transport position — hides side effects, fragile if another command races.
**Status:** ACTIVE
**ID:** D-8.5a
