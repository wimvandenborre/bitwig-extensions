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

## 2026-03-08 — Build DSL: Kotlin DSL or Groovy DSL for all modules

**Decision:** Standardize on Kotlin DSL (`.gradle.kts`) for all build files — root `settings.gradle.kts`, root `build.gradle.kts`, and convert `launchpad-mk2/build.gradle` to `build.gradle.kts`.
**Rationale:** gig-maestro already uses Kotlin DSL. Kotlin DSL provides type-safe accessors, better IDE support, and is Gradle's recommended default. Both modules should be consistent.
**Alternatives considered:** Keep Groovy for launchpad-mk2 — rejected because mixed DSLs in one multi-module project is confusing and the conversion is trivial (3 files, simple config).
**Status:** ACTIVE
**ID:** D-1.1

## 2026-03-08 — API Dependency: Maven vs local JAR

**Decision:** Standardize both modules on the Maven dependency `com.bitwig:extension-api:25` from `maven.bitwig.com`. Remove the `bitwigApiPath` property.
**Rationale:** v25 is confirmed available on Maven (published Sept 2025). Using Maven ensures reproducible builds without depending on a local Bitwig installation. launchpad-mk2 already uses Maven (just at v22). gig-maestro's `.gig/STATE.md` note about v25 not being on Maven is outdated.
**Alternatives considered:** Keep local JAR for gig-maestro — rejected because Maven v25 exists and local JAR creates a machine-specific build dependency.
**Status:** ACTIVE
**ID:** D-1.2

## 2026-03-08 — API Scope: compileOnly vs implementation

**Decision:** Use `compileOnly` for the Bitwig API in both modules. Bitwig provides the API at runtime — it should not be bundled into `.bwextension` JARs.
**Rationale:** gig-maestro already uses `compileOnly`. launchpad-mk2 currently uses `implementation`, which works for plain JARs but is semantically wrong — the API classes come from Bitwig at runtime. Using `compileOnly` is correct and keeps JAR sizes smaller.
**Alternatives considered:** `implementation` — rejected because it unnecessarily bundles API classes (especially problematic with ShadowJar).
**Status:** ACTIVE
**ID:** D-1.3

## 2026-03-08 — Version Catalog: scope and location

**Decision:** Promote gig-maestro's `gradle/libs.versions.toml` to the root project. Add the Bitwig API as a catalog entry. Each module references catalog entries rather than hardcoding versions.
**Rationale:** gig-maestro already has a well-structured version catalog. Promoting it to root makes it available to all modules. launchpad-mk2 has no extra deps beyond the API, so it just gains the `libs.bitwig.api` entry.
**Alternatives considered:** Separate catalogs per module — rejected because the whole point is shared dependency management.
**Status:** ACTIVE
**ID:** D-1.4

## 2026-03-08 — Shared Config: what goes in root vs modules

**Decision:** Root `build.gradle.kts` defines via `subprojects {}`: Java 21 toolchain, `maven.bitwig.com` + `mavenCentral()` repositories, and `compileOnly(libs.bitwig.api)`. Each module's build file contains only module-specific config (shadow plugin, extra deps, jar naming, install tasks).
**Rationale:** Java version, API dependency, and repos are identical across modules — no reason to duplicate. Module-specific concerns (shadow jar, CLI source set, output filenames) stay in the module.
**Alternatives considered:** Convention plugins — rejected as over-engineering for 2 modules. Also considered putting everything in modules — rejected because it duplicates config.
**Status:** ACTIVE
**ID:** D-1.5

## 2026-03-08 — Gradle Wrapper: single root wrapper

**Decision:** Keep one Gradle wrapper at the root. Remove wrapper files (`gradlew`, `gradlew.bat`, `gradle/wrapper/`) from both subprojects.
**Rationale:** Multi-module projects use a single wrapper at the root. Having three wrappers is confusing and they must be kept in sync. All builds run from root: `./gradlew :gig-maestro:shadowJar`, `./gradlew :launchpad-mk2:build`.
**Alternatives considered:** Keep subproject wrappers — rejected because Gradle multi-module convention is one wrapper at root.
**Status:** ACTIVE
**ID:** D-1.6

## 2026-03-08 — Git: root repo vs subproject repos

**Decision:** Initialize a new git repo at the root (`extensions/`). Each subproject's `.git/` remains untouched for now — they are independent histories. The root repo tracks the multi-module build files and can optionally use `.gitignore` to exclude subproject internals, or the user can migrate later.
**Rationale:** The root needs version control for the new build files. Merging the two independent git histories is a separate, larger decision. Starting with a root repo for the new files is non-destructive and immediately useful.
**Alternatives considered:** (a) No root git — rejected, need version control for build files. (b) Merge all histories now — rejected, too large a scope change for this milestone.
**Status:** ACTIVE
**ID:** D-1.7
