# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-15 — WS Subscriptions: Subscribe via JSON-RPC over WebSocket

**Decision:** Clients subscribe by sending JSON-RPC requests over the WebSocket connection: `state/subscribe` with `{topics: ["transport", "tracks"]}`, `state/unsubscribe` with `{topics: [...]}`, and `state/subscribeAll` (no params). Default on connect is ALL topics (backward compatible).
**Rationale:** JSON-RPC over WS is already supported — the WS server handles RPC requests identically to HTTP. Using the existing RPC mechanism means no new protocol, no query params, no handshake changes. Backward compatible because unsubscribed clients get everything (current behavior).
**Alternatives considered:** URL query params on connect — rejected because it requires reconnection to change subscriptions. Custom WS frames — rejected as non-standard.
**Status:** ACTIVE
**ID:** D-1.1

## 2026-03-15 — WS Subscriptions: Per-client topic set in WsRpcServer

**Decision:** Store a `Map<WebSocket, Set<String>>` in WsRpcServer mapping each client to their subscribed topics. On broadcast, filter the delta per-client: only include sections the client subscribed to. If a client has no subscription entry (or subscribeAll), send everything.
**Rationale:** Per-client filtering at the server level is the right abstraction. WsRpcServer already tracks clients in a `CopyOnWriteArraySet<WebSocket>`. The map lookup is O(1) per section per client — negligible overhead.
**Alternatives considered:** Filtering in StateCache — rejected because StateCache shouldn't know about WebSocket clients. Filtering in GigMaestroExtension — rejected because it would need WS client references.
**Status:** ACTIVE
**ID:** D-1.2

## 2026-03-15 — WS Subscriptions: Valid topic names match delta section names

**Decision:** Valid topic names are the 14 delta section names: transport, tracks, scenes, device, clip, master, application, arranger, arrangement, masterDevice, browser, arpeggiator, noteLatch, groove. Invalid topics return an error.
**Rationale:** Direct 1:1 mapping with existing sections. No abstraction layer needed. Clients can discover valid topics via `state/getTopics` which returns the list.
**Alternatives considered:** Coarser groupings (e.g., "mixing" = tracks+master+sends) — rejected as premature abstraction. Finer-grained (per-track) — rejected as too complex for the hash-based change detection.
**Status:** ACTIVE
**ID:** D-1.3

## 2026-03-15 — WS Subscriptions: Broadcast changes to per-client filtered delta

**Decision:** Change `WsRpcServer.broadcast(String json)` to accept the parsed delta object and filter per-client before serializing. New signature: `broadcastDelta(JsonObject delta)`. The method extracts `changed` and `data`, filters both by client subscription, and sends only matching sections. If no sections match for a client, skip sending entirely.
**Rationale:** Filtering before serialization avoids sending unnecessary data. Each client gets a custom-tailored notification. The slight overhead of per-client JSON construction is offset by reduced network I/O.
**Alternatives considered:** Broadcasting full delta and letting clients filter — rejected because it defeats the purpose of server-side subscriptions.
**Status:** ACTIVE
**ID:** D-1.4

## 2026-03-15 — WS Subscriptions: Add state/getTopics RPC

**Decision:** Add a `state/getTopics` RPC that returns the list of valid topic names. This helps clients discover available subscription topics.
**Rationale:** Without this, clients must hardcode topic names. A discovery endpoint makes the API self-documenting.
**Alternatives considered:** Embedding topics in api/list — rejected because topics are metadata, not methods.
**Status:** ACTIVE
**ID:** D-1.5
