# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-08 — Scope: Which handlers to cover

**Decision:** Cover 11 remaining direct-API handlers: ApplicationHandler, TransportHandler, ArrangerHandler, SceneHandler, ClipHandler, NoteHandler, DeviceHandler, MasterDeviceHandler, BrowserHandler, NoteInputHandler, SendHandler. Defer MacroHandler and TransactionHandler (orchestration handlers that call `dispatcher.handleInternal()` rather than Bitwig API directly) to a future phase.
**Rationale:** The 11 direct-API handlers follow the same mocking pattern proven in Phase 5 — mock Bitwig interfaces, verify API method calls. MacroHandler and TransactionHandler use a fundamentally different pattern (they orchestrate other RPC methods via the dispatcher), requiring dispatcher mocking rather than Bitwig API mocking. Mixing patterns in one phase adds complexity without benefit.
**Alternatives considered:** (a) All 13 remaining handlers — rejected, orchestration handlers need a different mocking strategy. (b) Only the 5 largest handlers — rejected, leaves too many gaps.
**Status:** ACTIVE
**ID:** D-6.1

## 2026-03-08 — Test scope: Bitwig API calls only

**Decision:** Write behavioral tests only for methods that call Bitwig API objects. Skip methods that only read from StateCache (e.g., `getClipLauncherSettings`, `getScrollInfo`, `getState` on handlers that delegate to stateCache). Exception: ApplicationHandler's `getState` which reads from Application mock methods, not StateCache.
**Rationale:** StateCache is our own code with its own tests. Verifying `stateCache.getX()` was called adds no value — the validation tests already confirm these methods exist and return results. Behavioral tests should verify interaction with the external Bitwig API boundary.
**Alternatives considered:** (a) Test every method including StateCache reads — rejected, inflates test count with low-value assertions. (b) Mock StateCache too — rejected, it's internal code, not a boundary.
**Status:** ACTIVE
**ID:** D-6.2

## 2026-03-08 — Pattern: Reuse Phase 5 conventions

**Decision:** Same `@ExtendWith(MockitoExtension.class)` + `@Mock` fields + arrange-act-verify pattern from Phase 5. Use `@MockitoSettings(strictness = Strictness.LENIENT)` only for handlers that mix validation tests with common setUp stubs. Add behavioral tests alongside existing validation tests in the same test class.
**Rationale:** Phase 5 proved this pattern works across all chain depths (1-level through 3-level). Consistency across all handler tests makes the codebase easier to navigate and maintain.
**Alternatives considered:** None — this is a continuation of an established pattern.
**Status:** ACTIVE
**ID:** D-6.3

## 2026-03-08 — Batching: Group by domain affinity

**Decision:** 7 batches grouped by API domain: (1) simple handlers — ApplicationHandler + SendHandler + NoteInputHandler, (2) TransportHandler alone, (3) ArrangerHandler + SceneHandler, (4) ClipHandler + NoteHandler, (5) DeviceHandler + MasterDeviceHandler, (6) BrowserHandler alone, (7) verify full build. Independent batches run as team (parallel).
**Rationale:** Grouping by domain keeps related mocking patterns together. TransportHandler, ClipHandler, and BrowserHandler are large enough to warrant their own batches. DeviceHandler and MasterDeviceHandler share CursorDevice/RemoteControlsPage patterns. ArrangerHandler and SceneHandler share SceneBank.
**Alternatives considered:** (a) One handler per batch (11 batches) — rejected, too granular. (b) Two mega-batches — rejected, too large to review.
**Status:** ACTIVE
**ID:** D-6.4
