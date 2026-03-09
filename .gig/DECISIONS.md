# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-08 — Shared Module: Create a common module or not

**Decision:** Do NOT create a shared utilities module. The overlap between gig-maestro and launchpad-mk2 is too thin to justify a third module.
**Rationale:** Deep research found only one duplicated pattern: JSON parameter validation (`requireInt`, `requireString`, etc.) — but this is duplicated *within* gig-maestro's 8 handlers, not *between* modules. Launchpad-mk2 has no RPC layer and doesn't need these validators. Other overlap (MIDI, color, observer patterns) is either too trivial, too hardware-specific, or framework-driven. A shared module would add build complexity (dependency management, package namespacing) for nearly zero cross-module code sharing.
**Alternatives considered:** (a) Create `common-bitwig` module with JsonParamValidator — rejected because this is a gig-maestro internal refactor, not a shared concern. (b) Create shared color utilities — rejected, LaunchpadMk2Colors is hardware-specific (Novation LED SysEx). (c) Create extension base class — rejected, boilerplate is minimal and extensions diverge significantly.
**Status:** ACTIVE
**ID:** D-3.1

## 2026-03-08 — Internal Refactor: Extract JsonParamValidator within gig-maestro

**Decision:** Extract `JsonParamValidator` utility class within gig-maestro to eliminate ~400 lines of duplicated validation code across 8 handlers. This is an internal refactor, not a shared module.
**Rationale:** 8 handlers each duplicate `requireInt()`, `requireString()`, `requireBoolean()`, `requireDouble()`, `optionalInt()`. Extracting to a single utility class reduces duplication by ~12% of handler code and makes adding new validation patterns easier.
**Alternatives considered:** (a) Leave as-is — rejected, duplication is real and measurable. (b) Put in shared module — rejected per D-3.1.
**Status:** ACTIVE
**ID:** D-3.2
