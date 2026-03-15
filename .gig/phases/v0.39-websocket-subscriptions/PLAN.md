# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 39 — WebSocket Subscriptions (v0.39.x)

> Add per-client topic subscriptions to the WebSocket server. Clients can subscribe to specific state sections (transport, tracks, device, etc.) and only receive deltas for those topics. Backward compatible — unsubscribed clients get everything.

**Decisions:** D-1.1, D-1.2, D-1.3, D-1.4, D-1.5

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 39.1 | `0.39.1` | WsRpcServer per-client subscriptions + filtered broadcast | in-session | done |
| 39.2 | `0.39.2` | RPC methods: subscribe, unsubscribe, subscribeAll, getTopics | in-session | done |
| 39.3 | `0.39.3` | Unit tests | in-session | done |
| 39.4 | `0.39.4` | Tool definitions + system prompt + smoke tests | in-session | done |
| 39.5 | `0.39.5` | Build verification | in-session | done |

### Batch 39.1 — WsRpcServer per-client subscriptions + filtered broadcast

**Delegation:** in-session
**Decisions:** D-1.2, D-1.4
**Files:** `gig-maestro/src/main/java/dev/gregross/gig/server/WsRpcServer.java`, `gig-maestro/src/main/java/dev/gregross/gig/server/ServerManager.java`
**Work:**
- WsRpcServer: Add `ConcurrentHashMap<WebSocket, Set<String>> subscriptions` field.
- On `onClose()`: remove client from subscriptions map.
- Add `setSubscription(WebSocket, Set<String>)`, `clearSubscription(WebSocket)`, `getSubscription(WebSocket)` methods.
- Change `broadcast(String)` to `broadcastDelta(JsonObject delta)`: parse `changed` array and `data` object, for each client filter to subscribed topics, serialize per-client, send. If client has no subscription entry → send full delta (backward compat).
- ServerManager: Update `broadcast()` to call `broadcastDelta()`, change GigMaestroExtension to pass the delta JsonObject instead of pre-serialized string.
**Test criteria:** `./gradlew :gig-maestro:compileJava` passes
**Acceptance:** Per-client filtering works, backward compatible

### Batch 39.2 — RPC methods: subscribe, unsubscribe, subscribeAll, getTopics

**Delegation:** in-session
**Decisions:** D-1.1, D-1.3, D-1.5
**Files:** `gig-maestro/src/main/java/dev/gregross/gig/extension/GigMaestroExtension.java`
**Work:**
- Define `VALID_TOPICS` set (14 section names) as a constant.
- Register `state/subscribe` — takes `{topics: [...]}`, validates each topic, stores subscription via ServerManager → WsRpcServer. Returns `{ok: true, topics: [...]}`. Problem: RPC handler doesn't know which WebSocket client sent the request.
- **Key design issue:** The RPC dispatcher doesn't have access to the WebSocket connection. Need to either: (a) pass WebSocket reference through the RPC pipeline, or (b) register subscribe methods directly in WsRpcServer bypassing the dispatcher. Choose (b): handle subscription RPCs directly in WsRpcServer.onMessage() before dispatching to the normal handler. This avoids threading the WebSocket through the entire RPC stack.
- Register `state/unsubscribe` — removes topics from client subscription.
- Register `state/subscribeAll` — clears subscription (reverts to all topics).
- Register `state/getTopics` — returns list of valid topic names. This one CAN go through normal dispatcher since it doesn't need client identity.
**Test criteria:** `./gradlew :gig-maestro:compileJava` passes
**Acceptance:** 4 new RPC methods, subscription state persists per-client

### Batch 39.3 — Unit tests

**Delegation:** in-session
**Decisions:** all
**Files:** `gig-maestro/src/test/java/dev/gregross/gig/server/WsRpcServerTest.java` (NEW), `gig-maestro/src/test/java/dev/gregross/gig/extension/HandlerRegistrationIntegrationTest.java`
**Work:**
- WsRpcServerTest: test subscription storage, filtered broadcast logic, backward compat (no subscription = all topics), subscribe/unsubscribe/subscribeAll via direct method calls
- Test invalid topic validation
- Test delta filtering: client subscribed to ["transport"] only gets transport changes
- Update integration test for state/getTopics
**Test criteria:** `./gradlew :gig-maestro:test` — all tests pass
**Acceptance:** 8+ test methods covering subscription lifecycle and filtering

### Batch 39.4 — Tool definitions + system prompt + smoke tests

**Delegation:** in-session
**Decisions:** all
**Files:** `claude-tools.json`, `system-prompt.md`, `offline-schemas.sh`
**Work:**
- Add 4 tool definitions: state_subscribe, state_unsubscribe, state_subscribeAll, state_getTopics
- Add "WebSocket Subscriptions" section to system prompt
- Add smoke test checks
- Update tool count threshold
**Test criteria:** `./scripts/smoke-test.sh --offline` passes
**Acceptance:** All tools documented, smoke tests green

### Batch 39.5 — Build verification

**Delegation:** in-session
**Decisions:** all
**Files:** none
**Work:** Full build + test + smoke verification
**Test criteria:** All three commands exit 0
**Acceptance:** Clean build, all tests green

**Phase Acceptance Criteria:**
- [ ] Per-client topic subscriptions in WsRpcServer
- [ ] Filtered delta broadcast — clients only get subscribed sections
- [ ] Backward compatible — unsubscribed clients get full delta
- [ ] 4 new RPC methods: state/subscribe, state/unsubscribe, state/subscribeAll, state/getTopics
- [ ] Unit tests cover subscription lifecycle, filtering, and edge cases
- [ ] Tool definitions and system prompt documented
- [ ] Clean build with shadowJar

**Completion triggers Phase 40 → version `0.40.0`**
