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

## 2026-03-01 — Scope: What does Phase 14 cover?

**Decision:** Phase 14 adds **master bus device control** — a dedicated CursorDevice on the master track for inserting, removing, navigating, enabling/disabling, and controlling parameters of master bus effects. This closes the known limitation "No master track CursorDevice" flagged since Phase 5. The agent gains the ability to build complete signal chains on the master bus (e.g., EQ → compressor → limiter) alongside the existing track device capabilities.
**Rationale:** Master bus FX is a standard mixing workflow gap. The API confirms `MasterTrack` inherits all `DeviceChain` methods including `createCursorDevice()` and insertion points. No API limitations prevent this. Phase 13 completed mixer routing — master device control is the natural next step for a complete mixing surface.
**Alternatives considered:** Bundling with track device refactoring — deferred to keep scope focused. Adding plugin discovery (VST/CLAP scan) — separate concern, out of scope.
**Status:** ACTIVE
**ID:** D-14.1

## 2026-03-01 — Infrastructure: How to create master device cursors?

**Decision:** Create a second `CursorDevice` and `CursorRemoteControlsPage` on the master track in `GigMaestroExtension.init()`. Use `masterTrack.createCursorDevice("gig-master-device", "Gig Master Device", 0, CursorDeviceFollowMode.FOLLOW_SELECTION)` and `masterCursorDevice.createCursorRemoteControlsPage(8)`. These are independent of the existing track cursor device — both can coexist.
**Rationale:** Bitwig allows multiple cursor devices across different tracks. The master cursor device follows device selection on the master track only, while the existing track cursor follows the selected regular track's device. No conflict. 8 remote control parameters per page matches the existing track device pattern.
**Alternatives considered:** Sharing a single CursorDevice and switching targets — would break observation state and require complex context-switching. Using DeviceBank instead of CursorDevice — loses cursor navigation and insertion point methods.
**Status:** ACTIVE
**ID:** D-14.2

## 2026-03-01 — API: RPC method naming convention

**Decision:** Use `masterDevice/` prefix for all master device methods: `masterDevice/selectNext`, `masterDevice/selectPrevious`, `masterDevice/setEnabled`, `masterDevice/insertBitwigDevice`, `masterDevice/insertPluginDevice`, `masterDevice/remove`, `masterDevice/selectPage`, `masterDevice/nextPage`, `masterDevice/previousPage`, `masterDevice/setParameterValue`. This mirrors the existing `device/` namespace but scoped to master.
**Rationale:** Clean separation from track `device/` methods. The `masterDevice/` prefix makes it unambiguous which device chain is being targeted. Follows the existing pattern where `master/setVolume` parallels `track/setVolume`.
**Alternatives considered:** Adding a `target` parameter to existing `device/` methods (e.g., `{"target": "master"}`) — breaks existing tool schemas and adds conditional logic to every method. Using `master/device/` (nested slash) — inconsistent with flat namespace convention used everywhere else.
**Status:** ACTIVE
**ID:** D-14.3

## 2026-03-01 — API: Which methods to implement?

**Decision:** 10 RPC methods mirroring the core track device methods: (1) `masterDevice/selectNext`, (2) `masterDevice/selectPrevious` — navigate the master device chain. (3) `masterDevice/setEnabled` — bypass master devices. (4) `masterDevice/insertBitwigDevice` — insert Bitwig native device on master. (5) `masterDevice/insertPluginDevice` — insert VST2/VST3/CLAP on master. (6) `masterDevice/remove` — remove selected master device. (7) `masterDevice/selectPage` — jump to parameter page by index. (8) `masterDevice/nextPage`, (9) `masterDevice/previousPage` — page navigation. (10) `masterDevice/setParameterValue` — set remote control parameter. Exclude automation methods (touch, writeEnvelope, deleteAllAutomation, etc.) — master bus automation is an edge case that can be added later.
**Rationale:** These 10 methods cover the full device lifecycle on master: insert → navigate → configure parameters → remove. Automation on master devices is rare (usually static settings on limiters/EQ). Excluding automation keeps scope tight — 10 methods instead of 15.
**Alternatives considered:** Full parity with all 15 device methods — over-engineered for master bus use case. Minimal set (insert + remove only) — too limited, can't control parameters.
**Status:** ACTIVE
**ID:** D-14.4

## 2026-03-01 — Architecture: Code organization

**Decision:** Create a new `MasterDeviceHandler` class in `handlers/`. Constructor takes `(MasterTrack masterTrack, CursorDevice masterCursorDevice, CursorRemoteControlsPage masterRemoteControlsPage, DeviceLibrary deviceLibrary)`. Reuse the existing `DeviceLibrary` for Bitwig device path resolution. The handler is self-contained — no changes to existing `DeviceHandler`.
**Rationale:** Follows the one-handler-per-domain pattern. MasterDeviceHandler is structurally similar to DeviceHandler but simpler (no automation, no cursor track switching). Keeping it separate avoids touching the well-tested DeviceHandler. `DeviceLibrary` is already a shared utility — no duplication needed.
**Alternatives considered:** Extracting a shared `AbstractDeviceHandler` base class — adds abstraction without clear benefit since the methods differ (no automation on master). Extending DeviceHandler with master support — violates single responsibility.
**Status:** ACTIVE
**ID:** D-14.5

## 2026-03-01 — Snapshot: Master device state in snapshot

**Decision:** Add a `masterDevice` section to the snapshot (sibling of existing `device` section). Include: `name`, `isEnabled`, `isPlugin`, `position`, `presetName`, `presetCategory`, `presetCreator`, `pageIndex`, `pageCount`, `pageNames`, and `parameters` array (8 entries with name, value, displayedValue). Register master device observers in `StateCache` using the same pattern as track device observers.
**Rationale:** The agent needs to see what's on the master bus to make mixing decisions. The snapshot structure mirrors the existing `device` section for consistency. Separate `masterDevice` key avoids confusion with the track device section.
**Alternatives considered:** Nesting under `master.device` — would change the existing `master` structure. Omitting snapshot entirely (control-only) — the agent can't see what it's controlling.
**Status:** ACTIVE
**ID:** D-14.6

## 2026-03-01 — Tools: Tool schemas and system prompt

**Decision:** 10 new tool schemas (124 total): `masterDevice_selectNext`, `masterDevice_selectPrevious`, `masterDevice_setEnabled`, `masterDevice_insertBitwigDevice`, `masterDevice_insertPluginDevice`, `masterDevice_remove`, `masterDevice_selectPage`, `masterDevice_nextPage`, `masterDevice_previousPage`, `masterDevice_setParameterValue`. Update system prompt with a "Master Bus FX" subsection under the existing Mixer & Routing section documenting the master device workflow.
**Rationale:** 1:1 method-to-schema pattern continues. System prompt needs to explain when to use `masterDevice/` vs `device/` methods so the agent routes commands to the correct chain.
**Alternatives considered:** None — standard pattern.
**Status:** ACTIVE
**ID:** D-14.7
