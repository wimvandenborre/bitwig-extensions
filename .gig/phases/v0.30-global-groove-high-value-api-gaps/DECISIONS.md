# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-14 — Scope: What high-value API gaps to address in this phase?

**Decision:** Combine three high-value gaps into one phase: Global Groove control (new handler), Exclusive Solo (TrackHandler addition), and Application Zoom (ApplicationHandler addition). These are all small, independent additions that share no dependencies.
**Rationale:** Each gap is 1-3 methods — too small for individual phases. Combining keeps momentum without sacrificing quality. Research confirmed Groove is entirely missing (6 params), exclusive solo is 1 method, and zoom is ~6 methods.
**Alternatives considered:** One phase per gap (too granular), or including medium-value items too (too large — mixer visibility and drum pads are better as a separate phase).
**Status:** ACTIVE
**ID:** D-1.1

## 2026-03-14 — Groove: Create new GrooveHandler or add to existing handler?

**Decision:** Create a new `GrooveHandler` class with its own `groove/` RPC namespace. The Groove object must be created via `host.createGroove()` in GigMaestroExtension and passed to the handler.
**Rationale:** Groove is a distinct Bitwig concept with its own interface. It doesn't belong in TransportHandler or any existing handler. New handler follows the established pattern (constructor + register). Need 3 RPC methods: `groove/getState` (read all 6 params), `groove/setEnabled` (toggle), `groove/setParameter` (set any of the 5 ranged params by name).
**Alternatives considered:** Adding to TransportHandler (groove is transport-adjacent but conceptually separate), individual setters per param (6 separate RPCs is excessive for simple parameter writes).
**Status:** ACTIVE
**ID:** D-1.2

## 2026-03-14 — Groove: How to expose groove parameters?

**Decision:** Expose groove via 3 methods: `groove/getState` returns all 6 parameter values as a JSON object, `groove/setEnabled` takes a boolean, and `groove/setParameter` takes `name` (one of "shuffleAmount", "shuffleRate", "accentAmount", "accentRate", "accentPhase") and `value` (0.0–1.0). All Parameter objects need `markInterested()` during observer registration.
**Rationale:** Groove params are all `Parameter` (ranged values), so a generic setter with name dispatch is cleaner than 5 individual RPCs. The `getState` method follows the snapshot pattern used throughout the codebase. Enabled is a separate method because it's a boolean toggle, not a ranged value.
**Alternatives considered:** Individual setters per param (verbose), single `groove/set` that handles both enabled and params (mixing concerns).
**Status:** ACTIVE
**ID:** D-1.3

## 2026-03-14 — Solo: How to expose exclusive solo?

**Decision:** Add `track/toggleSolo` to TrackHandler that accepts `index` (int) and optional `exclusive` (boolean, default false). Calls `track.solo().toggle(exclusive)` when exclusive is true, otherwise `track.solo().toggle()`.
**Rationale:** The existing `track/setSolo` uses `set(boolean)` for absolute state. `toggle(exclusive)` is a different operation — exclusive solo unsolos all other tracks. Adding a separate `toggleSolo` method preserves backward compatibility and clearly signals the toggle behavior.
**Alternatives considered:** Adding `exclusive` param to existing `setSolo` (confusing — set vs toggle are different operations), separate `track/exclusiveSolo` (too narrow — toggle is the natural API).
**Status:** ACTIVE
**ID:** D-1.4

## 2026-03-14 — Zoom: Which zoom methods to expose?

**Decision:** Add 4 zoom methods to ApplicationHandler: `app/zoomIn`, `app/zoomOut`, `app/zoomToFit`, `app/zoomToSelection`. These are the essential zoom operations. Skip `zoomToSelectionOrAll`, `zoomToSelectionOrPrevious`, and `zoomLevel()` stepper (hardware-oriented, not useful for RPC).
**Rationale:** The 4 core zoom methods cover all practical needs for programmatic zoom control. The "OrAll" and "OrPrevious" variants are toggle behaviors better suited for hardware bindings. The `zoomLevel()` stepper requires a relative hardware control binding, not applicable to RPC.
**Alternatives considered:** Exposing all 12 zoom variants (many are hardware-oriented), only zoomToFit (too limited).
**Status:** ACTIVE
**ID:** D-1.5

## 2026-03-14 — StateCache: Should groove state be cached?

**Decision:** Add groove state to StateCache via a new `registerGrooveObservers(Groove)` method. Include groove data in the session snapshot under a `groove` key and in delta notifications.
**Rationale:** Follows the established pattern — all observable state goes through StateCache for snapshot and delta broadcasts. Groove params are small (6 values) and useful for UI clients monitoring shuffle state.
**Alternatives considered:** Not caching (inconsistent with other state), lazy read only (breaks delta notification pattern).
**Status:** ACTIVE
**ID:** D-1.6
