# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-11 — Scope: What to test in launchpad-mk2

**Decision:** Test `LaunchpadMk2Colors` (pure utility, 10 static methods with complex color correction math) and `LaunchpadMk2ExtensionDefinition` (metadata getters). Defer `LaunchpadMk2Extension` behavioral tests to a future phase — the 569-line stateful controller requires mocking the entire `init()` chain (20+ Bitwig API factory calls, observer registration, MIDI callback capture) which is disproportionate effort for Phase 9.
**Rationale:** Colors has the highest bug risk — 12 hue zones with channel-specific corrections, SysEx byte construction, coordinate mapping. All pure functions, zero mocking needed. Definition is trivial but establishes test coverage baseline. Extension testing requires capturing private MIDI callbacks from `init()` plus setting up mock observable chains (markInterested + addValueObserver) for ~64 clip slots — a separate phase-sized effort.
**Alternatives considered:** (a) Test everything including Extension — rejected, Extension mock setup is a full phase on its own. (b) Only test Colors — rejected, Definition is trivial to add and completes the "no-mock" coverage. (c) Test Extension via reflection — rejected, fragile and not idiomatic.
**Status:** ACTIVE
**ID:** D-9.1

## 2026-03-11 — Test infrastructure: Add JUnit + Mockito to launchpad-mk2

**Decision:** Add `testImplementation` dependencies for JUnit 5 and Mockito to `launchpad-mk2/build.gradle.kts` using the existing version catalog (`libs.junit.jupiter`, `libs.junit.platform.launcher`, `libs.mockito.core`, `libs.mockito.junit.jupiter`). Add `useJUnitPlatform()` to the test task. Mockito is included for future Extension tests even though Phase 9 doesn't need it.
**Rationale:** The version catalog already declares these dependencies for gig-maestro. Reusing the same versions ensures consistency. Including Mockito now avoids a separate setup batch in the future Extension testing phase.
**Alternatives considered:** (a) Only add JUnit, skip Mockito — rejected, we'll need it next phase and it's one line. (b) Create a shared test convention plugin — rejected, over-engineering for 2 modules.
**Status:** ACTIVE
**ID:** D-9.2

## 2026-03-11 — LaunchpadMk2ColorsTest scope and approach

**Decision:** ~25 tests covering all 10 static methods: (1) `gridNote` — boundary and mid-range formula checks, (2) `sceneLaunchNote` — boundary checks, (3) `packRgb` — all 12 hue zones + low-saturation fallback + near-black input, (4) `dimPackedRgb` — brightness reduction verification, (5) `setLedRgb` — SysEx byte format and RGB unpacking, (6) `findClosestVelocity` — all 12 hue zones + low-saturation white fallback, (7) `flashLed` / `pulseLed` — SysEx format, (8) `resetLeds` / `setSessionLayout` — fixed SysEx output. Use `@ParameterizedTest` with `@CsvSource` for hue zone coverage in packRgb and findClosestVelocity.
**Rationale:** The color correction logic in `packRgb` and `findClosestVelocity` is the most complex code in the module — 12 hue-dependent branches with channel multipliers. Testing each zone ensures LED color fidelity. SysEx tests verify byte-level protocol compliance.
**Alternatives considered:** (a) Only test edge cases — rejected, the hue zone branching is the core complexity and deserves per-zone tests. (b) Snapshot testing — rejected, not worth the tooling for simple assertions.
**Status:** ACTIVE
**ID:** D-9.3

## 2026-03-11 — Batch structure: 3 batches

**Decision:** 3 batches: (1) Add test dependencies to build.gradle.kts, (2) Add LaunchpadMk2ColorsTest + LaunchpadMk2ExtensionDefinitionTest, (3) Verify full build. Batch 1 is separate because it changes build config.
**Rationale:** Separating build config from test code keeps commits clean and makes it easy to verify the dependency setup works before writing tests.
**Alternatives considered:** (a) 2 batches (combine deps + tests) — viable, but mixing build config and test code in one commit is messy. (b) 1 batch per test class — rejected, unnecessary overhead.
**Status:** ACTIVE
**ID:** D-9.4
