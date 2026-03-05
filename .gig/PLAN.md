# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 23 — NoteInput & Arpeggiator (v0.23.x)

> Create a NoteInput in the extension for real-time MIDI injection, expose arpeggiator control (17 modes, rate, gate length, humanize, etc.) and note latch control via RPC. Add arpeggiator and note latch state to the session snapshot. 10 new RPC methods total.

**Decisions:** D-23.1, D-23.2, D-23.3, D-23.4, D-23.5

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 23.1 | `0.23.1` | NoteInput infrastructure + snapshot observers | in-session | done |
| 23.2 | `0.23.2` | NoteInputHandler — 10 RPC methods | in-session | done |
| 23.3 | `0.23.3` | Unit tests | in-session | pending |
| 23.4 | `0.23.4` | Tool schemas + system prompt update | in-session | pending |
| 23.5 | `0.23.5` | Smoke tests | in-session | pending |

---

### Batch 23.1 — NoteInput infrastructure + snapshot observers

**Delegation:** in-session
**Decisions:** D-23.2, D-23.4
**Files:** `src/main/java/dev/gregross/gig/extension/GigMaestroExtension.java`, `src/main/java/dev/gregross/gig/extension/StateCache.java`
**Work:**
1. In `GigMaestroExtension.init()`, create a NoteInput: `host.createNoteInput("Gig Maestro")` (no MIDI mask — accepts all).
2. Get arpeggiator and noteLatch references from the NoteInput.
3. In `StateCache`, add `registerNoteInputObservers(Arpeggiator, NoteLatch)` to observe all settable properties:
   - Arpeggiator: mode, octaves, rate, gateLength, shuffle, humanize, isEnabled, isFreeRunning, enableOverlappingNotes, usePressureToVelocity, terminateNotesImmediately (11 props)
   - NoteLatch: isEnabled, mode, mono, velocityThreshold, activeNotes (5 props)
4. Add `arpeggiator` and `noteLatch` sections to `getSnapshot()`.

**Test criteria:** `./gradlew shadowJar` compiles. Extension loads in Bitwig. Snapshot includes `arpeggiator` and `noteLatch` sections.
**Acceptance:** NoteInput created at init, arpeggiator and noteLatch state visible in snapshot.

---

### Batch 23.2 — NoteInputHandler — 10 RPC methods

**Delegation:** in-session (depends on 23.1 — needs NoteInput/Arpeggiator/NoteLatch references)
**Decisions:** D-23.3, D-23.5
**Files:** `src/main/java/dev/gregross/gig/handlers/NoteInputHandler.java`, `src/main/java/dev/gregross/gig/extension/GigMaestroExtension.java`
**Work:**
1. Create `NoteInputHandler` class with constructor `(NoteInput, Arpeggiator, NoteLatch)`.
2. Register 10 RPC methods:
   - `noteInput/sendNote` — `{note, velocity, channel?}`, velocity=0 → note-off. Constructs status byte: `0x90|channel` for note-on, `0x80|channel` for note-off.
   - `noteInput/sendMidi` — `{status, data0, data1}`, raw passthrough.
   - `arpeggiator/configure` — batch-set optional props: mode, octaves, rate, gateLength, shuffle, humanize, isFreeRunning, enableOverlappingNotes, usePressureToVelocity, terminateNotesImmediately.
   - `arpeggiator/setEnabled` — `{enabled}`.
   - `arpeggiator/releaseNotes` — no params.
   - `arpeggiator/getState` — returns all arp properties as JSON.
   - `noteLatch/configure` — batch-set optional props: mode, mono, velocityThreshold.
   - `noteLatch/setEnabled` — `{enabled}`.
   - `noteLatch/releaseNotes` — no params.
   - `noteLatch/getState` — returns all latch properties as JSON.
3. Wire handler in extension init: `new NoteInputHandler(noteInput, arpeggiator, noteLatch).register(dispatcher)`.
4. Validate arpeggiator mode against 17 valid strings (case-insensitive).

**Test criteria:** `./gradlew shadowJar` compiles. All 10 methods respond correctly in Bitwig.
**Acceptance:** 10 new RPC methods registered and functional.

---

### Batch 23.3 — Unit tests

**Delegation:** in-session (depends on 23.2)
**Decisions:** D-23.1, D-23.3
**Files:** `src/test/java/dev/gregross/gig/handlers/NoteInputHandlerTest.java`
**Work:**
1. Test registration of all 10 methods.
2. Test total method count.
3. Test arpeggiator mode validation — all 17 valid modes.

**Test criteria:** `./gradlew test` passes all new tests.
**Acceptance:** 8+ new unit tests passing.

---

### Batch 23.4 — Tool schemas + system prompt update

**Delegation:** in-session (depends on 23.2)
**Decisions:** D-23.3, D-23.5
**Files:** `tools/claude-tools.json`, `tools/system-prompt.md`
**Work:**
1. Add 10 tool schemas: `noteInput_sendNote`, `noteInput_sendMidi`, `arpeggiator_configure`, `arpeggiator_setEnabled`, `arpeggiator_releaseNotes`, `arpeggiator_getState`, `noteLatch_configure`, `noteLatch_setEnabled`, `noteLatch_releaseNotes`, `noteLatch_getState`.
2. Update `session_snapshot` schema description to mention arpeggiator and noteLatch sections.
3. Add "NoteInput & Arpeggiator" section to system prompt with mode list, property ranges, and workflow guidance.

**Test criteria:** `jq . tools/claude-tools.json` validates. System prompt mentions arpeggiator modes.
**Acceptance:** 10 new schemas + updated snapshot description + system prompt section.

---

### Batch 23.5 — Smoke tests

**Delegation:** in-session (depends on 23.4)
**Decisions:** D-23.1, D-23.3
**Files:** `scripts/smoke-test.sh`
**Work:**
1. Schema validation: 10 new tool schemas present.
2. Schema validation: arpeggiator_configure has mode enum.
3. Schema validation: noteInput_sendNote has note and velocity params.
4. System prompt mentions NoteInput & Arpeggiator section.
5. Snapshot schema description mentions arpeggiator.

**Test criteria:** `./scripts/smoke-test.sh --offline` passes all new assertions.
**Acceptance:** 10+ new smoke assertions passing.

---

**Phase Acceptance Criteria:**
- [ ] NoteInput created in extension init (appears in Bitwig track input choosers)
- [ ] `noteInput/sendNote` plays notes in Bitwig in real-time
- [ ] `noteInput/sendMidi` sends raw MIDI events
- [ ] `arpeggiator/configure` batch-sets mode, octaves, rate, gateLength, etc.
- [ ] `arpeggiator/setEnabled` enables/disables the arpeggiator
- [ ] `arpeggiator/releaseNotes` releases held arp notes
- [ ] `noteLatch/configure` sets mode, mono, velocityThreshold
- [ ] `noteLatch/setEnabled` enables/disables note latch
- [ ] Session snapshot includes `arpeggiator` and `noteLatch` sections
- [ ] All 17 arpeggiator modes validated (case-insensitive)
- [ ] All unit tests pass (`./gradlew test`)
- [ ] All smoke tests pass (`./scripts/smoke-test.sh --offline`)
- [ ] 10 new tool schemas in claude-tools.json
- [ ] System prompt documents NoteInput & Arpeggiator

**Completion triggers Phase 24 → version `0.24.0`**
