# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-15 — Modulation: Use RemoteControl.isBeingMapped() not deprecated ModulationSource

**Decision:** Use `RemoteControl.isBeingMapped()` (SettableBooleanValue) for mapping mode control. Do NOT use the deprecated `ModulationSource` or `Device.getModulationSource()` APIs.
**Rationale:** `ModulationSource` is accessible only through deprecated `Macro` or deprecated `Device.getModulationSource(int)`. `RemoteControl.isBeingMapped()` is the modern API (v2+) for the same purpose — it puts a remote control slot into mapping mode so the user can assign a parameter. We already have `RemoteControl` references via `CursorRemoteControlsPage.getParameter(i)`.
**Alternatives considered:** Using deprecated ModulationSource — rejected because it's deprecated and may be removed in future API versions.
**Status:** ACTIVE
**ID:** D-1.1

## 2026-03-15 — Modulation: Extend DeviceHandler with mapping RPCs

**Decision:** Add 2 methods to DeviceHandler: `device/setParameterMapping` (set isBeingMapped for a remote control by index) and `device/getParameterMapping` (read isBeingMapped state for all 8 parameters). Also add equivalent methods for MasterDeviceHandler.
**Rationale:** DeviceHandler already manages `CursorRemoteControlsPage` and all parameter operations. Adding mapping mode control is a natural extension. Both device and master device need the same capability.
**Alternatives considered:** Separate MappingHandler — rejected as over-engineering for 2 methods per handler.
**Status:** ACTIVE
**ID:** D-1.2

## 2026-03-15 — Modulation: Cache isBeingMapped in StateCache

**Decision:** Add `isBeingMapped` boolean array to StateCache device observers, include in snapshot and delta. Same for master device.
**Rationale:** Mapping state changes are infrequent (only when user enters/exits mapping mode) — safe for delta. Including in snapshot lets Claude know which parameters are in mapping mode without polling.
**Alternatives considered:** Poll-only (no cache) — rejected because mapping state changes rarely, making it ideal for delta tracking.
**Status:** ACTIVE
**ID:** D-1.3

## 2026-03-15 — Modulation: Also add clip key scrolling in this phase

**Decision:** Bundle clip key scrolling (5 methods on Clip) into this phase since both are small, low-effort additions. Add to NoteHandler: `note/scrollToKey`, `note/scrollKeysPageUp`, `note/scrollKeysPageDown`, `note/scrollKeysStepUp`, `note/scrollKeysStepDown`.
**Rationale:** Both features are small (2-5 methods each). Combining them avoids a standalone phase for 5 trivial methods. NoteHandler already holds the cursorClip reference needed for these methods.
**Alternatives considered:** Separate phase — rejected as wasteful for 5 one-liner methods.
**Status:** ACTIVE
**ID:** D-1.4
