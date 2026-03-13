# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 21 — Toggle Panel Visibility RPC (v0.21.x)

> Expose Bitwig's Application-level panel toggle methods as parameterless RPC calls. Enables workflow scripts to show/hide the device panel, note editor, mixer, etc. without changing the panel layout. Adds 9 new RPC methods with unit tests, tool definitions, and system prompt updates.

**Decisions:** D-21.1, D-21.2, D-21.3

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 21.1 | `0.21.1` | RPC method implementations | in-session | done |
| 21.2 | `0.21.2` | Unit tests | in-session | done |
| 21.3 | `0.21.3` | Tool definitions + system prompt | in-session | done |
| 21.4 | `0.21.4` | Update manual/workflow scripts + build verify | in-session | done |

### Batch 21.1 — RPC method implementations

**Delegation:** in-session
**Decisions:** D-21.1, D-21.2
**Files:**
- `gig-maestro/src/main/java/dev/gregross/gig/handlers/ApplicationHandler.java`

**Work:**
Register 9 new parameterless RPC methods in ApplicationHandler:
1. `app/toggleInspector` → `application.toggleInspector()`
2. `app/toggleDevices` → `application.toggleDevices()`
3. `app/toggleMixer` → `application.toggleMixer()`
4. `app/toggleNoteEditor` → `application.toggleNoteEditor()`
5. `app/toggleAutomationEditor` → `application.toggleAutomationEditor()`
6. `app/toggleBrowser` → `application.toggleBrowserVisibility()`
7. `app/toggleFullScreen` → `application.toggleFullScreen()`
8. `app/previousSubPanel` → `application.previousSubPanel()`
9. `app/nextSubPanel` → `application.nextSubPanel()`

All return `JsonPrimitive("ok")`, no parameters needed.

**Test criteria:** `./gradlew :gig-maestro:shadowJar` builds successfully.

---

### Batch 21.2 — Unit tests

**Delegation:** in-session
**Decisions:** D-21.3
**Depends on:** Batch 21.1
**Files:**
- `gig-maestro/src/test/java/dev/gregross/gig/handlers/ApplicationHandlerTest.java`

**Work:**
1. Add method registration assertions for all 9 new methods
2. Add behavioral tests verifying each RPC calls the correct Application method

**Test criteria:** `./gradlew :gig-maestro:test` passes with new tests.

---

### Batch 21.3 — Tool definitions + system prompt

**Delegation:** in-session
**Decisions:** D-21.3
**Depends on:** Batch 21.1
**Files:**
- `gig-maestro/tools/claude-tools.json`
- `gig-maestro/tools/system-prompt.md`

**Work:**
1. Add 9 tool definitions to claude-tools.json (parameterless tools)
2. Add "Panel Visibility" section to system-prompt.md documenting all toggle methods
3. Update offline smoke test tool list if needed

**Test criteria:** `./scripts/smoke-test.sh --offline` passes (tool existence + system prompt checks).

---

### Batch 21.4 — Update manual/workflow scripts + build verify

**Delegation:** in-session
**Decisions:** D-21.3
**Depends on:** Batch 21.1
**Files:**
- `gig-maestro/scripts/manual/devices.sh` (use toggleDevices instead of layout switch)
- `gig-maestro/scripts/workflows/create-track-with-synth-and-melody.sh` (use toggleDevices for device panel)

**Work:**
1. Update workflow script: replace "press D" pause with `app/toggleDevices` RPC call
2. Update manual devices.sh: use `app/toggleDevices` to show device panel
3. Build extension JAR and verify full smoke suite
4. Manual verification of toggleDevices in Bitwig

**Test criteria:** `./gradlew :gig-maestro:shadowJar` builds. `./scripts/smoke-test.sh --offline` passes. Manual toggle works in Bitwig.

---

**Phase Acceptance Criteria:**
- [ ] 9 new RPC methods registered and callable
- [ ] Unit tests for all 9 methods
- [ ] Tool definitions in claude-tools.json
- [ ] System prompt documents panel toggles
- [ ] Offline smoke tests pass
- [ ] Manual verification: toggleDevices shows/hides device panel in Bitwig
- [ ] Workflow script uses toggleDevices instead of manual prompt

**Completion triggers Phase 22 → version `0.22.0`**
