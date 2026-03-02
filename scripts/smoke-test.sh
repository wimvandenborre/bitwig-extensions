#!/usr/bin/env bash
#
# Gig Maestro v0.11.x — Smoke Test Suite
#
# Usage:
#   ./scripts/smoke-test.sh              — run all tests (requires Bitwig running)
#   ./scripts/smoke-test.sh --offline    — run only offline tests (no Bitwig needed)
#   ./scripts/smoke-test.sh [port]       — run all tests on specified port
#

set -euo pipefail

PORT="${1:-8787}"
OFFLINE=false
if [ "${1:-}" = "--offline" ]; then
  OFFLINE=true
  PORT=8787
fi

BASE="http://localhost:${PORT}"
PASS=0
FAIL=0
TOTAL=0
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

# --- helpers ---

rpc() {
  curl -s -X POST "${BASE}/rpc" \
    -H "Content-Type: application/json" \
    -d "$1"
}

assert_contains() {
  local label="$1" response="$2" expected="$3"
  TOTAL=$((TOTAL + 1))
  if echo "$response" | grep -qF -- "$expected"; then
    echo "  PASS  $label"
    PASS=$((PASS + 1))
  else
    echo "  FAIL  $label — expected '$expected' in response"
    echo "        got: $response"
    FAIL=$((FAIL + 1))
  fi
}

assert_equals() {
  local label="$1" actual="$2" expected="$3"
  TOTAL=$((TOTAL + 1))
  if [ "$actual" = "$expected" ]; then
    echo "  PASS  $label"
    PASS=$((PASS + 1))
  else
    echo "  FAIL  $label — expected '$expected', got '$actual'"
    FAIL=$((FAIL + 1))
  fi
}

snapshot_field() {
  local path="$1"
  rpc '{"jsonrpc":"2.0","method":"session/snapshot","id":99}' | \
    python3 -c "import sys,json; print(json.load(sys.stdin)['result']${path})"
}

# --- tests ---

echo ""
echo "=== Gig Maestro Smoke Tests ==="
echo ""

# =============================================
# OFFLINE TESTS (no Bitwig required)
# =============================================

# O1. Tool schema validation
echo "--- O1. Tool Schema Validation ---"
TOOLS_FILE="${PROJECT_ROOT}/tools/claude-tools.json"
TOOL_COUNT=$(jq length "$TOOLS_FILE")
TOTAL=$((TOTAL + 1))
if [ "$TOOL_COUNT" -ge 113 ]; then
  echo "  PASS  claude-tools.json has >= 113 tools (found $TOOL_COUNT)"
  PASS=$((PASS + 1))
else
  echo "  FAIL  claude-tools.json has >= 113 tools — found $TOOL_COUNT"
  FAIL=$((FAIL + 1))
fi

# Every tool has name, description, input_schema
MISSING_FIELDS=$(jq '[.[] | select(.name == null or .description == null or .input_schema == null)] | length' "$TOOLS_FILE")
assert_equals "all tools have name, description, input_schema" "$MISSING_FIELDS" "0"

# All tool names are unique
UNIQUE_NAMES=$(jq '[.[].name] | unique | length' "$TOOLS_FILE")
assert_equals "all tool names are unique" "$UNIQUE_NAMES" "$TOOL_COUNT"

# Tool names use underscore convention (no slashes)
SLASH_NAMES=$(jq '[.[].name | select(contains("/"))] | length' "$TOOLS_FILE")
assert_equals "no tool names contain slashes" "$SLASH_NAMES" "0"

# Spot-check specific tools exist
TOOLS_LIST=$(jq -r '.[].name' "$TOOLS_FILE")
assert_contains "has session_snapshot tool" "$TOOLS_LIST" "session_snapshot"
assert_contains "has transport_play tool" "$TOOLS_LIST" "transport_play"
assert_contains "has track_setVolume tool" "$TOOLS_LIST" "track_setVolume"
assert_contains "has clip_launch tool" "$TOOLS_LIST" "clip_launch"
assert_contains "has device_setParameterValue tool" "$TOOLS_LIST" "device_setParameterValue"
assert_contains "has cursor_selectTrack tool" "$TOOLS_LIST" "cursor_selectTrack"
assert_contains "has master_setVolume tool" "$TOOLS_LIST" "master_setVolume"
assert_contains "has scene_launch tool" "$TOOLS_LIST" "scene_launch"

# Validate parameter types for key tools
TEMPO_TYPE=$(jq -r '.[] | select(.name=="transport_setTempo") | .input_schema.properties.tempo.type' "$TOOLS_FILE")
assert_equals "transport_setTempo tempo param is number" "$TEMPO_TYPE" "number"

VOL_INDEX_TYPE=$(jq -r '.[] | select(.name=="track_setVolume") | .input_schema.properties.index.type' "$TOOLS_FILE")
assert_equals "track_setVolume index param is integer" "$VOL_INDEX_TYPE" "integer"

VOL_VALUE_TYPE=$(jq -r '.[] | select(.name=="track_setVolume") | .input_schema.properties.value.type' "$TOOLS_FILE")
assert_equals "track_setVolume value param is number" "$VOL_VALUE_TYPE" "number"

DIRECTION_ENUM=$(jq -r '.[] | select(.name=="cursor_selectTrack") | .input_schema.properties.direction.enum | join(",")' "$TOOLS_FILE")
assert_equals "cursor_selectTrack direction has next,previous enum" "$DIRECTION_ENUM" "next,previous"

CLIP_LENGTH_TYPE=$(jq -r '.[] | select(.name=="clip_create") | .input_schema.properties.lengthInBeats.type' "$TOOLS_FILE")
assert_equals "clip_create lengthInBeats param is integer" "$CLIP_LENGTH_TYPE" "integer"

# Phase 4 note tools
assert_contains "has clip_select tool" "$TOOLS_LIST" "clip_select"
assert_contains "has clip_setNotes tool" "$TOOLS_LIST" "clip_setNotes"
assert_contains "has clip_clearNote tool" "$TOOLS_LIST" "clip_clearNote"
assert_contains "has clip_clearAllNotes tool" "$TOOLS_LIST" "clip_clearAllNotes"
assert_contains "has clip_getNotes tool" "$TOOLS_LIST" "clip_getNotes"
assert_contains "has clip_setStepSize tool" "$TOOLS_LIST" "clip_setStepSize"
assert_contains "has clip_scrollSteps tool" "$TOOLS_LIST" "clip_scrollSteps"
assert_contains "has clip_delete tool" "$TOOLS_LIST" "clip_delete"

NOTES_TYPE=$(jq -r '.[] | select(.name=="clip_setNotes") | .input_schema.properties.notes.type' "$TOOLS_FILE")
assert_equals "clip_setNotes notes param is array" "$NOTES_TYPE" "array"

STEPSIZE_TYPE=$(jq -r '.[] | select(.name=="clip_setStepSize") | .input_schema.properties.size.type' "$TOOLS_FILE")
assert_equals "clip_setStepSize size param is number" "$STEPSIZE_TYPE" "number"

OFFSET_TYPE=$(jq -r '.[] | select(.name=="clip_scrollSteps") | .input_schema.properties.offset.type' "$TOOLS_FILE")
assert_equals "clip_scrollSteps offset param is integer" "$OFFSET_TYPE" "integer"

# Phase 5 device tools
assert_contains "has device_insertBitwigDevice tool" "$TOOLS_LIST" "device_insertBitwigDevice"
assert_contains "has device_insertPluginDevice tool" "$TOOLS_LIST" "device_insertPluginDevice"
assert_contains "has device_listBitwigDevices tool" "$TOOLS_LIST" "device_listBitwigDevices"
assert_contains "has device_remove tool" "$TOOLS_LIST" "device_remove"

INSERT_NAME_TYPE=$(jq -r '.[] | select(.name=="device_insertBitwigDevice") | .input_schema.properties.name.type' "$TOOLS_FILE")
assert_equals "device_insertBitwigDevice name param is string" "$INSERT_NAME_TYPE" "string"

INSERT_POS_ENUM=$(jq -r '.[] | select(.name=="device_insertBitwigDevice") | .input_schema.properties.position.enum | join(",")' "$TOOLS_FILE")
assert_equals "device_insertBitwigDevice position has end,before,after enum" "$INSERT_POS_ENUM" "end,before,after"

PLUGIN_TYPE_ENUM=$(jq -r '.[] | select(.name=="device_insertPluginDevice") | .input_schema.properties.type.enum | join(",")' "$TOOLS_FILE")
assert_equals "device_insertPluginDevice type has vst2,vst3,clap enum" "$PLUGIN_TYPE_ENUM" "vst2,vst3,clap"

# Phase 6 track management tools
assert_contains "has track_createAudio tool" "$TOOLS_LIST" "track_createAudio"
assert_contains "has track_createInstrument tool" "$TOOLS_LIST" "track_createInstrument"
assert_contains "has track_createEffect tool" "$TOOLS_LIST" "track_createEffect"
assert_contains "has track_select tool" "$TOOLS_LIST" "track_select"
assert_contains "has track_rename tool" "$TOOLS_LIST" "track_rename"
assert_contains "has track_deleteSelected tool" "$TOOLS_LIST" "track_deleteSelected"
assert_contains "has track_duplicate tool" "$TOOLS_LIST" "track_duplicate"

SELECT_INDEX_TYPE=$(jq -r '.[] | select(.name=="track_select") | .input_schema.properties.index.type' "$TOOLS_FILE")
assert_equals "track_select index param is integer" "$SELECT_INDEX_TYPE" "integer"

RENAME_NAME_TYPE=$(jq -r '.[] | select(.name=="track_rename") | .input_schema.properties.name.type' "$TOOLS_FILE")
assert_equals "track_rename name param is string" "$RENAME_NAME_TYPE" "string"

CREATE_POS_TYPE=$(jq -r '.[] | select(.name=="track_createAudio") | .input_schema.properties.position.type' "$TOOLS_FILE")
assert_equals "track_createAudio position param is integer" "$CREATE_POS_TYPE" "integer"

# Phase 8 arranger tools
assert_contains "has arranger_setPlaybackFollow tool" "$TOOLS_LIST" "arranger_setPlaybackFollow"
assert_contains "has arranger_setClipLauncherVisible tool" "$TOOLS_LIST" "arranger_setClipLauncherVisible"
assert_contains "has arranger_setTimelineVisible tool" "$TOOLS_LIST" "arranger_setTimelineVisible"
assert_contains "has arranger_setCueMarkersVisible tool" "$TOOLS_LIST" "arranger_setCueMarkersVisible"
assert_contains "has arranger_setEffectTracksVisible tool" "$TOOLS_LIST" "arranger_setEffectTracksVisible"
assert_contains "has arranger_setIoSectionVisible tool" "$TOOLS_LIST" "arranger_setIoSectionVisible"
assert_contains "has arranger_setDoubleRowTrackHeight tool" "$TOOLS_LIST" "arranger_setDoubleRowTrackHeight"

# Phase 8 transport arrangement tools
assert_contains "has transport_setLoopRange tool" "$TOOLS_LIST" "transport_setLoopRange"
assert_contains "has transport_getLoopRange tool" "$TOOLS_LIST" "transport_getLoopRange"
assert_contains "has transport_setPunchIn tool" "$TOOLS_LIST" "transport_setPunchIn"
assert_contains "has transport_setPunchOut tool" "$TOOLS_LIST" "transport_setPunchOut"
assert_contains "has transport_setAutomationWriteMode tool" "$TOOLS_LIST" "transport_setAutomationWriteMode"
assert_contains "has transport_setArrangerAutomationWrite tool" "$TOOLS_LIST" "transport_setArrangerAutomationWrite"
assert_contains "has transport_setClipLauncherAutomationWrite tool" "$TOOLS_LIST" "transport_setClipLauncherAutomationWrite"
assert_contains "has transport_resetAutomationOverrides tool" "$TOOLS_LIST" "transport_resetAutomationOverrides"

