# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-13 — Fix strategy: How to reposition cursor after device/remove

**Decision:** After `cursorDevice.deleteObject()`, call `cursorDevice.selectFirst()` to reposition the cursor to the first remaining device in the chain. If the chain is now empty, the cursor naturally stays at position -1 (no device), which is correct behavior.
**Rationale:** `selectFirst()` is the simplest recovery — it's always valid, doesn't require async state checks, and puts the cursor in a predictable position. `selectNext()`/`selectPrevious()` won't work from position -1. `selectLast()` would also work but is less intuitive (user expects to land near where they were or at the start). The FOLLOW_SELECTION mode means the cursor will also track any UI selection the user makes afterward.
**Alternatives considered:** (1) `selectNext()` before delete (save position, navigate, then delete) — complex and fragile, cursor may shift during async delete. (2) `selectLast()` — valid but less predictable. (3) Do nothing, document as limitation — user already hit this, it's a real usability issue.
**Status:** REVISED — `selectFirst()` was a no-op from position -1 in the same flush cycle. Changed to `selectFirstInChannel(channel)` via `scheduler.schedule()` with 100ms delay.
**ID:** D-20.1

## 2026-03-13 — Scope: Apply same fix to masterDevice/remove

**Decision:** Apply the same `selectFirst()` recovery to `masterDevice/remove` in DeviceHandler. Both cursor devices (track and master) have the same issue.
**Rationale:** The master cursor device is created identically and will exhibit the same position -1 behavior after deletion. Fixing only the track cursor would leave a gap.
**Alternatives considered:** (1) Fix only track cursor — inconsistent. (2) Extract shared helper method — over-engineering for 2 one-liners.
**Status:** ACTIVE
**ID:** D-20.2

## 2026-03-13 — Testing: Unit test + manual verification

**Decision:** Add unit tests verifying `selectFirst()` is called after `deleteObject()` for both device/remove and masterDevice/remove. Update the manual devices.sh script to re-test the double-remove scenario that previously failed. Update the automated devices.sh smoke test if applicable.
**Rationale:** The fix is small but the behavior was verified as broken during Phase 19 manual testing. Both layers (unit + manual) should confirm the fix.
**Alternatives considered:** (1) Unit test only — doesn't confirm real Bitwig behavior. (2) Manual only — no regression protection.
**Status:** ACTIVE
**ID:** D-20.3
