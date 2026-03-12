# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-11 — Scope: What integration tests add the most value

**Decision:** Add two new test classes: (1) `HandlerRegistrationIntegrationTest` — instantiates all 16 handlers with Mockito mocks mirroring `GigMaestroExtension.init()`, registers them on a real `JsonRpcDispatcher`, then verifies `api/list` returns all expected methods and a few sample RPC calls succeed end-to-end (raw JSON → dispatcher → handler → response). (2) `GigMaestroDefinitionTest` — validates extension metadata (UUID, name, author, version, API version, port counts).
**Rationale:** The 542 existing tests cover individual handlers in isolation — each creates its own dispatcher. No test verifies that all handlers wire together correctly. A registration wiring test catches: missing registrations, method name conflicts, broken constructors, incorrect dependency injection. The definition test is trivial but ensures metadata consistency. These two classes close the most impactful gap with minimal effort.
**Alternatives considered:** (a) Test ServerManager start/stop — rejected, adds network port flakiness for minimal value. (b) Test CLI commands — rejected, they're thin wrappers over RpcClient and require HTTP server. (c) Test StateCache observer registration — rejected, too coupled to Bitwig runtime objects.
**Status:** ACTIVE
**ID:** D-8.1

## 2026-03-11 — Integration test structure and mocking strategy

**Decision:** `HandlerRegistrationIntegrationTest` lives in `extension/` test package (since it tests extension-level wiring). Uses `@ExtendWith(MockitoExtension.class)` with `@MockitoSettings(strictness = Strictness.LENIENT)` — needs 20+ mocks (one per Bitwig API type used in constructors) but most go unused per call. Registers `session/snapshot` and `api/list` as built-in methods to match real init. Verifies: (a) total registered method count matches expected, (b) every handler's namespace is present, (c) 3-5 sample RPC calls return valid responses (not error). DeviceLibrary is constructed via temp dir (it's a regular class, not Bitwig API).
**Rationale:** Lenient strictness is required because most mocks exist only to satisfy constructor signatures — only the mocks relevant to a specific sample call will be interacted with. The extension/ package is the natural home since this tests the assembly point.
**Alternatives considered:** (a) Strict Mockito — rejected, would require per-test stubbing for 20+ unused mocks. (b) Put in handlers/ package — rejected, this isn't a handler test, it's a wiring test. (c) Use manual mock classes instead of Mockito — rejected, Mockito is already in use and the Bitwig API is all interfaces.
**Status:** ACTIVE
**ID:** D-8.2

## 2026-03-11 — Batch structure: 2 batches

**Decision:** 2 batches: (1) Add both test classes, (2) verify full build. Small scope — no need for team parallelism.
**Rationale:** Total new code is ~2 test classes. A single implementation batch keeps overhead proportional.
**Alternatives considered:** (a) One batch per test class — rejected, unnecessary overhead.
**Status:** ACTIVE
**ID:** D-8.3