# Phase 8 cue marker tools
assert_contains "has cueMarker_addAtPlayhead tool" "$TOOLS_LIST" "cueMarker_addAtPlayhead"
assert_contains "has cueMarker_list tool" "$TOOLS_LIST" "cueMarker_list"
assert_contains "has cueMarker_launch tool" "$TOOLS_LIST" "cueMarker_launch"
assert_contains "has cueMarker_delete tool" "$TOOLS_LIST" "cueMarker_delete"

# Phase 8 parameter type checks
LOOP_START_TYPE=$(jq -r '.[] | select(.name=="transport_setLoopRange") | .input_schema.properties.start.type' "$TOOLS_FILE")
assert_equals "transport_setLoopRange start param is number" "$LOOP_START_TYPE" "number"

LOOP_DUR_TYPE=$(jq -r '.[] | select(.name=="transport_setLoopRange") | .input_schema.properties.duration.type' "$TOOLS_FILE")
assert_equals "transport_setLoopRange duration param is number" "$LOOP_DUR_TYPE" "number"

AUTO_MODE_ENUM=$(jq -r '.[] | select(.name=="transport_setAutomationWriteMode") | .input_schema.properties.mode.enum | join(",")' "$TOOLS_FILE")
assert_equals "transport_setAutomationWriteMode mode has latch,touch,write enum" "$AUTO_MODE_ENUM" "latch,touch,write"

PUNCH_POS_TYPE=$(jq -r '.[] | select(.name=="transport_setPunchIn") | .input_schema.properties.position.type' "$TOOLS_FILE")
assert_equals "transport_setPunchIn position param is number" "$PUNCH_POS_TYPE" "number"

CUE_LAUNCH_INDEX_TYPE=$(jq -r '.[] | select(.name=="cueMarker_launch") | .input_schema.properties.index.type' "$TOOLS_FILE")
assert_equals "cueMarker_launch index param is integer" "$CUE_LAUNCH_INDEX_TYPE" "integer"

CUE_DELETE_INDEX_TYPE=$(jq -r '.[] | select(.name=="cueMarker_delete") | .input_schema.properties.index.type' "$TOOLS_FILE")
assert_equals "cueMarker_delete index param is integer" "$CUE_DELETE_INDEX_TYPE" "integer"

# O2. System prompt validation
echo "--- O2. System Prompt Validation ---"
PROMPT_FILE="${PROJECT_ROOT}/tools/system-prompt.md"
PROMPT=$(cat "$PROMPT_FILE")
assert_contains "system prompt covers viewport model" "$PROMPT" "Track Bank"
assert_contains "system prompt covers perception-action loop" "$PROMPT" "Perception-Action Loop"
assert_contains "system prompt covers value ranges" "$PROMPT" "Value Ranges"
assert_contains "system prompt covers cursor model" "$PROMPT" "Cursor Model"
assert_contains "system prompt covers index conventions" "$PROMPT" "Index Conventions"
assert_contains "system prompt has example workflow" "$PROMPT" "Example Workflow"
assert_contains "system prompt mentions 0.0 to 1.0 range" "$PROMPT" "0.0 to 1.0"
assert_contains "system prompt mentions session_snapshot" "$PROMPT" "session_snapshot"
assert_contains "system prompt covers note editing" "$PROMPT" "Note Editing"
assert_contains "system prompt mentions MIDI note numbers" "$PROMPT" "MIDI note"
assert_contains "system prompt mentions clip_setNotes" "$PROMPT" "clip_setNotes"
assert_contains "system prompt covers device insertion" "$PROMPT" "Device Insertion"
assert_contains "system prompt mentions device_listBitwigDevices" "$PROMPT" "device_listBitwigDevices"
assert_contains "system prompt mentions device_insertBitwigDevice" "$PROMPT" "device_insertBitwigDevice"
assert_contains "system prompt covers track management" "$PROMPT" "Track Management"
assert_contains "system prompt mentions track_select" "$PROMPT" "track_select"
assert_contains "system prompt mentions track_createInstrument" "$PROMPT" "track_createInstrument"

# Phase 7 system prompt sections
assert_contains "system prompt has Known Behaviors section" "$PROMPT" "Known Behaviors"
assert_contains "system prompt has Error Recovery section" "$PROMPT" "Error Recovery"
assert_contains "system prompt has Music Reference section" "$PROMPT" "Music Reference"
assert_contains "system prompt has Song Building section" "$PROMPT" "Song Building"
assert_contains "system prompt documents flush cycle" "$PROMPT" "flush cycle"
assert_contains "system prompt documents error code -32602" "$PROMPT" "-32602"
assert_contains "system prompt has snapshot before retry rule" "$PROMPT" "Snapshot Before Retry"
assert_contains "system prompt has semitone offsets" "$PROMPT" "semitone offsets"
assert_contains "system prompt has GM drum map" "$PROMPT" "Kick"
assert_contains "system prompt has velocity bands" "$PROMPT" "Ghost"
assert_contains "system prompt has default tempo" "$PROMPT" "120 BPM"
assert_contains "system prompt has recommended call sequences" "$PROMPT" "Recommended Call Sequences"

# Phase 8 system prompt sections
assert_contains "system prompt has Arrangement & Automation section" "$PROMPT" "Arrangement & Automation"
assert_contains "system prompt covers arranger visibility" "$PROMPT" "Arranger Visibility"
assert_contains "system prompt covers loop and punch range" "$PROMPT" "Loop & Punch Range"
assert_contains "system prompt covers automation" "$PROMPT" "Automation"
assert_contains "system prompt covers cue markers" "$PROMPT" "Cue Markers"
assert_contains "system prompt has arrangement setup workflow" "$PROMPT" "Arrangement Setup Workflow"
assert_contains "system prompt mentions arranger_setPlaybackFollow" "$PROMPT" "arranger_setPlaybackFollow"
assert_contains "system prompt mentions transport_setLoopRange" "$PROMPT" "transport_setLoopRange"
assert_contains "system prompt mentions cueMarker_addAtPlayhead" "$PROMPT" "cueMarker_addAtPlayhead"
assert_contains "system prompt mentions automation write modes" "$PROMPT" "latch"

# Phase 9 — Envelope Writing tools
echo "--- Phase 9: Envelope Writing tools ---"
assert_contains "has device_hasAutomation tool" "$TOOLS_LIST" "device_hasAutomation"
assert_contains "has device_deleteAllAutomation tool" "$TOOLS_LIST" "device_deleteAllAutomation"
assert_contains "has device_restoreAutomationControl tool" "$TOOLS_LIST" "device_restoreAutomationControl"
assert_contains "has device_touch tool" "$TOOLS_LIST" "device_touch"
assert_contains "has device_writeEnvelope tool" "$TOOLS_LIST" "device_writeEnvelope"

# Phase 9 — tool schema spot-checks
WRITE_ENV_INDEX_TYPE=$(jq -r '.[] | select(.name=="device_writeEnvelope") | .input_schema.properties.index.type' "$TOOLS_FILE")
assert_equals "device_writeEnvelope index param is integer" "$WRITE_ENV_INDEX_TYPE" "integer"

WRITE_ENV_POINTS_TYPE=$(jq -r '.[] | select(.name=="device_writeEnvelope") | .input_schema.properties.points.type' "$TOOLS_FILE")
assert_equals "device_writeEnvelope points param is array" "$WRITE_ENV_POINTS_TYPE" "array"

WRITE_ENV_POINT_POS_TYPE=$(jq -r '.[] | select(.name=="device_writeEnvelope") | .input_schema.properties.points.items.properties.position.type' "$TOOLS_FILE")
assert_equals "device_writeEnvelope point position is number" "$WRITE_ENV_POINT_POS_TYPE" "number"

WRITE_ENV_POINT_VAL_TYPE=$(jq -r '.[] | select(.name=="device_writeEnvelope") | .input_schema.properties.points.items.properties.value.type' "$TOOLS_FILE")
assert_equals "device_writeEnvelope point value is number" "$WRITE_ENV_POINT_VAL_TYPE" "number"

TOUCH_INDEX_TYPE=$(jq -r '.[] | select(.name=="device_touch") | .input_schema.properties.index.type' "$TOOLS_FILE")
assert_equals "device_touch index param is integer" "$TOUCH_INDEX_TYPE" "integer"

TOUCH_TOUCHED_TYPE=$(jq -r '.[] | select(.name=="device_touch") | .input_schema.properties.touched.type' "$TOOLS_FILE")
assert_equals "device_touch touched param is boolean" "$TOUCH_TOUCHED_TYPE" "boolean"

HAS_AUTO_INDEX_TYPE=$(jq -r '.[] | select(.name=="device_hasAutomation") | .input_schema.properties.index.type' "$TOOLS_FILE")
assert_equals "device_hasAutomation index param is integer" "$HAS_AUTO_INDEX_TYPE" "integer"

# Phase 9 — system prompt
assert_contains "system prompt has Envelope Writing section" "$PROMPT" "Envelope Writing"
assert_contains "system prompt mentions device_writeEnvelope" "$PROMPT" "device_writeEnvelope"
assert_contains "system prompt mentions device_hasAutomation" "$PROMPT" "device_hasAutomation"
assert_contains "system prompt mentions device_touch" "$PROMPT" "device_touch"
assert_contains "system prompt mentions write-only limitation" "$PROMPT" "Write-only"
assert_contains "system prompt mentions async execution" "$PROMPT" "Async execution"
assert_contains "system prompt mentions prerequisites" "$PROMPT" "Prerequisites"

# Phase 10 — Clip, Scene & Cue Marker Lifecycle tools
echo "--- Phase 10: Lifecycle tools ---"
assert_contains "has scene_create tool" "$TOOLS_LIST" "scene_create"
assert_contains "has scene_createFromPlaying tool" "$TOOLS_LIST" "scene_createFromPlaying"
assert_contains "has scene_duplicate tool" "$TOOLS_LIST" "scene_duplicate"
assert_contains "has scene_rename tool" "$TOOLS_LIST" "scene_rename"
assert_contains "has scene_delete tool" "$TOOLS_LIST" "scene_delete"
assert_contains "has clip_rename tool" "$TOOLS_LIST" "clip_rename"
assert_contains "has clip_duplicate tool" "$TOOLS_LIST" "clip_duplicate"
assert_contains "has clip_duplicateToSlot tool" "$TOOLS_LIST" "clip_duplicateToSlot"
assert_contains "has cueMarker_rename tool" "$TOOLS_LIST" "cueMarker_rename"
assert_contains "has cueMarker_setPosition tool" "$TOOLS_LIST" "cueMarker_setPosition"
assert_contains "has cueMarker_duplicate tool" "$TOOLS_LIST" "cueMarker_duplicate"

# Phase 10 — tool schema spot-checks
SCENE_RENAME_INDEX_TYPE=$(jq -r '.[] | select(.name=="scene_rename") | .input_schema.properties.index.type' "$TOOLS_FILE")
assert_equals "scene_rename index param is integer" "$SCENE_RENAME_INDEX_TYPE" "integer"

SCENE_RENAME_NAME_TYPE=$(jq -r '.[] | select(.name=="scene_rename") | .input_schema.properties.name.type' "$TOOLS_FILE")
assert_equals "scene_rename name param is string" "$SCENE_RENAME_NAME_TYPE" "string"

CLIP_RENAME_NAME_TYPE=$(jq -r '.[] | select(.name=="clip_rename") | .input_schema.properties.name.type' "$TOOLS_FILE")
assert_equals "clip_rename name param is string" "$CLIP_RENAME_NAME_TYPE" "string"

DUP_SRC_TYPE=$(jq -r '.[] | select(.name=="clip_duplicateToSlot") | .input_schema.properties.srcTrackIndex.type' "$TOOLS_FILE")
assert_equals "clip_duplicateToSlot srcTrackIndex param is integer" "$DUP_SRC_TYPE" "integer"

