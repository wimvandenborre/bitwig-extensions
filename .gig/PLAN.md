# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 23 — Live Preset Export (v0.23.x)

> Add a `format: "preset"` option to `device/getDiscoveryResult` that returns the current device state in a format directly compatible with `macro/createSound`. Update system prompt to document the capture-and-save preset workflow. The AI handles default filtering client-side using reference files.

**Decisions:** D-23.1, D-23.2, D-23.3

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 23.1 | `0.23.1` | Preset format in getDiscoveryResult | in-session | done |
| 23.2 | `0.23.2` | Unit tests for preset format | in-session | done |
| 23.3 | `0.23.3` | Tool definition + system prompt + smoke tests | in-session | done |
| 23.4 | `0.23.4` | Build verification | in-session | done |

---

### Batch 23.1 — Preset format in getDiscoveryResult

**Delegation:** in-session
**Decisions:** D-23.1
**Files:**
- `gig-maestro/src/main/java/dev/gregross/gig/handlers/DeviceHandler.java`

**Work:**
Modify the `device/getDiscoveryResult` handler to accept an optional `format` parameter:
- If `format` is absent or `"full"` — return the existing discovery format (no change)
- If `format` is `"preset"` — restructure the discovery result into preset-compatible JSON:
  ```json
  {
    "deviceName": "Polymer",
    "pageCount": 7,
    "pages": [
      {
        "pageIndex": 0,
        "params": [
          { "index": 0, "value": 0.385 },
          { "index": 1, "value": 0.5 },
          ...
        ]
      },
      ...
    ]
  }
  ```
- The transformation reads the existing `discoveryResult` JsonObject and remaps each page's `parameters` array to the simpler `params` format (drop `name`, `displayedValue`, rename `parameters` → `params`)
- Rename `index` (page-level) → `pageIndex` to match `macro/createSound` format

**Test criteria:** `device/getDiscoveryResult` with `format: "preset"` returns preset-compatible JSON.
**Acceptance:** Result is directly passable to `macro/createSound` pages array.

---

### Batch 23.2 — Unit tests for preset format

**Delegation:** in-session
**Decisions:** D-23.1
**Files:**
- `gig-maestro/src/test/java/dev/gregross/gig/handlers/DeviceHandlerTest.java`

**Work:**
Add tests:
1. `getDiscoveryResult_presetFormat_returnsPresetCompatibleJson` — run discovery, then getDiscoveryResult with `format: "preset"`, verify output has `pageIndex`/`params` structure instead of `index`/`parameters`
2. `getDiscoveryResult_defaultFormat_returnsFullJson` — verify existing format unchanged when no `format` param
3. `getDiscoveryResult_presetFormat_noDiscovery_returnsError` — error when no discovery has been run

**Test criteria:** `./gradlew :gig-maestro:test` passes.
**Acceptance:** All 3 tests green.

---

### Batch 23.3 — Tool definition + system prompt + smoke tests

**Delegation:** in-session
**Decisions:** D-23.2, D-23.3
**Files:**
- `gig-maestro/tools/claude-tools.json`
- `gig-maestro/tools/system-prompt.md`
- `gig-maestro/scripts/tests/offline-schemas.sh`

**Work:**
1. Update `device_getDiscoveryResult` tool definition in claude-tools.json to document the optional `format` parameter (`"full"` or `"preset"`)
2. Update system prompt:
   - Add "Capturing Presets" section documenting the workflow: tweak → discoverAll → getDiscoveryResult(format=preset) → diff against reference → save as JSON
   - Mention that the AI should compare against `data/devices/{name}.json` to strip default values
3. Update offline-schemas.sh:
   - Add system prompt keyword checks for "Capturing Presets", "format", "preset"

**Test criteria:** `./scripts/smoke-test.sh --offline` passes with new assertions.
**Acceptance:** Tool updated, prompt documented, smoke tests green.

---

### Batch 23.4 — Build verification

**Delegation:** in-session
**Decisions:** all
**Files:** none (verification only)

**Work:**
1. `./gradlew clean build` — full build passes
2. `./gradlew :gig-maestro:test` — all tests pass
3. `./scripts/smoke-test.sh --offline` — offline smoke tests pass

**Test criteria:** All three commands exit 0.
**Acceptance:** Clean build with no warnings or failures.

---

**Phase Acceptance Criteria:**
- [ ] `device/getDiscoveryResult` with `format: "preset"` returns preset-compatible JSON
- [ ] Default format unchanged (backward compatible)
- [ ] System prompt documents capture-and-save workflow
- [ ] All tests and smoke tests pass
- [ ] Build clean

**Completion triggers Phase 24 → version `0.24.0`**
