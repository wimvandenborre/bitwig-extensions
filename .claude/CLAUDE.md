# Gig Maestro — Project Context

## Bitwig API Reference

The full Bitwig Controller API v25 reference is at `docs/bitwig-api-reference.txt` (18K+ lines).
**Always read this file** (or relevant sections via grep) when:
- Investigating what API methods are available for a feature
- Checking method signatures, parameter types, return types
- Looking up class hierarchies (e.g., Track extends Channel extends DeviceChain)
- Verifying if a capability exists before making decisions

## Key Project Paths

- Extension source: `src/main/java/dev/gregross/gig/`
- CLI source: `src/cli/java/dev/gregross/gig/cli/`
- Tests: `src/test/java/dev/gregross/gig/`
- Tool schemas: `tools/claude-tools.json`
- System prompt: `tools/system-prompt.md`
- Smoke tests: `scripts/smoke-test.sh`
- Gig state: `.gig/STATE.md`, `.gig/PLAN.md`, `.gig/DECISIONS.md`

## Build Commands

- `./gradlew shadowJar` — build extension (outputs to Bitwig Extensions dir)
- `./gradlew cliShadowJar` — build CLI JAR
- `./gradlew test` — run unit tests
- `./scripts/smoke-test.sh` — run full smoke suite (requires Bitwig running)
- `./scripts/smoke-test.sh --offline` — run offline tests only
