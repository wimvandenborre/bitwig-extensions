# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-08 — Strategy: Monorepo vs submodules vs subtree

**Decision:** Monorepo via `git subtree add` — import each subproject's full history into the root repo at their existing paths, then remove the nested `.git/` directories.
**Rationale:** All repos are local-only (no remotes), single-branch, clean history. Subtree add preserves full commit history without rewriting it. No coordination with upstream needed. A single repo means one branch, one commit, one push — simplest workflow for solo development. Submodules would add complexity with no benefit since there's no upstream to track.
**Alternatives considered:** (a) Submodules — rejected, adds complexity for no benefit with local-only repos. (b) Filter-branch rewrite — rejected, unnecessary since subtree add already prefixes paths. (c) Shallow import — rejected, loses 190 commits of history. (d) Keep separate repos — rejected, defeats the purpose of multi-module unification.
**Status:** ACTIVE
**ID:** D-2.1

## 2026-03-08 — Tags: Namespace subproject tags

**Decision:** Prefix imported tags to avoid collisions: `gig-maestro/v0.X.Y` and `launchpad-mk2/v0.X.Y`. Root-level tags (like `v0.1.4`) remain unprefixed for the unified project.
**Rationale:** Both repos have tags like `v0.1.3` that would collide. Prefixing preserves the history while keeping the root tag namespace clean for the unified project going forward.
**Alternatives considered:** (a) Drop subproject tags — rejected, loses phase history references. (b) Suffix instead of prefix — rejected, less readable. (c) Keep as-is and let collisions overwrite — rejected, loses information.
**Status:** ACTIVE
**ID:** D-2.2

## 2026-03-08 — Subproject .gig/: Preserve or archive

**Decision:** Move each subproject's `.gig/` to `.gig/modules/{module-name}/` in the root project. This preserves the full phase history for reference without cluttering the active `.gig/` directory.
**Rationale:** Each subproject has valuable phase history (25 and 5 phases). Merging them into the root `.gig/phases/` would create naming collisions. A `modules/` subdirectory keeps them accessible and organized.
**Alternatives considered:** (a) Delete subproject `.gig/` — rejected, loses history. (b) Keep in subproject dirs — rejected, confusing to have nested `.gig/` dirs. (c) Merge into root phases — rejected, version collision between projects.
**Status:** ACTIVE
**ID:** D-2.3

## 2026-03-08 — Subproject .claude/: Consolidate

**Decision:** Move gig-maestro's `.claude/CLAUDE.md` content to a root-level `.claude/CLAUDE.md` that covers both modules. Launchpad-mk2 has no `.claude/` to migrate.
**Rationale:** With a single repo, Claude context should be at the root. The gig-maestro CLAUDE.md has useful content (API reference path, build commands, git preferences) that should be updated to reflect the multi-module structure.
**Status:** ACTIVE
**ID:** D-2.4

## 2026-03-08 — .gitignore: Consolidate into root

**Decision:** Merge both subproject `.gitignore` files into the root `.gitignore`. Remove subproject-level `.gitignore` files. Add `.gig/modules/` entries if needed.
**Rationale:** Single repo means single ignore file. The existing root `.gitignore` already covers the common patterns. Just need to add any subproject-specific patterns (like `*.bwextension` from launchpad-mk2).
**Alternatives considered:** Keep per-directory ignores — rejected, unnecessary complexity for 2 modules with similar patterns.
**Status:** ACTIVE
**ID:** D-2.5

## 2026-03-08 — Workflow: Branch strategy for working on different extensions

**Decision:** Use the existing gig branch convention (`feature/v0.{N}-{phase-name}`) for the unified repo. When a phase targets a specific module, prefix the phase name (e.g., `feature/v0.3-gig-maestro-midi-routing`). Cross-module phases use a descriptive name without prefix. All work happens in the single repo — no switching directories or repos.
**Rationale:** The gig workflow already handles branching well. Adding the module name to phase names makes it clear which extension is being worked on. Build commands already use module prefixes (`:gig-maestro:shadowJar`).
**Alternatives considered:** Separate branches per module — rejected, overcomplicates for solo development with the gig workflow already providing structure.
**Status:** ACTIVE
**ID:** D-2.6