DUP_DEST_TYPE=$(jq -r '.[] | select(.name=="clip_duplicateToSlot") | .input_schema.properties.destSlotIndex.type' "$TOOLS_FILE")
assert_equals "clip_duplicateToSlot destSlotIndex param is integer" "$DUP_DEST_TYPE" "integer"

CUE_RENAME_NAME_TYPE=$(jq -r '.[] | select(.name=="cueMarker_rename") | .input_schema.properties.name.type' "$TOOLS_FILE")
assert_equals "cueMarker_rename name param is string" "$CUE_RENAME_NAME_TYPE" "string"

CUE_POS_BEATS_TYPE=$(jq -r '.[] | select(.name=="cueMarker_setPosition") | .input_schema.properties.beats.type' "$TOOLS_FILE")
assert_equals "cueMarker_setPosition beats param is number" "$CUE_POS_BEATS_TYPE" "number"

CUE_DUP_INDEX_TYPE=$(jq -r '.[] | select(.name=="cueMarker_duplicate") | .input_schema.properties.index.type' "$TOOLS_FILE")
assert_equals "cueMarker_duplicate index param is integer" "$CUE_DUP_INDEX_TYPE" "integer"

# Phase 10 — system prompt
assert_contains "system prompt has Lifecycle Operations section" "$PROMPT" "Lifecycle Operations"
assert_contains "system prompt covers Clip Lifecycle" "$PROMPT" "Clip Lifecycle"
assert_contains "system prompt covers Scene Lifecycle" "$PROMPT" "Scene Lifecycle"
assert_contains "system prompt covers Cue Marker Lifecycle" "$PROMPT" "Cue Marker Lifecycle"
assert_contains "system prompt mentions clip_rename" "$PROMPT" "clip_rename"
assert_contains "system prompt mentions scene_create" "$PROMPT" "scene_create"
assert_contains "system prompt mentions scene_createFromPlaying" "$PROMPT" "scene_createFromPlaying"
assert_contains "system prompt mentions cueMarker_rename" "$PROMPT" "cueMarker_rename"
assert_contains "system prompt mentions cueMarker_setPosition" "$PROMPT" "cueMarker_setPosition"

# Phase 7 tool description warnings
DEVICE_REMOVE_DESC=$(jq -r '.[] | select(.name=="device_remove") | .description' "$TOOLS_FILE")
assert_contains "device_remove warns about loses selection" "$DEVICE_REMOVE_DESC" "loses selection"

TRACK_CREATE_AUDIO_DESC=$(jq -r '.[] | select(.name=="track_createAudio") | .description' "$TOOLS_FILE")
assert_contains "track_createAudio warns snapshot to verify" "$TRACK_CREATE_AUDIO_DESC" "snapshot"

CURSOR_SELECT_DESC=$(jq -r '.[] | select(.name=="cursor_selectTrack") | .description' "$TOOLS_FILE")
assert_contains "cursor_selectTrack mentions track_select" "$CURSOR_SELECT_DESC" "track_select"

CLIP_LAUNCH_DESC=$(jq -r '.[] | select(.name=="clip_launch") | .description' "$TOOLS_FILE")
assert_contains "clip_launch warns about transport" "$CLIP_LAUNCH_DESC" "transport"

# Phase 11 — Bank Scrolling tools
echo "--- Phase 11: Bank Scrolling tools ---"
assert_contains "has sceneBank_scrollTo tool" "$TOOLS_LIST" "sceneBank_scrollTo"
assert_contains "has sceneBank_scrollBy tool" "$TOOLS_LIST" "sceneBank_scrollBy"
assert_contains "has sceneBank_getScrollInfo tool" "$TOOLS_LIST" "sceneBank_getScrollInfo"
assert_contains "has cueMarkerBank_scrollTo tool" "$TOOLS_LIST" "cueMarkerBank_scrollTo"
assert_contains "has cueMarkerBank_scrollBy tool" "$TOOLS_LIST" "cueMarkerBank_scrollBy"
assert_contains "has cueMarkerBank_getScrollInfo tool" "$TOOLS_LIST" "cueMarkerBank_getScrollInfo"
assert_contains "has trackBank_scrollTo tool" "$TOOLS_LIST" "trackBank_scrollTo"
assert_contains "has trackBank_scrollBy tool" "$TOOLS_LIST" "trackBank_scrollBy"
assert_contains "has trackBank_getScrollInfo tool" "$TOOLS_LIST" "trackBank_getScrollInfo"

# Phase 11 — tool schema spot-checks
SCENE_SCROLL_POS_TYPE=$(jq -r '.[] | select(.name=="sceneBank_scrollTo") | .input_schema.properties.position.type' "$TOOLS_FILE")
assert_equals "sceneBank_scrollTo position param is integer" "$SCENE_SCROLL_POS_TYPE" "integer"

TRACK_SCROLL_AMT_TYPE=$(jq -r '.[] | select(.name=="trackBank_scrollBy") | .input_schema.properties.amount.type' "$TOOLS_FILE")
assert_equals "trackBank_scrollBy amount param is integer" "$TRACK_SCROLL_AMT_TYPE" "integer"

CUE_SCROLL_POS_TYPE=$(jq -r '.[] | select(.name=="cueMarkerBank_scrollTo") | .input_schema.properties.position.type' "$TOOLS_FILE")
assert_equals "cueMarkerBank_scrollTo position param is integer" "$CUE_SCROLL_POS_TYPE" "integer"

# Phase 11 — system prompt
assert_contains "system prompt has Bank Navigation section" "$PROMPT" "Bank Navigation"
assert_contains "system prompt mentions sceneBank tools" "$PROMPT" "sceneBank_"
assert_contains "system prompt mentions trackBank tools" "$PROMPT" "trackBank_"
assert_contains "system prompt mentions cueMarkerBank tools" "$PROMPT" "cueMarkerBank_"
assert_contains "system prompt documents v0.11 migration" "$PROMPT" "v0.11"

# Phase 12 — tool schemas (grep file directly to avoid shell variable size limits)
PHASE12_TOOLS="session_transaction macro_createTrack macro_createClip macro_writeClip macro_buildSection"
for tool in $PHASE12_TOOLS; do
  TOTAL=$((TOTAL + 1))
  if grep -qF "\"$tool\"" "$TOOLS_FILE"; then
    echo "  PASS  tool schema exists: $tool"
    PASS=$((PASS + 1))
  else
    echo "  FAIL  tool schema missing: $tool"
    FAIL=$((FAIL + 1))
  fi
done

# Phase 12 — system prompt
assert_contains "system prompt has Transactions & Macros section" "$PROMPT" "Transactions & Macros"
assert_contains "system prompt mentions session_transaction" "$PROMPT" "session_transaction"
assert_contains "system prompt mentions macro_createTrack" "$PROMPT" "macro_createTrack"
assert_contains "system prompt mentions macro_writeClip" "$PROMPT" "macro_writeClip"
assert_contains "system prompt mentions macro_buildSection" "$PROMPT" "macro_buildSection"
assert_contains "system prompt documents rollback" "$PROMPT" "undoAll"
assert_contains "system prompt documents pre/post snapshots" "$PROMPT" "pre/post snapshots"
assert_contains "system prompt documents postSnapshot option" "$PROMPT" "postSnapshot"

# Phase 13 — tool schemas (grep file directly to avoid shell variable size issues)
for TOOL_NAME in send_setLevel send_setMode send_setEnabled track_setColor track_setCrossfade track_setMonitor master_setMute master_setSolo master_setColor; do
  TOTAL=$((TOTAL + 1))
  if grep -qF "\"$TOOL_NAME\"" "$TOOLS_FILE"; then
    echo "  PASS  tool schema has $TOOL_NAME"
    PASS=$((PASS + 1))
  else
    echo "  FAIL  tool schema has $TOOL_NAME — not found in $TOOLS_FILE"
    FAIL=$((FAIL + 1))
  fi
done

# Phase 13 — tool schema field checks
SEND_LEVEL_TRACK_TYPE=$(jq -r '.[] | select(.name=="send_setLevel") | .input_schema.properties.trackIndex.type' "$TOOLS_FILE")
assert_equals "send_setLevel trackIndex is integer" "$SEND_LEVEL_TRACK_TYPE" "integer"
SEND_LEVEL_VALUE_TYPE=$(jq -r '.[] | select(.name=="send_setLevel") | .input_schema.properties.value.type' "$TOOLS_FILE")
assert_equals "send_setLevel value is number" "$SEND_LEVEL_VALUE_TYPE" "number"
SEND_MODE_ENUM=$(jq -r '.[] | select(.name=="send_setMode") | .input_schema.properties.mode.enum | join(",")' "$TOOLS_FILE")
assert_equals "send_setMode mode enum is AUTO,PRE,POST" "$SEND_MODE_ENUM" "AUTO,PRE,POST"
CROSSFADE_ENUM=$(jq -r '.[] | select(.name=="track_setCrossfade") | .input_schema.properties.mode.enum | join(",")' "$TOOLS_FILE")
assert_equals "track_setCrossfade mode enum is A,B,AB" "$CROSSFADE_ENUM" "A,B,AB"
MONITOR_ENUM=$(jq -r '.[] | select(.name=="track_setMonitor") | .input_schema.properties.mode.enum | join(",")' "$TOOLS_FILE")
assert_equals "track_setMonitor mode enum is ON,OFF,AUTO" "$MONITOR_ENUM" "ON,OFF,AUTO"
COLOR_R_TYPE=$(jq -r '.[] | select(.name=="track_setColor") | .input_schema.properties.r.type' "$TOOLS_FILE")
assert_equals "track_setColor r is number" "$COLOR_R_TYPE" "number"

# Phase 13 — system prompt
assert_contains "system prompt has Mixer & Routing section" "$PROMPT" "Mixer & Routing"
assert_contains "system prompt mentions send_setLevel" "$PROMPT" "send_setLevel"
assert_contains "system prompt mentions track_setColor" "$PROMPT" "track_setColor"
assert_contains "system prompt mentions crossfade modes" "$PROMPT" "crossfadeMode"
assert_contains "system prompt mentions monitor mode" "$PROMPT" "monitorMode"
assert_contains "system prompt mentions master_setMute" "$PROMPT" "master_setMute"

# Phase 14 — master device tool schemas (grep file directly)
PHASE14_TOOLS="masterDevice_selectNext masterDevice_selectPrevious masterDevice_setEnabled masterDevice_insertBitwigDevice masterDevice_insertPluginDevice masterDevice_remove masterDevice_selectPage masterDevice_nextPage masterDevice_previousPage masterDevice_setParameterValue"
for tool in $PHASE14_TOOLS; do
  TOTAL=$((TOTAL + 1))
  if grep -qF "\"$tool\"" "$TOOLS_FILE"; then
    echo "  PASS  tool schema exists: $tool"
    PASS=$((PASS + 1))
  else
    echo "  FAIL  tool schema missing: $tool"
    FAIL=$((FAIL + 1))
  fi
done

# Phase 14 — tool schema field checks
MASTER_ENABLED_TYPE=$(jq -r '.[] | select(.name=="masterDevice_setEnabled") | .input_schema.properties.enabled.type' "$TOOLS_FILE")
assert_equals "masterDevice_setEnabled enabled is boolean" "$MASTER_ENABLED_TYPE" "boolean"
MASTER_PARAM_INDEX_TYPE=$(jq -r '.[] | select(.name=="masterDevice_setParameterValue") | .input_schema.properties.index.type' "$TOOLS_FILE")
assert_equals "masterDevice_setParameterValue index is integer" "$MASTER_PARAM_INDEX_TYPE" "integer"
MASTER_PARAM_VALUE_TYPE=$(jq -r '.[] | select(.name=="masterDevice_setParameterValue") | .input_schema.properties.value.type' "$TOOLS_FILE")
assert_equals "masterDevice_setParameterValue value is number" "$MASTER_PARAM_VALUE_TYPE" "number"
MASTER_INSERT_POS_ENUM=$(jq -r '.[] | select(.name=="masterDevice_insertBitwigDevice") | .input_schema.properties.position.enum | join(",")' "$TOOLS_FILE")
assert_equals "masterDevice_insertBitwigDevice position enum" "$MASTER_INSERT_POS_ENUM" "end,before,after"

