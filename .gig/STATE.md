# State

## Current State

| Field | Value |
|-------|-------|
| **Version** | `0.1.0` |
| **Phase** | 1 — Gradle Multi-Module Setup |
| **Status** | `GATHERED` |
| **Last Batch** | — |
| **Last Updated** | 2026-03-08 |

---

## Batch History

<!-- Newest first. Type: PLANNED or UNPLANNED -->

| Version | Phase | Batch Title | Type | Status | Timestamp |
|---------|-------|-------------|------|--------|-----------|
| 0.0.1 | 0 | Project discovery & scaffold | PLANNED | done | 2026-03-08 |

---

## Active Decisions

- D-1.1: Kotlin DSL for all build files
- D-1.2: Maven `extension-api:25` for both modules
- D-1.3: `compileOnly` scope for Bitwig API
- D-1.4: Root version catalog with Bitwig API entry
- D-1.5: Root build defines shared config; modules own specifics
- D-1.6: Single Gradle wrapper at root
- D-1.7: New root git repo; subproject `.git/` untouched

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
