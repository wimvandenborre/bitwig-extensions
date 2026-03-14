# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## D-28.1 — macro/writeAutomation as a standalone macro

**Status:** ACTIVE
**Date:** 2026-03-14

**Decision:** Create `macro/writeAutomation` that writes multiple parameter envelopes to the cursor device in one call. Takes an array of `{paramIndex, pageIndex?, points: [{position, value}]}` entries. Automatically enables arranger automation write if not already enabled, writes all envelopes sequentially with proper delays, then restores the automation write state.

**Rationale:** The existing `device/writeEnvelope` handles one parameter at a time and requires the caller to manage automation write state. A macro batches N parameters into 1 call, reducing round-trips. The cursor device is already positioned by prior macro calls (createTrack/createSound).

**Alternatives considered:** Integrating automation directly into `macro/buildSong` sections — rejected because arranger automation is position-based (beat offset from start), not scene-based, so it's conceptually separate from clip launcher macros.

---

## D-28.2 — Page switching for multi-page automation

**Status:** ACTIVE
**Date:** 2026-03-14

**Decision:** When `pageIndex` is provided on an envelope entry, switch to that remote controls page before writing points. Entries are grouped by pageIndex and written page-by-page to minimize page switches. If `pageIndex` is omitted, write to the current page.

**Rationale:** Device parameters span multiple pages (0-7 params per page). The existing `device/writeEnvelope` only operates on the current page. Multi-page automation is common — e.g., automating both oscillator and filter params.

**Alternatives considered:** Requiring all entries to be on the same page — too limiting for real use.

---

## D-28.3 — Auto-enable and restore automation write state

**Status:** ACTIVE
**Date:** 2026-03-14

**Decision:** `macro/writeAutomation` checks `isArrangerAutomationWriteEnabled`. If disabled, enables it before writing, and restores the original state after completion. This removes a precondition the caller would otherwise need to manage.

**Rationale:** `device/writeEnvelope` throws if automation write is disabled. The macro should handle this transparently. Restoring state prevents leaving automation write enabled after the macro completes.

**Alternatives considered:** Requiring the caller to enable it first (like `device/writeEnvelope` does) — poor UX for a macro that's supposed to simplify workflows.

---

## D-28.4 — Sequential envelope writing with cumulative delays

**Status:** ACTIVE
**Date:** 2026-03-14

**Decision:** Each envelope is written by delegating to `device/writeEnvelope` internally. Envelopes are chained sequentially — envelope N+1 starts after envelope N completes. Delay per envelope = `100ms * (pointCount + 2)` (points + cleanup overhead). Page switches add an additional `FLUSH_DELAY_MS`.

**Rationale:** `device/writeEnvelope` manipulates transport position and touch state — concurrent writes would conflict. Sequential chaining with calculated delays ensures each envelope completes before the next starts.

**Alternatives considered:** Parallel writes — impossible due to transport position sharing.

---

## D-28.5 — Return shape includes per-envelope results

**Status:** ACTIVE
**Date:** 2026-03-14

**Decision:** Return `{ok: true, envelopeCount: N, totalPoints: M}`. The macro is optimistic — point writes are deferred, so the return doesn't confirm writes completed.

**Rationale:** Consistent with other macro return shapes (writeClip returns count, buildSong returns counts). Deferred operations can't be confirmed synchronously.

**Alternatives considered:** Returning per-envelope status — adds complexity for no benefit since all are deferred.
