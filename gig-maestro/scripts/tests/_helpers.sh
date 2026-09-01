#!/usr/bin/env bash
#
# Shared test helpers for Gig Maestro smoke tests.
# Sourced by individual test scripts and the runner.
#

# --- connection ---

PORT="${PORT:-8787}"
BASE="${BASE:-http://localhost:${PORT}}"
TOKEN_FILE="${GIG_MAESTRO_TOKEN_FILE:-${HOME}/.gig-maestro/token}"
AUTH_TOKEN=""
[ -f "$TOKEN_FILE" ] && AUTH_TOKEN="$(tr -d '\r\n' < "$TOKEN_FILE")"

# --- paths ---

# Resolve from this file's location: scripts/tests/_helpers.sh → gig-maestro/
if [ -z "${PROJECT_ROOT:-}" ]; then
  PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
fi
REPO_ROOT="$(cd "$PROJECT_ROOT/.." && pwd)"
TOOLS_FILE="${PROJECT_ROOT}/tools/claude-tools.json"
PROMPT_FILE="${PROJECT_ROOT}/tools/system-prompt.md"

# --- counters ---

PASS=${PASS:-0}
FAIL=${FAIL:-0}
SKIP=${SKIP:-0}
TOTAL=${TOTAL:-0}

# --- helpers ---

rpc() {
  curl -s -X POST "${BASE}/rpc" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${AUTH_TOKEN}" \
    -d "$1"
}

assert_contains() {
  local label="$1" response="$2" expected="$3"
  TOTAL=$((TOTAL + 1))
  if [[ "$response" == *"$expected"* ]]; then
    echo "  PASS  $label"
    PASS=$((PASS + 1))
  else
    echo "  FAIL  $label — expected '$expected' in response"
    echo "        got: ${response:0:200}..."
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

assert_skip() {
  local label="$1" reason="$2"
  TOTAL=$((TOTAL + 1))
  SKIP=$((SKIP + 1))
  echo "  SKIP  $label — $reason"
}

snapshot_field() {
  local path="$1"
  rpc '{"jsonrpc":"2.0","method":"session/snapshot","id":99}' | \
    python3 -c "import sys,json; print(json.load(sys.stdin)['result']${path})"
}

# --- summary ---

print_summary() {
  local label="${1:-Tests}"
  echo ""
  echo "=== ${label}: ${PASS} passed, ${FAIL} failed, ${SKIP} skipped, ${TOTAL} total ==="
  echo ""
}
