# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-12 — Scope: What macro/createSound does

**Decision:** `macro/createSound` is a single RPC call that handles: (1) optionally insert a Bitwig device, (2) wait one flush cycle for the device to initialize, (3) batch-set parameters across multiple pages via the existing `device/setParameters` logic. No embedded sound recipes — synthesis knowledge stays in the system prompt where the LLM can reason about it. Device name is optional: if omitted, parameters are applied to the currently selected device (enabling "reshape current sound" workflows).
**Rationale:** The LLM's current gap is coordinating flush-cycle-dependent operations. It can call `device/insertBitwigDevice` but can't wait for the device to load before setting parameters. This macro bridges that timing gap. Keeping recipes out of the macro makes it generic (works for any device, any sound) and avoids hardcoding synthesis knowledge that varies by device and Bitwig version. The system prompt already teaches discover→design→apply.
**Alternatives considered:** (1) Embed recipe templates in the macro — rigid, device-specific, duplicates system prompt content. (2) Full discover→design→apply pipeline — requires async snapshot reads mid-macro, architecturally complex. (3) No macro, use session/transaction instead — transactions execute synchronously without flush-cycle spacing, risky for device insertion.
**Status:** ACTIVE
**ID:** D-18.1

## 2026-03-12 — API shape for macro/createSound

**Decision:** Accept `{device?: string, position?: "end"|"before"|"after", pages: [{pageIndex: int, params: [{index: int, value: double}]}]}`. The `device` field triggers insertion; omit it to apply to current device. The `pages` array uses the same format as `device/setParameters`. Returns `{ok: true, device: string, pageCount: int, paramCount: int, inserted: boolean}`.
**Rationale:** Reuses the proven `device/setParameters` schema for the parameter payload — no new format to learn. The optional `device` field makes the macro work for both "create from scratch" and "reshape existing sound" use cases. Including `inserted` in the response tells the caller whether a new device was added.
**Alternatives considered:** (1) Separate `device` and `params` top-level fields with different schemas — unnecessary complexity. (2) Accept a `recipe` string instead of explicit params — moves synthesis knowledge into Java code where it doesn't belong.
**Status:** ACTIVE
**ID:** D-18.2

## 2026-03-12 — Implementation: flush-cycle choreography

**Decision:** Phase 1 (immediate): if `device` is provided, call `dispatcher.handleInternal("device/insertBitwigDevice", ...)`. Phase 2 (scheduled at FLUSH_DELAY_MS): call `dispatcher.handleInternal("device/setParameters", ...)` which internally schedules its own page changes. Return immediately with confirmation — the caller doesn't need to wait for all pages to complete. This follows the same fire-and-forget pattern as `macro/buildSection`.
**Rationale:** Device insertion via `insertFile()` is processed by Bitwig asynchronously — the cursor device won't point to the new device until the next flush cycle. By scheduling parameter setting after FLUSH_DELAY_MS (100ms), we ensure the new device is initialized and the remote controls page is populated. The `device/setParameters` handler then manages its own multi-page scheduling internally. Two levels of deferred execution: macro→handler→pages.
**Alternatives considered:** (1) Set params in same flush cycle as insertion — device may not be initialized yet. (2) Wait for snapshot confirmation before setting — requires async callback infrastructure that doesn't exist. (3) Double FLUSH_DELAY — 100ms has proven sufficient for all other macro operations.
**Status:** ACTIVE
**ID:** D-18.3

## 2026-03-12 — Testing and tool definition

**Decision:** Add `macro/createSound` to MacroHandlerTest using the IMMEDIATE_SCHEDULER + call-log stub pattern. Tests: (1) device insertion + parameter setting sequence, (2) no device (params-only mode), (3) validation errors (empty pages, bad index/value). Add tool definition to claude-tools.json. Update system prompt to reference the new macro in the "Apply" step of from-scratch recipes.
**Rationale:** IMMEDIATE_SCHEDULER executes all deferred tasks synchronously, so tests can verify the full choreography without real flush cycles. Call-log stubs capture the exact sequence of internal RPC calls for assertion.
**Alternatives considered:** None — standard testing approach for all macros.
**Status:** ACTIVE
**ID:** D-18.4
