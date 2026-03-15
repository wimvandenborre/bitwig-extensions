# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-14 — Scope: What arranger view capabilities to add?

**Decision:** Rename phase to "Arranger Lane Zoom & Timeline Navigation". Add: (1) 4 lane height zoom methods on ArrangerHandler (zoomInLaneHeightsAll, zoomOutLaneHeightsAll, zoomInLaneHeightsSelected, zoomOutLaneHeightsSelected), (2) precision timeline navigation via `zoomToContentRegion(from, to)` on the Arranger's ScrollbarModel, (3) `zoomToFitSelectionOrAll` as an additional convenience method. Skip scroll position (not in API), per-track visibility (not in API), and track height beyond double-row (not in API).
**Rationale:** Research confirmed the original roadmap description was speculative — scroll position, per-lane visibility, and multi-level track height don't exist in API v25. The real value is lane zoom (Claude can enlarge specific track lanes for detail work) and zoomToContentRegion (Claude can frame a specific bar range in the arranger). These are 6 new RPC methods total.
**Alternatives considered:** Skipping the phase entirely (valid — the wins are smaller than expected, but lane zoom + region zoom are still useful), adding all TimelineEditor zoom variants (most already covered by app/zoomIn etc. in phase 30).
**Status:** ACTIVE
**ID:** D-1.1

## 2026-03-14 — Lane zoom: Where to add?

**Decision:** Add 4 lane zoom methods to ArrangerHandler with `arranger/` prefix: `arranger/zoomInLanes`, `arranger/zoomOutLanes`, `arranger/zoomInSelectedLanes`, `arranger/zoomOutSelectedLanes`. No params, simple void calls.
**Rationale:** These are Arranger-specific operations (not Application methods like app/zoomIn). ArrangerHandler already has 7 arranger visibility methods. Lane zoom is a natural extension. Skip hardware stepper variants (not useful for RPC).
**Alternatives considered:** Adding to ApplicationHandler (wrong — these are Arranger methods), including steppers (hardware-oriented).
**Status:** ACTIVE
**ID:** D-1.2

## 2026-03-14 — Timeline navigation: How to expose zoomToContentRegion?

**Decision:** Add `arranger/zoomToRegion` that takes `{from: double, to: double}` (beat positions) and calls `scrollbarModel.zoomToContentRegion(from, to)`. Also add `arranger/zoomToFitSelectionOrAll` (no params). The ScrollbarModel needs to be created in GigMaestroExtension via `arranger.getHorizontalScrollbarModel()` and passed to ArrangerHandler.
**Rationale:** zoomToContentRegion is the most powerful navigation tool — Claude can frame any bar range (e.g., bars 17-24 for a chorus). zoomToFitSelectionOrAll is a useful toggle that cycles between fitting the selection and fitting everything. Both need the ScrollbarModel which must be created during init.
**Alternatives considered:** Exposing zoomAtPosition (more complex API with logarithmic distance param — harder to use), exposing all 4 zoom-to-fit variants (diminishing returns — the basic ones are already in app/).
**Status:** ACTIVE
**ID:** D-1.3
