# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 29 — Song Dump Fidelity (v0.29.x)

> Extend the CLI song dump/rebuild to capture and restore device parameters (via discovery) and clip launch/playback settings. All changes are in `SongCommand.java` — the extension already has all needed RPC methods.

**Decisions:** D-29.1, D-29.2, D-29.3, D-29.4, D-29.5

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 29.1 | `0.29.1` | Dump: device parameters + clip settings | in-session | pending |
| 29.2 | `0.29.2` | Rebuild: device parameters + clip settings | in-session | pending |
| 29.3 | `0.29.3` | Build verification | in-session | pending |

---

### Batch 29.1 — Dump: device parameters + clip settings

**Decisions:** D-29.1, D-29.2
**Files:** `SongCommand.java` (DumpCommand)
**Work:**

**Device parameters (Step 4 — instrument reading, ~line 185):**
- After reading device name/preset for each track, call `device/discoverAll` (async — returns immediately, discovery runs in background)
- Wait `CLIP_SETTLE_MS` for discovery to complete
- Call `device/getDiscoveryResult` with `{format: "preset"}` to get `{pages: [{pageIndex, params: [{index, value}]}]}`
- Add the `pages` array to the instrument entry (alongside existing `device` and `preset` fields)
- If discovery returns no pages (e.g., device has no remote controls), omit `pages`

**Clip launch/playback settings (Step 3 — clip reading, ~line 157):**
- After reading clip snapshot, extract launch and playback settings from the snapshot
- The snapshot already has: `launchQuantization`, `launchMode`, `shuffle`, `accent`, `useLoopStartAsQuantizationReference`, `playStart`, `playStop`, `loopStart`, `loopLength`, `isLoopEnabled`
- Add `launchSettings` object to clip entry (only if non-default: quantization != "default", mode != "default", shuffle != false, accent != 0.0)
- Add `playbackSettings` object to clip entry (only if non-default: playStart != 0, loopEnabled != true as it usually is)
- Keep defaults compact — skip trivial settings that Bitwig already applies

**Test criteria:** `./gradlew :gig-maestro:cliShadowJar` compiles successfully

---

### Batch 29.2 — Rebuild: device parameters + clip settings

**Decisions:** D-29.3, D-29.4, D-29.5
**Files:** `SongCommand.java` (RebuildCommand)
**Work:**

**Device parameters (after Step 5 — track mix, or new step):**
- After all tracks are set up, iterate instruments array
- For each instrument with a `pages` array:
  - Select the track: `track/select`
  - Wait for cursor to settle
  - Call `device/setParameters` with the `pages` array
  - Wait for parameter writes to complete (100ms per page)
- Log instrument parameters being restored
- Update "manual steps" message: if `pages` is present, don't list as manual step (params are auto-restored)

**Clip launch/playback settings (in Step 3 — clip writing, after macro/writeClip):**
- After each clip's notes are written, if `launchSettings` is present:
  - `clip/setLaunchQuantization` (if `launchQuantization` present)
  - `clip/setLaunchMode` (if `launchMode` present)
  - `clip/setShuffle` (if `shuffle` present)
  - `clip/setAccent` (if `accent` present)
  - `clip/setUseLoopStartAsQuantizationReference` (if present)
- If `playbackSettings` is present:
  - `clip/setPlayStart`, `clip/setPlayStop`, `clip/setLoopStart`, `clip/setLoopLength`, `clip/setLoopEnabled` (each if present)
- All optional — old dump files without these fields skip gracefully

**Bump total steps from 7 to 8** (new step for device parameter restoration)

**Test criteria:** `./gradlew :gig-maestro:cliShadowJar` compiles successfully

---

### Batch 29.3 — Build verification

**Steps:**
- `./gradlew :gig-maestro:test` — all unit tests still pass (no regression)
- `./gradlew :gig-maestro:cliShadowJar` — CLI builds
- `./gradlew :gig-maestro:shadowJar` — extension builds
- `gig-maestro/scripts/smoke-test.sh --offline` — all offline smoke tests pass

---

**Phase Acceptance Criteria:**
- [ ] Dump captures device parameter pages (preset format) per instrument
- [ ] Dump captures clip launch settings (non-default only)
- [ ] Dump captures clip playback settings (non-default only)
- [ ] Rebuild restores device parameters via `device/setParameters`
- [ ] Rebuild restores clip launch/playback settings
- [ ] Backward compatible — old dumps without new fields rebuild without errors
- [ ] All builds pass, no regressions

**Completion triggers Phase 30 → version `0.30.0`**
