#
# Error handling tests (online — requires Bitwig running)
#
# Tests extracted from legacy smoke-test.sh: Tests 9, 18
#

# --- 9. Error Handling ---
echo "--- Error Handling ---"

# Malformed JSON
ERR=$(rpc '{bad json}')
assert_contains "malformed JSON returns -32700" "$ERR" '-32700'

# Unknown method
ERR=$(rpc '{"jsonrpc":"2.0","method":"does/not/exist","id":60}')
assert_contains "unknown method returns -32601" "$ERR" '-32601'

# Invalid params
ERR=$(rpc '{"jsonrpc":"2.0","method":"track/setVolume","params":{"index":999,"value":0.5},"id":61}')
assert_contains "invalid params returns -32602" "$ERR" '-32602'

# --- 18. Error Handling (Phase 2) ---
echo "--- Error Handling (Phase 2) ---"

ERR=$(rpc '{"jsonrpc":"2.0","method":"clip/launch","params":{"trackIndex":999,"slotIndex":0},"id":84}')
assert_contains "clip invalid trackIndex returns -32602" "$ERR" '-32602'

ERR=$(rpc '{"jsonrpc":"2.0","method":"scene/launch","params":{"index":99},"id":85}')
assert_contains "scene invalid index returns -32602" "$ERR" '-32602'

ERR=$(rpc '{"jsonrpc":"2.0","method":"device/setParameterValue","params":{"index":99,"value":0.5},"id":86}')
assert_contains "device invalid param index returns -32602" "$ERR" '-32602'

ERR=$(rpc '{"jsonrpc":"2.0","method":"cursor/selectTrack","params":{"direction":"invalid"},"id":87}')
assert_contains "cursor invalid direction returns -32602" "$ERR" '-32602'
