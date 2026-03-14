# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-14 — Scope: What mixer/track capabilities to add?

**Decision:** Focus on the Mixer interface: 6 section visibility toggles (get/set), 4 track width zoom methods, and 2 track mixer navigation methods (selectInMixer, makeVisibleInMixer). Create a new MixerHandler. This is 12 new RPC methods total — a focused, cohesive phase around mixer panel control.
**Rationale:** The Mixer interface is completely unexposed — zero methods currently available via RPC. All 6 visibility properties are SettableBooleanValue (read + write), the 4 zoom methods are simple void calls, and selectInMixer/makeVisibleInMixer are on the Channel/Track interface. This gives Claude full mixer panel control. Drum pads and track type queries are better as separate phases.
**Alternatives considered:** Including track type queries (already cached, low effort but different concern), drum pad access (requires setShouldIncludeAllMixerChannels foundation work — separate phase), arranger visibility RPCs (already have arranger toggle methods in ApplicationHandler — lower priority).
**Status:** ACTIVE
**ID:** D-1.1

## 2026-03-14 — Handler: New MixerHandler or extend existing?

**Decision:** Create a new `MixerHandler` class with `mixer/` RPC namespace. The Mixer object is created via `host.createMixer()` in GigMaestroExtension and passed to the handler.
**Rationale:** Mixer is a distinct Bitwig interface (like Groove). It doesn't belong in ApplicationHandler (which handles Application methods) or TrackHandler. Follows the established handler pattern: constructor + register. Need 3 categories of methods: visibility (6 get/set pairs), zoom (4 void calls), and a getState method for reading all visibility at once.
**Alternatives considered:** Adding to ApplicationHandler (mixer is panel-related but has its own dedicated API object — wrong abstraction), splitting visibility and zoom into separate handlers (over-engineering for 12 methods).
**Status:** ACTIVE
**ID:** D-1.2

## 2026-03-14 — Visibility: How to expose mixer section visibility?

**Decision:** Expose via 2 methods: `mixer/getState` returns all 6 visibility booleans as a JSON object, and `mixer/setSection` takes `{section: string, visible: boolean}` to set any section. Valid section names: "meter", "io", "sends", "clipLauncher", "devices", "crossFade". This mirrors the groove pattern (getState + setParameter).
**Rationale:** A generic setter with name dispatch is cleaner than 12 individual RPCs (6 getters + 6 setters). The getState method follows the snapshot pattern used throughout. All 6 properties are SettableBooleanValue, so the API is uniform.
**Alternatives considered:** Individual get/set per section (verbose — 12 methods), toggle-only without read (breaks the get/set pattern), adding to StateCache (mixer visibility is UI state, not music state — doesn't belong in session snapshot).
**Status:** ACTIVE
**ID:** D-1.3

## 2026-03-14 — Zoom: How to expose mixer track width zoom?

**Decision:** Add 4 zoom methods: `mixer/zoomInAll`, `mixer/zoomOutAll`, `mixer/zoomInSelected`, `mixer/zoomOutSelected`. No params, simple void calls. Skip the stepper variants (hardware-oriented, not useful for RPC).
**Rationale:** The 4 core zoom methods cover all practical needs. The stepper variants (`zoomTrackWidthsAllStepper`, `zoomTrackWidthsSelectedStepper`) are RelativeHardwareControlBindable — designed for hardware knobs, not RPC.
**Alternatives considered:** Including steppers (wrong abstraction for RPC), only "all" variants (selected zoom is useful for focused mixing).
**Status:** ACTIVE
**ID:** D-1.4

## 2026-03-14 — Navigation: How to expose mixer track navigation?

**Decision:** Add 2 methods to TrackHandler: `track/selectInMixer` takes `{index: int}` and calls `track.selectInMixer()`, `track/makeVisibleInMixer` takes `{index: int}` and calls `track.makeVisibleInMixer()`. These belong in TrackHandler since they operate on Track objects from the track bank.
**Rationale:** `selectInMixer()` and `makeVisibleInMixer()` are Channel methods (Track extends Channel). They take a track index like other TrackHandler methods. Placing them in MixerHandler would require MixerHandler to access the TrackBank — violating single-responsibility.
**Alternatives considered:** Adding to MixerHandler (would need TrackBank reference — wrong handler), combining into one method with a mode param (different operations with different UI effects).
**Status:** ACTIVE
**ID:** D-1.5

## 2026-03-14 — StateCache: Should mixer state be cached?

**Decision:** Do NOT add mixer visibility to StateCache. Mixer section visibility is UI panel state, not music/session state. It doesn't belong in the session snapshot or delta notifications. The `mixer/getState` RPC reads directly from the Mixer object.
**Rationale:** StateCache tracks music state (transport, tracks, devices, clips, groove). Mixer panel visibility is ephemeral UI state — it changes when the user clicks panels and isn't part of the musical content. Adding it would bloat the snapshot with non-musical data. The getState RPC provides on-demand reads when needed.
**Alternatives considered:** Adding to StateCache for consistency (would add noise to every snapshot/delta — other clients don't need mixer panel state), adding a separate UI cache (over-engineering for 6 booleans).
**Status:** ACTIVE
**ID:** D-1.6