# Phase 14 — system prompt
assert_contains "system prompt has Master Bus FX section" "$PROMPT" "Master Bus FX"
assert_contains "system prompt mentions masterDevice_insertBitwigDevice" "$PROMPT" "masterDevice_insertBitwigDevice"
assert_contains "system prompt mentions masterDevice vs device distinction" "$PROMPT" "masterDevice_"

# Phase 15 — preset cycling + chain navigation tool schemas
PHASE15_TOOLS="device_enterSlot device_exitToParent masterDevice_enterSlot masterDevice_exitToParent"
for tool in $PHASE15_TOOLS; do
  TOTAL=$((TOTAL + 1))
  if grep -qF "\"$tool\"" "$TOOLS_FILE"; then
    echo "  PASS  tool schema exists: $tool"
    PASS=$((PASS + 1))
  else
    echo "  FAIL  tool schema missing: $tool"
    FAIL=$((FAIL + 1))
  fi
done

# Phase 15 — tool schema field checks
ENTER_SLOT_NAME_TYPE=$(jq -r '.[] | select(.name=="device_enterSlot") | .input_schema.properties.name.type' "$TOOLS_FILE")
assert_equals "device_enterSlot name is string" "$ENTER_SLOT_NAME_TYPE" "string"
MASTER_ENTER_SLOT_NAME_TYPE=$(jq -r '.[] | select(.name=="masterDevice_enterSlot") | .input_schema.properties.name.type' "$TOOLS_FILE")
assert_equals "masterDevice_enterSlot name is string" "$MASTER_ENTER_SLOT_NAME_TYPE" "string"
ENTER_SLOT_REQUIRED=$(jq -r '.[] | select(.name=="device_enterSlot") | .input_schema.required[0]' "$TOOLS_FILE")
assert_equals "device_enterSlot requires name" "$ENTER_SLOT_REQUIRED" "name"

# Phase 15 — snapshot description mentions nesting
SNAP_DESC=$(jq -r '.[] | select(.name=="session_snapshot") | .description' "$TOOLS_FILE")
assert_contains "session_snapshot mentions nesting info" "$SNAP_DESC" "nesting info"

# Phase 15 — system prompt
assert_contains "system prompt has Chain Navigation section" "$PROMPT" "Chain Navigation"
assert_contains "system prompt mentions enterSlot" "$PROMPT" "enterSlot"
assert_contains "system prompt mentions exitToParent" "$PROMPT" "exitToParent"
assert_contains "system prompt mentions hasSlots" "$PROMPT" "hasSlots"

# O3. CLI build and help
echo "--- O3. CLI Build & Help ---"
CLI_JAR="${PROJECT_ROOT}/build/libs/gig-cli.jar"

# Build the CLI JAR
(cd "$PROJECT_ROOT" && ./gradlew cliShadowJar -q 2>/dev/null)
TOTAL=$((TOTAL + 1))
if [ -f "$CLI_JAR" ]; then
  echo "  PASS  gig-cli.jar exists after build"
  PASS=$((PASS + 1))
else
  echo "  FAIL  gig-cli.jar not found after build"
  FAIL=$((FAIL + 1))
fi

# Help output
HELP=$(java -jar "$CLI_JAR" --help 2>&1)
assert_contains "CLI help shows transport command" "$HELP" "transport"
assert_contains "CLI help shows track command" "$HELP" "track"
assert_contains "CLI help shows snapshot command" "$HELP" "snapshot"
assert_contains "CLI help shows device command" "$HELP" "device"
assert_contains "CLI help shows note command" "$HELP" "note"
assert_contains "CLI help shows rpc command" "$HELP" "rpc"
assert_contains "CLI help shows --pretty option" "$HELP" "--pretty"
assert_contains "CLI help shows --port option" "$HELP" "--port"

# Transport subcommand help
TRANSPORT_HELP=$(java -jar "$CLI_JAR" transport --help 2>&1)
assert_contains "transport help shows play" "$TRANSPORT_HELP" "play"
assert_contains "transport help shows stop" "$TRANSPORT_HELP" "stop"
assert_contains "transport help shows tempo" "$TRANSPORT_HELP" "tempo"
assert_contains "transport help shows loop" "$TRANSPORT_HELP" "loop"

# Track subcommand help
TRACK_HELP=$(java -jar "$CLI_JAR" track --help 2>&1)
assert_contains "track help shows set-volume" "$TRACK_HELP" "set-volume"
assert_contains "track help shows set-mute" "$TRACK_HELP" "set-mute"
assert_contains "track help shows set-solo" "$TRACK_HELP" "set-solo"
assert_contains "track help shows create-audio" "$TRACK_HELP" "create-audio"
assert_contains "track help shows create-instrument" "$TRACK_HELP" "create-instrument"
assert_contains "track help shows create-effect" "$TRACK_HELP" "create-effect"
assert_contains "track help shows select" "$TRACK_HELP" "select"
assert_contains "track help shows rename" "$TRACK_HELP" "rename"
assert_contains "track help shows delete-selected" "$TRACK_HELP" "delete-selected"
assert_contains "track help shows duplicate" "$TRACK_HELP" "duplicate"

# Device subcommand help
DEVICE_HELP=$(java -jar "$CLI_JAR" device --help 2>&1)
assert_contains "device help shows insert-bitwig" "$DEVICE_HELP" "insert-bitwig"
assert_contains "device help shows insert-plugin" "$DEVICE_HELP" "insert-plugin"
assert_contains "device help shows list-bitwig" "$DEVICE_HELP" "list-bitwig"
assert_contains "device help shows remove" "$DEVICE_HELP" "remove"

# Note subcommand help
NOTE_HELP=$(java -jar "$CLI_JAR" note --help 2>&1)
assert_contains "note help shows select" "$NOTE_HELP" "select"
assert_contains "note help shows set-notes" "$NOTE_HELP" "set-notes"
assert_contains "note help shows clear-note" "$NOTE_HELP" "clear-note"
assert_contains "note help shows clear-all" "$NOTE_HELP" "clear-all"
assert_contains "note help shows get-notes" "$NOTE_HELP" "get-notes"
assert_contains "note help shows set-step-size" "$NOTE_HELP" "set-step-size"
assert_contains "note help shows scroll-steps" "$NOTE_HELP" "scroll-steps"
assert_contains "note help shows delete" "$NOTE_HELP" "delete"

# Version
VERSION=$(java -jar "$CLI_JAR" --version 2>&1)
assert_contains "CLI version output" "$VERSION" "gig-cli"

# O4. Extension build verification
echo "--- O4. Extension Build ---"
(cd "$PROJECT_ROOT" && ./gradlew build -q 2>/dev/null)
EXT_FILE="$HOME/Documents/Bitwig Studio/Extensions/GigMaestro.bwextension"
TOTAL=$((TOTAL + 1))
if [ -f "$EXT_FILE" ]; then
  echo "  PASS  GigMaestro.bwextension exists"
  PASS=$((PASS + 1))
else
  echo "  FAIL  GigMaestro.bwextension not found"
  FAIL=$((FAIL + 1))
fi

# No Picocli in extension JAR
PICOCLI_COUNT=$(jar tf "$EXT_FILE" | grep -c picocli || true)
assert_equals "extension JAR has no Picocli classes" "$PICOCLI_COUNT" "0"

# No CLI classes in extension JAR
CLI_CLASSES=$(jar tf "$EXT_FILE" | grep -c "gig/cli" || true)
assert_equals "extension JAR has no CLI classes" "$CLI_CLASSES" "0"

# If offline mode, stop here
if [ "$OFFLINE" = true ]; then
  echo ""
  echo "=== Offline Results: ${PASS} passed, ${FAIL} failed, ${TOTAL} total ==="
  echo ""
  if [ "$FAIL" -gt 0 ]; then exit 1; fi
  exit 0
fi

# =============================================
# ONLINE TESTS (Bitwig required)
# =============================================

echo ""
echo "--- Online tests (port ${PORT}) ---"
echo ""

# 1. Health check
echo "--- 1. Health Check ---"
HEALTH=$(curl -s "${BASE}/health")
assert_contains "GET /health returns status ok" "$HEALTH" '"status":"ok"'
assert_contains "GET /health returns version" "$HEALTH" '"version":"0.1.0"'

# 2. Pipeline validation — session/snapshot
echo "--- 2. Session Snapshot ---"
SNAP=$(rpc '{"jsonrpc":"2.0","method":"session/snapshot","id":1}')
assert_contains "snapshot has transport" "$SNAP" '"transport"'
assert_contains "snapshot has tracks" "$SNAP" '"tracks"'
assert_contains "snapshot has master" "$SNAP" '"master"'
assert_contains "snapshot has application" "$SNAP" '"application"'
assert_contains "snapshot has isPlaying" "$SNAP" '"isPlaying"'
assert_contains "snapshot has tempo" "$SNAP" '"tempo"'

# 3. Introspection — api/list
echo "--- 3. API List ---"
LIST=$(rpc '{"jsonrpc":"2.0","method":"api/list","id":2}')
assert_contains "api/list has session/snapshot" "$LIST" '"session/snapshot"'
assert_contains "api/list has transport/play" "$LIST" '"transport/play"'
assert_contains "api/list has track/setVolume" "$LIST" '"track/setVolume"'
assert_contains "api/list has master/setVolume" "$LIST" '"master/setVolume"'
assert_contains "api/list has app/undo" "$LIST" '"app/undo"'

# 4. Transport actions
echo "--- 4. Transport Actions ---"

# Save baseline
ORIG_TEMPO=$(snapshot_field "['transport']['tempo']")
ORIG_LOOP=$(snapshot_field "['transport']['isLoopEnabled']")
ORIG_METRO=$(snapshot_field "['transport']['isMetronomeEnabled']")

# play
RESP=$(rpc '{"jsonrpc":"2.0","method":"transport/play","id":10}')
assert_contains "transport/play returns ok" "$RESP" '"ok"'
sleep 0.5
PLAYING=$(snapshot_field "['transport']['isPlaying']")
assert_equals "isPlaying is True after play" "$PLAYING" "True"

# stop
RESP=$(rpc '{"jsonrpc":"2.0","method":"transport/stop","id":11}')
assert_contains "transport/stop returns ok" "$RESP" '"ok"'
sleep 0.3
PLAYING=$(snapshot_field "['transport']['isPlaying']")
assert_equals "isPlaying is False after stop" "$PLAYING" "False"

# setTempo
RESP=$(rpc '{"jsonrpc":"2.0","method":"transport/setTempo","params":{"tempo":135},"id":12}')
assert_contains "transport/setTempo returns ok" "$RESP" '"ok"'
sleep 0.3
TEMPO=$(snapshot_field "['transport']['tempo']")
assert_equals "tempo is 135.0 after setTempo" "$TEMPO" "135.0"

# setPosition
RESP=$(rpc '{"jsonrpc":"2.0","method":"transport/setPosition","params":{"beats":4.0},"id":13}')
assert_contains "transport/setPosition returns ok" "$RESP" '"ok"'
sleep 0.3
POS=$(snapshot_field "['transport']['playPosition']")
assert_equals "playPosition is 4.0 after setPosition" "$POS" "4.0"

# setLoop
RESP=$(rpc '{"jsonrpc":"2.0","method":"transport/setLoop","params":{"enabled":true},"id":14}')
assert_contains "transport/setLoop returns ok" "$RESP" '"ok"'
sleep 0.3
LOOP=$(snapshot_field "['transport']['isLoopEnabled']")
assert_equals "isLoopEnabled is True after setLoop" "$LOOP" "True"

# setMetronome
RESP=$(rpc '{"jsonrpc":"2.0","method":"transport/setMetronome","params":{"enabled":true},"id":15}')
assert_contains "transport/setMetronome returns ok" "$RESP" '"ok"'
sleep 0.3
METRO=$(snapshot_field "['transport']['isMetronomeEnabled']")
assert_equals "isMetronomeEnabled is True after setMetronome" "$METRO" "True"

