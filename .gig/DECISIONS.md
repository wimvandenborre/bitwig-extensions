# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-12 — Notification content: Include state data in WebSocket notifications

**Decision:** Enhance the existing `state/changed` WebSocket notification to include the actual state data for each changed section. New format: `{"jsonrpc":"2.0","method":"state/changed","params":{"changed":["device","tracks"],"data":{"device":{...},"tracks":{...}}}}`. The `data` object contains only the sections that changed, using the same JSON structure as `session/snapshot`. This eliminates the round-trip where clients receive a change notification and must make a separate `session/snapshot` call to get the new values.
**Rationale:** The current notification tells clients WHAT changed but not HOW. After calling `device/setParameters`, the LLM has no way to verify parameter values took effect without polling `session/snapshot`. Including the delta data in the notification gives immediate confirmation. The data is already being computed — `getChangedSections()` hashes the JSON output, so we're generating the JSON and throwing it away. Reusing it is essentially free.
**Alternatives considered:** (1) Keep section-name-only notifications and require explicit snapshot — adds latency, wastes a round-trip. (2) Send full snapshot on every change — too large (full state is 13 sections including 64 tracks). (3) Create a separate `state/delta` notification — unnecessary duplication, better to enhance the existing one.
**Status:** ACTIVE
**ID:** D-17.1

## 2026-03-12 — StateCache: getDelta() method returning changed sections with data

**Decision:** Add a `getDelta()` method to StateCache that returns a `JsonObject` with two keys: `changed` (JsonArray of section names) and `data` (JsonObject mapping section name → section state). Internally, refactor `getChangedSections()` to compute the JSON for each section, compare hashes, and if changed, include both the name and the data. This avoids computing section JSON twice (once for hash comparison, once for data inclusion).
**Rationale:** The current `getChangedSections()` already generates section JSON strings to hash them — it just discards the actual objects. `getDelta()` captures what's already being computed. The flush loop can call `getDelta()` once and get everything needed for the notification, replacing the current `getChangedSections()` call.
**Alternatives considered:** (1) Keep `getChangedSections()` and add a separate `getDataForSections(List<String>)` — recomputes JSON twice. (2) Cache section JSON on every observer update — adds memory pressure and complexity for marginal benefit.
**Status:** ACTIVE
**ID:** D-17.2

## 2026-03-12 — Granularity: Section-level change detection is sufficient

**Decision:** Keep section-level change detection (e.g., "device" section changed). Do NOT add parameter-level tracking (e.g., "parameter 3 on page 2 changed"). The "device" section already includes the current page's 8 parameters — the LLM can diff the values itself. Section-level is the right granularity because: (a) Bitwig's observers fire per-field, but most use cases care about the whole section, (b) parameter-level tracking would require per-field dirty flags across 179+ fields, adding significant complexity.
**Rationale:** The LLM's workflow after `device/setParameters` is: set values → wait for notification → compare expected vs actual. Section-level delta gives it the full device state including all parameter values. It can verify by comparing against what it sent. Finer granularity adds complexity without meaningful benefit for this use case.
**Alternatives considered:** (1) Per-parameter dirty flags — 179+ volatile booleans, complex reset logic, marginal benefit. (2) Ring buffer of changes — over-engineered for the current need.
**Status:** ACTIVE
**ID:** D-17.3

## 2026-03-12 — Testing: StateCacheDelta test + WebSocket notification test

**Decision:** Add tests for the new `getDelta()` method in a new `StateCacheDeltaNotificationTest` (or extend existing `StateCacheDeltaTest`). Test: (1) getDelta returns empty when nothing changed, (2) getDelta returns section name + data when a field changes, (3) getDelta clears changed state after read (so next call returns empty). For the flush-loop integration, add a test in `GigMaestroExtension` test or verify manually — the flush loop change is minimal (swap `getChangedSections()` for `getDelta()` and include data in broadcast).
**Rationale:** The StateCache delta logic is the critical piece. The flush loop change is a 5-line modification to an existing working broadcast. Unit testing the delta method gives confidence; integration testing the broadcast is lower priority since the broadcast mechanism is already proven.
**Status:** ACTIVE
**ID:** D-17.4
