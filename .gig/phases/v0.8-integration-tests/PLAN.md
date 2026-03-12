# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 8 — gig-maestro: Integration Tests (v0.8.x)

> Add integration tests that verify the full handler registration wiring (all 16 handlers + 2 built-in methods via a shared dispatcher) and extension definition metadata. Closes the gap between isolated handler tests and the real extension init() assembly.

**Decisions:** D-8.1, D-8.2, D-8.3

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 8.1 | `0.8.1` | HandlerRegistrationIntegrationTest + GigMaestroDefinitionTest | in-session | pending |
| 8.2 | `0.8.2` | Verify full build | in-session | pending |

### Batch 8.1 — HandlerRegistrationIntegrationTest + GigMaestroDefinitionTest

**Delegation:** in-session
**Decisions:** D-8.1, D-8.2
**Files:**
- Create `gig-maestro/src/test/java/dev/gregross/gig/extension/HandlerRegistrationIntegrationTest.java`
- Create `gig-maestro/src/test/java/dev/gregross/gig/extension/GigMaestroDefinitionTest.java`

**Work:**
1. **HandlerRegistrationIntegrationTest** — ~10 tests:
   - Create 20+ `@Mock` fields for all Bitwig API types needed by handler constructors
   - Mirror `GigMaestroExtension.init()` registration sequence: register `session/snapshot`, `api/list`, then all 16 handlers
   - `allHandlersRegisterSuccessfully` — verify total method count matches expected
   - `apiListContainsAllMethods` — send `api/list` RPC call, verify response is array with correct count
   - Per-namespace verification tests (~8): verify each handler namespace has at least 1 method registered (app/, transport/, track/, clip/, device/, masterDevice/, macro/, session/)
   - `sampleRpcCallReturnsValidResponse` — send a real JSON-RPC call to a simple method (e.g., `api/list`) and verify valid JSON-RPC response structure

2. **GigMaestroDefinitionTest** — ~5 tests:
   - `name_returnsGigMaestro`
   - `author_returnsGregross`
   - `apiVersion_returns25`
   - `id_isNotNull`
   - `midiPorts_correctCounts` (1 in, 0 out)

**Test criteria:** `./gradlew :gig-maestro:test` passes; ~15 new tests added.
**Acceptance:** Integration wiring verified; all handler namespaces present; definition metadata stable.

### Batch 8.2 — Verify full build

**Delegation:** in-session (depends on 8.1)
**Decisions:** D-8.3
**Files:** None (verification only)

**Work:**
1. Run `./gradlew clean :gig-maestro:test` — full clean build.
2. Count total tests and confirm increase from 542.
3. Verify all test files pass.

**Test criteria:** Clean build succeeds; all tests pass; test count ≥ 557 (542 + 15).
**Acceptance:** Full build green with integration tests included.

**Phase Acceptance Criteria:**
- [ ] HandlerRegistrationIntegrationTest verifies all 16 handlers register
- [ ] Per-namespace checks confirm all handler namespaces present
- [ ] GigMaestroDefinitionTest validates extension metadata
- [ ] All existing 542 tests still pass
- [ ] `./gradlew clean :gig-maestro:test` succeeds
- [ ] Total test count ≥ 557

**Completion triggers Phase 9 → version `0.9.0`**

---

## Plan Amendments

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| — | — | — | — |
