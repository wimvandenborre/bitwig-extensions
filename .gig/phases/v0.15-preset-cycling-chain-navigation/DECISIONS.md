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

## 2026-03-01 — Scope: What capabilities does Phase 15 include?

**Decision:** Phase 15 covers "Preset Cycling & Device Chain Navigation" — two concerns: (1) preset browsing via simple next/prev cycling on both track and master devices, (2) nested device chain navigation via slot entry/exit. Excludes PopupBrowser API (complex, future phase), full layer/drum pad bank management (requires new bank objects + observers), and modulation mapping (separate concern).
**Rationale:** Sound design with Bitwig default devices requires inserting devices (already done), selecting presets (new), and navigating container devices like Instrument Layer/FX Layer (new). Preset cycling + slot navigation cover the core workflows without the complexity of PopupBrowser or drum pad banks.
**Alternatives considered:** Full PopupBrowser integration (powerful but complex — 9 filter columns, async commit/cancel, needs its own handler and extensive state). Layer/drum pad bank management (requires DeviceLayerBank/DrumPadBank creation, per-layer observers, significant StateCache additions). Both deferred to keep Phase 15 focused.
**Status:** ACTIVE
**ID:** D-15.1

## 2026-03-01 — Preset: How should preset browsing work?

**Decision:** Use deprecated `CursorDevice` preset methods: `switchToNextPreset()`, `switchToPreviousPreset()`, `switchToNextPresetCategory()`, `switchToPreviousPresetCategory()`, `switchToNextPresetCreator()`, `switchToPreviousPresetCreator()`. Six methods total (next/prev for preset, category, creator).
**Rationale:** These methods are deprecated since API v1 in favor of PopupBrowser, but still functional in Bitwig 6.0. They are single fire-and-forget calls — perfect for an LLM agent. Preset name, category, and creator are already observed in the snapshot, so cycling updates are immediately visible. PopupBrowser is the "proper" replacement but requires managing a stateful browsing session with 9 filter columns.
**Alternatives considered:** PopupBrowser API (`host.createPopupBrowser()`) — modern, powerful, supports filtering and audition, but requires a new handler class, session state management, and ~10+ methods to be usable. `createDeviceBrowser()` — also deprecated (in favor of PopupBrowser), more complex than simple cycling.
**Status:** REVISED
**ID:** D-15.2

## 2026-03-02 — Preset: Deprecated methods throw runtime error — removed

**Decision:** Remove all 8 preset cycling methods. The deprecated `switchToNext/PreviousPreset*()` methods throw runtime errors in Bitwig API v25 ("deprecated since API version 2"). Preset browsing requires PopupBrowser API (deferred to future phase).
**Rationale:** UAT discovered all 6 `Device.switchTo*()` methods throw `-32603` at runtime despite compiling. Not "deprecated but functional" — actually broken. Removing is cleaner than wrapping in error handlers.
**Alternatives considered:** Wrapping in try-catch and returning a descriptive error — but returning "this method doesn't work" is worse than not offering it.
**Status:** ACTIVE
**ID:** D-15.2r

## 2026-03-01 — Navigation: How should nested device chain navigation work?

**Decision:** Use `CursorDevice.selectFirstInSlot(String chain)` to enter nested chains and `CursorDevice.selectParent()` to exit. Add `hasSlots`, `slotNames`, `hasLayers`, `hasDrumPads`, `isNested` to the device and masterDevice snapshot sections so agents can inspect device structure before navigating.
**Rationale:** Container devices (Instrument Layer, FX Layer, Note FX Layer, Drum Machine) expose nested chains as named slots. `selectFirstInSlot` + `selectParent` are the CursorDevice's built-in navigation for this. No new bank objects needed — the existing CursorDevice and RemoteControlsPage follow the cursor as it moves into/out of slots. Snapshot nesting info tells the agent whether navigation is possible.
**Alternatives considered:** DeviceLayerBank approach (create a bank, iterate layers, select by index) — more powerful for layer enumeration but requires new bank objects, observers, and snapshot sections. Deferred because slot navigation covers the primary use case (entering a specific chain by name).
**Status:** ACTIVE
**ID:** D-15.3

## 2026-03-01 — Architecture: How should new methods be organized?

**Decision:** Extend existing DeviceHandler with ~8 new methods (`device/nextPreset`, `device/previousPreset`, `device/nextPresetCategory`, `device/previousPresetCategory`, `device/nextPresetCreator`, `device/previousPresetCreator`, `device/enterSlot`, `device/exitToParent`). Extend MasterDeviceHandler with ~4 new methods (`masterDevice/nextPreset`, `masterDevice/previousPreset`, `masterDevice/enterSlot`, `masterDevice/exitToParent`). No new handler class.
**Rationale:** These methods operate on the existing CursorDevice objects — they belong in the existing handlers. Adding them maintains the pattern of one handler per API domain. Master device gets preset cycling and slot navigation but not category/creator cycling (less relevant for master bus FX).
**Alternatives considered:** New PresetHandler class — would split device operations across two handlers, breaking the established pattern. New BrowserHandler — premature since we're using deprecated cycling, not PopupBrowser.
**Status:** ACTIVE
**ID:** D-15.4

## 2026-03-01 — Snapshot: What new state should be observed?

**Decision:** Add 5 nesting-related fields to both `device` and `masterDevice` snapshot sections: `isNested` (boolean), `hasSlots` (boolean), `slotNames` (string array), `hasLayers` (boolean), `hasDrumPads` (boolean). These are BooleanValue/StringArrayValue observers on CursorDevice. Preset name/category/creator are already observed — no additions needed there.
**Rationale:** Agents need to know the device structure to make navigation decisions. `hasSlots` + `slotNames` tells them which chains are available. `isNested` tells them if they're already inside a chain. `hasLayers`/`hasDrumPads` signals future capabilities (layer bank navigation) even before those methods exist.
**Alternatives considered:** Only add `hasSlots` and `slotNames` (minimal) — but `isNested`, `hasLayers`, `hasDrumPads` are cheap boolean observers and provide valuable context for agent decision-making.
**Status:** ACTIVE
**ID:** D-15.5
