# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

**Phase 21 — Song Rebuild from JSON**
**Branch:** `feature/v0.21-song-rebuild`
**Status:** IMPLEMENTING

---

### Batch 1 — SongCommand CLI: `gig song dump` (v0.21.1)
**Delegation:** in-session
**Decisions:** D-21.3, D-21.5, D-21.6r
**Status:** pending

Add `SongCommand` to CLI with `dump` subcommand. CLI drives multi-step RPC:
1. Call `session/snapshot` — extract transport, tracks, scenes, master
2. Identify clips with content from snapshot's track slot data
3. For each clip: `clip/select` → sleep 200ms → `clip/getNotes` → store notes
4. Call `device/getDrumPads` if applicable
5. Assemble JSON with `meta.formatVersion: "1"`, `lengthBeats`, `stepSize`, `color`, `name` per clip

Options:
- `gig song dump` — write to stdout
- `gig song dump --output <file>` — write to file

**Files:**
- `src/cli/java/dev/gregross/gig/cli/SongCommand.java` — new file
- `src/cli/java/dev/gregross/gig/cli/GigCli.java` — register SongCommand

**Test criteria:**
- Compiles: `./gradlew cliShadowJar`
- `java -jar build/libs/gig-cli.jar song dump --help` works

---

### Batch 2 — SongCommand CLI: `gig song rebuild` (v0.21.2)
**Delegation:** in-session
**Decisions:** D-21.1, D-21.2, D-21.4
**Status:** pending

Add `rebuild` subcommand to `SongCommand`:
- `gig song rebuild <file>` — reads JSON, validates format version, executes rebuild phases

Rebuild phases (sequential with delays per D-21.4):
1. **Transport** — set tempo, time signature
2. **Scenes** — call `macro/setupScenes` with scene names, then set colors (500ms settle)
3. **Clips** — for each clip: call `macro/writeClip` with notes (200ms between)
4. **Clip colors** — select each clip, set color (50ms between)
5. **Track mix** — set volume, pan, mute, solo, color per track
6. **Master mix** — set volume, pan
7. **Cue markers** — for each marker: set transport position, add at playhead, rename (200ms between)

Progress logging to stdout.

**Files:**
- `src/cli/java/dev/gregross/gig/cli/SongCommand.java` — add rebuild subcommand

**Test criteria:**
- Compiles: `./gradlew cliShadowJar`
- `java -jar build/libs/gig-cli.jar song rebuild --help` works

---

### Batch 3 — Unit tests (v0.21.3)
**Delegation:** in-session
**Status:** pending

- SongCommand test: CLI registration, help output

**Files:**
- `src/test/java/dev/gregross/gig/cli/` (tests)

**Test criteria:**
- `./gradlew test` passes

---

### Batch 4 — Tool schemas + system prompt update (v0.21.4)
**Delegation:** in-session
**Status:** pending

- Update `system-prompt.md` with song dump/rebuild CLI workflow documentation

**Files:**
- `tools/system-prompt.md`

**Test criteria:**
- Content review

---

### Batch 5 — Smoke tests (v0.21.5)
**Delegation:** in-session
**Status:** pending

- Offline: CLI help for song subcommands
- Online: full dump→rebuild round-trip test

**Files:**
- `scripts/smoke-test.sh`

**Test criteria:**
- `./scripts/smoke-test.sh --offline` passes

---

### Acceptance Criteria

- [ ] `gig song dump` exports a complete song JSON with `formatVersion: "1"`
- [ ] `gig song rebuild <file>` restores transport, scenes, clips+notes, colors, mix, cue markers
- [ ] Round-trip: dump → clear project → rebuild produces matching snapshot
- [ ] All existing tests still pass
- [ ] CLI `--help` documents both subcommands
