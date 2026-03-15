# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-15 — DetailEditor: New handler vs extend ArrangerHandler

**Decision:** Create a new `DetailEditorHandler` class. Do not extend ArrangerHandler.
**Rationale:** DetailEditor and Arranger are separate Bitwig API objects with different method sets. ArrangerHandler already has 23 methods covering arranger + cue markers. A separate handler keeps concerns clean and avoids a bloated class. The `detailEditor/` RPC namespace clearly distinguishes from `arranger/`.
**Alternatives considered:** Adding to ArrangerHandler — rejected because it conflates two separate panels and would make the handler too large.
**Status:** ACTIVE
**ID:** D-1.1

## 2026-03-15 — DetailEditor: RPC method set

**Decision:** Expose 7 methods: `detailEditor/zoomIn`, `detailEditor/zoomOut`, `detailEditor/zoomToFit`, `detailEditor/zoomToSelection`, `detailEditor/zoomToFitSelectionOrAll`, `detailEditor/zoomInLanes`, `detailEditor/zoomOutLanes`. Plus 1 ScrollbarModel method: `detailEditor/zoomToRegion`.
**Rationale:** These mirror the useful arranger zoom methods. `zoomToFitSelectionOrPrevious` is omitted as niche (toggles between selection and previous zoom — confusing UX). The lane stepper (`zoomLaneHeightsStepper`) is hardware-binding only, not callable via RPC.
**Alternatives considered:** Including zoomToFitSelectionOrPrevious — rejected as confusing for AI-driven workflows where explicit targets are clearer.
**Status:** ACTIVE
**ID:** D-1.2

## 2026-03-15 — DetailEditor: No StateCache integration

**Decision:** No StateCache changes. DetailEditor has no observable state properties — it's purely command-based (zoom actions with no readable state).
**Rationale:** Unlike Arranger which has 7 SettableBooleanValue visibility toggles, DetailEditor exposes zero observable values. Nothing to cache or delta.
**Alternatives considered:** None — there's literally nothing to observe.
**Status:** ACTIVE
**ID:** D-1.3

## 2026-03-15 — DetailEditor: Window parameter

**Decision:** Use `host.createDetailEditor()` (default, no window param). Do not expose window selection via RPC.
**Rationale:** Multi-window support is niche — most users have a single Bitwig window. The default (-1) targets the first available detail editor. Adding window params complicates every RPC call for minimal gain.
**Alternatives considered:** Passing window index — rejected as over-engineering for the common single-window case.
**Status:** ACTIVE
**ID:** D-1.4
