# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

**Phase 21 — Song Rebuild from JSON**
**Branch:** `feature/v0.21-song-rebuild`
**Status:** PLANNED

---

### Batch 1 — `macro/dumpSong` RPC method (v0.21.1)
**Delegation:** in-session
**Decisions:** D-21.5, D-21.6

Add `macro/dumpSong` to MacroHandler. This method:
1. Reads snapshot for transport, tracks, master, scenes
2. Iterates each track × scene slot, selects clips with content, reads notes via `clip/getNotes`
3. Captures per-clip metadata: `lengthBeats` (from `loopLength`), `stepSize`, `color`, `name`
4. Reads cue markers from arranger snapshot
5. Reads instrument info (device name + preset) per track
6. Reads drum pad map if current device has drum pads
7. Assembles full JSON with `meta.formatVersion: "1"`
8. Returns the complete JSON in a single RPC response

Because clip reads require cursor settle (async), uses deferred scheduling internally:
- Phase 1: capture snapshot sections (synchronous)
- Phase 2+: select each clip, wait for cursor settle, read notes (deferred at FLUSH_DELAY_MS intervals)
- Final: assemble and return via a callback/future pattern

**Note:** This is the most complex method — needs a response mechanism for deferred results. Options: (a) block the RPC thread with a CountDownLatch, (b) return a job ID and add a `macro/dumpSongResult` polling method, or (c) accept that it takes N seconds and have the CLI poll. Will use option (a) — block with CountDownLatch + timeout.

**Files:**
- `src/main/java/dev/gregross/gig/handlers/MacroHandler.java` — add `macro/dumpSong`
- `src/main/java/dev/gregross/gig/extension/GigMaestroExtension.java` — pass additional deps to MacroHandler if needed

**Test criteria:**
- Unit test: method registered, parameter validation
- Smoke test (online): call `macro/dumpSong`, verify JSON has all expected sections

---

### Batch 2 — SongCommand CLI: `gig song dump` (v0.21.2)
**Delegation:** in-session
**Decisions:** D-21.3

Add `SongCommand` to CLI with `dump` subcommand:
- `gig song dump` — calls `macro/dumpSong`, writes JSON to stdout (or `--output <file>`)
- `gig song dump --output songs/my-song.json` — writes to file
- Validates response has `meta.formatVersion`
- Pretty-prints by default

**Files:**
- `src/cli/java/dev/gregross/gig/cli/SongCommand.java` — new file
- `src/cli/java/dev/gregross/gig/cli/GigCli.java` — register SongCommand

**Test criteria:**
- Unit test: command registered in CLI
- Smoke test (offline): `gig song dump --help` works

---

### Batch 3 — SongCommand CLI: `gig song rebuild` (v0.21.3)
**Delegation:** in-session
**Decisions:** D-21.1, D-21.2, D-21.3, D-21.4

Add `rebuild` subcommand to `SongCommand`:
- `gig song rebuild <file>` — reads JSON, validates format version, executes rebuild phases

Rebuild phases (sequential with delays per D-21.4):
1. **Transport** — set tempo, time signature
2. **Scenes** — call `macro/setupScenes` with scene names, then set colors (500ms settle)
3. **Clips** — for each clip: call `macro/writeClip` with notes (200ms between clips)
4. **Clip colors** — select each clip, set color (50ms between)
5. **Track mix** — set volume, pan, mute, solo, color per track
6. **Master mix** — set volume, pan
7. **Cue markers** — for each marker: set transport position, add at playhead, rename (200ms between)

Progress logging to stdout:
```
[1/7] Setting transport: 92 BPM, 4/4
[2/7] Creating 9 scenes...
[3/7] Writing 36 clips...
  [3.1] Drums / Intro (20 notes)
  [3.2] Bass / Intro (12 notes)
  ...
[4/7] Setting clip colors...
[5/7] Mixing tracks...
[6/7] Setting master...
[7/7] Adding 17 cue markers...
Done! Song rebuilt in 18.3s
```

**Files:**
- `src/cli/java/dev/gregross/gig/cli/SongCommand.java` — add rebuild subcommand

**Test criteria:**
- Smoke test (offline): `gig song rebuild --help` works, validates JSON format
- Smoke test (online): rebuild from saved JSON, verify snapshot matches

---

### Batch 4 — Unit tests (v0.21.4)
**Delegation:** in-session

- MacroHandler test: `macro/dumpSong` registered, method count updated
- SongCommand test: CLI registration, help output

**Files:**
- `src/test/java/dev/gregross/gig/handlers/MacroHandlerTest.java`
- `src/test/java/dev/gregross/gig/cli/SongCommandTest.java` (new if needed)

**Test criteria:**
- `./gradlew test` passes

---

### Batch 5 — Tool schemas + system prompt update (v0.21.5)
**Delegation:** in-session

- Add `macro_dumpSong` tool schema to `claude-tools.json`
- Update `system-prompt.md` with song dump/rebuild workflow documentation

**Files:**
- `tools/claude-tools.json`
- `tools/system-prompt.md`

**Test criteria:**
- Smoke test (offline): schema validates, no duplicate tool names

---

### Batch 6 — Smoke tests (v0.21.6)
**Delegation:** in-session

- Offline: schema count, CLI help for song subcommands
- Online: full dump→rebuild round-trip test

**Files:**
- `scripts/smoke-test.sh`

**Test criteria:**
- `./scripts/smoke-test.sh --offline` passes
- `./scripts/smoke-test.sh` passes (with Bitwig running)

---

### Acceptance Criteria

- [ ] `gig song dump` exports a complete song JSON with `formatVersion: "1"`
- [ ] `gig song rebuild <file>` restores transport, scenes, clips+notes, colors, mix, cue markers
- [ ] Round-trip: dump → clear project → rebuild produces matching snapshot (transport, tracks, scenes, clip note counts)
- [ ] `macro/dumpSong` RPC method works standalone (curl)
- [ ] All existing tests still pass
- [ ] CLI `--help` documents both subcommands
