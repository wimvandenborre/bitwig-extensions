# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 41 — Documentation (v0.41.x)

> Create user-facing documentation for the extensions monorepo: root README, gig-maestro README with setup/architecture overview, CLI command reference, and comprehensive RPC API reference with WebSocket streaming docs.

**Decisions:** D-1.1, D-1.2, D-1.3, D-1.4, D-1.5, D-1.6, D-1.7

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 41.1 | `0.41.1` | Root README | team | done |
| 41.2 | `0.41.2` | gig-maestro README | team | done |
| 41.3 | `0.41.3` | CLI reference | team | done |
| 41.4 | `0.41.4` | RPC API reference | team | done |
| 41.5 | `0.41.5` | Cross-link review & build verification | in-session | done |

### Batch 41.1 — Root README

**Delegation:** team
**Decisions:** D-1.1, D-1.2
**Files:** `README.md` (create)
**Work:**
- Write root `README.md` for the extensions monorepo
- Include: project overview, module descriptions (gig-maestro + launchpad-mk2), build prerequisites (Java 21, Gradle), quick-start build commands, links to module READMEs
- Keep concise — this is a signpost, not a deep-dive
**Test criteria:** File exists, markdown renders correctly, all links point to real files
**Acceptance:** Root README provides clear overview and navigation to both modules

### Batch 41.2 — gig-maestro README

**Delegation:** team
**Decisions:** D-1.1, D-1.2
**Files:** `gig-maestro/README.md` (create)
**Work:**
- Write gig-maestro README covering: what it is (RPC-based Bitwig controller extension + CLI), architecture overview (extension ↔ HTTP/WebSocket ↔ clients), prerequisites, build & install instructions, quick-start usage (start Bitwig, curl example, CLI example), links to detailed docs (CLI reference, RPC API reference)
- Include the extension's key concepts: JSON-RPC 2.0 protocol, HTTP endpoint, WebSocket streaming, state snapshot model
**Test criteria:** File exists, build commands are accurate, example curl command is correct
**Acceptance:** New user can build, install, and make their first RPC call by following the README

### Batch 41.3 — CLI Reference

**Delegation:** team
**Decisions:** D-1.1, D-1.2, D-1.4
**Files:** `gig-maestro/docs/cli-reference.md` (create)
**Work:**
- Document all 12 CLI command groups with their subcommands
- For each command: synopsis, description, options table, usage examples
- Cover: global options (--host, --port, --pretty), config file (~/.gig-maestro/config.json), output format
- Include the `watch` command's WebSocket streaming behavior and topic filtering
- Source of truth: CLI source files in `src/cli/java/dev/gregross/gig/cli/`
**Test criteria:** Every CLI command and subcommand is documented; example commands are syntactically correct
**Acceptance:** User can find usage info for any CLI command without reading source code

### Batch 41.4 — RPC API Reference

**Delegation:** team
**Decisions:** D-1.1, D-1.2, D-1.3, D-1.5
**Files:** `gig-maestro/docs/rpc-api-reference.md` (create)
**Work:**
- Document all 200+ RPC methods grouped by domain (transport, track, device, clip, note, browser, arranger, mixer, project, macro, state, etc.)
- For each method: name, description, parameters table (name, type, required, description), example request/response
- Include introductory section: JSON-RPC 2.0 protocol, HTTP endpoint (`POST /rpc`), request/response format, error codes
- Include WebSocket streaming section: endpoint (`ws://host:port+1/`), subscription model, topic filtering, delta notifications
- Include session/transaction section: snapshot format, batch operations
- Source of truth: `tools/claude-tools.json` for schemas, handler source files for behavior
**Test criteria:** Every method from `api/list` is documented; JSON examples are valid; domain groupings match handler organization
**Acceptance:** Developer can integrate with any RPC method using only this reference

### Batch 41.5 — Cross-link Review & Build Verification

**Delegation:** in-session
**Decisions:** D-1.1, D-1.2
**Files:** All 4 doc files (review)
**Work:**
- Verify all cross-references between docs resolve correctly
- Verify build commands in READMEs work (`./gradlew :gig-maestro:shadowJar`, `./gradlew :gig-maestro:cliShadowJar`)
- Run `./gradlew :gig-maestro:test` to confirm no regressions
- Spot-check RPC method count against `tools/claude-tools.json`
**Test criteria:** All links valid, build succeeds, tests pass
**Acceptance:** Documentation is internally consistent and build-verified

Depends on Batch 41.1, 41.2, 41.3, 41.4

**Phase Acceptance Criteria:**
- [ ] Root README exists with module overview and build instructions
- [ ] gig-maestro README exists with setup, architecture, and quick-start
- [ ] CLI reference covers all 12 command groups with examples
- [ ] RPC API reference covers all 200+ methods grouped by domain
- [ ] WebSocket streaming protocol is documented
- [ ] All cross-links between docs resolve correctly
- [ ] Build and tests pass

**Completion triggers Phase 42 -> version `0.42.0`**
