# Gig Maestro

RPC-based controller extension for Bitwig Studio.

Control Bitwig Studio programmatically via JSON-RPC 2.0 over HTTP and WebSocket.
Send commands from the CLI, curl, Claude, or any HTTP client to automate
transport, tracks, devices, clips, notes, mixing, browsing, and more.

## Architecture

```
Bitwig Studio
  └── Gig Maestro Extension
        ├── HTTP Server (port 8787) ← JSON-RPC 2.0
        └── WebSocket Server (port 8788) ← State streaming
              ↑
        CLI / curl / Claude / any client
```

The extension runs inside Bitwig as a controller script. It starts an HTTP
server accepting JSON-RPC 2.0 requests and a WebSocket server for real-time
state subscriptions. External clients connect over the network to read and
manipulate the Bitwig session.

## Features

- **200+ RPC methods** across 25+ domains: transport, track, device, clip,
  note, browser, arranger, mixer, project, macro, state, and more
- **CLI tool** with 12 commands for terminal-based workflows
- **WebSocket streaming** with topic-based subscriptions for real-time state
  updates (transport position, track meters, parameter changes)
- **Device control** for Bitwig instruments, VST2, VST3, and CLAP plugins
- **Macro operations** for song building, sound design, and automation
- **Session snapshots** capturing full project state in a single call
- **Browser integration** for searching and loading presets, samples, and devices

## Prerequisites

- Java 21 or later
- Bitwig Studio 6.0 or later

## Build and Install

All commands run from the repository root.

Build the extension:

```sh
./gradlew :gig-maestro:shadowJar
```

Build the CLI:

```sh
./gradlew :gig-maestro:cliShadowJar
```

The extension JAR is output directly to the Bitwig Extensions directory.

To enable it in Bitwig:

1. Open Bitwig Studio
2. Go to **Settings** > **Controllers**
3. Click **Add Controller**
4. Select **Greg Ross** > **Gig Maestro**

## Quick Start

Once the extension is enabled in Bitwig, the HTTP server starts on port 8787.

**Get a session snapshot** (full project state):

```sh
curl -s http://localhost:8787/rpc \
  -d '{"jsonrpc":"2.0","method":"session/snapshot","params":{},"id":1}' | jq .result
```

**Start playback:**

```sh
curl -s http://localhost:8787/rpc \
  -d '{"jsonrpc":"2.0","method":"transport/play","params":{},"id":1}'
```

**Using the CLI:**

```sh
gig transport play
gig transport stop
gig --pretty snapshot
gig track set-volume -i 0 -v 0.8
gig watch --topics transport
```

## Documentation

- [CLI Reference](docs/cli-reference.md) -- all CLI commands, flags, and usage examples
- [RPC API Reference](docs/rpc-api-reference.md) -- every RPC method with parameters and responses

## Testing

**Unit tests** (no Bitwig required):

```sh
./gradlew :gig-maestro:test
```

24+ JUnit 5 test classes covering RPC handlers, validation, and serialization.

**Smoke tests** (automated integration scripts):

```sh
# All tests (requires Bitwig running)
gig-maestro/scripts/smoke-test.sh

# Offline only (schema validation, build checks)
gig-maestro/scripts/smoke-test.sh --offline

# Online only (requires Bitwig running)
gig-maestro/scripts/smoke-test.sh --online

# List available test scripts
gig-maestro/scripts/smoke-test.sh --list

# Run a specific test
gig-maestro/scripts/smoke-test.sh --only transport
```

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Java 21 |
| Bitwig API | Controller API v25 |
| JSON serialization | Gson |
| WebSocket server | Java-WebSocket |
| CLI framework | PicoCLI |
| Testing | JUnit 5 |
| Build | Gradle (Shadow plugin) |

## Configuration

The CLI reads configuration from `~/.gig-maestro/config.json`. This file is
created automatically on first use and stores connection settings (host, port).
