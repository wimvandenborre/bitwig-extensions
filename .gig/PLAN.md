# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 42 — Interactive Documentation (v0.42.x)

> Generate an OpenAPI 3.1 spec from claude-tools.json and serve interactive API documentation via Scalar, both as a static HTML file and from the extension's HTTP server at `/docs`.

**Decisions:** D-1.1, D-1.2, D-1.3, D-1.4, D-1.5

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 42.1 | `0.42.1` | OpenAPI spec generator script | in-session | done |
| 42.2 | `0.42.2` | Scalar HTML page | in-session | done |
| 42.3 | `0.42.3` | Serve /docs from extension HTTP server | in-session | done |
| 42.4 | `0.42.4` | Build verification + docs update | in-session | done |

### Batch 42.1 — OpenAPI spec generator script

**Delegation:** in-session
**Decisions:** D-1.2, D-1.3, D-1.5
**Files:** `gig-maestro/scripts/generate-openapi.mjs` (create), `gig-maestro/docs/openapi.json` (generated output)
**Work:**
- Create a Node.js script that reads `tools/claude-tools.json` and generates `docs/openapi.json`
- Map each tool to a POST path: tool name `transport_play` → path `/transport/play`
- Wrap each method's input_schema as the JSON-RPC `params` field in the request body
- Include OpenAPI metadata: title, version, server URL, JSON-RPC protocol description
- Group methods by domain using OpenAPI tags (transport, track, device, clip, etc.)
- Include JSON-RPC request/response envelope schemas as reusable components
- Run the script and commit the generated openapi.json
**Test criteria:** Script runs without errors; generated openapi.json validates against OpenAPI 3.1 schema; all 306 methods present
**Acceptance:** Valid OpenAPI 3.1 spec with all RPC methods, ready for Scalar consumption

Depends on: nothing

### Batch 42.2 — Scalar HTML page

**Delegation:** in-session
**Decisions:** D-1.1, D-1.4
**Files:** `gig-maestro/docs/api.html` (create)
**Work:**
- Create a single HTML page that loads Scalar via CDN
- Reference the OpenAPI spec (relative path for static use, `/openapi.json` for server use)
- Configure Scalar theme, title, and server URL (localhost:8787)
- Test that the page renders correctly when opened as a local file
**Test criteria:** HTML file opens in browser and renders all 306 methods with interactive playground
**Acceptance:** Scalar page shows all API methods grouped by domain with working "Try It" feature

Depends on: Batch 42.1

### Batch 42.3 — Serve /docs from extension HTTP server

**Delegation:** in-session
**Decisions:** D-1.4
**Files:** `gig-maestro/src/main/java/dev/gregross/gig/rpc/HttpRpcServer.java` (modify)
**Work:**
- Add GET `/docs` route that serves the Scalar HTML page from classpath resources
- Add GET `/openapi.json` route that serves the OpenAPI spec from classpath resources
- Add CORS headers for local development
- Bundle `api.html` and `openapi.json` as resources in the shadowJar build
- Ensure existing POST `/rpc` route is unaffected
**Test criteria:** `curl http://localhost:8787/docs` returns HTML; `curl http://localhost:8787/openapi.json` returns valid JSON; POST /rpc still works
**Acceptance:** Opening `http://localhost:8787/docs` in a browser shows Scalar with live "Try It" against the running Bitwig instance

Depends on: Batch 42.2

### Batch 42.4 — Build verification + docs update

**Delegation:** in-session
**Decisions:** D-1.1, D-1.4
**Files:** `gig-maestro/README.md` (modify), `gig-maestro/docs/rpc-api-reference.md` (modify)
**Work:**
- Run `./gradlew :gig-maestro:test` — confirm no regressions
- Run `./gradlew :gig-maestro:shadowJar` — confirm extension builds with bundled resources
- Update gig-maestro README to mention interactive docs at `/docs`
- Update RPC API reference intro to mention interactive docs
- Add smoke test instructions for manual validation
**Test criteria:** Build succeeds, tests pass, docs reference the interactive API page
**Acceptance:** Full build green, documentation updated with interactive docs link

Depends on: Batch 42.3

**Phase Acceptance Criteria:**
- [ ] Node.js script generates valid OpenAPI 3.1 spec from claude-tools.json
- [ ] All 306 methods present in the generated spec
- [ ] Scalar HTML page renders all methods with interactive playground
- [ ] Extension serves /docs and /openapi.json at runtime
- [ ] POST /rpc endpoint unaffected
- [ ] Build and tests pass
- [ ] README and RPC reference updated with interactive docs link

**Completion triggers Phase 43 -> version `0.43.0`**
