# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-14 — Scope: What project capabilities to add?

**Decision:** Add cue mix control (get/set cueVolume and cueMix) and scene creation (createScene, createSceneFromPlayingLauncherClips). Skip track group references — getRootTrackGroup/getShownTopLevelTrackGroup return Track objects that need child traversal, which is architecturally complex and low value for RPC. The cue mix and scene creation are the practical wins.
**Rationale:** Cue volume/mix enables headphone monitoring control — critical for live monitoring workflows. Scene creation from playing clips is a powerful workflow tool (capture current state as a scene). Track group references would require creating sub-track-banks per group — significant wiring for marginal RPC value. Rename phase to "Cue Mix & Scene Creation" to reflect actual scope.
**Alternatives considered:** Including track group traversal (too complex — needs child track banks, out of scope), only cue mix (missing easy scene creation wins).
**Status:** ACTIVE
**ID:** D-1.1

## 2026-03-14 — Cue mix: Cache in StateCache or read live?

**Decision:** Cache cueVolume and cueMix in StateCache as doubles. Register observers via `Parameter.value().addValueObserver()`. Include in the project section of the snapshot (alongside hasSoloedTracks etc.) and in delta. Also expose `project/setCueVolume` and `project/setCueMix` RPCs in ProjectHandler.
**Rationale:** Cue mix values rarely change (user adjusts once for monitoring), so delta noise is negligible. Caching in the snapshot means Claude sees the full project state in one call. The set RPCs follow the same pattern as transport_setTempo — direct Parameter.value().set().
**Alternatives considered:** Poll-only RPC (inconsistent with other project state in snapshot), separate CueHandler (only 4 methods — over-engineering).
**Status:** ACTIVE
**ID:** D-1.2

## 2026-03-14 — Scene creation: Where to add?

**Decision:** Add `scene/create` and `scene/createFromPlaying` to SceneHandler (which already handles scene_launch). These call `project.createScene()` and `project.createSceneFromPlayingLauncherClips()` respectively. SceneHandler needs the Project reference passed to it.
**Rationale:** Scene operations belong in SceneHandler, not ProjectHandler. The Project object is needed because these are Project-level methods, but they create scenes — conceptually scene operations. SceneHandler already exists and handles scene_launch/scene_rename.
**Alternatives considered:** Adding to ProjectHandler (scene operations should be grouped together in SceneHandler), new SceneCreationHandler (over-engineering for 2 methods).
**Status:** ACTIVE
**ID:** D-1.3

## 2026-03-14 — Cue mix: Include cue state in project/getState?

**Decision:** Extend the existing `project/getState` RPC to include `cueVolume` and `cueMix` values alongside the existing boolean flags. No new RPC needed — the getState method already returns project state from StateCache.
**Rationale:** project/getState already reads from StateCache. Adding two doubles to the response is trivial and keeps all project state in one place. Claude already calls project/getState to check solo/mute/arm status — cue values come along for free.
**Alternatives considered:** Separate project/getCueState (unnecessary fragmentation), only in snapshot (misses the dedicated getState RPC).
**Status:** ACTIVE
**ID:** D-1.4
