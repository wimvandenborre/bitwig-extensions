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

## 2026-03-02 — Scope: What should Phase 16 cover?

**Decision:** Focus on **state management and transport navigation** — audio engine control, project-wide state operations (unsolo/unmute/unarm all + observation), transport navigation commands (cue marker jumping, continue, restart, return to arrangement), metronome volume, pre-roll, and popup notifications. Defer UI-visual features (zoom, panel layout, panel toggles, keyboard emulation) since the LLM agent can't see the screen.
**Rationale:** The LLM agent controls Bitwig via RPC — it needs state queries and actions, not visual UI manipulation. Engine control, bulk state reset, and transport navigation are directly useful for music production workflows. Zoom/panel toggles only benefit human users with a visible Bitwig window.
**Alternatives considered:** (1) Include everything (zoom, panels, toggles) — bloats phase with 30+ methods the agent rarely uses. (2) Only engine + project state — too narrow, misses useful transport nav methods.
**Status:** ACTIVE
**ID:** D-16.1

## 2026-03-02 — Architecture: How to organize new methods?

**Decision:** Extend **ApplicationHandler** with 4 new methods (engine on/off, notification, panel layout set). Create a new **ProjectHandler** for 6 project-level methods (unsolo/unmute/unarm all + createScene + createSceneFromPlaying). Extend **TransportHandler** with 6 new methods (continue, restart, return to arrangement, cue marker prev/next, pre-roll set). Total: 16 new RPC methods.
**Rationale:** ApplicationHandler already owns app-level operations (undo/redo/getState). ProjectHandler is new because `host.getProject()` returns a distinct `Project` object with its own method set — mixing it into ApplicationHandler would be semantically wrong. TransportHandler already owns all transport operations. This keeps handler boundaries aligned with Bitwig API object boundaries.
**Alternatives considered:** (1) Single "SessionHandler" for everything — too broad, violates existing one-handler-per-API-object pattern. (2) Put project methods in ApplicationHandler — `Project` and `Application` are separate Bitwig objects with different purposes.
**Status:** ACTIVE
**ID:** D-16.2

## 2026-03-02 — Snapshot: What project state to observe?

**Decision:** Add 4 project-level fields to the `application` snapshot section: `hasSoloedTracks` (boolean), `hasMutedTracks` (boolean), `hasArmedTracks` (boolean), `isModified` (boolean). Add 2 transport fields: `metronomeVolume` (double, 0.0–1.0), `preRoll` (string enum: "none"/"one_bar"/"two_bars"/"four_bars"). Also add `panelLayout` (string: "ARRANGE"/"MIX"/"EDIT") to application section.
**Rationale:** These fields let the agent reason about project state — e.g., check if any tracks are soloed before mixdown, verify project is saved (isModified=false), or read current pre-roll setting. MetronomeVolume is useful for adjusting click loudness during recording workflows. PanelLayout lets the agent know which view the user is in.
**Alternatives considered:** (1) Skip isModified — useful signal, low cost. (2) Add all transport settings to snapshot — most are already available via getLoopRange, keeping it focused.
**Status:** ACTIVE
**ID:** D-16.3

## 2026-03-02 — Notifications: How to surface feedback to the user?

**Decision:** Add `app/showNotification` method calling `host.showPopupNotification(text)`. This shows a temporary overlay in Bitwig's UI. Single string parameter, fire-and-forget. No observation needed.
**Rationale:** The LLM agent has no way to communicate with the user inside Bitwig. Popup notifications let it signal completion ("Song structure built"), warnings ("Track 3 is muted"), or status updates. It's a single API call with zero infrastructure cost.
**Alternatives considered:** (1) Skip notifications — agent loses its only in-DAW communication channel. (2) Use console println — not visible to user, only in controller debug log.
**Status:** ACTIVE
**ID:** D-16.4

## 2026-03-02 — Transport: Which navigation methods to add?

**Decision:** Add 6 transport methods: `transport/continuePlayback` (resume without resetting position), `transport/restart` (restart from play-start position), `transport/returnToArrangement` (exit clip launcher override), `transport/jumpToPreviousCueMarker`, `transport/jumpToNextCueMarker`, `transport/setPreRoll` (set pre-roll mode: none/one_bar/two_bars/four_bars). Also add `transport/setMetronomeVolume` (normalized 0.0–1.0).
**Rationale:** `continuePlayback` vs `play` distinction matters — `play` resets to play-start position, `continuePlayback` resumes from current position. `returnToArrangement` is essential after clip launcher use. Cue marker jumping complements existing cue marker creation/rename in ArrangerHandler. Pre-roll and metronome volume round out the recording workflow controls.
**Alternatives considered:** (1) Add fill mode toggle — niche feature, defer. (2) Add time signature setter — risky mid-project, defer. (3) Add crossfade control — rare use case for LLM agent.
**Status:** ACTIVE
**ID:** D-16.5
