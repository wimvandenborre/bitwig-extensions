# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 20 — Device Cursor Fix (v0.20.x)

> Fix ISS-3: after `device/remove` deletes the cursor device, the CursorDevice drops to position -1 and becomes unresponsive. Add `selectFirst()` after `deleteObject()` for both track and master device cursors. Add unit tests and update manual verification.

**Decisions:** D-20.1, D-20.2, D-20.3

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 20.1 | `0.20.1` | Fix device/remove + masterDevice/remove cursor recovery | in-session | done |
| 20.2 | `0.20.2` | Unit tests for cursor recovery | in-session | done |
| 20.3 | `0.20.3` | Update manual + smoke tests, resolve ISS-3 | in-session | done |

### Batch 20.1 — Fix device/remove + masterDevice/remove cursor recovery

**Delegation:** in-session
**Decisions:** D-20.1, D-20.2
**Files:**
- `gig-maestro/src/main/java/dev/gregross/gig/handlers/DeviceHandler.java`

**Work:**
1. In `device/remove` handler: add `cursorDevice.selectFirst()` after `cursorDevice.deleteObject()`
2. In `masterDevice/remove` handler: add `masterCursorDevice.selectFirst()` after `masterCursorDevice.deleteObject()`

**Test criteria:** `./gradlew :gig-maestro:test` passes. Code compiles.

---

### Batch 20.2 — Unit tests for cursor recovery

**Delegation:** in-session
**Decisions:** D-20.3
**Depends on:** Batch 20.1
**Files:**
- `gig-maestro/src/test/java/dev/gregross/gig/handlers/DeviceHandlerTest.java`

**Work:**
1. Add test: `device_remove_callsSelectFirstAfterDelete` — verify `deleteObject()` then `selectFirst()` called in order
2. Add test: `masterDevice_remove_callsSelectFirstAfterDelete` — same for master cursor

**Test criteria:** `./gradlew :gig-maestro:test` passes with new tests.

---

### Batch 20.3 — Update manual + smoke tests, resolve ISS-3

**Delegation:** in-session
**Decisions:** D-20.3
**Depends on:** Batch 20.1
**Files:**
- `gig-maestro/scripts/manual/devices.sh` (update double-remove test)
- `.gig/ISSUES.md` (mark ISS-3 RESOLVED)

**Work:**
1. Update manual `devices.sh`: re-add or adjust the double-remove scenario to verify cursor recovery works
2. Mark ISS-3 as RESOLVED in ISSUES.md with evidence

**Test criteria:** Manual devices.sh double-remove passes. `./scripts/smoke-test.sh --only devices` passes.

---

**Phase Acceptance Criteria:**
- [ ] `device/remove` calls `selectFirst()` after `deleteObject()`
- [ ] `masterDevice/remove` calls `selectFirst()` after `deleteObject()`
- [ ] Unit tests verify call order
- [ ] Manual double-remove scenario passes in Bitwig
- [ ] ISS-3 resolved

**Completion triggers Phase 21 → version `0.21.0`**
