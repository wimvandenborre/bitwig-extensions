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

## 2026-02-28 — Device Insertion: Which insertion mechanism to use?

**Decision:** Use `InsertionPoint.insertFile(String path)` with `.bwdevice` file paths from Bitwig's bundled library (`/Applications/Bitwig Studio.app/Contents/Resources/Library/devices/`). This is the primary insertion method. Also expose plugin insertion via `insertVST2Device(int)`, `insertVST3Device(String)`, and `insertCLAPDevice(String)` for third-party plugins. Skip `insertBitwigDevice(UUID)` for now — the UUIDs are stored in binary `.bwdevice` files with no documented mapping. **Important:** `insertFile()` has not been verified to work from a Controller API extension at API v25 — a spike task must validate this mechanism before building the full pipeline.
**Rationale:** The `.bwdevice` files have human-readable filenames (e.g., `Polymer.bwdevice`, `Drum Machine.bwdevice`) and there are 151 of them covering all built-in devices. `insertFile()` accepts file paths, which is straightforward and debuggable. The UUID approach requires discovering undocumented device IDs from binary files. VST2/VST3/CLAP insertion uses standard plugin IDs that users already know.
**Alternatives considered:** (a) `insertBitwigDevice(UUID)` — requires UUID discovery; binary `.bwdevice` format makes extraction impractical. (b) Browser API (`createDeviceBrowser()`) — complex multi-step flow (start browsing, navigate, commit) not suited to RPC. **Fallback if insertFile() fails:** Try `insertBitwigDevice(UUID)` with UUIDs extracted from `device-settings/` folder names (247 UUIDs exist, mapping to device names is unknown but could be brute-force tested).
**Status:** ACTIVE
**ID:** D-5.1

## 2026-02-28 — Device Insertion: Where to insert devices?

**Decision:** Insert devices at the end of the cursor track's device chain using `cursorTrack.endOfDeviceChainInsertionPoint()`. Also expose `cursorDevice.afterDeviceInsertionPoint()` and `cursorDevice.beforeDeviceInsertionPoint()` for inserting relative to the currently selected device. Do NOT expose `replaceDeviceInsertionPoint()` in Phase 5 — too destructive for initial implementation.
**Rationale:** End-of-chain is the safest default — adds a new device without disturbing existing ones. The cursor-relative methods (before/after) give the LLM fine-grained control when it needs to insert effects in a specific order. Replace is intentionally omitted to prevent accidental device deletion.
**Alternatives considered:** (a) Track-index-based insertion (`trackBank.getItemAt(i).endOfDeviceChainInsertionPoint()`) — would allow inserting on any track, not just the cursor track. Adds complexity; the LLM can already switch cursor track via `cursor/selectTrack`. (b) Including replace — too risky; defer to a later phase.
**Status:** ACTIVE
**ID:** D-5.2

## 2026-02-28 — Device Insertion: What RPC methods to expose?

**Decision:** Add 5 new RPC methods:
1. `device/insertBitwigDevice` — takes `name` (string, e.g. "Polymer"), optional `position` ("end", "before", "after"; default "end"). Resolves name to `.bwdevice` path internally.
2. `device/insertVST2` — takes `id` (int VST2 plugin ID), optional `position`.
3. `device/insertVST3` — takes `id` (string VST3 plugin ID), optional `position`.
4. `device/insertCLAP` — takes `id` (string CLAP plugin ID), optional `position`.
5. `device/listBitwigDevices` — returns array of available built-in device names (scans the `.bwdevice` directory). No params.
**Rationale:** Separate methods per plugin type matches the Bitwig API's distinct `insertVST2Device(int)` / `insertVST3Device(String)` / `insertCLAPDevice(String)` signatures. `device/insertBitwigDevice` wraps `insertFile()` with name resolution so the LLM doesn't need to know filesystem paths. `device/listBitwigDevices` enables discovery — the LLM can query what's available before inserting.
**Alternatives considered:** (a) Single `device/insert` method with a `type` field — polymorphic dispatch adds parsing complexity and makes tool schemas less clear. (b) Skip listing — the LLM would need to guess device names; discoverability is important.
**Status:** AMENDED
**ID:** D-5.3

## 2026-02-28 — Device Insertion: What RPC methods to expose? (amended)

