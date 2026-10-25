#!/usr/bin/env bash
# Case runner for ../scripts/large-read-guard.sh. Builds a fixture project with files of known
# line counts in a temp dir; never touches a real checkout.
set -u

here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
hook="$here/../scripts/large-read-guard.sh"
failures=0

tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
default_proj="$tmp/default"
custom_proj="$tmp/custom"
bad_proj="$tmp/bad"
mkdir -p "$default_proj/.claude" "$custom_proj/.claude" "$bad_proj/.claude"
echo '{}' > "$default_proj/.claude/rhaydus.json"
echo '{"largeReadGuard": {"maxLines": 50}}' > "$custom_proj/.claude/rhaydus.json"
echo '{"largeReadGuard": {"maxLines": "lots"}}' > "$bad_proj/.claude/rhaydus.json"

lines_file() { seq 1 "$2" > "$1"; }
for proj in "$default_proj" "$custom_proj" "$bad_proj"; do
  lines_file "$proj/at600.txt" 600
  lines_file "$proj/at601.txt" 601
  lines_file "$proj/at50.txt" 50
  lines_file "$proj/at51.txt" 51
  lines_file "$proj/at100.png" 700
done
head -c 4000 /dev/zero > "$default_proj/binary.bin"
lines_file "$tmp/outside.txt" 900

# check <name> <project> <expected deny|allow> <payload jq args...>
check() {
  local name=$1 project=$2 expected=$3 payload=$4 output actual
  output=$(printf '%s' "$payload" | CLAUDE_PROJECT_DIR="$project" bash "$hook")
  if [ -z "$output" ]; then actual=allow; else actual=$(printf '%s' "$output" | jq -r '.hookSpecificOutput.permissionDecision'); fi
  if [ "$actual" = "$expected" ]; then
    echo "ok    $name"
  else
    echo "FAIL  $name (expected $expected, got $actual)"
    failures=$((failures + 1))
  fi
}

read_payload() { jq -n --arg p "$1" '{tool_name: "Read", tool_input: {file_path: $p}}'; }

check "default: 601 lines denied" "$default_proj" deny "$(read_payload "$default_proj/at601.txt")"
check "default: 600 lines allowed" "$default_proj" allow "$(read_payload "$default_proj/at600.txt")"
check "default: 51 lines allowed" "$default_proj" allow "$(read_payload "$default_proj/at51.txt")"
check "custom 50: 51 lines denied" "$custom_proj" deny "$(read_payload "$custom_proj/at51.txt")"
check "custom 50: 50 lines allowed" "$custom_proj" allow "$(read_payload "$custom_proj/at50.txt")"
check "custom 50: 601 lines denied" "$custom_proj" deny "$(read_payload "$custom_proj/at601.txt")"
check "non-numeric maxLines keeps the default: 601 denied" "$bad_proj" deny "$(read_payload "$bad_proj/at601.txt")"
check "non-numeric maxLines keeps the default: 51 allowed" "$bad_proj" allow "$(read_payload "$bad_proj/at51.txt")"

check "limit passes a large file" "$default_proj" allow \
  "$(jq -n --arg p "$default_proj/at601.txt" '{tool_input: {file_path: $p, limit: 100}}')"
check "limit passes under a custom maxLines" "$custom_proj" allow \
  "$(jq -n --arg p "$custom_proj/at601.txt" '{tool_input: {file_path: $p, limit: 20, offset: 10}}')"
check "subagent read passes" "$default_proj" allow \
  "$(jq -n --arg p "$default_proj/at601.txt" '{agent_id: "a1", tool_input: {file_path: $p}}')"
check "image extension passes" "$default_proj" allow "$(read_payload "$default_proj/at100.png")"
check "binary content passes" "$default_proj" allow "$(read_payload "$default_proj/binary.bin")"
check "path outside the project passes" "$default_proj" allow "$(read_payload "$tmp/outside.txt")"
check "missing file passes" "$default_proj" allow "$(read_payload "$default_proj/nope.txt")"

reason=$(read_payload "$default_proj/at601.txt" | CLAUDE_PROJECT_DIR="$default_proj" bash "$hook" | jq -r '.hookSpecificOutput.permissionDecisionReason')
case "$reason" in
  "at601.txt has 601 lines."*) echo "ok    denial names the relative path and line count" ;;
  *) echo "FAIL  denial names the relative path and line count ($reason)"; failures=$((failures + 1)) ;;
esac

exit "$failures"
