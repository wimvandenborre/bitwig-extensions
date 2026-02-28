#!/usr/bin/env bash
#
# Gig Maestro v0.2.x — Smoke Test Suite
#
# Requires: Bitwig Studio running with Gig Maestro extension loaded.
# Usage: ./scripts/smoke-test.sh [port]
#

set -euo pipefail

PORT="${1:-8787}"
BASE="http://localhost:${PORT}"
PASS=0
FAIL=0
TOTAL=0

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
echo "=== Gig Maestro Smoke Tests (port ${PORT}) ==="
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
assert_equals "track 0 has 8 clip slots" "$CLIP_COUNT" "8"
SCENE_COUNT=$(echo "$SNAP" | python3 -c "import sys,json; print(len(json.load(sys.stdin)['result']['scenes']))")
assert_equals "snapshot has 8 scenes" "$SCENE_COUNT" "8"
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

# --- summary ---
echo ""
echo "=== Results: ${PASS} passed, ${FAIL} failed, ${TOTAL} total ==="
echo ""

if [ "$FAIL" -gt 0 ]; then
  exit 1
fi
exit 0
