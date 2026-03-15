# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-15 — UI Tool: Which interactive documentation UI?

**Decision:** Scalar
**Rationale:** Most modern open-source option — beautiful design, interactive "Try It" playground for live API calls, single-file CDN embed, adopted by .NET 9 as default. Outperforms Swagger UI (cluttered) and Redoc (read-only, no playground).
**Alternatives considered:** Redoc (no interactive playground), Swagger UI (dated UI, cluttered at 300+ endpoints), RapiDoc (less polished), OpenRPC Playground (correct for JSON-RPC but immature tooling)
**Status:** ACTIVE
**ID:** D-1.1

## 2026-03-15 — Spec Format: API specification format?

**Decision:** OpenAPI 3.1 — model each JSON-RPC method as a POST path
**Rationale:** Wider tool support than OpenRPC; Scalar/Redoc/Swagger all consume OpenAPI natively. Modeling RPC methods as individual POST paths (e.g., `/transport/play`) is a common pattern that unlocks the full ecosystem.
**Alternatives considered:** OpenRPC (semantically correct for JSON-RPC but limited UI tooling), custom format (no tool support)
**Status:** ACTIVE
**ID:** D-1.2

## 2026-03-15 — Generation: How to generate the spec?

**Decision:** Node.js script converting claude-tools.json → openapi.json
**Rationale:** claude-tools.json already contains all 306 methods with JSON Schema parameter definitions — it's the structured source of truth. A Node.js script is the most natural tool for JSON→JSON transformation. Output as JSON (not YAML) avoids extra dependencies.
**Alternatives considered:** Gradle task (awkward for JSON transformation), Java main class (verbose), shell script with jq (fragile for complex transforms)
**Status:** ACTIVE
**ID:** D-1.3

## 2026-03-15 — Hosting: Where to serve the interactive docs?

**Decision:** Static HTML file in repo + serve from extension HTTP server at GET /docs
**Rationale:** Static file works offline and can be opened locally. Serving from the extension enables live "Try It" calls against the running Bitwig instance — the key interactive feature.
**Alternatives considered:** Static-only (no live playground), external hosted (unnecessary complexity)
**Status:** ACTIVE
**ID:** D-1.4

## 2026-03-15 — Source of Truth: What drives the spec generation?

**Decision:** claude-tools.json remains the single source of truth
**Rationale:** No new metadata format needed. The spec is a generated build artifact. When new RPC methods are added, updating claude-tools.json automatically updates the interactive docs.
**Alternatives considered:** Separate API metadata file (duplication), Java annotations (requires annotation processor, heavy)
**Status:** ACTIVE
**ID:** D-1.5
