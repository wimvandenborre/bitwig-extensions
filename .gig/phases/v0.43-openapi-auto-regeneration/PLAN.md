# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 43 — OpenAPI Auto-Regeneration (v0.43.x)

> Wire the OpenAPI spec generator into the Gradle build so the spec is automatically regenerated from claude-tools.json with up-to-date checking, and add an offline smoke test for validation.

**Decisions:** D-1.1, D-1.2, D-1.3

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 43.1 | `0.43.1` | Gradle task + up-to-date wiring | in-session | done |
| 43.2 | `0.43.2` | Offline smoke test + build verification | in-session | done |

### Batch 43.1 — Gradle task + up-to-date wiring

**Delegation:** in-session
**Decisions:** D-1.1, D-1.2
**Files:** `gig-maestro/build.gradle.kts` (modify)
**Work:**
- Add a `generateOpenApi` Exec task that runs `node scripts/generate-openapi.mjs`
- Declare inputs: `tools/claude-tools.json`, `scripts/generate-openapi.mjs`
- Declare outputs: `docs/openapi.json`
- Wire `copyDocsToResources` to depend on `generateOpenApi`
- Remove redundant `docs/openapi.json` from git tracking (it's now a build artifact)
- Verify `./gradlew :gig-maestro:shadowJar` regenerates the spec and bundles it
- Verify up-to-date checking works (second build skips generation)
**Test criteria:** `./gradlew shadowJar` produces JAR with current openapi.json; second run shows UP-TO-DATE; modifying claude-tools.json triggers regeneration
**Acceptance:** Zero manual steps between editing claude-tools.json and having updated spec in the extension JAR

Depends on: nothing

### Batch 43.2 — Offline smoke test + build verification

**Delegation:** in-session
**Decisions:** D-1.3
**Files:** `gig-maestro/scripts/tests/offline-openapi.sh` (create)
**Work:**
- Create smoke test script that validates `docs/openapi.json`:
  - File exists
  - Valid JSON
  - Has `openapi` field with value `3.1.0`
  - Method count matches `tools/claude-tools.json` tool count
- Register in smoke-test runner for `--offline` mode
- Run `./gradlew :gig-maestro:test` to confirm no regressions
- Run `smoke-test.sh --offline` to confirm new test passes
**Test criteria:** `smoke-test.sh --offline` includes and passes the OpenAPI validation test
**Acceptance:** Offline smoke tests catch spec/tool count mismatch

Depends on: Batch 43.1

**Phase Acceptance Criteria:**
- [ ] `./gradlew :gig-maestro:shadowJar` auto-regenerates openapi.json from claude-tools.json
- [ ] Up-to-date checking skips regeneration when inputs unchanged
- [ ] Offline smoke test validates spec structure and method count
- [ ] All existing tests and smoke tests still pass

**Completion triggers Phase 44 -> version `0.44.0`**
