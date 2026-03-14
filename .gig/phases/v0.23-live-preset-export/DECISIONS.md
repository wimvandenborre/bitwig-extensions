# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-13 — RPC: Extend getDiscoveryResult with preset format option

**Decision:** Add an optional `format` parameter to `device/getDiscoveryResult`. When `format: "preset"`, return the discovery data restructured as preset-compatible JSON (`{ deviceName, pageCount, pages: [{ pageIndex, params: [{ index, value }] }] }`) — directly passable to `macro/createSound`. Default format remains unchanged for backward compatibility. This reuses the existing `discoverAll` page-cycling infrastructure.
**Rationale:** Avoids duplicating page-cycling code in a separate `exportPreset` method. The discovery infrastructure already handles async page scanning, page restoration, and thread safety. The "preset" format strips names/displayedValues and restructures to match the `macro/createSound` input format.
**Alternatives considered:** Separate `device/exportPreset` method (duplicates page-cycling logic); client-side JSON transformation (error-prone, puts burden on AI).
**Status:** ACTIVE
**ID:** D-23.1

## 2026-03-13 — Filtering: Client-side default filtering using reference files

**Decision:** The preset format from `getDiscoveryResult` includes ALL parameter values (no filtering). The AI then compares against the device's reference file (`data/devices/{name}.json`) to strip parameters that match their defaults, producing a minimal preset. If no reference file exists, the AI keeps all values.
**Rationale:** The Java extension can't access repo data files at runtime (they're not bundled in the JAR). The AI already has access to reference files and can trivially diff two JSON structures. This keeps the RPC method simple and pure — it returns current state, the client decides what to keep.
**Alternatives considered:** Server-side filtering (can't access data files from JAR); bundle reference files in JAR (couples build to data, bloats JAR); always include all values (presets become verbose and hard to read).
**Status:** ACTIVE
**ID:** D-23.2

## 2026-03-13 — System prompt: Document capture-and-save preset workflow

**Decision:** Update the system prompt to document the capture workflow: (1) tweak device via UI or RPC, (2) run `discoverAll` + `getDiscoveryResult?format=preset` to capture state, (3) diff against reference file to filter defaults, (4) save as preset JSON. Add to the "Creating Sounds From Scratch" section.
**Rationale:** Without documentation, the AI won't discover the capture workflow.
**Alternatives considered:** No prompt changes (AI won't use it).
**Status:** ACTIVE
**ID:** D-23.3
