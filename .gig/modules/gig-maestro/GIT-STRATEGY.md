# Git Strategy

> Branch, commit, tag, and merge conventions aligned with gig versioning.

---

## Branch Model

```
main                          ← stable, verified, tagged
  └── feature/v0.1-auth       ← phase 1 work
  └── feature/v0.2-crud       ← phase 2 work
  └── feature/v0.3-tests      ← phase 3 work
```

### Branch Naming

| Branch | Pattern | Created By | Lifecycle |
|--------|---------|-----------|-----------|
| **main** | `main` | — | Permanent. Always deployable. |
| **phase** | `feature/v0.{N}-{phase-name}` | `gig:apply` | Created at phase start, deleted after merge. |
| **team task** | `feature/v0.{N}-{phase-name}/batch-{P}` | `gig:apply` (team mode) | Created per parallel batch, merged into phase branch. |

### Rules

- One phase branch at a time. No long-lived feature branches.
- Phase branches are created from `main` HEAD at phase start.
- Team task branches are created from the phase branch.
- Never work directly on `main`.

---

## Commit Strategy

### Per-Batch Commits

Every completed batch gets one commit on the phase branch.

**Format:**
```
{type}(v0.{N}.{P}): {batch description}
```

**Examples:**
```
feat(v0.1.1): add database schema and drizzle config
feat(v0.1.2): add task CRUD routes
fix(v0.1.3): [UNPLANNED] fix auth redirect bug
feat(v0.1.4): add input validation
test(v0.1.5): add integration tests
```

### Commit Types

| Type | When |
|------|------|
| `feat` | New functionality |
| `fix` | Bug fix |
| `refactor` | Restructuring without behavior change |
| `test` | Adding or updating tests |
| `docs` | Documentation only |
| `chore` | Tooling, deps, config |

### Staging Rules

- Stage specific files by name. Never `git add -A` or `git add .`.
- Do not commit `.env`, credentials, or secrets.
- Each commit should be atomic — one batch, one concern.

### Unplanned Work

Unplanned batches (`fix [thing]`) follow the same commit format with `[UNPLANNED]` in the commit body:

```
fix(v0.2.4): fix auth redirect on expired tokens

[UNPLANNED] — inserted during phase 2 apply.
```

---

## Merge Strategy

### Phase → Main

When `gig:verify` approves a phase:

1. **Switch to main:** `git checkout main`
2. **Merge the phase branch** using regular merge (default):
   ```
   git merge --no-ff feature/v0.{N}-{phase-name}
   ```
   This preserves batch-level commit history on main.
3. **Delete the phase branch:** `git branch -d feature/v0.{N}-{phase-name}`

> **Note:** Do not prompt for merge strategy. Regular merge (`--no-ff`) is the default.
> The user can explicitly request squash if they prefer it for a specific phase.

### Team Task → Phase Branch

When parallel batches complete in team mode:

1. **Merge each task branch** into the phase branch (regular merge).
2. **Resolve conflicts** if any — flag to user.
3. **Delete task branches** after merge.

---

## Tag Strategy

Tags mark significant points aligned with the version timeline.

### Phase Tags

Created automatically when a phase merges to main. The tag is the **actual last batch version** — not a reset.

```
git tag -a v0.{N}.{last-P} -m "Phase {N}: {phase name}"
```

**Examples:**
```
v0.1.3  — Phase 1: Database & Schema (3 batches)
v0.2.6  — Phase 2: Task CRUD Routes (3 batches, including unplanned)
v0.3.2  — Phase 3: Validation & Error Handling (2 batches)
```

### Milestone Tags

Created by `gig:milestone` when a milestone completes:

```
git tag -a v{MAJOR}.0.0 -m "Milestone: {milestone name}"
```

**Examples:**
```
v1.0.0  — Milestone: MVP Release
v2.0.0  — Milestone: Multi-tenant Support
```

### Tag Rules

- Tags are created on `main` after merge.
- Phase tags use the `v0.{N}.{last-P}` format (actual last batch version).
- Milestone tags use the `v{MAJOR}.0.0` format.
- Never move or delete tags.

---

## Full Lifecycle Example

```
main:     ──●────────────────●──────────────────●──── ...
              \              ↑ merge + tag        \
               \             │ v0.1.4              \
feature/v0.1:   ──●──●──●──●                       ──●──●──●
                  │   │   │  │                        │   │   │
                 0.1.1│  0.1.3│                      0.2.1│  0.2.3
                    0.1.2  0.1.4                       0.2.2
```

**Reading the graph:**
- Each `●` on a feature branch = one batch commit
- Each batch commit is versioned `v0.{N}.{P}`
- Phase merges back to main tagged with the **last batch version** (e.g., `v0.1.4`)
- The next phase starts at `v0.{N+1}.1` (first batch of next phase)
- Main always has clean, verified, tagged code

---

## Git Init (for new projects)

When `gig:init` runs in a directory without `.git/`:

1. `git init`
2. Create `.gitignore` (if not present) — include common ignores for detected stack.
3. Initial commit: `chore: initialize project`
4. All subsequent work follows the branch model above.

---

## Recovery

If a phase branch gets messy:
- **Soft reset:** `gig:verify` can flag issues and send back to `gig:apply`.
- **Hard reset:** Abandon the phase branch, create a new one from main, re-apply.
- **Cherry-pick:** Pull specific batch commits from an abandoned branch.

Never force-push. Never rewrite history on main.
