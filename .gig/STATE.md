# State

## Current State

| Field | Value |
|-------|-------|
| **Version** | `0.3.1` |
| **Phase** | — |
| **Status** | `IDLE` |
| **Last Batch** | Phase 3 complete |
| **Last Updated** | 2026-03-08 |

---

## Batch History

<!-- Newest first. Type: PLANNED or UNPLANNED -->

| Version | Phase | Batch Title | Type | Status | Timestamp |
|---------|-------|-------------|------|--------|-----------|
| 0.3.1 | 3 | Scene launch feedback with colors + state | PLANNED | done | 2026-03-08 |
| 0.2.1 | 2 | Track filtering + cursor track nav + nav colors | PLANNED | done | 2026-03-07 |
| 0.1.7 | 1 | CCW rotation, empty slots off, pulse red, hue-aware color correction | UNPLANNED | done | 2026-03-07 |
| 0.1.6 | 1 | Manual testing fixes | UNPLANNED | done | 2026-03-06 |
| 0.1.5 | 1 | Integration testing + polish | PLANNED | done | 2026-03-06 |
| 0.1.4 | 1 | Scene launch buttons + top row navigation | PLANNED | done | 2026-03-06 |
| 0.1.3 | 1 | Clip launcher grid (8x8 pads + LED feedback) | PLANNED | done | 2026-03-06 |
| 0.1.2 | 1 | LED colors constants + SysEx helpers | PLANNED | done | 2026-03-06 |
| 0.1.1 | 1 | Gradle project scaffold + extension definition | PLANNED | done | 2026-03-06 |
| 0.0.1 | 0 | Project discovery & scaffold | PLANNED | done | 2026-03-06 |

---

## Active Decisions

<!-- Decisions that affect current/upcoming work -->

_None — phase archived._

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
