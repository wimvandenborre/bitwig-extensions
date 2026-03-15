# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-15 — Scope: What documentation to create?

**Decision:** Create four docs: root README, gig-maestro README, CLI reference, RPC API reference
**Rationale:** Covers the full user surface — overview, extension setup, CLI usage, and API integration. Currently zero user-facing documentation exists.
**Alternatives considered:** Single monolithic README (rejected: too long for 200+ RPC methods + 80+ CLI subcommands); wiki (rejected: adds tooling complexity for a small project)
**Status:** ACTIVE
**ID:** D-1.1

## 2026-03-15 — Location: Where to put the docs?

**Decision:** `README.md` (root), `gig-maestro/README.md`, `gig-maestro/docs/cli-reference.md`, `gig-maestro/docs/rpc-api-reference.md`
**Rationale:** Standard locations — READMEs at module roots for GitHub visibility, detailed references in the existing docs/ directory alongside the Bitwig API reference.
**Alternatives considered:** All in root (rejected: clutters root with module-specific content); GitHub wiki (rejected: not versioned with code)
**Status:** ACTIVE
**ID:** D-1.2

## 2026-03-15 — Format: RPC API reference structure?

**Decision:** Grouped by domain (transport, track, device, etc.), each method with signature, params table, and example response
**Rationale:** Mirrors the handler organization in code; domain grouping is more useful than alphabetical for a music production API where users think in terms of workflow domains.
**Alternatives considered:** Alphabetical (rejected: loses contextual grouping); auto-generated from JSON (rejected: loses narrative context and usage notes)
**Status:** ACTIVE
**ID:** D-1.3

## 2026-03-15 — Format: CLI reference structure?

**Decision:** Command tree with synopsis, options table, and usage examples per command group
**Rationale:** Matches picocli's natural command hierarchy; examples make commands immediately usable without reading the full RPC docs.
**Alternatives considered:** Man page format (rejected: markdown more accessible); auto-generated picocli docs (rejected: lacks curated examples)
**Status:** ACTIVE
**ID:** D-1.4

## 2026-03-15 — WebSocket: Document streaming protocol?

**Decision:** Yes, as a dedicated section in the RPC API reference
**Rationale:** WebSocket streaming is a key feature for real-time state updates with non-obvious details (topic filtering, per-client subscriptions, delta broadcast model).
**Alternatives considered:** Separate doc (rejected: too small for its own file); omit (rejected: it's a primary integration path)
**Status:** ACTIVE
**ID:** D-1.5

## 2026-03-15 — Sound design: Document workflows?

**Decision:** No — keep sound design recipes in system-prompt.md only
**Rationale:** Sound design recipes (Polymer presets, synthesis principles, from-scratch workflows) are Claude-facing operational knowledge, not user-facing API docs. Users interact with these through Claude, not directly.
**Alternatives considered:** Include as appendix (rejected: couples user docs to AI-specific workflows)
**Status:** ACTIVE
**ID:** D-1.6

## 2026-03-15 — Source code: Add Javadoc?

**Decision:** No — out of scope for this phase
**Rationale:** Source has minimal comments by design; external docs serve users better than inline Javadoc for an extension whose primary interface is RPC, not a Java API.
**Alternatives considered:** Full Javadoc pass (rejected: large effort, low user value since nobody imports this as a library)
**Status:** ACTIVE
**ID:** D-1.7