**Decision:** Add 4 new RPC methods:
1. `device/insertBitwigDevice` — takes `name` (string, e.g. "Polymer"), optional `position` ("end", "before", "after"; default "end"). Resolves name to `.bwdevice` path internally.
2. `device/insertPluginDevice` — takes `type` (enum: "vst2", "vst3", "clap"), `id` (string — int-as-string for VST2, string for VST3/CLAP), optional `position`. Handler dispatches internally to `insertVST2Device(int)`, `insertVST3Device(String)`, or `insertCLAPDevice(String)`.
3. `device/listBitwigDevices` — returns array of available built-in device names from the pre-scanned in-memory map (D-5.4). The scan directory is flat (151 files, no nested folders). No params.
4. `device/remove` — removes the currently selected device (from D-5.6).
**Rationale:** Collapsing VST2/VST3/CLAP into one method reduces surface area — the LLM doesn't care about plugin format distinctions; it just wants "put Diva on this track." The handler parses type and dispatches internally. `device/insertBitwigDevice` stays separate because it uses name resolution (not raw IDs). `device/listBitwigDevices` uses D-5.4's pre-scanned map, not runtime queries. Overridden by user — original: 3 separate plugin methods.
**Alternatives considered:** (a) Original D-5.3 with 3 separate plugin methods — surface area bloat for the same operation. (b) Collapsing everything into one `device/insert` — Bitwig devices use name resolution while plugins use IDs, so keeping them separate is clearer.
**Status:** ACTIVE
**ID:** D-5.3a

## 2026-02-28 — Device Insertion: How to handle the device path discovery?

**Decision:** On extension init, scan the Bitwig device library directory for `.bwdevice` files and build an in-memory name→path map. The directory path is derived from the Bitwig installation (macOS: `/Applications/Bitwig Studio.app/Contents/Resources/Library/devices/`). Device names are derived from filenames (strip `.bwdevice` extension). Name matching is case-insensitive. If a device name is not found, return a JSON-RPC error with the list of closest matches.
**Rationale:** Pre-scanning at init avoids filesystem access on every RPC call. Case-insensitive matching prevents frustration ("polymer" vs "Polymer"). Returning close matches on failure helps the LLM self-correct. The device library is stable — files don't change during a session.
**Alternatives considered:** (a) Lazy scanning on first call — adds latency to the first insert. (b) Hardcoded device list — breaks when Bitwig updates add new devices. (c) Require exact filenames — error-prone for LLM agents.
**Status:** ACTIVE
**ID:** D-5.4

## 2026-02-28 — Architecture: Where to put device insertion code?

**Decision:** Add device insertion methods to the existing `DeviceHandler` class. Pass the `CursorTrack` (for `endOfDeviceChainInsertionPoint()`) and `CursorDevice` (for before/after insertion points) — both are already constructor parameters. Add a new `DeviceLibrary` utility class that handles `.bwdevice` scanning and name resolution.
**Rationale:** `DeviceHandler` already owns device-related RPC methods (select, enable, parameters). Adding insertion here keeps all device operations in one handler. The `DeviceLibrary` is a pure utility (no Bitwig API dependency) that maps names to paths — it's testable in isolation.
**Alternatives considered:** (a) New `DeviceInsertionHandler` class — unnecessary separation when DeviceHandler already exists and has the right references. (b) Inline the scanning logic in DeviceHandler — mixes concerns; the name→path mapping is reusable.
**Status:** ACTIVE
**ID:** D-5.5

## 2026-02-28 — Device Insertion: How to handle device removal?

**Decision:** Add `device/remove` RPC method that calls `cursorDevice.deleteObject()` to remove the currently selected device. The LLM navigates to the target device first using existing `device/selectNext`/`device/selectPrevious`, then calls `device/remove`.
**Rationale:** Completing the CRUD cycle for devices. `CursorDevice` extends `DeleteableObject` (same pattern as `ClipLauncherSlot.deleteObject()` for clip/delete). Using cursor-based deletion is consistent with the existing navigation model — no need for device indexing.
**Alternatives considered:** (a) Index-based deletion — Bitwig API doesn't expose device-by-index removal; only cursor-based. (b) Skip removal — limits the LLM's ability to manage device chains.
**Status:** ACTIVE
**ID:** D-5.6
