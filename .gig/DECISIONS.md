# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-14 — Actions: Where to register action RPCs

**Decision:** Add 3 new RPC methods to the existing ApplicationHandler: `action/list`, `action/invoke`, `action/listCategories`.
**Rationale:** ApplicationHandler already holds the `Application` reference. Action methods logically belong in the same handler since they're Application API methods. Namespace prefix `action/` distinguishes them from `app/` convenience methods.
**Alternatives considered:** Separate ActionHandler — rejected because it would need the same Application reference and adds unnecessary class for 3 methods.
**Status:** ACTIVE
**ID:** D-1.1

## 2026-03-14 — Actions: List response format

**Decision:** `action/list` returns flat array of `{id, name, category, menuItemText}` objects. Optional `category` filter param to narrow results.
**Rationale:** Flat array is simplest to consume. Category filter avoids returning hundreds of actions when only one group is needed. Including menuItemText gives Claude the user-facing label for better context.
**Alternatives considered:** Nested by-category structure — rejected because it complicates parsing and the flat format with optional filter covers both use cases.
**Status:** ACTIVE
**ID:** D-1.2

## 2026-03-14 — Actions: Invoke error handling

**Decision:** `action/invoke` takes `{id}` string param, calls `application.getAction(id)`. If null, throw RPC error -32001 "ACTION_NOT_FOUND". No return value beyond `{ok: true}`.
**Rationale:** `getAction()` returns null for invalid IDs (per API docs), so we must check. Structured error code lets callers handle gracefully. No state to return — actions are fire-and-forget.
**Alternatives considered:** Silently ignoring invalid IDs — rejected because it hides bugs.
**Status:** ACTIVE
**ID:** D-1.3

## 2026-03-14 — Actions: Track group navigation additions

**Decision:** Also add `app/navigateIntoTrackGroup` and `app/navigateToParentTrackGroup` to ApplicationHandler in this phase. These are small Application methods that complement cursor navigation.
**Rationale:** Two one-liner methods on the same Application object. Adding them now avoids a standalone phase for 2 lines of code. Track group navigation + action invoke together make this a complete "Application API completion" phase.
**Alternatives considered:** Deferring to a separate phase — rejected as wasteful for 2 trivial methods.
**Status:** ACTIVE
**ID:** D-1.4

## 2026-03-14 — Actions: No StateCache integration needed

**Decision:** No StateCache changes. Actions are commands (fire-and-forget), not observable state. Track group navigation is also stateless.
**Rationale:** Actions don't emit value changes. There's nothing to cache or include in snapshots/deltas.
**Alternatives considered:** Caching action list at init — rejected because action list is static and caching adds complexity for no benefit.
**Status:** ACTIVE
**ID:** D-1.5
