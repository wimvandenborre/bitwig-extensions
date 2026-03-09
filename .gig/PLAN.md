# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 1 — Gradle Multi-Module Setup (v0.1.x)

> Unify gig-maestro and launchpad-mk2 into a single Gradle multi-module project with shared build configuration, a root version catalog, Bitwig API v25 from Maven, and a single Gradle wrapper.

**Decisions:** D-1.1, D-1.2, D-1.3, D-1.4, D-1.5, D-1.6, D-1.7

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 1.1 | `0.1.1` | Root project scaffold | in-session | pending |
| 1.2 | `0.1.2` | Convert gig-maestro build | in-session | pending |
| 1.3 | `0.1.3` | Convert launchpad-mk2 build | in-session | pending |
| 1.4 | `0.1.4` | Verify full build | in-session | pending |

### Batch 1.1 — Root project scaffold

**Delegation:** in-session
**Decisions:** D-1.4, D-1.5, D-1.6, D-1.7
**Files:**
- Create `settings.gradle.kts` (root)
- Create `build.gradle.kts` (root)
- Create `gradle.properties` (root)
- Move `gig-maestro/gradle/libs.versions.toml` → `gradle/libs.versions.toml` (add `bitwig-api` entry)
- Move `gig-maestro/gradle/wrapper/` → `gradle/wrapper/`
- Move `gig-maestro/gradlew` + `gradlew.bat` → root
- Create `.gitignore` (root)
- Init git repo at root

**Work:**
1. Create root `settings.gradle.kts` with `rootProject.name = "bitwig-extensions"`, foojay toolchain resolver, and `include("gig-maestro", "launchpad-mk2")`.
2. Create root `build.gradle.kts` with `subprojects {}` block defining: Java 21 toolchain, `maven.bitwig.com` + `mavenCentral()` repositories, `compileOnly(libs.bitwig.api)`.
3. Create root `gradle.properties` merging both: `org.gradle.configuration-cache=true` and `bitwigExtensionsDir`.
4. Move version catalog to root `gradle/libs.versions.toml`, add `bitwig-api = { module = "com.bitwig:extension-api", version = "25" }`.
5. Move Gradle wrapper files to root; remove from both subprojects.
6. Create root `.gitignore` (`.gradle/`, `build/`, `.idea/`, `.DS_Store`).
7. `git init` at root.

**Test criteria:** `./gradlew projects` lists both modules.
**Acceptance:** Root project resolves both subprojects; wrapper runs from root.

### Batch 1.2 — Convert gig-maestro build

**Delegation:** in-session (depends on 1.1)
**Decisions:** D-1.1, D-1.2, D-1.3, D-1.5
**Files:**
- Edit `gig-maestro/build.gradle.kts`
- Delete `gig-maestro/settings.gradle.kts`
- Delete `gig-maestro/gradle.properties`
- Delete `gig-maestro/gradlew`, `gig-maestro/gradlew.bat`, `gig-maestro/gradle/wrapper/`

**Work:**
1. Remove from `build.gradle.kts`: `repositories {}` block (now in root), `java {}` toolchain block (now in root), `compileOnly(files(bitwigApiPath))` (replaced by root catalog dep).
2. Keep module-specific: `plugins { java; alias(libs.plugins.shadow) }`, shadow jar config, CLI source set, module-specific deps (websocket, gson, picocli, junit).
3. Change `testImplementation(files(bitwigApiPath))` → `testImplementation(libs.bitwig.api)`.
4. Update `bitwigExtensionsDir` reference to use root `gradle.properties` value.
5. Delete `settings.gradle.kts`, `gradle.properties`, wrapper files.

**Test criteria:** `./gradlew :gig-maestro:shadowJar` builds `GigMaestro.bwextension`; `./gradlew :gig-maestro:test` passes.
**Acceptance:** Extension builds with all deps bundled; tests pass; no local JAR reference remains.

### Batch 1.3 — Convert launchpad-mk2 build

**Delegation:** in-session (depends on 1.1)
**Decisions:** D-1.1, D-1.2, D-1.3, D-1.5
**Files:**
- Replace `launchpad-mk2/build.gradle` with `launchpad-mk2/build.gradle.kts`
- Edit `launchpad-mk2/src/main/java/com/gregross/bitwig/launchpadmk2/LaunchpadMk2ExtensionDefinition.java` (bump API version)
- Delete `launchpad-mk2/settings.gradle`
- Delete `launchpad-mk2/gradle.properties`
- Delete `launchpad-mk2/gradlew`, `launchpad-mk2/gradlew.bat`, `launchpad-mk2/gradle/wrapper/`

**Work:**
1. Create `build.gradle.kts` (Kotlin DSL) with only module-specific config: `plugins { java }`, jar output name, install task.
2. Remove repos, java toolchain, API dep (all from root now).
3. Bump `getRequiredAPIVersion()` from 22 → 25.
4. Delete old `build.gradle`, `settings.gradle`, `gradle.properties`, wrapper files.

**Test criteria:** `./gradlew :launchpad-mk2:build` produces `LaunchpadMk2.bwextension`.
**Acceptance:** Extension compiles against API v25; JAR contains only extension classes.

### Batch 1.4 — Verify full build

**Delegation:** in-session (depends on 1.2, 1.3)
**Decisions:** All
**Files:** None (verification only)

**Work:**
1. Run `./gradlew clean build` from root — both modules must build.
2. Run `./gradlew :gig-maestro:test` — all tests pass.
3. Verify output artifacts exist in expected locations.
4. Run `./gradlew :gig-maestro:cliShadowJar` — CLI JAR builds.

**Test criteria:** Clean build succeeds; all tests pass; both `.bwextension` files produced.
**Acceptance:** Full multi-module build works end-to-end from a single `./gradlew` invocation.

**Phase Acceptance Criteria:**
- [ ] `./gradlew projects` shows both modules
- [ ] `./gradlew clean build` succeeds
- [ ] `./gradlew :gig-maestro:shadowJar` produces `GigMaestro.bwextension`
- [ ] `./gradlew :gig-maestro:test` passes
- [ ] `./gradlew :gig-maestro:cliShadowJar` produces `gig-cli.jar`
- [ ] `./gradlew :launchpad-mk2:build` produces `LaunchpadMk2.bwextension`
- [ ] No references to `bitwigApiPath` or local JAR remain
- [ ] No duplicate Gradle wrappers in subprojects
- [ ] Single version catalog at root with `bitwig-api` entry

**Completion triggers Phase 2 → version `0.2.0`**

---

## Plan Amendments

<!-- Log any changes to the plan after creation -->

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| — | — | — | — |
