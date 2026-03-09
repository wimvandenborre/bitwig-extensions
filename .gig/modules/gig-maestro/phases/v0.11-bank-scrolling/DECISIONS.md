# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

<!-- Decision statuses:
  PROPOSED  — Claude's recommendation, awaiting user approval
  ACTIVE    — Approved and in effect
  AMENDED   — Overridden by user (original preserved, new entry appended)
  REVISED   — Claude revised based on new information (original preserved)
-->

<!-- Entry format:
## YYYY-MM-DD — Domain: Question

**Decision:** What was decided.
**Rationale:** Why this choice was made.
**Alternatives considered:** What else was evaluated.
**Status:** PROPOSED | ACTIVE | AMENDED | REVISED
**ID:** D-{batch}.{num}
-->

## 2026-02-28 — Scope: What does Phase 11 cover?

**Decision:** Phase 11 adds bank scrolling to all three banks: SceneBank (priority 1), CueMarkerBank (priority 2), and TrackBank (priority 3). Also adds `itemCount` to snapshot for each bank so the agent knows how many items exist beyond the visible window.
**Rationale:** The immediate trigger is SceneBank — with a window of 8, any project with 9+ scenes has unreachable scenes (can't delete, rename, or interact). CueMarkerBank (window 16) is less urgent but still limited. TrackBank (window 64) rarely overflows but should get scroll for completeness. All three banks inherit from `Scrollable` so the implementation pattern is identical. The Bitwig API provides `scrollPosition()` (SettableIntegerValue), `scrollBy(int)`, `scrollIntoView(int)`, `canScrollForwards()`, `canScrollBackwards()`, and `itemCount()` on all banks.
**Alternatives considered:** Only doing SceneBank — but the implementation is identical for all three banks, so the marginal cost is low and it prevents future gaps.
**Status:** ACTIVE
**ID:** D-11.1

## 2026-02-28 — API Design: Which scroll methods to expose per bank?

**Decision:** Expose 3 RPC methods per bank (9 total): `{domain}/scrollTo`, `{domain}/scrollBy`, `{domain}/getScrollInfo`. Where domain = `scene`, `cueMarker`, `track`.
**Rationale:** `scrollTo` wraps `scrollPosition().set(int)` — jump to absolute position. `scrollBy` wraps `scrollBy(int)` — relative scroll (positive/negative). `getScrollInfo` returns `{ scrollPosition, itemCount, bankSize, canScrollForwards, canScrollBackwards }` — the agent needs this to know if there's more beyond the window. Page-level scroll (`scrollPageForwards/Backwards`) and single-step scroll (`scrollForwards/Backwards`) are just special cases of `scrollBy` with amount=bankSize or amount=1, so they're unnecessary as separate methods. Three methods per bank keeps the API minimal.
**Alternatives considered:** Exposing all 10 Scrollable methods per bank (30 total RPC methods) — massive bloat for no gain. A single `bank/scroll` unified method — breaks the domain-based namespace pattern.
**Status:** AMENDED — original used `scene/scrollTo` etc. User redlined to `sceneBank/scrollTo`, `trackBank/scrollTo`, `cueMarkerBank/scrollTo` for consistent domain naming.
**ID:** D-11.2

## 2026-02-28 — API Design (amended): Consistent bank-domain naming + getScrollInfo as convenience wrapper

**Decision:** Expose 3 RPC methods per bank (9 total) with bank-domain naming: `sceneBank/scrollTo`, `sceneBank/scrollBy`, `sceneBank/getScrollInfo` (and same for `trackBank/`, `cueMarkerBank/`). `getScrollInfo` is a convenience wrapper that returns cached observer values `{ scrollPosition, itemCount, bankSize, canScrollForwards, canScrollBackwards }` — NOT a new Bitwig API call. `scrollTo(position)` takes an absolute global index (not bank-relative). Validates `position >= 0 && position < itemCount`, returns `POSITION_OUT_OF_RANGE` error with `{ itemCount, requestedPosition }` on failure.
**Rationale:** User redline: the Bitwig `Scrollable` API doesn't have a single "getScrollInfo" method — it exposes individual observables. Our RPC should be explicit that it's a convenience wrapper returning cached state. Bank-domain naming (`sceneBank/`, `trackBank/`, `cueMarkerBank/`) is cleaner than overloading existing domain prefixes. Absolute index semantics for `scrollTo` match how the underlying `scrollPosition().set()` works.
**Alternatives considered:** Original D-11.2 naming (`scene/scrollTo`) — overloads the `scene/` namespace which is for scene lifecycle operations.
**Status:** ACTIVE
**ID:** D-11.2a

## 2026-02-28 — Architecture: Where do scroll methods go?

**Decision:** Add scroll methods to each domain's existing handler: `SceneHandler` gets `sceneBank/scrollTo`, `sceneBank/scrollBy`, `sceneBank/getScrollInfo`. `ArrangerHandler` gets `cueMarkerBank/scrollTo`, `cueMarkerBank/scrollBy`, `cueMarkerBank/getScrollInfo`. `TrackHandler` gets `trackBank/scrollTo`, `trackBank/scrollBy`, `trackBank/getScrollInfo`.
**Rationale:** Follows the one-handler-per-domain pattern. Each handler already has access to its bank. No new handlers or dependencies needed. The `Scrollable` interface is on all bank objects already held by these handlers.
**Alternatives considered:** A centralized `BankScrollHandler` that handles all three banks — breaks the domain pattern and requires passing all three bank objects to one handler.
**Status:** ACTIVE
**ID:** D-11.3

## 2026-02-28 — Snapshot: What metadata to add?

**Decision:** Restructure all three bank sections in the snapshot to follow a uniform bank-window pattern: `{ bankSize, scrollPosition, itemCount, canScrollBackwards, canScrollForwards, items: [...] }`. SceneBank already has `bankOffset`/`bankSize` — rename `bankOffset` to `scrollPosition` for consistency with the RPC naming, add `itemCount`, `canScroll*`. CueMarkerBank restructured from flat array to bank-window object. TrackBank restructured from flat `tracks` array to bank-window object.
**Rationale:** Uniform structure across all three banks makes the agent's mental model consistent. The agent needs `itemCount` to know what's beyond the window and `canScroll*` to decide when to navigate. All values come from Bitwig API observers that are already (or will be) registered.
**Alternatives considered:** Keeping tracks as flat top-level array with separate metadata fields — inconsistent with scene/cueMarker bank-window pattern.
**Status:** ACTIVE
**ID:** D-11.4

## 2026-02-28 — Observers: What new observers are needed in StateCache?

**Decision:** Add 4 observers per bank × 3 banks = 12 total: `scrollPosition`, `itemCount`, `canScrollForwards`, `canScrollBackwards` for each of SceneBank, CueMarkerBank, TrackBank. SceneBank `scrollPosition` is already observed, so 11 new observer registrations.
**Rationale:** User redline: all three banks implement `Scrollable` and expose `scrollPosition()` — SceneBank was called out as already having it, but the observer count should be 4 per bank (12 total, minus the 1 existing = 11 new). All are `BooleanValue` or `IntegerValue` with simple `addValueObserver()` + `markInterested()` calls.
**Alternatives considered:** Original D-11.5 listed 11 observers but omitted SceneBank `scrollPosition` from the explicit list (it was already wired). Corrected to be explicit about all 12 observers with note that 1 pre-exists.
**Status:** ACTIVE
**ID:** D-11.5

## 2026-02-28 — Dependencies: What changes to extension wiring?

**Decision:** Pass `CueMarkerBank` to `ArrangerHandler` (if not already available) and ensure `TrackBank` is accessible in `TrackHandler`. No new Bitwig API objects needed — all banks already exist.
**Rationale:** `ArrangerHandler` already receives `CueMarkerBank` in its constructor. `TrackHandler` already receives `TrackBank`. `SceneHandler` already receives `SceneBank`. The scroll methods just call existing bank methods. StateCache already receives all three banks. Zero new API objects.
**Alternatives considered:** None — this is straightforward wiring.
**Status:** ACTIVE
**ID:** D-11.6
