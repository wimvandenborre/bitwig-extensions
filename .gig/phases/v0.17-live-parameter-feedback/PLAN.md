# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 17 — Live Parameter Feedback (v0.17.x)

> Enhance WebSocket notifications to include actual state data for changed sections, so the LLM can verify parameter changes took effect without a separate snapshot call.

**Decisions:** D-17.1, D-17.2, D-17.3, D-17.4

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 17.1 | `0.17.1` | StateCache getDelta() method | in-session | done |
| 17.2 | `0.17.2` | Flush loop: broadcast delta data | in-session | done |
| 17.3 | `0.17.3` | Delta notification tests | in-session | done |
| 17.4 | `0.17.4` | Build verification | in-session | done |

### Batch 17.1 — StateCache getDelta() method

**Delegation:** in-session
**Decisions:** D-17.2, D-17.3
**Files:**
- `gig-maestro/src/main/java/dev/gregross/gig/extension/StateCache.java`

**Work:**
1. Add `getDelta()` method that returns `JsonObject` with `changed` (JsonArray) and `data` (JsonObject)
2. Refactor internal logic: compute section JSON, hash it, compare to previous hash, if changed include section name + data
3. Keep `getChangedSections()` as a thin wrapper or deprecate if no longer needed
4. Section-level granularity only (D-17.3)

**Test criteria:** Compiles, existing tests pass

---

### Batch 17.2 — Flush loop: broadcast delta data

**Delegation:** in-session
**Decisions:** D-17.1
**Files:**
- `gig-maestro/src/main/java/dev/gregross/gig/extension/GigMaestroExtension.java`

**Work:**
1. Replace `getChangedSections()` call in `flush()` with `getDelta()`
2. Include delta data in the `state/changed` notification
3. New format: `{"jsonrpc":"2.0","method":"state/changed","params":{"changed":[...],"data":{...}}}`

**Depends on:** Batch 17.1
**Test criteria:** Compiles, existing tests pass

---

### Batch 17.3 — Delta notification tests

**Delegation:** in-session
**Decisions:** D-17.4
**Files:**
- `gig-maestro/src/test/java/dev/gregross/gig/extension/StateCacheDeltaTest.java` (existing file, extend)

**Work:**
1. Test: `getDelta()` returns null/empty when nothing changed
2. Test: `getDelta()` returns section name + data after a field update
3. Test: `getDelta()` clears changed state (second call returns empty)
4. Test: multiple sections changed simultaneously

**Depends on:** Batch 17.1
**Test criteria:** All new + existing tests pass

---

### Batch 17.4 — Build verification

**Work:**
1. `./gradlew :gig-maestro:test` — all tests pass
2. `./gradlew :gig-maestro:shadowJar` — builds cleanly

**Depends on:** All prior batches
**Test criteria:** Clean build, all tests green

---

**Phase Acceptance Criteria:**
- [ ] `getDelta()` returns changed section names + their state data
- [ ] `state/changed` WebSocket notifications include `data` field
- [ ] Existing tests unaffected
- [ ] New delta tests cover empty/single/multi-section scenarios

**Completion triggers Phase 18 → version `0.18.0`**
