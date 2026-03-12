# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-12 — Scope: What E2E integration tests to add

**Decision:** Focus on two untested layers: (1) CommandQueue round-trip (enqueue → drainAndExecute → CompletableFuture completion), and (2) HTTP pipeline integration (real HttpRpcServer + CommandQueue + JsonRpcDispatcher wired together, tested via java.net.HttpClient). Exclude StateCache observer testing — it would require invasive refactoring of 1,609 lines with 179 private volatile fields.
**Rationale:** CommandQueue is the threading bridge between network and session threads — it has zero tests. HttpRpcServerTest uses a stub handler (CompletableFuture.completedFuture), not the real CommandQueue+Dispatcher pipeline. Together these two layers cover the full request lifecycle without requiring a live Bitwig instance.
**Alternatives considered:** (1) Test StateCache observers — too invasive, fields are private volatile with no test seam. (2) Test WebSocket pipeline too — WsRpcServerTest already covers the WS-specific logic; the dispatch pipeline is identical.
**Status:** ACTIVE
**ID:** D-13.1

## 2026-03-12 — CommandQueue: What to verify

**Decision:** Create `CommandQueueTest` with 5 tests: (1) enqueue returns pending future, (2) drainAndExecute completes futures with dispatcher response, (3) drain returns count of processed commands, (4) multiple enqueues drain in FIFO order, (5) handler exception completes future exceptionally. Uses a real JsonRpcDispatcher with a simple registered handler.
**Rationale:** CommandQueue is 43 lines but has the critical threading contract — network threads enqueue, session thread drains. Testing the CompletableFuture lifecycle ensures the async handoff works correctly. Using real dispatcher (not mock) verifies the full drain→dispatch→complete chain.
**Alternatives considered:** (1) Mock the dispatcher — would miss integration between drain loop and dispatch. (2) Test concurrency with multiple threads — adds flakiness risk for minimal value; the ConcurrentLinkedQueue guarantees thread safety.
**Status:** ACTIVE
**ID:** D-13.2

## 2026-03-12 — HTTP Pipeline: What to verify

**Decision:** Create `HttpPipelineIntegrationTest` that wires a real HttpRpcServer → CommandQueue → JsonRpcDispatcher with registered handlers. Tests: (1) POST /rpc with valid method returns correct JSON-RPC response, (2) POST /rpc with unknown method returns -32601, (3) POST /rpc with invalid JSON returns parse error, (4) notification (no id) returns 204, (5) batch request returns array response. Drain is called from a background thread simulating Bitwig's flush cycle.
**Rationale:** This is the true E2E test — HTTP request enters the system, flows through CommandQueue, gets dispatched, response flows back through the CompletableFuture to the HTTP response. The existing HttpRpcServerTest bypasses CommandQueue entirely. This test proves the full pipeline works.
**Alternatives considered:** (1) Use the existing HandlerRegistrationIntegrationTest setup — it bypasses HTTP and CommandQueue. (2) Start a real ServerManager — would also start the WS server, adding port conflicts and complexity.
**Status:** ACTIVE
**ID:** D-13.3

## 2026-03-12 — Test Organization: Where to place E2E tests

**Decision:** Place `CommandQueueTest` in `dev.gregross.gig.rpc` package (alongside existing `JsonRpcDispatcherTest`). Place `HttpPipelineIntegrationTest` in `dev.gregross.gig.server` package (alongside existing `HttpRpcServerTest`).
**Rationale:** Follow existing convention: tests are in the same package as the class under test. CommandQueue is in `rpc`, the HTTP pipeline test primarily tests the server→queue→dispatcher integration.
**Alternatives considered:** (1) Create a new `integration` package — breaks the established pattern of co-locating tests.
**Status:** ACTIVE
**ID:** D-13.4
