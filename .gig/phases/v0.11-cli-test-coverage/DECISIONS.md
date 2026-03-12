# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-11 — Testing Strategy: How to intercept RPC calls for behavioral testing

**Decision:** Create a `FakeRpcClient` subclass of `RpcClient` that records calls instead of making HTTP requests. Override `GigCli.createClient()` in a test subclass (`TestableGigCli`) to return the fake. This lets tests verify that CLI commands produce the correct RPC method + params without needing a server.
**Rationale:** `RpcClient` is package-private with a package-private constructor — subclassable from the test package. `GigCli.createClient()` is also package-private. This allows clean interception at the boundary without modifying production code or using reflection. Alternative approaches (mocking HttpClient, starting a real server) are more complex for less value.
**Alternatives considered:** (1) Mock HttpClient — requires Mockito deep-stub of builder pattern, brittle. (2) Start a real HTTP server in tests — heavy, slow, tests network layer not CLI logic. (3) Only test command structure — misses param-building logic which is the main value.
**Status:** ACTIVE
**ID:** D-11.1

## 2026-03-11 — Test Scope: Which CLI classes to cover

**Decision:** Cover 5 test classes: (1) **RpcClientFormatTest** — pure-function tests for `format()` and `formatRaw()` (already testable, no HTTP). (2) **CliCommandStructureTest** — expand existing `SongCommandTest` into comprehensive subcommand/help/parameter validation for all 7 top-level commands. (3) **TransportCommandTest** — verify all 11 subcommands produce correct RPC method + params. (4) **TrackCommandTest** — verify all 12 subcommands produce correct RPC method + params. (5) **DeviceNoteCommandTest** — verify DeviceCommand (4) + NoteCommand (8) subcommands. Skip SnapshotCommand (trivial, 1 RPC call) and SongCommand behavioral tests (complex multi-step orchestration with Thread.sleep — disproportionate effort for this phase).
**Rationale:** TransportCommand and TrackCommand have the most subcommands and param-building logic. DeviceCommand and NoteCommand have moderate complexity. SongCommand's dump/rebuild involve multi-step loops with sleep — testing them requires either real timing or major refactoring. RpcCommand is a passthrough (1 test sufficient).
**Alternatives considered:** (1) Test every command including SongCommand — SongCommand.DumpCommand alone is 200+ lines of sequential RPC calls with Thread.sleep, disproportionate. (2) Test only structure — misses param-building bugs.
**Status:** ACTIVE
**ID:** D-11.2

## 2026-03-11 — FakeRpcClient Design: What to record and return

**Decision:** `FakeRpcClient` extends `RpcClient`, overrides `call()` to record `(method, params)` tuples in a list and return a configurable `JsonElement` (default: `JsonPrimitive("ok")`). Override `callRaw()` to record raw requests and return configurable response. No HTTP calls are made. Provide `getLastMethod()`, `getLastParams()`, `getCalls()` for assertions.
**Rationale:** Simple recording pattern. Most commands only make one RPC call per invocation, so `getLastMethod()/getLastParams()` covers the common case. `getCalls()` list supports commands that make multiple calls (future-proof for SongCommand tests if needed).
**Alternatives considered:** (1) Use Mockito to mock RpcClient — works but less readable than purpose-built fake with assertion helpers.
**Status:** ACTIVE
**ID:** D-11.3

## 2026-03-11 — System.exit Handling: How to prevent test JVM termination

**Decision:** Use PicoCLI's `CommandLine.execute()` return code instead of letting commands call `System.exit(1)`. In tests, catch the exit code and assert on it. For commands that call `System.exit(1)` in error paths, the `FakeRpcClient` won't throw exceptions, so those paths aren't triggered in normal tests. Add a separate test for error handling by configuring FakeRpcClient to throw, and use `System.setSecurityManager` or JUnit 5's `assertDoesNotExit` approach.
**Rationale:** The `System.exit(1)` calls only happen in catch blocks when RPC fails. Since FakeRpcClient returns success, happy-path tests never hit `System.exit`. Error-path testing can be deferred — the critical value is verifying correct method+params.
**Alternatives considered:** (1) Refactor production code to not call System.exit — out of scope for test-only phase. (2) Use SecurityManager to intercept exit — deprecated in Java 17+.
**Status:** ACTIVE
**ID:** D-11.4

## 2026-03-11 — Existing Test Migration: What to do with SongCommandTest

**Decision:** Rename `SongCommandTest` to `CliCommandStructureTest` and expand it to cover all 7 top-level commands' structure (subcommand count, help text, required parameters). Add tests for TransportCommand, TrackCommand, DeviceCommand, NoteCommand subcommand registration. Keep the existing 7 tests intact and add new ones.
**Rationale:** The existing tests are all structure/registration tests, not song-specific. Renaming clarifies their purpose. Expanding to all commands gives comprehensive structural coverage in one place.
**Alternatives considered:** (1) Keep SongCommandTest and create a separate CliStructureTest — splits related tests unnecessarily. (2) Leave as-is — misses structure validation for 6 of 7 commands.
**Status:** ACTIVE
**ID:** D-11.5
