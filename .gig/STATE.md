# State

## Current State

| Field | Value |
|-------|-------|
| **Version** | `0.1.0` |
| **Phase** | 1 — Clip Launcher |
| **Status** | `GATHERED` |
| **Last Batch** | — |
| **Last Updated** | 2026-03-06 |

---

## Batch History

<!-- Newest first. Type: PLANNED or UNPLANNED -->

| Version | Phase | Batch Title | Type | Status | Timestamp |
|---------|-------|-------------|------|--------|-----------|
| 0.0.1 | 0 | Project discovery & scaffold | PLANNED | done | 2026-03-06 |

---

## Active Decisions

<!-- Decisions that affect current/upcoming work -->

- D-1.1: Java + Gradle, standalone, extension-api:22
- D-1.2: MK2 Session layout, decimal grid mapping
- D-1.3: Clip launcher (8 tracks x 8 scenes)
- D-1.4: Top row = navigation arrows + reserved mode buttons
- D-1.5: Right-side = scene launch
- D-1.6: RGB 128-color palette + pulse SysEx for queued
- D-1.7: 3 classes (Definition, Extension, Colors) in launchpadmk2 package

---

## Open Flags

<!-- Items that need human attention -->

_None._

---

## Working Memory

<!-- Key context: file paths, patterns, naming conventions, gotchas.
     Updated during plan and apply. Keep under 100 lines. -->

- Package: `com.gregross.bitwig.launchpadmk2`
- Source root: `src/main/java/com/gregross/bitwig/launchpadmk2/`
- Build: `./gradlew build` -> `build/libs/LaunchpadMk2.bwextension`
- Deploy: `~/Documents/Bitwig Studio/Extensions/`
- MK2 grid notes: `(row+1)*10 + (col+1)` — 11-18, 21-28, ..., 81-88
- MK2 scene launch: 19, 29, 39, 49, 59, 69, 79, 89
- MK2 top row: CC 104-111
- MK2 SysEx header: `F0 00 20 29 02 18`
- Bitwig API: extension-api:22, Java 21

---

## Open Issues

<!-- Summary of deferred issues from ISSUES.md -->

_None._

---

## Session Recovery

1. Read this file — current state
2. Read `PLAN.md` — what's next
3. Read `DECISIONS.md` — what's been decided
4. Read `ISSUES.md` — open/deferred issues
5. Resume from next batch
