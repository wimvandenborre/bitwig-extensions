# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 3 — gig-maestro: Extract JsonParamValidator (v0.3.x)

> Extract duplicated JSON parameter validation methods from 8 gig-maestro handlers into a shared `JsonParamValidator` utility class. No shared module — this is an internal refactor.

**Decisions:** D-3.1, D-3.2

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 3.1 | `0.3.1` | Create JsonParamValidator + update handlers | in-session | pending |
| 3.2 | `0.3.2` | Verify tests pass | in-session | pending |

### Batch 3.1 — Create JsonParamValidator + update handlers

**Delegation:** in-session
**Decisions:** D-3.2
**Files:**
- Create `gig-maestro/src/main/java/dev/gregross/gig/rpc/JsonParamValidator.java`
- Edit 8 handler files: ClipHandler, BrowserHandler, DeviceHandler, MacroHandler, MasterDeviceHandler, NoteHandler, SceneHandler, TrackHandler

**Work:**
1. Create `JsonParamValidator` in the `rpc` package with static methods: `requireInt()`, `requireString()`, `requireBoolean()`, `requireDouble()`, `optionalInt()`, `optionalString()`, `optionalDouble()`, `optionalBoolean()` (whatever variants exist across handlers).
2. In each of the 8 handlers: remove private `requireX()`/`optionalX()` methods, add static import for `JsonParamValidator.*`, update call sites.
3. Keep any domain-specific validators (like `validateColorComponent()`) in their respective handlers — only extract the generic JSON param methods.

**Test criteria:** `./gradlew :gig-maestro:test` passes; `./gradlew :gig-maestro:shadowJar` builds.
**Acceptance:** No handler contains private `requireInt/String/Boolean/Double` or `optionalInt/String/Double/Boolean` methods. All use `JsonParamValidator`.

### Batch 3.2 — Verify tests pass

**Delegation:** in-session (depends on 3.1)
**Decisions:** D-3.1, D-3.2
**Files:** None (verification only)

**Work:**
1. Run `./gradlew clean build` — full build.
2. Run `./gradlew :gig-maestro:test` — all tests pass.
3. Verify no remaining private require/optional methods in handlers via grep.

**Test criteria:** Clean build succeeds; all tests pass; grep confirms no duplicates remain.
**Acceptance:** Refactor complete, no regressions.

**Phase Acceptance Criteria:**
- [ ] `JsonParamValidator.java` exists in `gig-maestro/src/main/java/dev/gregross/gig/rpc/`
- [ ] All 8 handlers use `JsonParamValidator` instead of private methods
- [ ] `./gradlew clean build` succeeds
- [ ] `./gradlew :gig-maestro:test` passes
- [ ] No private `requireInt/String/Boolean/Double` methods remain in handlers

**Completion triggers Phase 4 → version `0.4.0`**

---

## Plan Amendments

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| — | — | — | — |
