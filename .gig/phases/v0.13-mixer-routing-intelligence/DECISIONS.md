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

## 2026-03-01 — Scope: What does Phase 13 cover?

**Decision:** Phase 13 adds **send/return routing**, **track color control**, **crossfade mode**, **input monitoring**, and **expanded master track controls**. This brings the mixer from basic level/pan/mute/solo to a full mixing surface: sends to effect tracks, per-send level/mode/enable, color-coded tracks, crossfade assignments, and monitor mode. The agent gains the ability to set up a complete mix bus architecture (instrument → send → effect return) and visually organize tracks by color.
**Rationale:** The current mixer implementation has 7 RPC methods covering basic controls (volume, pan, mute, solo, arm + master volume/pan). The Bitwig API exposes substantially more: `SendBank` with per-send level/mode/enable, `SettableColorValue` for track colors, `crossFadeMode()` for A/B crossfading, and `monitorMode()` for input monitoring. These are all settable via the API and require no workarounds. Send routing is the highest-impact gap — without it, effect tracks are unreachable from instrument tracks.
**Alternatives considered:** Including track grouping (API can read groups but can't create them — limited value). Including audio I/O routing (API only exposes read-only `sourceSelector()` — can't set inputs/outputs). Both excluded as low-impact given API limitations.
**Status:** ACTIVE
**ID:** D-13.1

## 2026-03-01 — API: Send observation and control design

**Decision:** Create sends infrastructure: (1) Observe sends via `trackBank.getItemAt(i).sendBank()` with a bank size of 4 sends per track. Cache send name, volume, and pan per send per track in `StateCache`. (2) Add 4 RPC methods: `send/setLevel` (params: `trackIndex`, `sendIndex`, `value`), `send/setPan` (params: `trackIndex`, `sendIndex`, `value`), `send/setMode` (params: `trackIndex`, `sendIndex`, `mode` — enum `"AUTO"`, `"PRE"`, `"POST"`), `send/setEnabled` (params: `trackIndex`, `sendIndex`, `enabled`). (3) Include sends in snapshot under each track's entry as `sends: [{ name, volume, pan }]`.
**Rationale:** 4 sends per track matches Bitwig's default visible send count and keeps bank size manageable. The `Send` interface extends `Parameter` so volume/pan work identically to track volume/pan (`value().set()`). Send mode uses `SettableEnumValue` with the values `AUTO`, `PRE`, `POST`. Send enable uses `SettableBooleanValue`. All 4 operations are atomic single-flush calls — no scheduling needed.
**Alternatives considered:** 8 sends per track — excessive, most projects use 2-4 sends. Only level control (no mode/enable) — misses common mixing workflow of switching pre/post fader sends for headphone mixes. Exposing send bank scrolling — over-engineered for 4 sends.
**Status:** AMENDED — User corrected: `Send` has no pan. Replace send/setPan with snapshot-only readouts for `isPreFader` and `sendChannelColor`. See D-13.2a.
**ID:** D-13.2

## 2026-03-01 — API (amended): Send observation and control design — no send pan

**Decision:** Create sends infrastructure: (1) Observe sends via `trackBank.getItemAt(i).sendBank()` with a configurable bank size (default 4 sends per track, passed to `createMainTrackBank`). Cache per send per track: name, level (value), isPreFader (read-only boolean), sendChannelColor (read-only RGB), and enabled state. (2) Add 3 RPC methods: `send/setLevel` (params: `trackIndex`, `sendIndex`, `value` — uses `send.value().set()`), `send/setMode` (params: `trackIndex`, `sendIndex`, `mode` — enum `"AUTO"`, `"PRE"`, `"POST"` — uses `send.sendMode().set()`), `send/setEnabled` (params: `trackIndex`, `sendIndex`, `enabled` — uses `send.isEnabled().set()`). (3) Include sends in snapshot under each track's entry as `sends: [{ name, level, isPreFader, color: {r,g,b}, enabled }]`. Send count is configurable but defaults to 4.
**Rationale:** User corrected: `Send extends Parameter` exposes only a level (amount), not pan — there is no send pan in Bitwig's API. `isPreFader()` is a read-only `BooleanValue` reflecting the current pre/post state (derived from `sendMode`). `sendChannelColor()` returns the color of the destination effect track — useful for UI correlation. Configurable send count (via `createMainTrackBank(tracks, sends, scenes)`) future-proofs without over-allocating. Overridden from original: removed `send/setPan`, added `isPreFader` + `sendChannelColor` to snapshot.
**Alternatives considered:** Original D-13.2 with send pan — incorrect, `Send` has no pan method. Hard-coded 4 sends — user requested configurable count.
**Status:** ACTIVE
**ID:** D-13.2a

## 2026-03-01 — API: Track color setter

