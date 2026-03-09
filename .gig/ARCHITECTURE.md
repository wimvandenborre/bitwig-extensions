# Architecture

## Overview

Multi-module Gradle project containing Bitwig Studio controller extensions. Each module is an independent extension that compiles to a `.bwextension` file installed into Bitwig's Extensions directory. The root project provides shared build configuration (Java toolchain, Bitwig API dependency, repository settings) while each module defines its own extension-specific build logic.

## Stack

| Layer | Technology | Notes |
|-------|-----------|-------|
| Language | Java 21 | Both modules |
| Build | Gradle 9.3.1 (Kotlin DSL) | Multi-module, version catalog |
| API | Bitwig Extension API v25 | From `maven.bitwig.com` |
| Runtime | Bitwig Studio 6.0 | Extensions loaded at startup |

## Structure

```
extensions/                         # Root multi-module project
├── .gig/                           # Gig workflow state (this project)
├── gig-maestro/                    # Module: RPC-based extension + CLI
│   ├── .gig/                       # Module's own gig history (25 phases)
│   ├── .claude/CLAUDE.md           # Module-specific Claude context
│   ├── src/main/java/              # Extension source
│   ├── src/cli/java/               # CLI source (PicoCLI)
│   ├── src/test/java/              # JUnit 5 tests (24 classes)
│   ├── docs/                       # Bitwig API reference
│   ├── scripts/                    # Smoke tests
│   └── tools/                      # Claude tool schemas
└── launchpad-mk2/                  # Module: Novation Launchpad MK2 controller
    ├── .gig/                       # Module's own gig history (5 phases)
    └── src/main/java/              # 3 Java files
```

## Patterns

- **Independent modules:** Each extension is self-contained with its own package namespace (`dev.gregross.gig` and `com.gregross.bitwig.launchpadmk2`).
- **Shared build infra:** Java 21 toolchain, Bitwig API dependency, and `maven.bitwig.com` repository defined once at root.
- **Extension pattern:** Each module follows Bitwig's `ControllerExtensionDefinition` + `ControllerExtension` pattern with `META-INF/services` registration.
- **Output:** ShadowJar (gig-maestro, bundles deps) or plain JAR (launchpad-mk2, no extra deps) → `.bwextension` files.

## Boundaries

- **In scope:** Gradle multi-module unification, shared dependency management, build from one root.
- **Out of scope:** Merging extension source code, changing module functionality, combining git histories.
- **Constraint:** Each module's `.gig/` and `.git/` history is preserved — this root project manages the build structure only.

## External Dependencies

| Dependency | Purpose | Notes |
|-----------|---------|-------|
| Bitwig Extension API v25 | Controller extension framework | From `maven.bitwig.com` |
| Java-WebSocket 1.6.0 | WebSocket server (gig-maestro) | ShadowJar bundled |
| Gson 2.12.1 | JSON serialization | ShadowJar bundled |
| PicoCLI 4.7.6 | CLI framework (gig-maestro) | CLI JAR only |
| JUnit 5.12.1 | Testing (gig-maestro) | Test scope |
| Shadow Plugin 9.0.0-beta12 | Fat JAR packaging (gig-maestro) | Build only |
