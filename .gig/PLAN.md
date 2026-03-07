# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 1 — Clip Launcher (v0.1.x)

> Build a functional Bitwig controller extension for the Novation Launchpad MK2 that provides clip launching on the 8x8 grid, scene launching on the right-side buttons, and navigation/transport on the top row. LED feedback reflects clip state using the MK2's RGB color palette.

**Decisions:** D-1.1, D-1.2, D-1.3, D-1.4, D-1.5, D-1.6, D-1.7

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 1.1 | `0.1.1` | Gradle project scaffold + extension definition | in-session | pending |
| 1.2 | `0.1.2` | LED colors constants + SysEx helpers | in-session | pending |
| 1.3 | `0.1.3` | Clip launcher grid (8x8 pads + LED feedback) | in-session | pending |
| 1.4 | `0.1.4` | Scene launch buttons + top row navigation/transport | in-session | pending |
| 1.5 | `0.1.5` | Integration testing + polish | in-session | pending |

### Batch 1.1 — Gradle project scaffold + extension definition

**Delegation:** in-session
**Decisions:** D-1.1, D-1.2, D-1.7
**Files:**
- `build.gradle`
- `settings.gradle`
- `gradle.properties`
- `src/main/java/com/gregross/bitwig/launchpadmk2/LaunchpadMk2ExtensionDefinition.java`
- `src/main/java/com/gregross/bitwig/launchpadmk2/LaunchpadMk2Extension.java` (skeleton)
**Work:**
- Create Gradle build with `java` plugin, `com.bitwig:extension-api:22` dependency from `https://maven.bitwig.com/`
- Java 21 source compatibility, output JAR named `LaunchpadMk2.bwextension`
- Gradle task to copy `.bwextension` to `~/Documents/Bitwig Studio/Extensions/`
- `LaunchpadMk2ExtensionDefinition`: vendor "Novation", name "Launchpad MK2", 1 MIDI in + 1 MIDI out, auto-detect via SysEx device inquiry
- `LaunchpadMk2Extension`: empty `init()`/`exit()`/`flush()` skeleton
**Test criteria:** `./gradlew build` succeeds; `.bwextension` file is produced in `build/` output
**Acceptance:** Project compiles and produces a valid `.bwextension` file

### Batch 1.2 — LED colors constants + SysEx helpers

**Delegation:** in-session
**Decisions:** D-1.6, D-1.2
**Files:**
- `src/main/java/com/gregross/bitwig/launchpadmk2/LaunchpadMk2Colors.java`
**Work:**
- Define velocity constants for all clip states (off, stopped, playing, recording, queued, armed)
- Define top-row and scene-launch LED colors
- SysEx byte array helpers: LED set via note-on, LED pulse mode (`F0 00 20 29 02 18 28 <led> <color> F7`), reset all LEDs (`F0 00 20 29 02 18 0E 00 F7`)
- MIDI note mapping helpers: `gridNote(row, col)`, `sceneLaunchNote(row)`, top row CC constants
**Test criteria:** Project compiles; constants are referenced without errors
**Acceptance:** All LED color constants and MIDI mapping helpers defined and compiling

### Batch 1.3 — Clip launcher grid (8x8 pads + LED feedback)

**Delegation:** in-session
**Decisions:** D-1.3, D-1.6
**Depends on:** Batch 1.1, Batch 1.2
**Files:**
- `src/main/java/com/gregross/bitwig/launchpadmk2/LaunchpadMk2Extension.java` (modify)
**Work:**
- In `init()`: create `TrackBank` (8 tracks, 0 sends, 8 scenes) and `SceneBank` (8 scenes)
- Set up MIDI input callback for note-on messages on grid pads (notes 11-88)
- On pad press: toggle clip playback at `(track, scene)` position
- Set up clip state observers for each slot in the 8x8 grid
- In `flush()`: update grid LEDs based on clip state — send note-on with velocity from `LaunchpadMk2Colors` for each pad
- Handle states: empty, has-content/stopped, playing, recording, queued
- On `exit()`: send SysEx reset to turn off all LEDs
**Test criteria:** Load extension in Bitwig; pressing a grid pad triggers/stops the corresponding clip; LEDs update to reflect clip state
**Acceptance:** 8x8 grid fully functional as clip launcher with correct LED feedback

### Batch 1.4 — Scene launch buttons + top row navigation/transport

**Delegation:** in-session
**Decisions:** D-1.4, D-1.5
**Depends on:** Batch 1.3
**Files:**
- `src/main/java/com/gregross/bitwig/launchpadmk2/LaunchpadMk2Extension.java` (modify)
**Work:**
- Scene launch: MIDI input callback for right-side notes (19, 29, ..., 89) — launch corresponding scene. LED feedback for scene state.
- Top row navigation (CC 104-107): Up/Down scroll scene bank by 8, Left/Right scroll track bank by 8. LED feedback: lit when scrolling is possible in that direction.
- Reserved buttons (CC 108-111): Session lit as active mode indicator; User1/User2/Mixer unlit, no-op.
- Update `flush()` to send LED states for scene launch and top row buttons.
**Test criteria:** Scene launch buttons trigger full scenes; arrow buttons scroll the visible 8x8 window; LEDs update correctly
**Acceptance:** All 96 buttons (64 grid + 8 scene + 8 top) are functional with correct LED feedback

### Batch 1.5 — Integration testing + polish

**Delegation:** in-session
**Decisions:** All
**Depends on:** Batch 1.4
**Files:**
- `src/main/java/com/gregross/bitwig/launchpadmk2/LaunchpadMk2Extension.java` (modify)
- `README.md`
**Work:**
- Full end-to-end testing in Bitwig Studio
- Fix any LED update timing issues (ensure `flush()` is efficient)
- Add SysEx device inquiry response for auto-detection in Bitwig's controller list
- Write README with: setup instructions, button map diagram, build/install steps
- Ensure clean LED reset on extension exit
**Test criteria:** Extension appears in Bitwig controller list; auto-detects MK2; all functions work without errors in Bitwig console
**Acceptance:** Complete, polished extension ready for daily use

**Phase Acceptance Criteria:**
- [ ] `./gradlew build` produces a valid `.bwextension`
- [ ] Extension loads in Bitwig and auto-detects Launchpad MK2
- [ ] 8x8 grid launches/stops clips with correct LED feedback
- [ ] Scene launch buttons work for all 8 visible scenes
- [ ] Arrow buttons scroll track/scene bank with LED indicators
- [ ] LEDs reset cleanly on extension exit
- [ ] README documents setup and button mapping

**Completion triggers Phase 2 -> version `0.2.0`**

---

## Plan Amendments

<!-- Log any changes to the plan after creation -->

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| — | — | — | — |
