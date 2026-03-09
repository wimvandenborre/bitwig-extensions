# State

## Current State

| Field | Value |
|-------|-------|
| **Version** | `0.2.2` |
| **Phase** | 2 — Git Consolidation |
| **Status** | `IMPLEMENTING` |
| **Last Batch** | Namespace tags and clean up |
| **Last Updated** | 2026-03-08 |

---

## Batch History

<!-- Newest first. Type: PLANNED or UNPLANNED -->

| Version | Phase | Batch Title | Type | Status | Timestamp |
|---------|-------|-------------|------|--------|-----------|
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
- **Modules:** `gig-maestro` (Kotlin DSL, shadow plugin, CLI source set) and `launchpad-mk2` (Groovy DSL, simple JAR)
- **Gradle version:** 9.3.1 (both modules already match)
- **Java:** 21 (both modules)
- **Bitwig API:** v25 available on `maven.bitwig.com` — standardize both modules on Maven dep
- **gig-maestro** uses local JAR (`bitwigApiPath`) — switch to Maven
- **launchpad-mk2** uses Maven v22 — upgrade to v25, bump `getRequiredAPIVersion()` to 25
- **Version catalog:** gig-maestro has `libs.versions.toml`, launchpad-mk2 does not — promote to root
- **Each module has independent `.git/` and `.gig/`** — preserve these, root manages build only
- **bitwigExtensionsDir:** `~/Documents/Bitwig Studio/Extensions` (used by both for install)

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
