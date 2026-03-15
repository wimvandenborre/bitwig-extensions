# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-15 — Trigger: How to auto-regenerate the OpenAPI spec?

**Decision:** Gradle `Exec` task running the Node.js script, wired as dependency of `copyDocsToResources`
**Rationale:** Keeps everything in the Gradle build graph — `shadowJar` automatically regenerates the spec when claude-tools.json changes. No manual step needed.
**Alternatives considered:** npm script (adds package.json dependency), Git pre-commit hook (fragile, not part of build), manual run (current approach, error-prone)
**Status:** ACTIVE
**ID:** D-1.1

## 2026-03-15 — Caching: Use Gradle up-to-date checking?

**Decision:** Yes — input is `tools/claude-tools.json`, output is `docs/openapi.json`
**Rationale:** Skips regeneration when nothing changed, keeping builds fast. Gradle's incremental build support handles this natively.
**Alternatives considered:** Always regenerate (wasteful), file timestamp check in script (reinventing Gradle)
**Status:** ACTIVE
**ID:** D-1.2

## 2026-03-15 — Validation: Add offline smoke test?

**Decision:** Yes — a script that validates openapi.json has the expected structure and method count
**Rationale:** Catches spec drift during governance without needing Bitwig running. Fits into existing `smoke-test.sh --offline` framework.
**Alternatives considered:** No validation (risk of stale spec shipping), full OpenAPI validator (heavy dependency)
**Status:** ACTIVE
**ID:** D-1.3
