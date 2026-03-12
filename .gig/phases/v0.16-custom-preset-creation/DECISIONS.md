# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-12 — Scope: What to build for custom preset creation

**Decision:** Add a `device/setParameters` batch RPC method for efficient multi-page parameter writes, and comprehensively rewrite the system prompt's sound design section to teach from-scratch sound creation instead of preset browsing. Skip building an async "scan all pages" method — the existing `device/selectPage` + `session/snapshot` flow is sufficient for parameter discovery.
**Rationale:** The user wants Claude to CREATE sounds, not pick presets. The biggest gap is Claude's guidance (system prompt), not the API. The existing API already supports parameter discovery and setting — it's just not taught in the system prompt. A batch setter improves efficiency (one RPC call vs 20+) but the system prompt rewrite is the highest-impact change. An async multi-page scanner adds architectural complexity (CompletableFuture threading changes) with marginal benefit over sequential page snapshots.
**Alternatives considered:** (1) Build async `device/getAllParameters` scanner — requires changes to CommandQueue/handler return model, high complexity. (2) System prompt only — misses efficiency gains from batch setter. (3) Embed device parameter maps in system prompt — too brittle, varies by Bitwig version, better to discover at runtime.
**Status:** ACTIVE
**ID:** D-16.1

## 2026-03-12 — Batch Parameter Setter: How device/setParameters works

**Decision:** Add `device/setParameters` that accepts `{pages: [{pageIndex, params: [{index, value}]}]}`. Uses TaskScheduler to apply across flush cycles: in the initial handler call, navigate to the first page and set its params; for each subsequent page, schedule a delayed callback that navigates and sets. Returns immediately with `{ok: true, pageCount: N, paramCount: N}`. Parameter writes don't need to wait for observer feedback — Bitwig processes `value().set()` calls in order.
**Rationale:** When creating a sound from scratch, Claude needs to set 15-25 parameters across 3-5 pages. Without batch setting, that's 20+ sequential RPC calls (selectPage + setParameterValue × N, repeat). The batch setter reduces this to 1 RPC call. The TaskScheduler pattern is proven by MacroHandler (macro/buildSection uses the same deferred-execution approach). Setting values via `set()` within the same flush cycle as a page change works because Bitwig queues operations internally. However, we space page changes across flush cycles for safety.
**Alternatives considered:** (1) Synchronous multi-page set in single handler — risky if page rebinding is deferred. (2) Client-side sequential calls — works but 20+ round trips is slow. (3) Fire all page changes + sets in one flush — untested, could cause race conditions.
**Status:** ACTIVE
**ID:** D-16.2

## 2026-03-12 — System Prompt: From-scratch sound design methodology

**Decision:** Rewrite the "Sound Design Recipes" section in `system-prompt.md` to focus on creating sounds from scratch. Structure: (1) **Discover** — scan device pages via selectPage + snapshot to learn available parameters, (2) **Design** — apply synthesis knowledge to choose parameter values for the desired sound, (3) **Apply** — use device/setParameters to write all values at once. Replace preset-browsing recipes with from-scratch recipes that specify the parameter WORKFLOW (which pages to visit, what to look for) rather than specific parameter names (which vary by device). Include synthesis principles: oscillator types and their character, filter types and their use cases, envelope shaping for different articulations, modulation strategies for movement.
**Rationale:** The current recipes say "filter cutoff ~0.3" but don't teach WHY or how to discover the parameter. Claude needs to understand synthesis principles to create sounds on ANY device, not just memorized values for specific devices. The discover→design→apply workflow teaches Claude to be adaptive. Parameter names vary between Polymer, Polysynth, Phase-4, etc., so recipes based on page tags and synthesis concepts are more robust than device-specific parameter indices.
**Alternatives considered:** (1) Device-specific parameter maps — brittle, requires Bitwig running to document, breaks across versions. (2) Keep preset-browsing focus — contradicts user's stated goal. (3) Minimal changes — misses the core problem of Claude not knowing HOW to design sounds from scratch.
**Status:** ACTIVE
**ID:** D-16.3

## 2026-03-12 — Tool definition and testing

**Decision:** Add tool definition for `device/setParameters` to claude-tools.json. Add Mockito behavioral tests to DeviceHandlerTest verifying page navigation and parameter setting across the TaskScheduler. Test with a 2-page scenario: verify page 0 params are set immediately, page 1 params are scheduled via TaskScheduler.
**Rationale:** Standard practice — every new RPC method needs a tool definition and tests. The TaskScheduler behavior is the critical piece to verify.
**Alternatives considered:** None — required for quality standards.
**Status:** ACTIVE
**ID:** D-16.4