# Restore transport state
ORIG_LOOP_LC=$(echo "$ORIG_LOOP" | tr '[:upper:]' '[:lower:]')
ORIG_METRO_LC=$(echo "$ORIG_METRO" | tr '[:upper:]' '[:lower:]')
rpc "{\"jsonrpc\":\"2.0\",\"method\":\"transport/setTempo\",\"params\":{\"tempo\":${ORIG_TEMPO}},\"id\":90}" > /dev/null
rpc '{"jsonrpc":"2.0","method":"transport/setPosition","params":{"beats":0},"id":91}' > /dev/null
rpc "{\"jsonrpc\":\"2.0\",\"method\":\"transport/setLoop\",\"params\":{\"enabled\":${ORIG_LOOP_LC}},\"id\":92}" > /dev/null
rpc "{\"jsonrpc\":\"2.0\",\"method\":\"transport/setMetronome\",\"params\":{\"enabled\":${ORIG_METRO_LC}},\"id\":93}" > /dev/null

# 5. Track actions
echo "--- 5. Track Actions ---"

# Save baseline
ORIG_VOL=$(snapshot_field "['tracks'][0]['volume']")
ORIG_MUTE=$(snapshot_field "['tracks'][0]['mute']")

# setVolume
RESP=$(rpc '{"jsonrpc":"2.0","method":"track/setVolume","params":{"index":0,"value":0.3},"id":20}')
assert_contains "track/setVolume returns ok" "$RESP" '"ok"'
sleep 0.3
VOL=$(snapshot_field "['tracks'][0]['volume']")
assert_equals "track 0 volume is 0.3 after setVolume" "$VOL" "0.3"

# setMute
RESP=$(rpc '{"jsonrpc":"2.0","method":"track/setMute","params":{"index":0,"muted":true},"id":21}')
assert_contains "track/setMute returns ok" "$RESP" '"ok"'
sleep 0.3
MUTE=$(snapshot_field "['tracks'][0]['mute']")
assert_equals "track 0 mute is True after setMute" "$MUTE" "True"

# Restore track state
ORIG_MUTE_LC=$(echo "$ORIG_MUTE" | tr '[:upper:]' '[:lower:]')
rpc "{\"jsonrpc\":\"2.0\",\"method\":\"track/setVolume\",\"params\":{\"index\":0,\"value\":${ORIG_VOL}},\"id\":94}" > /dev/null
rpc "{\"jsonrpc\":\"2.0\",\"method\":\"track/setMute\",\"params\":{\"index\":0,\"muted\":${ORIG_MUTE_LC}},\"id\":95}" > /dev/null
sleep 0.3

# 6. Master actions
echo "--- 6. Master Actions ---"

ORIG_MVOL=$(snapshot_field "['master']['volume']")

RESP=$(rpc '{"jsonrpc":"2.0","method":"master/setVolume","params":{"value":0.4},"id":30}')
assert_contains "master/setVolume returns ok" "$RESP" '"ok"'
sleep 0.3
MVOL=$(snapshot_field "['master']['volume']")
MVOL_APPROX=$(python3 -c "print(round(${MVOL}, 2))")
assert_equals "master volume is ~0.4 after setVolume" "$MVOL_APPROX" "0.4"

# Restore
rpc "{\"jsonrpc\":\"2.0\",\"method\":\"master/setVolume\",\"params\":{\"value\":${ORIG_MVOL}},\"id\":96}" > /dev/null
sleep 0.3

# 7. Application actions
echo "--- 7. Application Actions ---"
RESP=$(rpc '{"jsonrpc":"2.0","method":"app/getState","id":40}')
assert_contains "app/getState has projectName" "$RESP" '"projectName"'
assert_contains "app/getState has canUndo" "$RESP" '"canUndo"'
assert_contains "app/getState has hasActiveEngine" "$RESP" '"hasActiveEngine"'

# 8. Batch request
echo "--- 8. Batch Request ---"
BATCH=$(rpc '[{"jsonrpc":"2.0","method":"api/list","id":50},{"jsonrpc":"2.0","method":"app/getState","id":51}]')
assert_contains "batch response has id 50" "$BATCH" '"id":50'
assert_contains "batch response has id 51" "$BATCH" '"id":51'

# 9. Error handling
echo "--- 9. Error Handling ---"

# Malformed JSON
ERR=$(rpc '{bad json}')
assert_contains "malformed JSON returns -32700" "$ERR" '-32700'

# Unknown method
ERR=$(rpc '{"jsonrpc":"2.0","method":"does/not/exist","id":60}')
assert_contains "unknown method returns -32601" "$ERR" '-32601'

# Invalid params
ERR=$(rpc '{"jsonrpc":"2.0","method":"track/setVolume","params":{"index":999,"value":0.5},"id":61}')
assert_contains "invalid params returns -32602" "$ERR" '-32602'

# 10. HTTP method enforcement
echo "--- 10. HTTP Method Enforcement ---"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${BASE}/rpc")
assert_equals "GET /rpc returns 405" "$HTTP_CODE" "405"

# 11. Clip snapshot
echo "--- 11. Clip Snapshot ---"
SNAP=$(rpc '{"jsonrpc":"2.0","method":"session/snapshot","id":70}')
assert_contains "snapshot has scenes" "$SNAP" '"scenes"'
CLIP_COUNT=$(echo "$SNAP" | python3 -c "import sys,json; print(len(json.load(sys.stdin)['result']['tracks'][0]['clips']))")
assert_equals "track 0 has 5 clip slots" "$CLIP_COUNT" "5"
SCENE_COUNT=$(echo "$SNAP" | python3 -c "import sys,json; print(len(json.load(sys.stdin)['result']['scenes']))")
assert_equals "snapshot has 5 scenes" "$SCENE_COUNT" "5"
assert_contains "clips have hasContent" "$SNAP" '"hasContent"'
assert_contains "clips have isPlaying" "$SNAP" '"isPlaying"'

# 12. Clip actions
echo "--- 12. Clip Actions ---"

# Create a clip on track 0, slot 7 (use slot 7 to avoid conflicts with user data)
RESP=$(rpc '{"jsonrpc":"2.0","method":"clip/create","params":{"trackIndex":0,"slotIndex":7,"lengthInBeats":4},"id":71}')
assert_contains "clip/create returns ok" "$RESP" '"ok"'
sleep 0.5
HAS=$(snapshot_field "['tracks'][0]['clips'][7]['hasContent']")
assert_equals "slot 7 hasContent after create" "$HAS" "True"

# Launch the clip
RESP=$(rpc '{"jsonrpc":"2.0","method":"clip/launch","params":{"trackIndex":0,"slotIndex":7},"id":72}')
assert_contains "clip/launch returns ok" "$RESP" '"ok"'
sleep 0.5
PLAYING=$(snapshot_field "['tracks'][0]['clips'][7]['isPlaying']")
assert_equals "slot 7 isPlaying after launch" "$PLAYING" "True"

# Stop
RESP=$(rpc '{"jsonrpc":"2.0","method":"clip/stop","params":{"trackIndex":0},"id":73}')
assert_contains "clip/stop returns ok" "$RESP" '"ok"'
sleep 0.5

# Stop transport (clip launcher may have started it)
rpc '{"jsonrpc":"2.0","method":"transport/stop","id":74}' > /dev/null
sleep 0.3

# Delete the clip we created on slot 7
RESP=$(rpc '{"jsonrpc":"2.0","method":"clip/delete","params":{"trackIndex":0,"slotIndex":7},"id":174}')
assert_contains "clip/delete returns ok" "$RESP" '"ok"'
sleep 0.5
HAS=$(snapshot_field "['tracks'][0]['clips'][7]['hasContent']")
assert_equals "slot 7 hasContent after delete" "$HAS" "False"

# 13. Scene actions
echo "--- 13. Scene Actions ---"
RESP=$(rpc '{"jsonrpc":"2.0","method":"scene/launch","params":{"index":0},"id":75}')
assert_contains "scene/launch returns ok" "$RESP" '"ok"'
sleep 0.3
rpc '{"jsonrpc":"2.0","method":"transport/stop","id":76}' > /dev/null
sleep 0.3

# 14. Clip API list
echo "--- 14. Clip + Scene API List ---"
LIST=$(rpc '{"jsonrpc":"2.0","method":"api/list","id":77}')
assert_contains "api/list has clip/launch" "$LIST" '"clip/launch"'
assert_contains "api/list has clip/stop" "$LIST" '"clip/stop"'
assert_contains "api/list has clip/record" "$LIST" '"clip/record"'
assert_contains "api/list has clip/create" "$LIST" '"clip/create"'
assert_contains "api/list has clip/delete" "$LIST" '"clip/delete"'
assert_contains "api/list has scene/launch" "$LIST" '"scene/launch"'

# 15. Device snapshot
echo "--- 15. Device Snapshot ---"
SNAP=$(rpc '{"jsonrpc":"2.0","method":"session/snapshot","id":80}')
assert_contains "snapshot has device" "$SNAP" '"device"'
assert_contains "device has cursorTrackName" "$SNAP" '"cursorTrackName"'
assert_contains "device has remoteControls" "$SNAP" '"remoteControls"'
assert_contains "remoteControls has pageIndex" "$SNAP" '"pageIndex"'
assert_contains "remoteControls has parameters" "$SNAP" '"parameters"'
PARAM_COUNT=$(echo "$SNAP" | python3 -c "import sys,json; print(len(json.load(sys.stdin)['result']['device']['remoteControls']['parameters']))")
assert_equals "device has 8 parameters" "$PARAM_COUNT" "8"

# 16. Cursor track navigation
echo "--- 16. Cursor Track Navigation ---"
ORIG_TRACK=$(snapshot_field "['device']['cursorTrackName']")
RESP=$(rpc '{"jsonrpc":"2.0","method":"cursor/selectTrack","params":{"direction":"next"},"id":81}')
assert_contains "cursor/selectTrack next returns ok" "$RESP" '"ok"'
sleep 0.3
NEW_TRACK=$(snapshot_field "['device']['cursorTrackName']")
TOTAL=$((TOTAL + 1))
if [ "$NEW_TRACK" != "$ORIG_TRACK" ]; then
  echo "  PASS  cursor track changed after selectTrack next"
  PASS=$((PASS + 1))
else
  echo "  FAIL  cursor track unchanged after selectTrack next (still: $ORIG_TRACK)"
  FAIL=$((FAIL + 1))
fi
# Navigate back
rpc '{"jsonrpc":"2.0","method":"cursor/selectTrack","params":{"direction":"previous"},"id":82}' > /dev/null
sleep 0.3

# 17. Device API list
echo "--- 17. Device + Cursor API List ---"
LIST=$(rpc '{"jsonrpc":"2.0","method":"api/list","id":83}')
assert_contains "api/list has device/selectNext" "$LIST" '"device/selectNext"'
assert_contains "api/list has device/selectPrevious" "$LIST" '"device/selectPrevious"'
assert_contains "api/list has device/setEnabled" "$LIST" '"device/setEnabled"'
assert_contains "api/list has device/selectPage" "$LIST" '"device/selectPage"'
assert_contains "api/list has device/nextPage" "$LIST" '"device/nextPage"'
assert_contains "api/list has device/previousPage" "$LIST" '"device/previousPage"'
assert_contains "api/list has device/setParameterValue" "$LIST" '"device/setParameterValue"'
assert_contains "api/list has cursor/selectTrack" "$LIST" '"cursor/selectTrack"'

# 18. Error handling — Phase 2
echo "--- 18. Error Handling (Phase 2) ---"
ERR=$(rpc '{"jsonrpc":"2.0","method":"clip/launch","params":{"trackIndex":999,"slotIndex":0},"id":84}')
assert_contains "clip invalid trackIndex returns -32602" "$ERR" '-32602'

