# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-12 — Scope: What to build for sound design

**Decision:** Add preset navigation RPC methods, modulated parameter value observation, and comprehensive system prompt updates with sound design recipes. Skip deprecated Macro API — Remote Controls pages already provide equivalent functionality. Defer layer bank listing and sample browsing to future phases.
**Rationale:** Preset cycling and modulated value readback are the two highest-impact API gaps for sound design. The system prompt is the primary interface for guiding Claude's sound design behavior — recipes for bass/leads/pads/ambient will have immediate impact. Macros are deprecated since Bitwig 2.x; Remote Controls (already supported) replaced them. Layer listing and sample browsing are nice-to-have but not critical for the stated workflow.
**Alternatives considered:** (1) Add macro control — deprecated API, Remote Controls serve the same purpose. (2) Full layer bank query — complex, low ROI for basic sound design. (3) Only system prompt changes — misses real API capability gaps.
**Status:** ACTIVE
**ID:** D-15.1

## 2026-03-12 — Preset Navigation: Which RPC methods to add

**Decision:** Add 6 preset navigation methods to DeviceHandler: `device/nextPreset`, `device/previousPreset`, `device/nextPresetCategory`, `device/previousPresetCategory`, `device/nextPresetCreator`, `device/previousPresetCreator`. Each calls the corresponding `CursorDevice.switchTo*()` method. Return `{ok: true}` — the snapshot will reflect the new preset name on the next flush. Mirror all 6 on MasterDeviceHandler for master bus devices.
**Rationale:** These map 1:1 to existing Bitwig API v25 methods on CursorDevice. Preset cycling is the fastest way to audition sounds — far faster than the browser flow. Including creator navigation enables filtering by vendor (Bitwig, third-party). Master bus needs parity for FX preset browsing.
**Alternatives considered:** (1) Only next/prev preset — too limited, can't jump categories. (2) Load preset by index — `loadPreset()` exists but requires knowing the preset list, more complex. (3) Add to browser flow only — slower, more steps.
**Status:** ACTIVE
**ID:** D-15.2

## 2026-03-12 — Modulated Values: How to expose parameter modulation state

**Decision:** Add `Parameter.modulatedValue()` observation to StateCache alongside existing parameter values. Store as `paramModulatedValues[8]` for track device and `masterParamModulatedValues[8]` for master device. Include `modulatedValue` in the snapshot's device/masterDevice `parameters` array objects. This lets Claude see what a parameter is actually doing after modulation (e.g., an LFO modulating filter cutoff).
**Rationale:** During sound design, parameters are frequently modulated by LFOs, envelopes, and other sources. The current `value` field shows the "base" value but not the live modulated result. Seeing both enables Claude to understand what modulation is doing and suggest adjustments. The API method `Parameter.modulatedValue()` returns a `RangedValue` with standard `addValueObserver`.
**Alternatives considered:** (1) Skip modulated values — leaves a blind spot during modulation-heavy sound design. (2) Expose full modulation source info — much more complex, low incremental value vs just seeing the result.
**Status:** ACTIVE
**ID:** D-15.3

## 2026-03-12 — System Prompt: Sound design recipes by category

**Decision:** Add a "Sound Design Recipes" section to `system-prompt.md` with structured guidance for 5 sound categories: **Bass** (sub, pluck, growl), **Leads** (mono, poly, pluck), **Pads** (warm, evolving, ambient), **Ambient/Texture** (drones, atmospheres, risers), and **Drums/Percussion** (layered kicks, snares, hats via Drum Machine). Each recipe includes: recommended Bitwig device, parameter page tag sequence, key parameter ranges, and common FX chains. Include a "Sound Design Workflow" section explaining the preset→tweak→layer→FX pipeline.
**Rationale:** The system prompt is what guides Claude when using the extension. Without sound-design-specific guidance, Claude must guess at parameter values and workflows. Structured recipes with specific parameter ranges (e.g., "filter cutoff 0.2–0.4 for warm bass") give Claude actionable starting points. Bitwig's built-in devices (Polymer, Polysynth, Phase-4, Sampler) are well-suited to these categories.
**Alternatives considered:** (1) Generic "adjust parameters" guidance — too vague to be useful. (2) External documentation link — not accessible during tool use. (3) Per-device parameter guides — too verbose, recipes are more actionable.
**Status:** ACTIVE
**ID:** D-15.4

## 2026-03-12 — Tool Definitions: Update claude-tools.json

**Decision:** Add tool definitions for the 12 new preset navigation methods (6 track device + 6 master device) to `claude-tools.json`. No new parameters needed — all are parameterless methods. Group under existing "Device Control" and "Master Device Control" sections.
**Rationale:** Tools in claude-tools.json are what Claude sees in the MCP tool list. Without definitions, the new RPC methods would be invisible. Parameterless methods have the simplest tool definitions.
**Alternatives considered:** None — tools must be defined for Claude to use them.
**Status:** ACTIVE
**ID:** D-15.5

## 2026-03-12 — Testing: How to test new methods

**Decision:** Add unit tests for the new DeviceHandler and MasterDeviceHandler preset navigation methods using the existing Mockito-based test pattern. ~6 tests per handler (one per new method) verifying the correct CursorDevice API method is called. Add StateCache tests for the new `paramModulatedValues` fields using the existing reflection helper. No integration tests needed — these are simple pass-through methods.
**Rationale:** Follows established test patterns from Phases 5-14. Preset navigation methods are trivial pass-through (handler → CursorDevice.switchTo*()), so behavioral verification is sufficient. Modulated values follow the same observer pattern already tested in StateCacheObserverTest.
**Alternatives considered:** (1) Skip tests — violates project quality standards. (2) Integration tests — overkill for simple pass-through methods.
**Status:** ACTIVE
**ID:** D-15.6
