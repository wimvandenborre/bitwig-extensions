# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 33 — gig-maestro: VU Metering & Activity Feedback (v0.33.x)

> Add live activity feedback: VU meter polling (RMS sum, 0-127 per track), playing notes polling (current MIDI notes per track), and isMutedBySolo in the session snapshot. VU meters and playing notes are cached but excluded from snapshot/delta to avoid noise. isMutedBySolo is delta-safe and included in the snapshot.

**Decisions:** D-1.1, D-1.2, D-1.3, D-1.4, D-1.5

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 33.1-2 | `0.33.1-2` | StateCache + TrackHandler RPCs | in-session | done |
| 33.3 | `0.33.3` | Unit tests | in-session | done |
| 33.4 | `0.33.4` | Tool definitions + system prompt + smoke tests | in-session | done |
| 33.5 | `0.33.5` | Build verification | in-session | done |

### Batch 33.1 — StateCache: VU meters, playing notes, isMutedBySolo

**Delegation:** in-session
**Decisions:** D-1.2, D-1.3, D-1.4
**Files:**
- `gig-maestro/src/main/java/dev/gregross/gig/extension/StateCache.java` (MODIFY)
**Work:**
1. Add fields:
   - `int[] trackVuMeter = new int[TRACK_COUNT]` — RMS sum level (0-127)
   - `int[][] trackPlayingNotes` — jagged array, each track has a variable-size `int[]` of `{pitch, velocity}` pairs (or store as snapshot of PlayingNote data)
   - `boolean[] trackMutedBySolo = new boolean[TRACK_COUNT]`
2. In `registerTrackObservers()` for each track:
   - `track.addVuMeterObserver(128, -1, false, v -> trackVuMeter[idx] = v)` — RMS sum
   - `track.playingNotes().markInterested()` + observer that snapshots current notes into a cached array
   - `track.isMutedBySolo().markInterested()` + observer
3. Add `isMutedBySolo` to the track snapshot JSON object (alongside mute/solo/arm)
4. Add public getter methods for VU meters and playing notes (for TrackHandler to read)
**Test criteria:** Unit tests in batch 33.3
**Acceptance:** All three data sources cached, isMutedBySolo in snapshot

### Batch 33.2 — TrackHandler: getVuMeters + getPlayingNotes RPCs

**Delegation:** in-session
**Decisions:** D-1.5
**Depends on:** Batch 33.1
**Files:**
- `gig-maestro/src/main/java/dev/gregross/gig/handlers/TrackHandler.java` (MODIFY)
**Work:**
1. Add `track/getVuMeters` — no params, returns JSON array of 8 integers (0-127 RMS per track). Reads from StateCache getter.
2. Add `track/getPlayingNotes` — params: `{index: int}`, returns JSON array of `{pitch: int, velocity: int}` objects for the specified track. Reads from StateCache getter.
**Test criteria:** Unit tests in batch 33.3
**Acceptance:** 2 new RPC methods registered and returning correct data

### Batch 33.3 — Unit tests

**Delegation:** in-session
**Depends on:** Batch 33.1, 33.2
**Files:**
- `gig-maestro/src/test/java/dev/gregross/gig/handlers/TrackHandlerTest.java` (MODIFY)
- `gig-maestro/src/test/java/dev/gregross/gig/extension/StateCacheSnapshotTest.java` (MODIFY)
**Work:**
1. TrackHandlerTest: add registration test for 2 new methods, update method count, behavioral test for getVuMeters and getPlayingNotes
2. StateCacheSnapshotTest: verify isMutedBySolo appears in track snapshot
**Test criteria:** `./gradlew :gig-maestro:test` passes
**Acceptance:** All new methods have test coverage

### Batch 33.4 — Tool definitions + system prompt + smoke tests

**Delegation:** in-session
**Depends on:** Batch 33.3
**Files:**
- `gig-maestro/tools/claude-tools.json` (MODIFY)
- `gig-maestro/tools/system-prompt.md` (MODIFY)
- `gig-maestro/scripts/tests/offline-schemas.sh` (MODIFY)
**Work:**
1. Add tool definitions for `track_getVuMeters` and `track_getPlayingNotes`
2. Update session_snapshot description to mention isMutedBySolo
3. Update system prompt with Activity Feedback section covering VU meters, playing notes, and muted-by-solo
4. Add smoke test assertions
**Test criteria:** `gig-maestro/scripts/smoke-test.sh --offline` passes
**Acceptance:** 2 new tools in schema, documented in prompt, validated by smoke tests

### Batch 33.5 — Build verification

**Delegation:** in-session
**Depends on:** Batch 33.4
**Files:** None
**Work:** Run `./gradlew :gig-maestro:shadowJar` and `./gradlew :gig-maestro:test`
**Test criteria:** Both commands exit 0
**Acceptance:** Extension builds and all tests pass

**Phase Acceptance Criteria:**
- [ ] VU meter values cached per track (0-127 RMS sum)
- [ ] Playing notes cached per track via observer
- [ ] isMutedBySolo in snapshot and delta per track
- [ ] track/getVuMeters returns all 8 meter values
- [ ] track/getPlayingNotes returns notes for a specific track
- [ ] All unit tests pass
- [ ] Tool schemas and system prompt updated
- [ ] Offline smoke tests pass
- [ ] Clean build with shadowJar

**Completion triggers Phase 34 -> version `0.34.0`**
