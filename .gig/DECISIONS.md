# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-08 — Dependency: Mockito version and integration

**Decision:** Add Mockito 5.x (mockito-core + mockito-junit-jupiter) to the version catalog and gig-maestro test dependencies.
**Rationale:** Mockito 5.x is the latest stable line, supports Java 21, and works seamlessly with JUnit 5 via the mockito-junit-jupiter extension. All Bitwig API types are interfaces, so standard Mockito mocking works with zero special config (no PowerMock, no byte-buddy workarounds). The `@ExtendWith(MockitoExtension.class)` + `@Mock` annotation pattern is idiomatic and reduces boilerplate.
**Alternatives considered:** (a) JMockit — rejected, less active maintenance, more invasive. (b) Hand-written stubs — rejected, too verbose for 153 methods with deep call chains. (c) Continue the MacroHandler dispatcher-stub pattern for all handlers — rejected, that pattern tests orchestration, not individual method behavior.
**Status:** ACTIVE
**ID:** D-5.1

## 2026-03-08 — Scope: Which handlers to mock first

**Decision:** Phase 5 covers 3 handlers as a proof-of-concept: ProjectHandler (3 methods, simplest), MasterHandler (5 methods, moderate), and TrackHandler (16 methods, complex chains). Remaining 10 handlers deferred to future phases.
**Rationale:** ProjectHandler and MasterHandler are simple 1-level API calls — ideal for establishing the pattern. TrackHandler demonstrates all complexity levels: 1-level calls (createAudio, deleteSelected), 2-level chains (mute().set(), solo().set()), and 3-level chains (volume().value().setImmediately()). Covering these three handlers (~24 methods) provides a complete template for the remaining ~130 methods. Tackling all 153 methods in one phase would be too large.
**Alternatives considered:** (a) All 13 handlers — rejected, too large for one phase (~143 tests). (b) Only ProjectHandler as proof — rejected, too narrow, doesn't demonstrate chain mocking patterns.
**Status:** ACTIVE
**ID:** D-5.2

## 2026-03-08 — Pattern: Test structure and mock setup

**Decision:** Use `@ExtendWith(MockitoExtension.class)` with `@Mock` fields for all Bitwig API interfaces. Create mock wiring in `@BeforeEach` using Mockito `when().thenReturn()` for common deep chains (e.g., `when(mockTrack.volume()).thenReturn(mockParameter)`). Existing validation tests in each handler test file remain untouched — new mock-based tests are added alongside them.
**Rationale:** The `@Mock` annotation pattern is the standard Mockito idiom and generates readable tests. Validation tests don't need mocks (they rely on exceptions thrown before API calls), so they coexist cleanly. Each test follows arrange-act-verify: set up mock returns, dispatch the RPC, verify the correct Bitwig API method was called with correct args.
**Alternatives considered:** (a) Separate mock test classes (e.g., `TrackHandlerMockTest`) — rejected, splitting tests across files obscures coverage. (b) Shared mock factory utility — rejected for now, premature abstraction; each handler has unique mock needs.
**Status:** ACTIVE
**ID:** D-5.3

## 2026-03-08 — Coexistence: Null-API tests vs mock tests

**Decision:** Keep existing null-API validation tests as-is. Add mock-based tests in a clearly labeled section below them. The `@BeforeEach` method will create real mocks instead of passing null, but existing validation tests will continue to work because validation logic throws before reaching any API call.
**Rationale:** Existing tests are stable and proven. Switching from `null` to mocked API objects doesn't break validation tests — `requireInt` still throws for missing params regardless of whether the TrackBank is null or mocked. This avoids any risk of regressions while expanding coverage.
**Alternatives considered:** (a) Replace null with mocks everywhere — rejected, risky and unnecessary for validation tests. (b) Keep null setUp and create separate mock setUp — rejected, Mockito mocks return reasonable defaults (null/0/false) that shouldn't affect validation paths.
**Status:** ACTIVE
**ID:** D-5.4
