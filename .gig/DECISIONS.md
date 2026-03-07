# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

<!-- Decision statuses:
  PROPOSED  — Claude's recommendation, awaiting user approval
  ACTIVE    — Approved and in effect
  AMENDED   — Overridden by user (original preserved, new entry appended)
  REVISED   — Claude revised based on new information (original preserved)
-->

<!-- Entry format:
## YYYY-MM-DD — Domain: Question

**Decision:** What was decided.
**Rationale:** Why this choice was made.
**Alternatives considered:** What else was evaluated.
**Status:** PROPOSED | ACTIVE | AMENDED | REVISED
**ID:** D-{batch}.{num}
-->

## 2026-03-06 — Build: What language and build system?

**Decision:** Java with Gradle, standalone project. Use `com.bitwig:extension-api:22` from `https://maven.bitwig.com/`. Java 21 source compatibility. Output: `.bwextension` JAR deployed to `~/Documents/Bitwig Studio/Extensions/`.
**Rationale:** Java is the standard for Bitwig `.bwextension` files. Gradle is what the official bitwig-extensions repo uses and is more concise than Maven. Standalone project keeps things self-contained. API version 22 matches current Bitwig releases.
**Alternatives considered:** Maven — works but more verbose; JavaScript (`.control.js`) — less type safety and IDE support; Kotlin — adds complexity for no clear benefit on a small project.
**Status:** ACTIVE
**ID:** D-1.1

## 2026-03-06 — Hardware: What MIDI layout mode to use?

**Decision:** Session layout mode. The Launchpad MK2 uses a decimal grid: pad note = `(row+1)*10 + (col+1)` where row 0-7 (bottom to top), col 0-7 (left to right). Grid pads: notes 11-18, 21-28, ..., 81-88. Right-side buttons: notes 19, 29, 39, 49, 59, 69, 79, 89. Top row buttons: CC 104-111.
**Rationale:** Session layout is the default and most logical mode for clip launching. The decimal grid numbering is clean and easy to compute. The MK2 is USB MIDI class compliant (no special drivers needed).
**Alternatives considered:** User 1/User 2 custom modes — unnecessary complexity when session mode fits perfectly.
**Status:** ACTIVE
**ID:** D-1.2

## 2026-03-06 — Function: What is the primary purpose of the 8x8 grid?

**Decision:** Clip launcher. The 8x8 grid maps to 8 tracks x 8 scenes in Bitwig's clip launcher. Each pad triggers/stops a clip slot. LED color reflects clip state (empty, has content, playing, recording, queued).
**Rationale:** The Launchpad was literally designed for clip launching — the 8x8 grid maps 1:1 to Bitwig's clip launcher grid. This is the most natural and useful mapping for a "dedicated controller" approach. Keeps it simple and immediately useful.
**Alternatives considered:** Drum sequencer — useful but more complex; mixer control — poor fit for a grid of identical buttons; note input — MK2 pads are not velocity-sensitive.
**Status:** ACTIVE
**ID:** D-1.3

## 2026-03-06 — Function: What do the top row buttons control?

**Decision:** Transport and navigation. Layout (CC 104-111): [Up] [Down] [Left] [Right] [Session] [User1] [User2] [Mixer]. Up/Down scroll the scene bank. Left/Right scroll the track bank. Session is the active mode indicator (lit). User1/User2/Mixer are reserved (unlit, no-op for now).
**Rationale:** Navigation is essential since the Launchpad can only show 8 tracks x 8 scenes at a time. The MK2's top row labels (arrows, Session, User1, User2, Mixer) suggest this natural mapping. Keeping 3 buttons reserved leaves room for future modes without overloading v0.1.
**Alternatives considered:** Mapping all to transport controls — wastes buttons; immediate multi-mode support — adds complexity counter to the "keep it simple" goal.
**Status:** ACTIVE
**ID:** D-1.4

## 2026-03-06 — Function: What do the right-side Scene Launch buttons control?

**Decision:** Scene launch. Each of the 8 buttons launches the corresponding scene (row) across all tracks. LED indicates scene state.
**Rationale:** This is the standard Launchpad behavior and the most useful complement to per-clip launching on the grid. Launching an entire scene is a core live performance action.
**Alternatives considered:** Track stop buttons — useful but scene launch is more fundamental; track select — could be added later as a mode.
**Status:** ACTIVE
**ID:** D-1.5

## 2026-03-06 — LED: What color scheme for clip states?

**Decision:** Use the MK2's 128-color velocity palette to indicate clip state:
- Off (0): Empty slot — no clip
- Green dim (23): Clip exists, stopped
- Green bright (21): Clip is playing
- Red bright (5): Clip is recording
- Amber/Yellow (61): Clip is queued (about to play/stop) — use MK2 pulse SysEx for animation
- Orange dim (11): Track is armed, empty slot
- White dim (119): Navigation button active indicator
- For top row: Green (21) = active mode, off = inactive
**Rationale:** The MK2's RGB palette allows much richer feedback than the MK1's bi-color. Using the built-in velocity palette avoids SysEx complexity for most states. Pulse mode (SysEx `F0 00 20 29 02 18 28 <led> <color> F7`) adds visual feedback for queued clips without custom timing code. Color choices follow the traffic-light convention (green=go, red=record, amber=pending).
**Alternatives considered:** Full SysEx RGB mode for every LED — more flexible but slower and more complex; MK1-style 4-color only — wastes the MK2's capabilities.
**Status:** ACTIVE
**ID:** D-1.6

## 2026-03-06 — Architecture: How to structure the Java code?

**Decision:** Three-class architecture plus constants:
1. `LaunchpadMk2ExtensionDefinition` — metadata, MIDI port definition, UUID
2. `LaunchpadMk2Extension` — main controller logic, `init()`/`exit()`/`flush()`
3. `LaunchpadMk2Colors` — LED velocity constants and SysEx helpers
Keep everything in a single package: `com.gregross.bitwig.launchpadmk2`. No abstract layers, no framework — direct Bitwig API usage.
**Rationale:** Simplest viable architecture for a dedicated controller. Matches the official Bitwig extension pattern (Definition + Extension). A colors class keeps magic numbers and SysEx byte arrays out of the main logic. No need for a framework when supporting exactly one controller.
**Alternatives considered:** Using DrivenByMoss framework — massive dependency for a simple controller; multi-layer architecture — over-engineering for this scope; single-file — too messy once LED + SysEx logic is added.
**Status:** ACTIVE
**ID:** D-1.7
