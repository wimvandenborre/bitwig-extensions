# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 28 — Macro Automation Curves (v0.28.x)

> Add `macro/writeAutomation` — a batch macro that writes arranger automation envelopes for multiple device parameters in one call. Handles page switching, auto-enables automation write, and chains envelope writes sequentially with proper delays. Delegates to `device/writeEnvelope` internally.

**Decisions:** D-28.1, D-28.2, D-28.3, D-28.4, D-28.5

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 28.1 | `0.28.1` | macro/writeAutomation implementation | in-session | pending |
| 28.2 | `0.28.2` | Unit tests | in-session | pending |
| 28.3 | `0.28.3` | Tool definition + system prompt + smoke tests | in-session | pending |
| 28.4 | `0.28.4` | Build verification | in-session | pending |

---

### Batch 28.1 — macro/writeAutomation implementation

**Decisions:** D-28.1, D-28.2, D-28.3, D-28.4, D-28.5
**Files:** `MacroHandler.java`
**Work:**
- Add `handleWriteAutomation` method registered as `macro/writeAutomation`
- Accept params: `{envelopes: [{paramIndex, pageIndex?, points: [{position, value}]}]}`
- Validate all envelopes up front (paramIndex 0-7, points non-empty, positions >= 0, values 0-1)
- Group envelopes by pageIndex (null/absent = current page, grouped last)
- Save automation write state; enable if not already enabled
- For each page group:
  - If pageIndex specified, dispatch `device/selectParameterPage` to switch
  - For each envelope in the group, schedule `device/writeEnvelope` with cumulative delay
  - Delay per envelope: `100 * (pointCount + 2)` ms
  - Add `FLUSH_DELAY_MS` between page groups
- After last envelope, schedule restore of automation write state
- Return `{ok: true, envelopeCount, totalPoints}`
- Update `registersSevenMacroMethods` → `registersEightMacroMethods` in test
**Test criteria:** Covered in batch 28.2

---

### Batch 28.2 — Unit tests

**Decisions:** All
**Files:** `MacroHandlerTest.java`
**Work:**
- Add stubs for `device/writeEnvelope`, `device/selectParameterPage`, `transport/setAutomationWrite`
- Test: basic envelope write (single param, no page switch) → calls `device/writeEnvelope`
- Test: multi-envelope write → calls `device/writeEnvelope` for each
- Test: envelopes with pageIndex → calls `device/selectParameterPage` before group
- Test: envelopes without pageIndex → no page switch call
- Test: mixed page groups → page switches between groups, no switch for null-page group
- Test: return shape has envelopeCount and totalPoints
- Test: empty envelopes array → error
- Test: paramIndex out of range → error
- Test: empty points array → error
**Test criteria:** `./gradlew :gig-maestro:test` passes

---

### Batch 28.3 — Tool definition + system prompt + smoke tests

**Files:**
- `claude-tools.json` — add `macro_writeAutomation` tool definition
- `system-prompt.md` — add automation macro docs with example
- `offline-schemas.sh` — assertions for new schema fields and prompt content
**Test criteria:** `smoke-test.sh --offline` passes

---

### Batch 28.4 — Build verification

**Steps:**
- `./gradlew :gig-maestro:test` — all unit tests pass
- `./gradlew :gig-maestro:shadowJar` — extension builds
- `gig-maestro/scripts/smoke-test.sh --offline` — all offline smoke tests pass

---

**Phase Acceptance Criteria:**
- [ ] `macro/writeAutomation` batches multiple parameter envelopes into one call
- [ ] Page switching handled automatically for multi-page envelopes
- [ ] Automation write state auto-enabled and restored
- [ ] Sequential chaining with calculated delays
- [ ] Validation: paramIndex range, non-empty points, position/value ranges
- [ ] Tool definition and system prompt updated
- [ ] All tests pass

**Completion triggers Phase 29 → version `0.29.0`**
