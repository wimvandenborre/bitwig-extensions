# State

## Current State

| Field | Value |
|-------|-------|
| **Version** | `0.2.4` |
| **Phase** | 2 — Git Consolidation |
| **Status** | `GOVERNED` |
| **Last Batch** | Verify build and final cleanup |
| **Last Updated** | 2026-03-08 |

---

## Batch History

<!-- Newest first. Type: PLANNED or UNPLANNED -->

| Version | Phase | Batch Title | Type | Status | Timestamp |
|---------|-------|-------------|------|--------|-----------|
| 0.2.4 | 2 | Verify build and final cleanup | PLANNED | done | 2026-03-08 |
| 0.2.3 | 2 | Consolidate .gig/, .claude/, .gitignore | PLANNED | done | 2026-03-08 |
| 0.2.2 | 2 | Namespace tags and clean up | PLANNED | done | 2026-03-08 |
| 0.2.1 | 2 | Import subproject histories via subtree | PLANNED | done | 2026-03-08 |
| 0.1.4 | 1 | Verify full build | PLANNED | done | 2026-03-08 |
| 0.1.3 | 1 | Convert launchpad-mk2 build | PLANNED | done | 2026-03-08 |
| 0.1.2 | 1 | Convert gig-maestro build | PLANNED | done | 2026-03-08 |
| 0.1.1 | 1 | Root project scaffold | PLANNED | done | 2026-03-08 |
| 0.0.1 | 0 | Project discovery & scaffold | PLANNED | done | 2026-03-08 |

---

## Active Decisions

- D-2.1: Monorepo via `git subtree add` — import full history, remove nested `.git/`
- D-2.2: Prefix imported tags: `gig-maestro/v0.X.Y`, `launchpad-mk2/v0.X.Y`
- D-2.3: Move subproject `.gig/` to `.gig/modules/{name}/`
- D-2.4: Consolidate `.claude/CLAUDE.md` to root
- D-2.5: Merge `.gitignore` into root, delete subproject copies
- D-2.6: Module name in phase name for branch convention

---

## Open Flags

<!-- Items that need human attention -->

_None._

---

## Working Memory

- **Root dir:** `/Users/gregrossdev/Devl/bitwig/extensions`
- **Monorepo:** Single git repo, 198 commits, 31 tags
- **Modules:** `gig-maestro` (shadow plugin, CLI source set) and `launchpad-mk2` (simple JAR)
- **Build:** Gradle 9.3.1, Java 21, Bitwig API v25 from Maven
- **Tags:** `gig-maestro/v0.X.Y` (26), `launchpad-mk2/v0.X.Y` (4), root `v0.1.4`
- **Module history:** `.gig/modules/gig-maestro/` (25 phases), `.gig/modules/launchpad-mk2/` (5 phases)
- **Claude context:** `.claude/CLAUDE.md` at root covers both modules
- **Build commands:** `./gradlew :gig-maestro:shadowJar`, `./gradlew :launchpad-mk2:build`, etc.

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
