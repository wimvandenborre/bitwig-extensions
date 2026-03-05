# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-05 — Scope: What does "Device Sound Design" mean for Phase 24?

**Decision:** Expose layer navigation (enter/exit layers by index or name), drum pad device chain navigation (enter by MIDI key), and parameter page filtering (jump to osc/filter/envelope pages by tag). These are the three missing CursorDevice capabilities that block efficient programmatic sound design. Exclude modulation routing (deprecated API), ChainSelector (no public accessor), and independent remote controls (not defined in API reference).

**Rationale:** Sound design requires navigating inside complex devices: Instrument Layer has sub-layers with their own device chains, Drum Machine has per-pad device chains accessible by MIDI key, and all synths have 10+ parameter pages that need tag-based jumping (e.g., "show me the filter page"). These are all non-deprecated, well-documented CursorDevice methods we simply haven't exposed yet. Modulation routing via `getModulationSource()` is explicitly deprecated in API v25 — the replacement is remote controls pages, which we already support.

**Alternatives considered:**
- Full modulation routing + layer nav + page filtering — modulation API is deprecated, can't rely on it
- Layer navigation only — misses parameter page filtering which is equally valuable for sound design
- Wait for SpecificDevice APIs — those are empty interfaces, no actionable capability yet

**Status:** ACTIVE
**ID:** D-24.1

---

## 2026-03-05 — Layer Navigation: Which methods to expose?

**Decision:** Expose 4 new CursorDevice methods for layer navigation: `selectFirstInLayer(int index)`, `selectFirstInLayer(String name)`, `selectFirstInKeyPad(int key)`, and `selectParent()` (already exposed as `exitToParent`). Combine into 2 RPC methods: `device/enterLayer` (accepts `index` OR `name`, mutually exclusive) and `device/enterKeyPad` (accepts `key` — MIDI note 0-127). Mirror both on masterDevice. Total: 4 new RPC methods.

**Rationale:** Layer navigation lets the LLM dive into Instrument Layer sub-chains (each layer has its own device chain with independent FX), and drum pad navigation lets it access per-pad device chains in Drum Machine (e.g., key 36 = kick). Using `selectFirst` (not `selectLast`) matches the `enterSlot` pattern — cursor moves to the first device in the chain. `exitToParent` already handles exiting back up. Two RPC methods (not four) because index/name are variants of the same operation.

**Alternatives considered:**
- Expose all 8 methods (selectFirst/LastInLayer × index/name + selectFirst/LastInKeyPad) — selectLast is rarely useful, index and name are variants
- Single `device/enter` mega-method with type parameter — too overloaded, enterSlot/enterLayer/enterKeyPad are distinct concepts
- Layer-only, no drum pad — drum pad navigation is equally important for Drum Machine sound design

**Status:** ACTIVE
**ID:** D-24.2

---

## 2026-03-05 — Parameter Page Filtering: How to expose tag-based page navigation?

**Decision:** Expose 2 new RPC methods: `device/selectPageByTag` and `masterDevice/selectPageByTag`. Each accepts `{tag, direction?, cycle?}` where tag is one of the known page tags, direction defaults to "next" (or "previous"), and cycle defaults to true. Maps to `CursorRemoteControlsPage.selectNextPageMatching(tag, cycle)` / `selectPreviousPageMatching(tag, cycle)`. Known tags: `env`, `eq`, `filter`, `fx`, `lfo`, `mixer`, `osc`, `perf`.

**Rationale:** Bitwig parameter pages are tagged by the device developer (e.g., Polymer's "Oscillator 1" page is tagged "osc", "Filter" is tagged "filter"). Tag-based navigation lets the LLM jump directly to relevant pages instead of cycling through all pages sequentially. This is critical for synths with 15+ pages. The 8 known tags cover all major sound design sections. Single method with direction parameter is cleaner than separate next/previous methods.

**Alternatives considered:**
- Separate `device/nextPageByTag` and `device/previousPageByTag` — unnecessary split, direction param is simpler
- No tag validation (pass through raw) — better to validate against known tags for clear errors
- Expose `createIndependentRemoteControls` instead — method definition missing from API reference, unreliable

**Status:** ACTIVE
**ID:** D-24.3

---

## 2026-03-05 — Snapshot: Should layer/keypad navigation state appear in snapshot?

**Decision:** No additional snapshot fields needed. The existing `isNested`, `hasLayers`, `hasDrumPads` fields already indicate whether layer/keypad navigation is possible. After entering a layer or keypad, the existing device snapshot updates automatically (cursor moves, so device name, parameters, nesting info all refresh). No new state to observe.

**Rationale:** Layer/keypad entry is a cursor navigation action (like `enterSlot`), not a new observable state. The cursor device's name, parameters, and nesting info already reflect wherever the cursor is pointing. Adding layer index/name to the snapshot would require new observers that don't add practical value — the LLM already knows which layer it entered because it just called the method.

**Alternatives considered:**
- Add `currentLayerIndex` / `currentLayerName` to device snapshot — redundant with device name after navigation
- Add layer count to snapshot — would require DeviceLayerBank creation with markInterested, overhead for minimal value

**Status:** ACTIVE
**ID:** D-24.4

---

## 2026-03-05 — API Design: How to structure the RPC methods?

**Decision:** 6 new RPC methods organized in 2 groups applied to both device and masterDevice:

**Layer/KeyPad Navigation (4 methods):**
1. `device/enterLayer` — `{index?: int, name?: string}` (one required, mutually exclusive)
2. `device/enterKeyPad` — `{key: int}` (MIDI note 0-127)
3. `masterDevice/enterLayer` — same params
4. `masterDevice/enterKeyPad` — same params

**Parameter Page Filtering (2 methods):**
5. `device/selectPageByTag` — `{tag: string, direction?: "next"|"previous", cycle?: boolean}`
6. `masterDevice/selectPageByTag` — same params

**Rationale:** Mirrors the existing pattern where device and masterDevice share the same method structure. Layer/keypad methods follow the `enterSlot`/`exitToParent` naming convention. Page tag method is a new concept but fits naturally alongside `selectPage`/`nextPage`/`previousPage`.

**Alternatives considered:**
- Combined device/masterDevice handler — breaks established handler separation
- More methods (separate next/previous for tags, separate index/name for layers) — unnecessary API surface

**Status:** ACTIVE
**ID:** D-24.5
