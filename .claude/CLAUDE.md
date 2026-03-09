# Bitwig Extensions — Project Context

Multi-module Gradle project containing Bitwig Studio controller extensions.

## Modules

### gig-maestro
RPC-based Bitwig controller extension + CLI tool.
- Extension source: `gig-maestro/src/main/java/dev/gregross/gig/`
- CLI source: `gig-maestro/src/cli/java/dev/gregross/gig/cli/`
- Tests: `gig-maestro/src/test/java/dev/gregross/gig/`
- Tool schemas: `gig-maestro/tools/claude-tools.json`
- System prompt: `gig-maestro/tools/system-prompt.md`
- Smoke tests: `gig-maestro/scripts/smoke-test.sh`

### launchpad-mk2
Novation Launchpad MK2 controller extension.
- Source: `launchpad-mk2/src/main/java/com/gregross/bitwig/launchpadmk2/`

## Bitwig API Reference

The full Bitwig Controller API v25 reference is at `gig-maestro/docs/bitwig-api-reference.txt` (18K+ lines).
**Always read this file** (or relevant sections via grep) when:
- Investigating what API methods are available for a feature
- Checking method signatures, parameter types, return types
- Looking up class hierarchies (e.g., Track extends Channel extends DeviceChain)
- Verifying if a capability exists before making decisions

## Build Commands

- `./gradlew :gig-maestro:shadowJar` — build gig-maestro extension (outputs to Bitwig Extensions dir)
- `./gradlew :gig-maestro:cliShadowJar` — build CLI JAR
- `./gradlew :gig-maestro:test` — run gig-maestro unit tests
- `./gradlew :launchpad-mk2:build` — build launchpad-mk2 extension
- `./gradlew :launchpad-mk2:install` — install to Bitwig Extensions dir
- `./gradlew clean build` — build everything
- `gig-maestro/scripts/smoke-test.sh` — full smoke suite (requires Bitwig running)
- `gig-maestro/scripts/smoke-test.sh --offline` — offline tests only

## Git Preferences

- **Merge strategy:** Always use regular merge (`--no-ff`), never squash. Do not ask.

## Project History

Module-specific gig phase history is archived at:
- `.gig/modules/gig-maestro/` (25 phases)
- `.gig/modules/launchpad-mk2/` (5 phases)
