# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-14 — Scope: What activity feedback to add?

**Decision:** Three features: (1) `isMutedBySolo` per track in StateCache + snapshot, (2) playing notes per track as a poll-only RPC (not cached), (3) VU meters as a poll-only RPC (not cached in snapshot/delta). Skip caching VU meters and playing notes — they're too noisy for delta and snapshot bloat.
**Rationale:** isMutedBySolo is rare (only changes on solo toggle) and small (1 boolean per track) — perfect for StateCache+delta. VU meters fire 60-100x/sec per track — caching them would spam every delta with meter noise. Playing notes change frequently during playback. Both are better served as on-demand poll RPCs that read cached values but stay out of the snapshot/delta system.
**Alternatives considered:** Caching everything in StateCache (VU meter noise overwhelms delta), throttled VU in delta (adds complexity for marginal value — Claude doesn't need real-time meters), skip VU entirely (useful for mixing analysis).
**Status:** ACTIVE
**ID:** D-1.1

## 2026-03-14 — VU Meters: How to implement without delta noise?

**Decision:** Cache VU meter values in StateCache using `addVuMeterObserver(128, -1, false, callback)` for RMS (sum of both channels) per track. Store as `int[] trackVuMeter` (0-127). Expose via a new RPC `track/getVuMeters` that returns all 8 track meter values in one call. Do NOT include in snapshot or delta.
**Rationale:** Range 128 gives sufficient resolution (0-127). Channel -1 (sum) is simpler than separate L/R — Claude doesn't need stereo metering. RMS (peak=false) is more useful for loudness analysis than peak. One RPC returns all 8 values so Claude can compare relative levels in a single call. Keeping it out of snapshot/delta avoids noise while still making the data available on demand.
**Alternatives considered:** Separate L/R channels (4x data, Claude doesn't need stereo imaging), peak instead of RMS (peak is transient-focused, less useful for mixing), per-track RPC (8 calls vs 1 — wasteful).
**Status:** ACTIVE
**ID:** D-1.2

## 2026-03-14 — Playing Notes: Cache or poll?

**Decision:** Cache playing notes per track in StateCache using `playingNotes().addValueObserver()`. Store as `PlayingNote[][]` array (per track). Expose via `track/getPlayingNotes` RPC that takes `{index: int}` and returns the current note array `[{pitch, velocity}, ...]`. Do NOT include in snapshot or delta — array changes are frequent during playback.
**Rationale:** The observer is event-driven (only fires on note on/off), so caching is low-overhead. But including in snapshot would add variable-length arrays to every track object, and during playback the delta would fire constantly. A poll RPC lets Claude check what's playing when needed.
**Alternatives considered:** Including in snapshot (bloats track objects with variable arrays), not caching at all (would need to poll Bitwig API directly — observers are the only way to get data).
**Status:** ACTIVE
**ID:** D-1.3

## 2026-03-14 — isMutedBySolo: Where to add?

**Decision:** Add `boolean[] trackMutedBySolo` to StateCache. Register observers in `registerTrackObservers()`. Include in the snapshot under each track as `isMutedBySolo`. This is safe for delta since solo changes are rare.
**Rationale:** Follows the pattern of mute/solo/arm — boolean flags per track in the snapshot. isMutedBySolo is the complementary read-only flag that tells Claude which tracks are silenced by another track's solo. Rare changes make it delta-safe. Already in the Channel interface (API v10+).
**Alternatives considered:** Not caching (inconsistent with other track booleans), separate RPC only (misses delta — Claude should see this alongside solo state).
**Status:** ACTIVE
**ID:** D-1.4

## 2026-03-14 — Handler: New handler or extend existing?

**Decision:** Add VU meter and playing notes RPCs to TrackHandler. Add isMutedBySolo to StateCache only (no new RPC — it flows through the snapshot). No new handler needed.
**Rationale:** VU meters and playing notes operate on Track objects from the track bank — they belong in TrackHandler. isMutedBySolo is a passive boolean that flows through the snapshot like mute/solo/arm — no RPC method needed. Creating a new handler for 2 RPC methods would be over-engineering.
**Alternatives considered:** New MeteringHandler (only 2 methods — not worth a new class), adding to MixerHandler (mixer handles panel visibility, not track-level data).
**Status:** ACTIVE
**ID:** D-1.5
