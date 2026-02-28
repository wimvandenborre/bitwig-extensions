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

## 2026-02-27 — Clips: What clip launcher state to observe and how many scene slots?

**Decision:** Add clip launcher state to StateCache via the existing TrackBank. 8 scene slots per track (matching Bitwig's default clip launcher grid). Observe per-slot: `hasContent`, `isPlaying`, `isRecording`, `isPlaybackQueued`, `isRecordingQueued`, `isStopQueued`, `name`, `color`. Use `ClipLauncherSlotBank` from each Track in the existing 64-track TrackBank. Include clip data in `session/snapshot` under each track as a `clips` array.
**Rationale:** The TrackBank already exists — `track.clipLauncherSlotBank()` gives direct access to slots without creating new API objects. 8 slots covers a typical session view. Per-slot observers use `IndexedBooleanValueChangedCallback` and `IndexedStringValueChangedCallback` patterns from the ClipLauncherSlotBank — efficient batch callbacks rather than per-slot individual observers.
**Alternatives considered:** 16 or 32 slots (overkill for most sessions, bloats snapshot), individual `ClipLauncherSlot.isPlaying()` observers (less efficient than bank-level indexed callbacks).
**Known limitation:** Fixed 8-slot window with no paging. Projects with >8 scenes require bank scrolling (deferred to a future phase).
**Status:** ACTIVE
**ID:** D-2.1

## 2026-02-27 — Clips: What clip launcher actions to expose?

**Decision:** Expose per-slot actions: `clip/launch` (params: `trackIndex`, `slotIndex`), `clip/stop` (params: `trackIndex` — stops the track's clip launcher), `clip/record` (params: `trackIndex`, `slotIndex`), `clip/create` (params: `trackIndex`, `slotIndex`, `lengthInBeats`). Expose scene actions: `scene/launch` (params: `index`), `scene/getState` (returns scene names and clip counts for all 8 scenes). Use `ClipLauncherSlotBank.launch(slot)`, `.stop()`, `.record(slot)`, `.createEmptyClip(slot, length)` and `SceneBank.launchScene(index)`.
**Rationale:** These are the essential clip launcher operations for an LLM agent — trigger clips, stop tracks, record into slots, create empty clips for recording. Scene launching enables triggering entire rows. `clip/select`, `clip/duplicate`, `clip/showInEditor` are UI-focused and low-value for programmatic control.
**Alternatives considered:** Including `launchWithOptions` (quantization/launch mode params add complexity, default behavior is correct for most use cases), per-slot `stop` (the API is per-track, not per-slot).
**Status:** ACTIVE
**ID:** D-2.2

## 2026-02-27 — Devices: How to create and wire cursor device navigation?

**Decision:** Create a `CursorTrack` via `host.createCursorTrack("gig-cursor", "Gig Maestro", 0, 8, true)` — follows user's track selection, 8 scene slots (matches clip grid), 0 sends. Create a `CursorDevice` from the CursorTrack via `cursorTrack.createCursorDevice("gig-device", "Gig Device", 0, CursorDeviceFollowMode.FOLLOW_SELECTION)` — follows user's device selection. Create a `CursorRemoteControlsPage` with 8 parameters via `cursorDevice.createCursorRemoteControlsPage(8)`.
**Rationale:** `FOLLOW_SELECTION` means the extension tracks whatever the user has selected in Bitwig's UI — the most intuitive behavior for LLM agents that want to inspect/modify "the current device." The CursorTrack's `shouldFollowSelection=true` keeps it in sync with UI selection. 8 parameters per page matches Bitwig's standard remote controls layout.
**Alternatives considered:** `FIRST_INSTRUMENT` follow mode (too restrictive — user may want to inspect effects), creating CursorDevice from Track instead of CursorTrack (loses track navigation capability), independent cursor that doesn't follow UI (confusing for users).
**Status:** ACTIVE
**ID:** D-2.3

## 2026-02-27 — Devices: What device state to observe and expose?

**Decision:** Observe on CursorDevice: `name`, `isEnabled`, `isPlugin`, `position`, `presetName`, `presetCategory`, `presetCreator`, `isWindowOpen`, `isExpanded`. Observe on CursorRemoteControlsPage: `selectedPageIndex`, `pageCount`, `pageNames`. Observe per-parameter (8): `name`, `value` (normalized 0-1), `displayedValue` (formatted string). Include device state in `session/snapshot` as a top-level `device` object with nested `remoteControls` containing `pageName`, `pageIndex`, `pageCount`, `pageNames`, and `parameters` array.
**Rationale:** This gives LLM agents complete read access to the selected device and its parameters — enough to understand what's loaded, what page is active, and what each knob does. `displayedValue` is critical for agents — it shows "1.2 kHz" instead of "0.34," making parameters human-interpretable. `presetName`/`presetCategory` help identify the device configuration.
**Alternatives considered:** Observing `modulatedValue` (adds complexity, normalized value is sufficient for read), observing device slots/layers (too deep for v0.2), skipping `displayedValue` (loses the most useful information for agents).
**Status:** ACTIVE
**ID:** D-2.4

## 2026-02-27 — Devices: What device actions to expose?

**Decision:** Navigation: `device/selectNext`, `device/selectPrevious` (navigate device chain), `device/selectPage` (params: `index` — select remote controls page), `device/nextPage`, `device/previousPage`. State: `device/setEnabled` (params: `enabled`), `device/setParameterValue` (params: `index`, `value` — set normalized 0-1 value via `setImmediately()`). Track navigation: `cursor/selectTrack` (params: `direction` — "next" or "previous"), `cursor/getTrackState` (returns current CursorTrack name, position, type).
**Rationale:** Device chain navigation + parameter page navigation + parameter mutation covers the core device workflow. Track cursor navigation lets agents move through the track list to inspect different devices. Using `setImmediately()` for parameters matches the pattern established in Phase 1 for volume/pan.
**Alternatives considered:** `device/selectParent` and nested device navigation (too complex for v0.2, layer/slot navigation deferred), preset browsing (requires PresetBrowser API, separate concern), `device/toggleEnabled` (explicit `setEnabled` is clearer for RPC).
**Status:** ACTIVE
**ID:** D-2.5

## 2026-02-27 — Architecture: How to organize the new handlers?

**Decision:** Three new handler classes: `ClipHandler` (clip/*, scene/* methods), `DeviceHandler` (device/* methods), `CursorHandler` (cursor/* methods). StateCache gains `registerClipObservers(TrackBank, SceneBank)` and `registerDeviceObservers(CursorDevice, CursorRemoteControlsPage)`. Snapshot gains `clips` array nested under each track and a top-level `device` object. Scene state included as top-level `scenes` array.
**Rationale:** Follows the Phase 1 pattern — one handler per domain, observers in StateCache, reads via snapshot. ClipHandler owns clip slot and scene actions (they're tightly coupled — scenes launch clips). DeviceHandler owns device-level mutations. CursorHandler owns track cursor navigation (separate from TrackHandler which mutates track state by index).
**Alternatives considered:** Merging scene into a separate SceneHandler (overkill — only 2 scene methods), putting cursor in TrackHandler (cursor is conceptually different from index-based track access), splitting DeviceHandler into navigation vs. mutation (unnecessary granularity).
**Status:** AMENDED
**ID:** D-2.6

## 2026-02-27 — Architecture: How to organize the new handlers? (amended)

**Decision:** Two new handler classes: `ClipHandler` (clip/*, scene/* methods) and `DeviceHandler` (device/*, cursor/* methods — owns cursor track navigation, device chain navigation, page navigation, and parameter mutations). StateCache gains `registerClipObservers(TrackBank)` and `registerDeviceObservers(CursorTrack, CursorDevice, CursorRemoteControlsPage)`. Snapshot gains `clips` array nested under each track, a top-level `scenes` array, and a top-level `device` object.
**Rationale:** Cursor track/device navigation and device params/pages are a single conceptual workflow from the LLM's perspective — "I'm looking at a device and tweaking it." Splitting cursor nav vs device actions across two classes fragments one domain.
**Alternatives considered:** Three handlers with separate CursorHandler (original proposal — splits a single workflow across two classes).
**Status:** ACTIVE
**ID:** D-2.6a
**Note:** Overridden by user — original: three handlers (ClipHandler, DeviceHandler, CursorHandler).
