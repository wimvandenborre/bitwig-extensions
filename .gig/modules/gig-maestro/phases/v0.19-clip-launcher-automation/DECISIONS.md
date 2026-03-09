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
**Status:** ACTIVE | ACTIVE | AMENDED | REVISED
**ID:** D-{batch}.{num}
-->

## 2026-03-02 — Scope: What clip launcher automation features to expose?

**Decision:** Expose per-clip launch settings (launchQuantization, launchMode, shuffle, accent, useLoopStartAsQuantizationReference), global transport clip launcher settings (defaultLaunchQuantization, postRecordingAction, postRecordingTimeOffset, overdub, fillMode), and enhanced launch methods (launchWithOptions on clips and scenes). No follow actions — not available in API v25.
**Rationale:** These are all the clip launcher automation features available in the Bitwig Controller API v25. Follow actions exist in the UI but are not exposed in the API, so we cannot support them. The scope covers the three levels of launch control: global defaults, per-clip settings, and per-launch overrides.
**Alternatives considered:** (1) Only per-clip settings without transport globals — rejected because agents need to control global defaults too. (2) Include follow actions — impossible, not in API.
**Status:** ACTIVE
**ID:** D-19.1

## 2026-03-02 — Observers: What clip cursor properties to observe?

**Decision:** Add 5 new cursor clip observers to StateCache: `launchQuantization` (SettableEnumValue), `launchMode` (SettableEnumValue), `shuffle` (SettableBooleanValue), `accent` (SettableRangedValue), `useLoopStartAsQuantizationReference` (SettableBooleanValue). Nest under the existing `clip` snapshot section.
**Rationale:** These properties are all on the Clip interface (which CursorClip extends) and follow the same observer pattern as existing clipPlayStart/clipPlayStop/clipLoopLength fields. Adding them to the clip snapshot section keeps the state model clean — agents already read clip state from `snapshot.clip`.
**Alternatives considered:** (1) Separate "clipLauncher" snapshot section — rejected, adds unnecessary section when clip section already exists. (2) Only observe on demand — rejected, snapshot model requires persistent observation.
**Status:** ACTIVE
**ID:** D-19.2

## 2026-03-02 — Observers: What transport clip launcher settings to observe?

**Decision:** Add 5 new transport-level observers to StateCache: `defaultLaunchQuantization` (SettableEnumValue), `clipLauncherPostRecordingAction` (SettableEnumValue), `clipLauncherPostRecordingTimeOffset` (SettableBeatTimeValue), `isClipLauncherOverdubEnabled` (SettableBooleanValue), `isFillModeActive` (SettableBooleanValue). Nest under the existing `transport` snapshot section.
**Rationale:** These are global clip launcher settings on the Transport interface. They belong in the transport snapshot section alongside existing transport state (tempo, position, loop, etc.). The observer pattern matches existing SettableEnumValue/SettableBooleanValue observers.
**Alternatives considered:** (1) New top-level "clipLauncher" snapshot section — rejected, these are Transport properties. (2) Skip postRecordingTimeOffset — rejected, it's useful alongside the action enum.
**Status:** ACTIVE
**ID:** D-19.3

## 2026-03-02 — Methods: What RPC methods to add for per-clip settings?

**Decision:** Add 5 setter methods to ClipHandler: `clip/setLaunchQuantization` (enum), `clip/setLaunchMode` (enum), `clip/setShuffle` (boolean), `clip/setAccent` (double 0.0–1.0), `clip/setUseLoopStartAsQuantizationReference` (boolean). These operate on the cursor clip. Add 1 getter: `clip/getLaunchSettings` that returns all 5 values.
**Rationale:** Each Clip property needs a dedicated setter for agents to modify individual settings. A combined getter avoids 5 separate getter calls. All operate on the cursor clip (same as clip/rename, clip/getNotes). Enum values use the exact Bitwig strings for consistency.
**Alternatives considered:** (1) Single `clip/setLaunchSettings` with optional params — rejected, atomic setters are simpler and match the handler pattern. (2) No getter (rely on snapshot) — rejected, a dedicated getter provides immediate confirmation.
**Status:** ACTIVE
**ID:** D-19.4

## 2026-03-02 — Methods: What RPC methods to add for transport clip launcher settings?

**Decision:** Add 5 setter methods to TransportHandler: `transport/setDefaultLaunchQuantization` (enum), `transport/setPostRecordingAction` (enum), `transport/setPostRecordingTimeOffset` (double beats), `transport/setClipLauncherOverdub` (boolean), `transport/setFillMode` (boolean). Add 1 getter: `transport/getClipLauncherSettings`.
**Rationale:** These are global settings that affect all clip launcher behavior. Each needs an individual setter for precision. A combined getter provides a single call to read all 5 settings. Naming follows existing transport method conventions (transport/setLoop, transport/setMetronome).
**Alternatives considered:** (1) Add to a new "clipLauncher/" namespace — rejected, these are Transport API methods. (2) Skip setPostRecordingTimeOffset — rejected, it's the complement to the action enum.
**Status:** ACTIVE
**ID:** D-19.5

## 2026-03-02 — Methods: How to expose launchWithOptions on clips and scenes?

**Decision:** Enhance existing `clip/launch` and `scene/launch` with optional `quantization` and `launchMode` parameters. When both are omitted, behavior is unchanged (calls `slot.launch()`). When provided, calls `slot.launchWithOptions(quantization, launchMode)`. Both params required together.
**Rationale:** Adding optional params to existing methods is cleaner than creating separate `clip/launchWithOptions` and `scene/launchWithOptions` methods. Agents already use `clip/launch` and `scene/launch` — they just gain optional overrides. The "both or neither" requirement matches the API signature.
**Alternatives considered:** (1) New dedicated methods `clip/launchWithOptions`, `scene/launchWithOptions` — rejected, unnecessary method duplication. (2) Allow partial params (only quantization) — rejected, API requires both.
**Status:** ACTIVE
**ID:** D-19.6