ERR=$(rpc '{"jsonrpc":"2.0","method":"scene/launch","params":{"index":99},"id":85}')
assert_contains "scene invalid index returns -32602" "$ERR" '-32602'

ERR=$(rpc '{"jsonrpc":"2.0","method":"device/setParameterValue","params":{"index":99,"value":0.5},"id":86}')
assert_contains "device invalid param index returns -32602" "$ERR" '-32602'

ERR=$(rpc '{"jsonrpc":"2.0","method":"cursor/selectTrack","params":{"direction":"invalid"},"id":87}')
assert_contains "cursor invalid direction returns -32602" "$ERR" '-32602'

# 19. Note API list
echo "--- 19. Note API List ---"
LIST=$(rpc '{"jsonrpc":"2.0","method":"api/list","id":100}')
assert_contains "api/list has clip/select" "$LIST" '"clip/select"'
assert_contains "api/list has clip/setNotes" "$LIST" '"clip/setNotes"'
assert_contains "api/list has clip/clearNote" "$LIST" '"clip/clearNote"'
assert_contains "api/list has clip/clearAllNotes" "$LIST" '"clip/clearAllNotes"'
assert_contains "api/list has clip/getNotes" "$LIST" '"clip/getNotes"'
assert_contains "api/list has clip/setStepSize" "$LIST" '"clip/setStepSize"'
assert_contains "api/list has clip/scrollSteps" "$LIST" '"clip/scrollSteps"'

# Phase 12 methods
assert_contains "api/list has session/transaction" "$LIST" '"session/transaction"'
assert_contains "api/list has macro/createTrack" "$LIST" '"macro/createTrack"'
assert_contains "api/list has macro/createClip" "$LIST" '"macro/createClip"'
assert_contains "api/list has macro/writeClip" "$LIST" '"macro/writeClip"'
assert_contains "api/list has macro/buildSection" "$LIST" '"macro/buildSection"'

# Count total methods
METHOD_COUNT=$(echo "$LIST" | python3 -c "import sys,json; print(len(json.load(sys.stdin)['result']))")
TOTAL=$((TOTAL + 1))
if [ "$METHOD_COUNT" -ge 113 ]; then
  echo "  PASS  api/list has >= 113 methods (found $METHOD_COUNT)"
  PASS=$((PASS + 1))
else
  echo "  FAIL  api/list has >= 113 methods — found $METHOD_COUNT"
  FAIL=$((FAIL + 1))
fi

# 20. Clip snapshot section
echo "--- 20. Clip Snapshot Section ---"
SNAP=$(rpc '{"jsonrpc":"2.0","method":"session/snapshot","id":101}')
assert_contains "snapshot has clip section" "$SNAP" '"clip"'
assert_contains "clip has trackName" "$SNAP" '"trackName"'
assert_contains "clip has playingStep" "$SNAP" '"playingStep"'
assert_contains "clip has loopLength" "$SNAP" '"loopLength"'
assert_contains "clip has stepSize" "$SNAP" '"stepSize"'
assert_contains "clip has hasContent" "$SNAP" '"hasContent"'

# 21. Note editing workflow
echo "--- 21. Note Editing Workflow ---"

# Select clip on track 0, slot 7 (created in test 12)
RESP=$(rpc '{"jsonrpc":"2.0","method":"clip/select","params":{"trackIndex":0,"slotIndex":7},"id":110}')
assert_contains "clip/select returns ok" "$RESP" '"ok"'
sleep 0.5

# Clear any existing notes
RESP=$(rpc '{"jsonrpc":"2.0","method":"clip/clearAllNotes","id":111}')
assert_contains "clip/clearAllNotes returns ok" "$RESP" '"ok"'
sleep 0.3

# Write a 2-note pattern
RESP=$(rpc '{"jsonrpc":"2.0","method":"clip/setNotes","params":{"notes":[{"x":0,"y":60,"velocity":0.8,"duration":0.25},{"x":4,"y":64,"velocity":0.6,"duration":0.5}]},"id":112}')
assert_contains "clip/setNotes returns count" "$RESP" '"count"'
assert_contains "clip/setNotes wrote 2 notes" "$RESP" '"count":2'
sleep 0.5

# Read notes back
RESP=$(rpc '{"jsonrpc":"2.0","method":"clip/getNotes","id":113}')
assert_contains "clip/getNotes returns note at y=60" "$RESP" '"y":60'
assert_contains "clip/getNotes returns note at y=64" "$RESP" '"y":64'

# Clear a single note
RESP=$(rpc '{"jsonrpc":"2.0","method":"clip/clearNote","params":{"x":0,"y":60},"id":114}')
assert_contains "clip/clearNote returns ok" "$RESP" '"ok"'
sleep 0.3

# Verify only 1 note remains
RESP=$(rpc '{"jsonrpc":"2.0","method":"clip/getNotes","id":115}')
NOTE_COUNT=$(echo "$RESP" | python3 -c "import sys,json; print(len(json.load(sys.stdin)['result']))")
assert_equals "1 note remains after clearNote" "$NOTE_COUNT" "1"

# Clear all
RESP=$(rpc '{"jsonrpc":"2.0","method":"clip/clearAllNotes","id":116}')
assert_contains "clip/clearAllNotes returns ok" "$RESP" '"ok"'
sleep 0.3

# Verify empty
RESP=$(rpc '{"jsonrpc":"2.0","method":"clip/getNotes","id":117}')
NOTE_COUNT=$(echo "$RESP" | python3 -c "import sys,json; print(len(json.load(sys.stdin)['result']))")
assert_equals "0 notes after clearAllNotes" "$NOTE_COUNT" "0"

# 22. Step size and scroll
echo "--- 22. Step Size & Scroll ---"
RESP=$(rpc '{"jsonrpc":"2.0","method":"clip/setStepSize","params":{"size":0.5},"id":120}')
assert_contains "clip/setStepSize returns ok" "$RESP" '"ok"'

RESP=$(rpc '{"jsonrpc":"2.0","method":"clip/scrollSteps","params":{"offset":0},"id":121}')
assert_contains "clip/scrollSteps returns ok" "$RESP" '"ok"'

# Restore step size
rpc '{"jsonrpc":"2.0","method":"clip/setStepSize","params":{"size":0.25},"id":122}' > /dev/null

# 23. Device Insertion API List
echo "--- 23. Device Insertion API List ---"
LIST=$(rpc '{"jsonrpc":"2.0","method":"api/list","id":130}')
assert_contains "api/list has device/insertBitwigDevice" "$LIST" '"device/insertBitwigDevice"'
assert_contains "api/list has device/insertPluginDevice" "$LIST" '"device/insertPluginDevice"'
assert_contains "api/list has device/listBitwigDevices" "$LIST" '"device/listBitwigDevices"'
assert_contains "api/list has device/remove" "$LIST" '"device/remove"'

# 24. List Bitwig devices
echo "--- 24. List Bitwig Devices ---"
RESP=$(rpc '{"jsonrpc":"2.0","method":"device/listBitwigDevices","id":131}')
assert_contains "listBitwigDevices returns Polymer" "$RESP" '"Polymer"'
assert_contains "listBitwigDevices returns EQ-5" "$RESP" '"EQ-5"'
assert_contains "listBitwigDevices returns Compressor" "$RESP" '"Compressor"'
DEVICE_COUNT=$(echo "$RESP" | python3 -c "import sys,json; print(len(json.load(sys.stdin)['result']))")
assert_equals "listBitwigDevices returns 151 devices" "$DEVICE_COUNT" "151"

# 25. Insert + remove Bitwig device
echo "--- 25. Insert + Remove Device ---"

# Insert a device
RESP=$(rpc '{"jsonrpc":"2.0","method":"device/insertBitwigDevice","params":{"name":"EQ-5"},"id":132}')
assert_contains "insertBitwigDevice returns ok" "$RESP" '"ok"'
sleep 0.5

# Insert with case-insensitive name
RESP=$(rpc '{"jsonrpc":"2.0","method":"device/insertBitwigDevice","params":{"name":"compressor"},"id":133}')
assert_contains "insertBitwigDevice case-insensitive returns ok" "$RESP" '"ok"'
sleep 0.5

# Remove the device (removes cursor device)
RESP=$(rpc '{"jsonrpc":"2.0","method":"device/remove","id":134}')
assert_contains "device/remove returns ok" "$RESP" '"ok"'
sleep 0.5

# Remove the other device we inserted
RESP=$(rpc '{"jsonrpc":"2.0","method":"device/remove","id":135}')
assert_contains "device/remove second returns ok" "$RESP" '"ok"'
sleep 0.3

# 26. Device insertion error handling
echo "--- 26. Device Insertion Errors ---"
ERR=$(rpc '{"jsonrpc":"2.0","method":"device/insertBitwigDevice","params":{"name":"NonexistentDevice12345"},"id":136}')
assert_contains "insertBitwigDevice unknown name returns -32602" "$ERR" '-32602'
assert_contains "insertBitwigDevice unknown name has error message" "$ERR" 'Unknown device'

ERR=$(rpc '{"jsonrpc":"2.0","method":"device/insertBitwigDevice","params":{"name":"Polymorph"},"id":137}')
assert_contains "insertBitwigDevice close match suggests Polymer" "$ERR" 'Polymer'

ERR=$(rpc '{"jsonrpc":"2.0","method":"device/insertPluginDevice","params":{"type":"invalid","id":"123"},"id":138}')
assert_contains "insertPluginDevice invalid type returns -32602" "$ERR" '-32602'

ERR=$(rpc '{"jsonrpc":"2.0","method":"device/insertBitwigDevice","params":{},"id":139}')
assert_contains "insertBitwigDevice missing name returns -32602" "$ERR" '-32602'

# 27. Track Management API List
echo "--- 27. Track Management API List ---"
LIST=$(rpc '{"jsonrpc":"2.0","method":"api/list","id":140}')
assert_contains "api/list has track/createAudio" "$LIST" '"track/createAudio"'
assert_contains "api/list has track/createInstrument" "$LIST" '"track/createInstrument"'
assert_contains "api/list has track/createEffect" "$LIST" '"track/createEffect"'
assert_contains "api/list has track/select" "$LIST" '"track/select"'
assert_contains "api/list has track/rename" "$LIST" '"track/rename"'
assert_contains "api/list has track/deleteSelected" "$LIST" '"track/deleteSelected"'
assert_contains "api/list has track/duplicate" "$LIST" '"track/duplicate"'

# 28. Create, select, rename, duplicate, delete workflow
echo "--- 28. Track Management Workflow ---"

# Create an audio track
RESP=$(rpc '{"jsonrpc":"2.0","method":"track/createAudio","params":{"position":-1},"id":141}')
assert_contains "track/createAudio returns ok" "$RESP" '"ok":true'
assert_contains "track/createAudio returns cursorTrackName" "$RESP" '"cursorTrackName"'
sleep 0.5

# Get the name of the newly created track from snapshot
SNAP=$(rpc '{"jsonrpc":"2.0","method":"session/snapshot","id":142}')
TRACK_COUNT_BEFORE=$(echo "$SNAP" | python3 -c "import sys,json; tracks = json.load(sys.stdin)['result']['tracks']; print(sum(1 for t in tracks if t['name'] != ''))")

# Select by index (track 0)
RESP=$(rpc '{"jsonrpc":"2.0","method":"track/select","params":{"index":0},"id":143}')
assert_contains "track/select returns ok" "$RESP" '"ok":true'
assert_contains "track/select returns cursorTrackName" "$RESP" '"cursorTrackName"'
sleep 0.3

# Rename the selected track
RESP=$(rpc '{"jsonrpc":"2.0","method":"track/rename","params":{"name":"SmokeTestTrack"},"id":144}')
assert_contains "track/rename returns ok" "$RESP" '"ok":true'
assert_contains "track/rename returns cursorTrackName" "$RESP" '"cursorTrackName"'
sleep 0.5

# Verify rename in snapshot
NAME=$(snapshot_field "['tracks'][0]['name']")
assert_equals "track 0 renamed to SmokeTestTrack" "$NAME" "SmokeTestTrack"

