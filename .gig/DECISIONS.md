# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

<!-- Decision statuses:
  PROPOSED  — Claude's recommendation, awaiting user approval
  ACTIVE    — Approved and in effect
  AMENDED   — Overridden by user (original preserved, new entry appended)
  REVISED   — Claude revised based on new information (original preserved)
-->

<!-- Entry format:
## YYYY-MM-DD — Domain: Question

**Decision:** What was decided.
**Rationale:** Why this choice was made.
**Alternatives considered:** What else was evaluated.
**Status:** PROPOSED | ACTIVE | AMENDED | REVISED
**ID:** D-{batch}.{num}
-->

## 2026-02-28 — Scope: What does Phase 9 cover?

**Decision:** Phase 9 adds programmatic automation envelope writing via simulated touch recording, plus per-parameter automation management. Four capabilities: (1) spike to validate position-jump + touch + setValue creates automation data without real-time playback, (2) `writeEnvelope` method that takes a parameter index and array of {position, value} points, (3) per-parameter automation queries (hasAutomation, deleteAutomation, restoreControl), (4) parameter touch/untouch for manual automation recording workflows. No automation curve reading (not exposed by API).
**Rationale:** The Bitwig Controller API v25 has no `AutomationLane`, `AutomationPoint`, or `AutomationCurve` classes. The only path to write automation data is through `Parameter.touch(true)` + value setting + transport write mode. This simulated approach needs validation via spike before committing to the full implementation. Per-parameter automation management (has/delete/restore) is straightforward and useful regardless of whether envelope writing works.
**Alternatives considered:** (a) Skip entirely — leaves a significant gap for an arrangement-focused tool. (b) Real-time playback recording — impractical for precise curves, requires timing synchronization. (c) Wait for future API versions — no indication Bitwig will add direct automation point access.
**Status:** AMENDED
**ID:** D-9.1

## 2026-02-28 — Spike: How to validate envelope writing works?

**Decision:** First batch is a spike: enable write mode ("write"), enable arranger automation write, set transport position to beat X, touch parameter, setImmediately to value Y, untouch, repeat for multiple positions, then check `hasAutomation()`. Test with a concrete device parameter (e.g., Polymer filter cutoff). If automation data appears in Bitwig's arranger, the approach is validated. If not, try the alternative: start playback, use scheduled delays to set values at timed intervals, stop — and document which approach works.
**Rationale:** The position-jump approach is the ideal path (deterministic, no real-time timing). But it's unproven — the Bitwig engine may require active playback for automation recording to engage. The spike must answer: does `touch() + setImmediately() + untouch()` at a stopped transport position create automation data? This is a hard prerequisite — the entire phase design depends on the answer.
**Alternatives considered:** (a) Skip spike, assume it works — too risky, could waste an entire phase. (b) Ask Bitwig community — no guaranteed answer timeline.
**Status:** ACTIVE
**ID:** D-9.2

## 2026-02-28 — Architecture: Where do envelope methods live?

**Decision:** Add methods to the existing `DeviceHandler`, which already owns `CursorRemoteControlsPage` and the `device/setParameterValue` method. New methods operate on `remoteControlsPage.getParameter(index)` — same parameter access pattern. No new handler class needed.
**Rationale:** Envelope writing targets device parameters accessed through the cursor remote controls page. `DeviceHandler` already resolves parameter indices and has the `remoteControlsPage` reference. Adding touch/envelope/automation methods here follows the same ownership model as `device/setParameterValue`. Creating a separate handler would require passing the same `CursorRemoteControlsPage` to two handlers.
**Alternatives considered:** (a) New `AutomationHandler` — separate handler for a related concern, adds unnecessary indirection for 4-6 methods. (b) Put on `TransportHandler` — wrong ownership; these operate on parameters, not transport state.
**Status:** ACTIVE
**ID:** D-9.3

## 2026-02-28 — API Design: What does writeEnvelope look like?

**Decision:** `device/writeEnvelope` takes `{index: integer, points: [{position: number, value: number}]}` where `index` is the remote control parameter index (0-7), `position` is in beats, and `value` is normalized 0.0-1.0. The method internally: enables write mode, sets transport position for each point, touches parameter, sets value, untouches. Points are sorted by position ascending. Returns `{ok: true, pointsWritten: N}`. Requires arranger automation write to be enabled (error if not).
**Rationale:** This is the highest-value method — lets the agent draw arbitrary automation curves in one RPC call. Sorting ensures deterministic position ordering. Requiring automation write to be pre-enabled keeps the method focused and gives the agent explicit control over the recording state. The beat-position + normalized-value coordinate system matches the existing transport/parameter conventions.
**Alternatives considered:** (a) One-point-at-a-time method — requires N round-trips for N points, slower. (b) Auto-enable write mode inside the method — hides side effects. (c) Raw parameter values instead of normalized — inconsistent with device/setParameterValue.
**Status:** AMENDED
**ID:** D-9.4

## 2026-02-28 — API Design: What per-parameter automation methods to add?

