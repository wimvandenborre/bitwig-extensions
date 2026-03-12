# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 9 — launchpad-mk2: Test Coverage (v0.9.x)

> Add test infrastructure and comprehensive unit tests for the launchpad-mk2 module. Covers all 10 static methods in LaunchpadMk2Colors (pure functions with complex color correction math) and metadata validation for LaunchpadMk2ExtensionDefinition. Extension behavioral tests deferred to a future phase.

**Decisions:** D-9.1, D-9.2, D-9.3, D-9.4

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 9.1 | `0.9.1` | Add test dependencies to launchpad-mk2 | in-session | pending |
| 9.2 | `0.9.2` | LaunchpadMk2ColorsTest + LaunchpadMk2ExtensionDefinitionTest | in-session | pending |
| 9.3 | `0.9.3` | Verify full build | in-session | pending |

### Batch 9.1 — Add test dependencies to launchpad-mk2

**Delegation:** in-session
**Decisions:** D-9.2
**Files:**
- Edit `launchpad-mk2/build.gradle.kts`

**Work:**
1. Add `testImplementation` dependencies using version catalog: `libs.junit.jupiter`, `libs.junit.platform.launcher`, `libs.mockito.core`, `libs.mockito.junit.jupiter`
2. Add `tasks.withType<Test> { useJUnitPlatform() }` for JUnit 5 platform
3. Verify `./gradlew :launchpad-mk2:test` runs (0 tests, no failures)

**Test criteria:** `./gradlew :launchpad-mk2:test` succeeds with 0 tests.
**Acceptance:** Test infrastructure in place; JUnit + Mockito available.

### Batch 9.2 — LaunchpadMk2ColorsTest + LaunchpadMk2ExtensionDefinitionTest

**Delegation:** in-session (depends on 9.1)
**Decisions:** D-9.1, D-9.3
**Files:**
- Create `launchpad-mk2/src/test/java/com/gregross/bitwig/launchpadmk2/LaunchpadMk2ColorsTest.java`
- Create `launchpad-mk2/src/test/java/com/gregross/bitwig/launchpadmk2/LaunchpadMk2ExtensionDefinitionTest.java`

**Work:**
1. **LaunchpadMk2ColorsTest** — ~25 tests:
   - `gridNote` — (0,0)→11, (7,7)→88, mid-range
   - `sceneLaunchNote` — (0)→19, (7)→89
   - `packRgb` — parameterized across all 12 hue zones: red, orange, yellow, lime, green, mint, cyan, light blue, blue, purple, magenta, pink. Plus low-saturation fallback, near-black, max-white
   - `dimPackedRgb` — verify ~40% brightness reduction
   - `setLedRgb` — SysEx header/footer, RGB unpacking, byte format
   - `findClosestVelocity` — parameterized across hue zones: returns correct velocity per zone, low-saturation returns 3 (white)
   - `flashLed` / `pulseLed` — SysEx format with correct command bytes
   - `resetLeds` / `setSessionLayout` — fixed byte arrays

2. **LaunchpadMk2ExtensionDefinitionTest** — ~6 tests:
   - `name_returnsLaunchpadMk2`
   - `author_returnsGregRoss`
   - `apiVersion_returns25`
   - `id_isNotNull`
   - `midiPorts_correctCounts` (1 in, 1 out)
   - `helpFilePath_returnsReadme`

**Test criteria:** `./gradlew :launchpad-mk2:test` passes; ~31 tests.
**Acceptance:** All LaunchpadMk2Colors methods covered; definition metadata validated.

### Batch 9.3 — Verify full build

**Delegation:** in-session (depends on 9.2)
**Decisions:** D-9.4
**Files:** None (verification only)

**Work:**
1. Run `./gradlew clean build` — full clean build of both modules.
2. Verify launchpad-mk2 test count ≥ 31.
3. Verify gig-maestro tests still pass (559).

**Test criteria:** Clean build succeeds; all tests pass across both modules.
**Acceptance:** Full project build green with launchpad-mk2 test coverage established.

**Phase Acceptance Criteria:**
- [ ] launchpad-mk2 has JUnit 5 + Mockito test dependencies
- [ ] LaunchpadMk2ColorsTest covers all 10 static methods (~25 tests)
- [ ] LaunchpadMk2ExtensionDefinitionTest validates metadata (~6 tests)
- [ ] All gig-maestro 559 tests still pass
- [ ] `./gradlew clean build` succeeds
- [ ] launchpad-mk2 test count ≥ 31

**Completion triggers Phase 10 → version `0.10.0`**

---

## Plan Amendments

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| — | — | — | — |