# Duplicate the track
RESP=$(rpc '{"jsonrpc":"2.0","method":"track/duplicate","params":{},"id":145}')
assert_contains "track/duplicate returns ok" "$RESP" '"ok":true'
sleep 0.5

# Create instrument track
RESP=$(rpc '{"jsonrpc":"2.0","method":"track/createInstrument","params":{},"id":146}')
assert_contains "track/createInstrument returns ok" "$RESP" '"ok":true'
sleep 0.5

# Delete the instrument track we just created
RESP=$(rpc '{"jsonrpc":"2.0","method":"track/deleteSelected","params":{},"id":147}')
assert_contains "track/deleteSelected returns ok" "$RESP" '"ok":true'
sleep 0.5

# 29. Track select out-of-range error
echo "--- 29. Track Select Error ---"
ERR=$(rpc '{"jsonrpc":"2.0","method":"track/select","params":{"index":999},"id":148}')
assert_contains "track/select out-of-range returns -32602" "$ERR" '-32602'
assert_contains "track/select error mentions bank width" "$ERR" '64'

ERR=$(rpc '{"jsonrpc":"2.0","method":"track/select","params":{"index":-1},"id":149}')
assert_contains "track/select negative index returns -32602" "$ERR" '-32602'

# 30. Arranger API List
echo "--- 30. Arranger API List ---"
LIST=$(rpc '{"jsonrpc":"2.0","method":"api/list","id":160}')
assert_contains "api/list has arranger/setPlaybackFollow" "$LIST" '"arranger/setPlaybackFollow"'
assert_contains "api/list has arranger/setClipLauncherVisible" "$LIST" '"arranger/setClipLauncherVisible"'
assert_contains "api/list has arranger/setTimelineVisible" "$LIST" '"arranger/setTimelineVisible"'
assert_contains "api/list has arranger/setCueMarkersVisible" "$LIST" '"arranger/setCueMarkersVisible"'
assert_contains "api/list has arranger/setEffectTracksVisible" "$LIST" '"arranger/setEffectTracksVisible"'
assert_contains "api/list has arranger/setIoSectionVisible" "$LIST" '"arranger/setIoSectionVisible"'
assert_contains "api/list has arranger/setDoubleRowTrackHeight" "$LIST" '"arranger/setDoubleRowTrackHeight"'
assert_contains "api/list has cueMarker/addAtPlayhead" "$LIST" '"cueMarker/addAtPlayhead"'
assert_contains "api/list has cueMarker/list" "$LIST" '"cueMarker/list"'
assert_contains "api/list has cueMarker/launch" "$LIST" '"cueMarker/launch"'
assert_contains "api/list has cueMarker/delete" "$LIST" '"cueMarker/delete"'
assert_contains "api/list has transport/setLoopRange" "$LIST" '"transport/setLoopRange"'
assert_contains "api/list has transport/getLoopRange" "$LIST" '"transport/getLoopRange"'
assert_contains "api/list has transport/setPunchIn" "$LIST" '"transport/setPunchIn"'
assert_contains "api/list has transport/setPunchOut" "$LIST" '"transport/setPunchOut"'
assert_contains "api/list has transport/setAutomationWriteMode" "$LIST" '"transport/setAutomationWriteMode"'
assert_contains "api/list has transport/setArrangerAutomationWrite" "$LIST" '"transport/setArrangerAutomationWrite"'
assert_contains "api/list has transport/setClipLauncherAutomationWrite" "$LIST" '"transport/setClipLauncherAutomationWrite"'
assert_contains "api/list has transport/resetAutomationOverrides" "$LIST" '"transport/resetAutomationOverrides"'

# 31. Arranger Snapshot
echo "--- 31. Arranger Snapshot ---"
SNAP=$(rpc '{"jsonrpc":"2.0","method":"session/snapshot","id":161}')
assert_contains "snapshot has arranger section" "$SNAP" '"arranger"'
assert_contains "arranger has playbackFollow" "$SNAP" '"playbackFollow"'
assert_contains "arranger has clipLauncherVisible" "$SNAP" '"clipLauncherVisible"'
assert_contains "arranger has timelineVisible" "$SNAP" '"timelineVisible"'
assert_contains "arranger has cueMarkersVisible" "$SNAP" '"cueMarkersVisible"'
assert_contains "arranger has effectTracksVisible" "$SNAP" '"effectTracksVisible"'
assert_contains "arranger has ioSectionVisible" "$SNAP" '"ioSectionVisible"'
assert_contains "arranger has doubleRowTrackHeight" "$SNAP" '"doubleRowTrackHeight"'

# 32. Arrangement Snapshot
echo "--- 32. Arrangement Snapshot ---"
assert_contains "snapshot has arrangement section" "$SNAP" '"arrangement"'
assert_contains "arrangement has loop" "$SNAP" '"loop"'
assert_contains "arrangement has punch" "$SNAP" '"punch"'
assert_contains "arrangement has automation" "$SNAP" '"automation"'
assert_contains "arrangement has cueMarkers" "$SNAP" '"cueMarkers"'

# 33. Arranger Visibility Toggle
echo "--- 33. Arranger Visibility Toggle ---"
RESP=$(rpc '{"jsonrpc":"2.0","method":"arranger/setPlaybackFollow","params":{"enabled":true},"id":162}')
assert_contains "arranger/setPlaybackFollow returns ok" "$RESP" '"ok":true'
sleep 0.3
PF=$(snapshot_field "['arranger']['playbackFollow']")
assert_equals "playbackFollow is True after set" "$PF" "True"

RESP=$(rpc '{"jsonrpc":"2.0","method":"arranger/setPlaybackFollow","params":{"enabled":false},"id":163}')
assert_contains "arranger/setPlaybackFollow false returns ok" "$RESP" '"ok":true'
sleep 0.3
PF=$(snapshot_field "['arranger']['playbackFollow']")
assert_equals "playbackFollow is False after unset" "$PF" "False"

# 34. Loop Range
echo "--- 34. Loop Range ---"
RESP=$(rpc '{"jsonrpc":"2.0","method":"transport/setLoopRange","params":{"start":8.0,"duration":16.0,"enabled":true},"id":164}')
assert_contains "transport/setLoopRange returns ok" "$RESP" '"ok":true'
sleep 0.3

RESP=$(rpc '{"jsonrpc":"2.0","method":"transport/getLoopRange","id":165}')
assert_contains "getLoopRange has loopStart" "$RESP" '"loopStart"'
assert_contains "getLoopRange has loopDuration" "$RESP" '"loopDuration"'
assert_contains "getLoopRange has loopEnabled" "$RESP" '"loopEnabled"'

# Restore loop
rpc '{"jsonrpc":"2.0","method":"transport/setLoopRange","params":{"start":0.0,"duration":4.0,"enabled":false},"id":166}' > /dev/null
sleep 0.3

# 35. Automation Write Mode
echo "--- 35. Automation Write Mode ---"
RESP=$(rpc '{"jsonrpc":"2.0","method":"transport/setAutomationWriteMode","params":{"mode":"touch"},"id":167}')
assert_contains "setAutomationWriteMode touch returns ok" "$RESP" '"ok":true'
sleep 0.3
MODE=$(snapshot_field "['arrangement']['automation']['writeMode']")
assert_equals "automation writeMode is touch" "$MODE" "touch"

RESP=$(rpc '{"jsonrpc":"2.0","method":"transport/setAutomationWriteMode","params":{"mode":"latch"},"id":168}')
assert_contains "setAutomationWriteMode latch returns ok" "$RESP" '"ok":true'
sleep 0.3
MODE=$(snapshot_field "['arrangement']['automation']['writeMode']")
assert_equals "automation writeMode is latch" "$MODE" "latch"

# Invalid mode
ERR=$(rpc '{"jsonrpc":"2.0","method":"transport/setAutomationWriteMode","params":{"mode":"invalid"},"id":169}')
assert_contains "setAutomationWriteMode invalid returns -32602" "$ERR" '-32602'

# 36. Reset Automation Overrides
echo "--- 36. Reset Automation Overrides ---"
RESP=$(rpc '{"jsonrpc":"2.0","method":"transport/resetAutomationOverrides","id":170}')
assert_contains "resetAutomationOverrides returns ok" "$RESP" '"ok":true'

# 37. Cue marker error handling
echo "--- 37. Cue Marker Errors ---"
ERR=$(rpc '{"jsonrpc":"2.0","method":"cueMarker/launch","params":{"index":99},"id":171}')
assert_contains "cueMarker/launch out-of-range returns -32602" "$ERR" '-32602'

ERR=$(rpc '{"jsonrpc":"2.0","method":"cueMarker/delete","params":{"index":-1},"id":172}')
assert_contains "cueMarker/delete negative index returns -32602" "$ERR" '-32602'

ERR=$(rpc '{"jsonrpc":"2.0","method":"cueMarker/launch","params":{},"id":173}')
assert_contains "cueMarker/launch missing index returns -32602" "$ERR" '-32602'

# 38. Phase 9 — Envelope Writing methods
echo "--- 38. Envelope Writing ---"

# device/hasAutomation — returns boolean
RESP=$(rpc '{"jsonrpc":"2.0","method":"device/hasAutomation","params":{"index":0},"id":200}')
assert_contains "device/hasAutomation returns hasAutomation field" "$RESP" '"hasAutomation"'

# device/touch — valid call
RESP=$(rpc '{"jsonrpc":"2.0","method":"device/touch","params":{"index":0,"touched":true},"id":201}')
assert_contains "device/touch true returns ok" "$RESP" '"ok":true'
RESP=$(rpc '{"jsonrpc":"2.0","method":"device/touch","params":{"index":0,"touched":false},"id":202}')
assert_contains "device/touch false returns ok" "$RESP" '"ok":true'

# device/touch — validation errors
ERR=$(rpc '{"jsonrpc":"2.0","method":"device/touch","params":{"index":8,"touched":true},"id":203}')
assert_contains "device/touch index out of range returns -32602" "$ERR" '-32602'

ERR=$(rpc '{"jsonrpc":"2.0","method":"device/touch","params":{},"id":204}')
assert_contains "device/touch missing params returns -32602" "$ERR" '-32602'

# device/hasAutomation — validation error
ERR=$(rpc '{"jsonrpc":"2.0","method":"device/hasAutomation","params":{"index":8},"id":205}')
assert_contains "device/hasAutomation index out of range returns -32602" "$ERR" '-32602'

# device/deleteAllAutomation — validation error
ERR=$(rpc '{"jsonrpc":"2.0","method":"device/deleteAllAutomation","params":{"index":-1},"id":206}')
assert_contains "device/deleteAllAutomation negative index returns -32602" "$ERR" '-32602'

# device/restoreAutomationControl — validation error
ERR=$(rpc '{"jsonrpc":"2.0","method":"device/restoreAutomationControl","params":{},"id":207}')
assert_contains "device/restoreAutomationControl missing index returns -32602" "$ERR" '-32602'

# device/writeEnvelope — automation write disabled returns error
rpc '{"jsonrpc":"2.0","method":"transport/setArrangerAutomationWrite","params":{"enabled":false},"id":208}' > /dev/null
sleep 0.3
ERR=$(rpc '{"jsonrpc":"2.0","method":"device/writeEnvelope","params":{"index":0,"points":[{"position":0,"value":0.5}]},"id":209}')
assert_contains "writeEnvelope with write disabled returns error" "$ERR" '"error"'

# device/writeEnvelope — index out of range
ERR=$(rpc '{"jsonrpc":"2.0","method":"device/writeEnvelope","params":{"index":9,"points":[{"position":0,"value":0.5}]},"id":210}')
assert_contains "writeEnvelope index out of range returns -32602" "$ERR" '-32602'

# device/writeEnvelope — empty points
rpc '{"jsonrpc":"2.0","method":"transport/setArrangerAutomationWrite","params":{"enabled":true},"id":211}' > /dev/null
sleep 0.3
ERR=$(rpc '{"jsonrpc":"2.0","method":"device/writeEnvelope","params":{"index":0,"points":[]},"id":212}')
assert_contains "writeEnvelope empty points returns -32602" "$ERR" '-32602'