**Decision:** Add 3 RPC methods: `device/hasAutomation` (`{index}` → `{hasAutomation: boolean}`), `device/deleteAutomation` (`{index}` → `{ok: true}`), `device/restoreAutomationControl` (`{index}` → `{ok: true}`). Also add `device/touchParameter` (`{index, touched: boolean}`) for manual touch recording workflows. All operate on `remoteControlsPage.getParameter(index)`.
**Rationale:** `hasAutomation` lets the agent check before delete. `deleteAutomation` enables cleanup. `restoreAutomationControl` returns a manually-overridden parameter back to its automation curve. `touchParameter` is the primitive that `writeEnvelope` uses internally, but exposing it lets the agent do fine-grained control (e.g., touch → set values over time → untouch). These are all single-line API calls on `Parameter`.
**Alternatives considered:** (a) Skip touchParameter — limits agent to only using writeEnvelope, no manual recording workflow. (b) Add a "read automation" method — not possible, API doesn't expose automation data points.
**Status:** AMENDED
**ID:** D-9.5

## 2026-02-28 — Snapshot: How to reflect automation state?

**Decision:** Extend the existing `device` snapshot section's `parameters` array. Each parameter object gets two new fields: `hasAutomation` (boolean) and `isTouched` (boolean, if trackable). No new snapshot section — automation state is per-parameter, belongs in the device section.
**Rationale:** The agent needs to know which parameters have automation data (to avoid overwriting or to clean up). This is a lightweight addition — `Parameter.hasAutomation()` is a `BooleanValue` that can be observed. Adding it to the existing parameter objects keeps the snapshot schema clean.
**Alternatives considered:** (a) Separate `automation` snapshot section with a parameter list — redundant with the device section. (b) Skip snapshot, use `device/hasAutomation` only — breaks the perception-action loop; agent should see automation state in snapshot.
**Status:** ACTIVE
**ID:** D-9.6

## 2026-02-28 — Scope: What does Phase 9 cover? (amended)

**Decision:** Phase 9 adds programmatic automation envelope writing via simulated touch recording, plus per-parameter automation management and state restoration guardrails. Five capabilities: (1) spike to validate position-jump + touch + setValue creates automation data without real-time playback, (2) `writeEnvelope` method that takes a parameter index and array of {position, value} points with full state save/restore (transport position, play/stop, automation write enables, touched-state cleanup in finally), (3) per-parameter automation methods matching API v25 names: `hasAutomation`, `deleteAllAutomation`, `restoreAutomationControl`, (4) `touch` method for manual automation recording workflows, (5) writeEnvelope preconditions + input validation guardrails. No automation curve reading (not exposed by API).
**Rationale:** Simulated touch recording requires manipulating transport position and automation write state — without save/restore, callers inherit unexpected side effects (moved playhead, changed write mode, stuck touched parameters). Matching API v25 method names exactly avoids confusion. Guardrails (sorted points, position >= 0, value in [0,1], precondition checks) prevent silent failures. Overridden by user — original: no state restoration, generic method names.
**Alternatives considered:** Same as D-9.1.
**Status:** ACTIVE
**ID:** D-9.1a

## 2026-02-28 — API Design: What does writeEnvelope look like? (amended)

**Decision:** `device/writeEnvelope` takes `{index: integer, points: [{position: number, value: number}]}` where `index` is the remote control parameter index (0-7), `position` is in beats (>= 0), and `value` is normalized 0.0-1.0. Preconditions: `isArrangerAutomationWriteEnabled` must be true (error if not), `automationWriteMode` must be set (uses current mode). Input validation: points sorted ascending by position, position >= 0.0, value clamped to [0.0, 1.0], duplicate positions use last-wins. State restoration: saves and restores transport position, play/stop state, and always untouches parameter in a finally block. Returns `{ok: true, pointsWritten: N}`.
**Rationale:** Preconditions tie to Phase 8 controls — the agent must explicitly enable automation write before calling writeEnvelope (no hidden side effects). Input validation prevents silent Bitwig API errors (negative positions, out-of-range values). State restoration is essential — without it, the transport position is left at the last point's position and the parameter may be stuck in touched state if an error occurs. Last-wins for duplicate positions is the simplest deterministic behavior. Overridden by user — original: no preconditions, no input validation, no state restoration.
**Alternatives considered:** Same as D-9.4, plus: (a) Auto-enable automation write — hides state changes. (b) Reject duplicate positions — unnecessarily strict.
**Status:** ACTIVE
**ID:** D-9.4a

## 2026-02-28 — API Design: What per-parameter automation methods to add? (amended)

**Decision:** Add 4 RPC methods matching Bitwig API v25 names exactly: `device/hasAutomation` (`{index}` → `{hasAutomation: boolean}`, wraps `Parameter.hasAutomation()`), `device/deleteAllAutomation` (`{index}` → `{ok: true}`, wraps `Parameter.deleteAllAutomation()`), `device/restoreAutomationControl` (`{index}` → `{ok: true}`, wraps `Parameter.restoreAutomationControl()`), `device/touch` (`{index, touched: boolean}`, wraps `Parameter.touch(boolean)`). All operate on `remoteControlsPage.getParameter(index)`.
**Rationale:** Method names match the Bitwig API v25 exactly: `deleteAllAutomation` (not `deleteAutomation`), `restoreAutomationControl` (not `restoreControl`), `touch` (not `touchParameter`). This prevents confusion when cross-referencing the API docs. Overridden by user — original: `deleteAutomation`, `restoreControl`, `touchParameter`.
**Alternatives considered:** Same as D-9.5.
**Status:** ACTIVE
**ID:** D-9.5a