**Decision:** Add `track/setColor` RPC method. Params: `{ index: int, r: number, g: number, b: number }` — RGB floats 0.0–1.0. Uses `trackBank.getItemAt(index).color().set(r, g, b)`. Also add `master/setColor` with same RGB params using `masterTrack.color().set(r, g, b)`. Color is already observed in `StateCache` — no new observers needed.
**Rationale:** Track color is already cached and in the snapshot (Phase 1 wired observers). The only missing piece is the setter. `SettableColorValue.set(float, float, float)` is a simple synchronous call. Adding both track and master color setters lets the agent color-code the entire project for visual organization.
**Alternatives considered:** Named color presets (e.g., "red", "blue") — adds a mapping layer with no real benefit since the agent can use RGB directly. HSL instead of RGB — Bitwig API uses RGB natively, HSL would require conversion.
**Status:** ACTIVE
**ID:** D-13.3

## 2026-03-01 — API: Master track expanded controls

**Decision:** Add 3 RPC methods for master track: `master/setMute` (params: `{ value: boolean }`), `master/setSolo` (params: `{ value: boolean }`), `master/setColor` (covered in D-13.3). Master mute and solo are already cached in `StateCache` as `masterMute` and `masterSolo` — just need the setter methods. No master arm (MasterTrack has no `arm()` method in API).
**Rationale:** Master mute/solo state is observed and reported in snapshots but cannot be controlled. This is a gap — the agent can see master is muted but can't unmute it. Simple `SettableBooleanValue.set()` calls.
**Alternatives considered:** Adding master send control — possible but unusual workflow (master rarely sends to effect tracks). Deferred to future if needed.
**Status:** ACTIVE
**ID:** D-13.4

## 2026-03-01 — API: Crossfade and monitor mode

**Decision:** Add 2 RPC methods: (1) `track/setCrossfade` — params: `{ index: int, mode: "A" | "B" | "AB" }`. Uses `track.crossFadeMode().set(mode)`. (2) `track/setMonitor` — params: `{ index: int, mode: "ON" | "OFF" | "AUTO" }`. Uses `track.monitorMode().set(mode)`. Add observers for both in `StateCache`: `trackCrossfadeModes` (String array) and `trackMonitorModes` (String array), included in snapshot per track.
**Rationale:** Crossfade mode is essential for live performance workflows (A/B deck transitions). Monitor mode is essential for recording workflows (ON = always monitor input, OFF = never, AUTO = monitor when armed). Both are `SettableEnumValue` — same pattern as send mode. Adding observers ensures the snapshot reflects current state.
**Alternatives considered:** Omitting crossfade (niche use case) — included because it's trivial to implement and enables DJ-style workflows. Omitting monitor (recording-focused) — included because it's a standard mixer feature the agent should control.
**Status:** ACTIVE
**ID:** D-13.5

## 2026-03-01 — Architecture: Code organization

**Decision:** All new methods go in existing handlers — no new handler classes. Send methods go in a new `SendHandler` class (takes `TrackBank` reference for `sendBank()` access). Track color/crossfade/monitor go in `TrackHandler`. Master color/mute/solo go in `MasterHandler`. `StateCache` gets new observer blocks for sends, crossfade modes, and monitor modes. Snapshot builder adds sends array per track and crossfade/monitor fields.
**Rationale:** Sends are a distinct concern (own bank, own parameters) warranting their own handler. Track-level features (color, crossfade, monitor) belong in `TrackHandler` since they operate on the same `trackBank` items. Master features belong in `MasterHandler`. This follows the existing pattern of one handler per API domain.
**Alternatives considered:** Putting sends in TrackHandler — muddies the handler with two bank types (track bank + send bank per track). A single MixerHandler for everything — violates the existing pattern of domain-specific handlers.
**Status:** ACTIVE
**ID:** D-13.6

## 2026-03-01 — Tools: New tool schemas and system prompt

**Decision:** 9 new RPC methods → 9 new tool schemas: `send_setLevel`, `send_setMode`, `send_setEnabled`, `track_setColor`, `track_setCrossfade`, `track_setMonitor`, `master_setMute`, `master_setSolo`, `master_setColor`. Total RPC methods: 114 (105 + 9). Update system prompt with a "Mixer & Routing" section documenting send routing workflow, color coding, crossfade modes, and monitor modes.
**Rationale:** 1:1 method-to-tool-schema pattern continues. System prompt needs a mixing workflow section so the agent knows to use sends for routing to effect tracks rather than trying to use direct output routing (which the API doesn't support). Count adjusted from original 10 to 9 after D-13.2a removed `send/setPan`.
**Alternatives considered:** Bundling send operations into a single `send/configure` method — breaks the atomic-operation pattern and makes tool schemas harder to understand.
**Status:** ACTIVE
**ID:** D-13.7