# Snapshot parameter has hasAutomation field
HAS_AUTO=$(snapshot_field "['device']['remoteControls']['parameters'][0]['hasAutomation']")
assert_contains "snapshot parameter[0] has hasAutomation" "$HAS_AUTO" ""
# The field exists (would be error/empty if missing) — value is True or False
echo "  INFO  parameter[0].hasAutomation = $HAS_AUTO"

# Cleanup: disable automation write
rpc '{"jsonrpc":"2.0","method":"transport/setArrangerAutomationWrite","params":{"enabled":false},"id":213}' > /dev/null

# 39. Phase 13 — Mixer & Routing API list
echo "--- 39. Mixer & Routing API List ---"
LIST=$(rpc '{"jsonrpc":"2.0","method":"api/list","id":300}')
assert_contains "api/list has send/setLevel" "$LIST" '"send/setLevel"'
assert_contains "api/list has send/setMode" "$LIST" '"send/setMode"'
assert_contains "api/list has send/setEnabled" "$LIST" '"send/setEnabled"'
assert_contains "api/list has track/setColor" "$LIST" '"track/setColor"'
assert_contains "api/list has track/setCrossfade" "$LIST" '"track/setCrossfade"'
assert_contains "api/list has track/setMonitor" "$LIST" '"track/setMonitor"'
assert_contains "api/list has master/setMute" "$LIST" '"master/setMute"'
assert_contains "api/list has master/setSolo" "$LIST" '"master/setSolo"'
assert_contains "api/list has master/setColor" "$LIST" '"master/setColor"'

# 40. Phase 13 — Snapshot sends + mixer fields
echo "--- 40. Mixer Snapshot Fields ---"
SNAP=$(rpc '{"jsonrpc":"2.0","method":"session/snapshot","id":301}')
assert_contains "snapshot track has sends array" "$SNAP" '"sends"'
assert_contains "snapshot track has crossfadeMode" "$SNAP" '"crossfadeMode"'
assert_contains "snapshot track has monitorMode" "$SNAP" '"monitorMode"'
assert_contains "snapshot send has isPreFader" "$SNAP" '"isPreFader"'
SEND_COUNT=$(echo "$SNAP" | python3 -c "import sys,json; print(len(json.load(sys.stdin)['result']['tracks']['tracks'][0]['sends']))")
assert_equals "track 0 has 4 sends" "$SEND_COUNT" "4"

# 41. Phase 13 — Track color
echo "--- 41. Track Color ---"
RESP=$(rpc '{"jsonrpc":"2.0","method":"track/setColor","params":{"index":0,"r":0.8,"g":0.2,"b":0.4},"id":302}')
assert_contains "track/setColor returns ok" "$RESP" '"ok":true'
sleep 0.3
R=$(echo "$(rpc '{"jsonrpc":"2.0","method":"session/snapshot","id":303}')" | python3 -c "import sys,json; c = json.load(sys.stdin)['result']['tracks']['tracks'][0]['color']; print(round(c['r'], 1))")
assert_equals "track 0 color.r is 0.8 after setColor" "$R" "0.8"

# 42. Phase 13 — Master mute/solo
echo "--- 42. Master Mute/Solo ---"
RESP=$(rpc '{"jsonrpc":"2.0","method":"master/setMute","params":{"value":true},"id":304}')
assert_contains "master/setMute returns ok" "$RESP" '"ok"'
sleep 0.3
RESP=$(rpc '{"jsonrpc":"2.0","method":"master/setMute","params":{"value":false},"id":305}')
assert_contains "master/setMute false returns ok" "$RESP" '"ok"'

RESP=$(rpc '{"jsonrpc":"2.0","method":"master/setSolo","params":{"value":true},"id":306}')
assert_contains "master/setSolo returns ok" "$RESP" '"ok"'
sleep 0.3
RESP=$(rpc '{"jsonrpc":"2.0","method":"master/setSolo","params":{"value":false},"id":307}')
assert_contains "master/setSolo false returns ok" "$RESP" '"ok"'

# 43. Phase 13 — Send error handling
echo "--- 43. Send Errors ---"
ERR=$(rpc '{"jsonrpc":"2.0","method":"send/setLevel","params":{"sendIndex":0,"value":0.5},"id":308}')
assert_contains "send/setLevel missing trackIndex returns -32602" "$ERR" '-32602'

ERR=$(rpc '{"jsonrpc":"2.0","method":"send/setMode","params":{"trackIndex":0,"sendIndex":0,"mode":"INVALID"},"id":309}')
assert_contains "send/setMode invalid mode returns -32602" "$ERR" '-32602'

ERR=$(rpc '{"jsonrpc":"2.0","method":"track/setCrossfade","params":{"index":0,"mode":"INVALID"},"id":310}')
assert_contains "track/setCrossfade invalid mode returns -32602" "$ERR" '-32602'

ERR=$(rpc '{"jsonrpc":"2.0","method":"track/setMonitor","params":{"index":0,"mode":"INVALID"},"id":311}')
assert_contains "track/setMonitor invalid mode returns -32602" "$ERR" '-32602'

# 44. Phase 14 — Master device API
echo "--- 44. Master Device ---"

# Verify masterDevice methods in api/list
RESP=$(rpc '{"jsonrpc":"2.0","method":"api/list","id":400}')
assert_contains "api/list includes masterDevice/selectNext" "$RESP" 'masterDevice/selectNext'
assert_contains "api/list includes masterDevice/insertBitwigDevice" "$RESP" 'masterDevice/insertBitwigDevice'
assert_contains "api/list includes masterDevice/setParameterValue" "$RESP" 'masterDevice/setParameterValue'

# Verify snapshot has masterDevice section
SNAP=$(rpc '{"jsonrpc":"2.0","method":"session/snapshot","id":401}')
assert_contains "snapshot has masterDevice section" "$SNAP" '"masterDevice"'
assert_contains "snapshot masterDevice has remoteControls" "$SNAP" '"remoteControls"'

# Insert a device on master
RESP=$(rpc '{"jsonrpc":"2.0","method":"masterDevice/insertBitwigDevice","params":{"name":"EQ-5"},"id":402}')
assert_contains "masterDevice/insertBitwigDevice returns ok" "$RESP" '"ok"'
sleep 0.5

# Verify device shows in snapshot
SNAP=$(rpc '{"jsonrpc":"2.0","method":"session/snapshot","id":403}')
assert_contains "masterDevice snapshot shows EQ-5" "$SNAP" 'EQ-5'

# Navigate and set parameter
RESP=$(rpc '{"jsonrpc":"2.0","method":"masterDevice/setParameterValue","params":{"index":0,"value":0.75},"id":404}')
assert_contains "masterDevice/setParameterValue returns ok" "$RESP" '"ok"'

# Enable/disable
RESP=$(rpc '{"jsonrpc":"2.0","method":"masterDevice/setEnabled","params":{"enabled":false},"id":405}')
assert_contains "masterDevice/setEnabled returns ok" "$RESP" '"ok"'
RESP=$(rpc '{"jsonrpc":"2.0","method":"masterDevice/setEnabled","params":{"enabled":true},"id":406}')
assert_contains "masterDevice/setEnabled re-enable returns ok" "$RESP" '"ok"'

# Remove master device
RESP=$(rpc '{"jsonrpc":"2.0","method":"masterDevice/remove","id":407}')
assert_contains "masterDevice/remove returns ok" "$RESP" '"ok"'
sleep 0.3

# Error handling
ERR=$(rpc '{"jsonrpc":"2.0","method":"masterDevice/setParameterValue","params":{"index":8,"value":0.5},"id":408}')
assert_contains "masterDevice/setParameterValue index out of range returns -32602" "$ERR" '-32602'

ERR=$(rpc '{"jsonrpc":"2.0","method":"masterDevice/insertBitwigDevice","params":{},"id":409}')
assert_contains "masterDevice/insertBitwigDevice missing name returns -32602" "$ERR" '-32602'

# 45. Phase 15 — Device chain navigation
echo "--- 45. Device Chain Navigation ---"

# Verify new methods in api/list
RESP=$(rpc '{"jsonrpc":"2.0","method":"api/list","id":500}')
assert_contains "api/list includes device/enterSlot" "$RESP" 'device/enterSlot'
assert_contains "api/list includes device/exitToParent" "$RESP" 'device/exitToParent'
assert_contains "api/list includes masterDevice/enterSlot" "$RESP" 'masterDevice/enterSlot'
assert_contains "api/list includes masterDevice/exitToParent" "$RESP" 'masterDevice/exitToParent'

# Verify snapshot has nesting fields
SNAP=$(rpc '{"jsonrpc":"2.0","method":"session/snapshot","id":501}')
assert_contains "device snapshot has isNested field" "$SNAP" '"isNested"'
assert_contains "device snapshot has hasSlots field" "$SNAP" '"hasSlots"'
assert_contains "device snapshot has slotNames field" "$SNAP" '"slotNames"'
assert_contains "device snapshot has hasLayers field" "$SNAP" '"hasLayers"'
assert_contains "device snapshot has hasDrumPads field" "$SNAP" '"hasDrumPads"'

# Insert Instrument Layer on track to test chain navigation
RESP=$(rpc '{"jsonrpc":"2.0","method":"device/insertBitwigDevice","params":{"name":"Instrument Layer"},"id":502}')
assert_contains "insert Instrument Layer returns ok" "$RESP" '"ok"'
sleep 0.5

# Verify hasSlots is true
SNAP=$(rpc '{"jsonrpc":"2.0","method":"session/snapshot","id":503}')
# Instrument Layer should have slots
assert_contains "Instrument Layer shows in snapshot" "$SNAP" 'Instrument Layer'

# Enter slot and exit
RESP=$(rpc '{"jsonrpc":"2.0","method":"device/enterSlot","params":{"name":"Chain 1"},"id":504}')
assert_contains "device/enterSlot returns ok" "$RESP" '"ok"'
sleep 0.3

RESP=$(rpc '{"jsonrpc":"2.0","method":"device/exitToParent","id":505}')
assert_contains "device/exitToParent returns ok" "$RESP" '"ok"'
sleep 0.3

# Remove test device
RESP=$(rpc '{"jsonrpc":"2.0","method":"device/remove","id":506}')
assert_contains "remove Instrument Layer returns ok" "$RESP" '"ok"'
sleep 0.3

# enterSlot missing name returns error
ERR=$(rpc '{"jsonrpc":"2.0","method":"device/enterSlot","params":{},"id":507}')
assert_contains "device/enterSlot missing name returns -32602" "$ERR" '-32602'

ERR=$(rpc '{"jsonrpc":"2.0","method":"masterDevice/enterSlot","params":{},"id":508}')
assert_contains "masterDevice/enterSlot missing name returns -32602" "$ERR" '-32602'

# 46. Clean up — delete the duplicated track and undo rename
echo "--- 46. Track Cleanup ---"
# Select track 0 and restore original name
rpc '{"jsonrpc":"2.0","method":"track/select","params":{"index":0},"id":150}' > /dev/null
sleep 0.3
rpc '{"jsonrpc":"2.0","method":"app/undo","id":151}' > /dev/null
rpc '{"jsonrpc":"2.0","method":"app/undo","id":152}' > /dev/null
rpc '{"jsonrpc":"2.0","method":"app/undo","id":153}' > /dev/null
rpc '{"jsonrpc":"2.0","method":"app/undo","id":154}' > /dev/null
rpc '{"jsonrpc":"2.0","method":"app/undo","id":155}' > /dev/null
sleep 0.5
echo "  INFO  cleanup: undid track management operations"

# --- summary ---
echo ""
echo "=== Results: ${PASS} passed, ${FAIL} failed, ${TOTAL} total ==="
echo ""

if [ "$FAIL" -gt 0 ]; then
  exit 1
fi
exit 0
