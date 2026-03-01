# Issues

> Discovered bugs, limitations, and workflow problems. Each entry tracks status and root cause.

---

## Open Issues

### ~~ISS-001: Notes truncated beyond 64-step grid viewport~~ → RESOLVED

Moved to [Resolved Issues](#resolved-issues).

---

### ISS-002: `macro/buildSection` fails after bulk scene deletion

**Severity:** Medium
**Discovered:** 2026-03-01 (song writing test)
**Affects:** `macro/buildSection`

**Symptom:** After deleting all scenes and immediately calling `buildSection`, the scenes are created but clips have `loopLength=0.0` and no content. Scene names may not stick. Manual scene creation + individual `writeClip` calls work correctly.

**Root cause:** After bulk scene deletion, Bitwig auto-creates empty default scenes. `stateCache.getSceneItemCount()` may return stale values (0 when defaults exist, or old count before deletion). The `buildSection` logic for calculating `newSceneBankIndex` depends on accurate scene counts. Stale cache + race between Bitwig auto-creation and macro execution causes clips to target wrong scene slots.

**Workaround:** Create scenes manually with `scene/create` + `scene/rename`, wait for observer sync, then use `macro/writeClip` per clip.

**Possible fixes:**
1. Add a `requestFlush()` + delay before reading scene count in `buildSection`
2. Have `buildSection` verify the scene was created by checking the bank state after creation
3. Accept a `sceneIndex` param instead of always appending (skip the create + scroll logic)

---

### ISS-003: `clip/getNotes` requires manual cursor settle delay

**Severity:** Low
**Discovered:** 2026-03-01 (live testing)
**Affects:** `clip/getNotes`, any read-after-select workflow

**Symptom:** After `clip/select`, calling `clip/getNotes` immediately returns empty or stale data. A 1-2 second wait is needed for the cursor clip to settle on the new clip and load its step data.

**Root cause:** `clip/select` calls `slot.select()` which triggers async cursor movement. The cursor clip's step data observer fires incrementally as data loads from the engine. `getNotes` reads from the live grid (`cursorClip.getStep()`), which reflects whatever has loaded so far.

**Impact:** Agent workflows that write then verify notes need artificial delays. Not a correctness issue for writing (deferred writes handle this), but makes read-back verification unreliable without waiting.

**Possible fixes:**
1. Add a "cursor settled" flag in StateCache that tracks when step data observer has received a full refresh
2. Have `getNotes` return a warning if cursor recently changed
3. Document the delay requirement in tool schema descriptions

---

### ISS-004: `track/select` cursor mapping doesn't match track bank indices

**Severity:** Low
**Discovered:** 2026-03-01 (device insertion)
**Affects:** `track/select`, `device/insertBitwigDevice`

**Symptom:** `track/select` with `index: 0` reported `cursorTrackName: "Arp"` instead of "Pad". Subsequent device insertions may land on wrong tracks.

**Root cause:** `track/select` navigates the CursorTrack, which follows its own selection order (possibly most-recently-selected). The track bank index and cursor track selection are loosely coupled — selecting bank index 0 doesn't guarantee the cursor moves to the first track.

**Workaround:** Verify `cursorTrackName` in the response and retry if wrong. Or use `macro/createTrack` with `device` param which handles device insertion during track creation.

**Impact:** Low — devices were inserted (all 4 tracks got instruments), just potentially on wrong tracks. User can reassign in Bitwig UI.

---

### ISS-005: Deferred macro writes are fire-and-forget (no error reporting)

**Severity:** Medium
**Discovered:** 2026-03-01 (architecture review)
**Affects:** `macro/writeClip`, `macro/buildSection`

**Symptom:** If a deferred note write fails (e.g., cursor didn't follow, invalid params), the error is silently swallowed. The RPC response already returned an optimistic `count` value that may be wrong.

**Root cause:** `host.scheduleTask()` callbacks can't propagate errors back to the original RPC response (it's already sent). The `catch` block in the scheduled task only logs — there's no feedback channel.

**Impact:** Agent thinks notes were written successfully when they may not have been. Verification requires a follow-up `clip/getNotes` call with delay.

**Possible fixes:**
1. Add a `macro/verify` method that checks the last macro's deferred writes completed
2. Emit a WebSocket notification when deferred writes complete (success or failure)
3. Track pending deferred writes in StateCache with completion status
4. Document in tool schemas that `count` is optimistic and verification is recommended

---

## Resolved Issues

### ISS-001: Notes truncated beyond 64-step grid viewport

**Severity:** Critical → **Resolved:** 2026-03-01
**Fix:** Increased `CLIP_GRID_WIDTH` from 64 to 256 in `GigMaestroExtension.java` and `GRID_WIDTH` in `NoteHandler.java`. Grid now covers 256 steps — at `stepSize=0.25` that's 64 beats (16 bars). Also added x/y bounds validation in `clip/setNotes` so out-of-range notes throw an error instead of being silently dropped.
**Files:** `GigMaestroExtension.java`, `NoteHandler.java`
