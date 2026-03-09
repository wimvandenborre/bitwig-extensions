# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 2 — Git Consolidation (v0.2.x)

> Consolidate three independent git repositories into a single monorepo using `git subtree add`. Preserve all commit history and tags (namespaced), archive subproject `.gig/` state, consolidate `.claude/` and `.gitignore`, and establish the workflow for working on both extensions from one repo.

**Decisions:** D-2.1, D-2.2, D-2.3, D-2.4, D-2.5, D-2.6

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 2.1 | `0.2.1` | Import subproject histories via subtree | in-session | done |
| 2.2 | `0.2.2` | Namespace tags and clean up | in-session | done |
| 2.3 | `0.2.3` | Consolidate .gig/, .claude/, .gitignore | in-session | pending |
| 2.4 | `0.2.4` | Verify build and final cleanup | in-session | pending |

### Batch 2.1 — Import subproject histories via subtree

**Delegation:** in-session
**Decisions:** D-2.1
**Files:**
- Root repo gains full `gig-maestro/` and `launchpad-mk2/` tracked trees
- Delete `gig-maestro/.git/` and `launchpad-mk2/.git/`

**Work:**
1. Commit any dirty state in root repo first.
2. Add gig-maestro as a subtree: `git subtree add --prefix=gig-maestro gig-maestro/.git main`
3. Add launchpad-mk2 as a subtree: `git subtree add --prefix=launchpad-mk2 launchpad-mk2/.git main`
4. Remove nested `.git/` directories from both subprojects.
5. Verify `git log --oneline | wc -l` includes imported history.

**Test criteria:** `git log --oneline gig-maestro/` shows historical commits; `git log --oneline launchpad-mk2/` shows historical commits; no nested `.git/` dirs remain.
**Acceptance:** All 190 subproject commits are in the root repo history.

### Batch 2.2 — Namespace tags and clean up

**Delegation:** in-session (depends on 2.1)
**Decisions:** D-2.2
**Files:** None (git operations only)

**Work:**
1. For each tag from gig-maestro (27 tags): create `gig-maestro/{tag}` pointing to same commit, delete original.
2. For each tag from launchpad-mk2 (5 tags): create `launchpad-mk2/{tag}` pointing to same commit, delete original.
3. Keep root tag `v0.1.4` as-is.
4. Verify with `git tag -l`.

**Test criteria:** `git tag -l 'gig-maestro/*'` shows 27 tags; `git tag -l 'launchpad-mk2/*'` shows 5 tags; `v0.1.4` still exists; no unprefixed subproject tags remain.
**Acceptance:** All tags namespaced without data loss.

### Batch 2.3 — Consolidate .gig/, .claude/, .gitignore

**Delegation:** in-session (depends on 2.1)
**Decisions:** D-2.3, D-2.4, D-2.5
**Files:**
- Move `gig-maestro/.gig/` → `.gig/modules/gig-maestro/`
- Move `launchpad-mk2/.gig/` → `.gig/modules/launchpad-mk2/`
- Move `gig-maestro/.claude/CLAUDE.md` → `.claude/CLAUDE.md` (update content for multi-module)
- Merge subproject `.gitignore` patterns into root `.gitignore`
- Delete `gig-maestro/.gitignore` and `launchpad-mk2/.gitignore`

**Work:**
1. Create `.gig/modules/` directory.
2. Move both subproject `.gig/` directories into it.
3. Read gig-maestro's `.claude/CLAUDE.md`, update paths and build commands for multi-module context, write to root `.claude/CLAUDE.md`.
4. Read both subproject `.gitignore` files, merge unique patterns into root `.gitignore`.
5. Delete subproject `.gitignore` and `.claude/` files.

**Test criteria:** `.gig/modules/gig-maestro/STATE.md` exists; `.gig/modules/launchpad-mk2/STATE.md` exists; `.claude/CLAUDE.md` exists at root with multi-module content; no subproject `.gitignore` files remain.
**Acceptance:** All project metadata consolidated at root level.

### Batch 2.4 — Verify build and final cleanup

**Delegation:** in-session (depends on 2.1, 2.2, 2.3)
**Decisions:** All
**Files:**
- Update `.gig/STATE.md` working memory

**Work:**
1. Run `./gradlew clean build` — both modules must build.
2. Run `./gradlew :gig-maestro:test` — tests pass.
3. Run `./gradlew :gig-maestro:cliShadowJar` — CLI builds.
4. Verify `git status` is clean.
5. Verify `git log --oneline | wc -l` shows full consolidated history.
6. Update working memory with final repo state.

**Test criteria:** Clean build succeeds; tests pass; git log includes all imported history; no dirty state.
**Acceptance:** Single monorepo with full history, all builds working.

**Phase Acceptance Criteria:**
- [ ] All gig-maestro commits (166) present in root history
- [ ] All launchpad-mk2 commits (24) present in root history
- [ ] No nested `.git/` directories
- [ ] Tags namespaced: `gig-maestro/*` (27) and `launchpad-mk2/*` (5)
- [ ] Root tag `v0.1.4` preserved
- [ ] Subproject `.gig/` archived to `.gig/modules/`
- [ ] Root `.claude/CLAUDE.md` covers both modules
- [ ] Single `.gitignore` at root
- [ ] `./gradlew clean build` succeeds
- [ ] `./gradlew :gig-maestro:test` passes

**Completion triggers Phase 3 → version `0.3.0`**

---

## Plan Amendments

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| — | — | — | — |
