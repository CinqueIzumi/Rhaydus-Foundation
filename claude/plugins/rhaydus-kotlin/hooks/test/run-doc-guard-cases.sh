#!/usr/bin/env bash
# Case runner for ../scripts/doc-guard.sh. Every case builds its own fixture project (docGuard config,
# budgets file, ACTIVE.md, the file under edit) in a temp dir; never touches a real checkout.
set -u

here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
hook="$here/../scripts/doc-guard.sh"
failures=0

b64dec() { printf '%s' "$1" | base64 -d; }

# Fixed fixture budgets shared by every case; doc-guard.cases holds the paths that exercise each rule
# (first-match-wins, `*` vs `**`, byte vs line units, the `## Now` ratchet).
budgets_content='fixture/special/exact.md            8B
fixture/special/*.md                1000B
docs/reference/*.md                 100B
docs/reference/**/*.md              20B
fixture/tiny.md                     20B
fixture/unicode.md                  10B
.claude/rules/*.md                  40L
.claude/agent-memory/*/MEMORY.md    40L
.claude/agent-memory/*/*.md         40L
## Now                              40L
'

fixture_config='{
  "docGuard": {
    "budgetsFile": "docs/doc-budgets.txt",
    "activeFile": "docs/working/ACTIVE.md",
    "referenceDocs": ["docs/reference/"],
    "workingDocs": ["docs/working/"],
    "permanentDocs": ["docs/reference/", "CLAUDE.md", ".claude/agent-memory/"],
    "defaultHint": "Trim it or move the detail elsewhere."
  }
}'

run_case() {
  local name="$1" tool="$2" relpath="$3" old_b64="$4" arg1_b64="$5" arg2_b64="$6" \
    replace_all="$7" expected="$8" reason="$9"
  local fixture_root input output actual_decision actual_reason path

  fixture_root=$(mktemp -d)
  mkdir -p "$fixture_root/docs/working" "$fixture_root/.claude"
  printf '%s\n' "$fixture_config" > "$fixture_root/.claude/rhaydus.json"
  printf '%s' "$budgets_content" > "$fixture_root/docs/doc-budgets.txt"
  printf '%s\n' "docs/working/now-test.md" > "$fixture_root/docs/working/ACTIVE.md"

  if [ "$tool" != "Bash" ] && [ "$old_b64" != "-" ]; then
    path="$fixture_root/$relpath"
    mkdir -p "$(dirname "$path")"
    b64dec "$old_b64" > "$path"
  fi

  case "$tool" in
    Write)
      input=$(jq -n --arg fp "$fixture_root/$relpath" --arg content "$(b64dec "$arg1_b64")" \
        '{tool_name: "Write", tool_input: {file_path: $fp, content: $content}}')
      ;;
    Edit)
      input=$(jq -n --arg fp "$fixture_root/$relpath" \
        --arg olds "$(b64dec "$arg1_b64")" --arg news "$(b64dec "$arg2_b64")" \
        --argjson all "$([ "$replace_all" = "true" ] && echo true || echo false)" \
        '{tool_name: "Edit", tool_input: {file_path: $fp, old_string: $olds, new_string: $news, replace_all: $all}}')
      ;;
    Bash)
      input=$(jq -n --arg cmd "$(b64dec "$arg1_b64")" '{tool_name: "Bash", tool_input: {command: $cmd}}')
      ;;
    EditNumberOldString)
      # old_string is a raw JSON number (not a string), which makes jq's split() error out and
      # exercises the "could not evaluate the edit" path rather than the not-found path.
      input=$(jq -n --arg fp "$fixture_root/$relpath" \
        --argjson olds "$(b64dec "$arg1_b64")" --arg news "$(b64dec "$arg2_b64")" \
        '{tool_name: "Edit", tool_input: {file_path: $fp, old_string: $olds, new_string: $news, replace_all: false}}')
      ;;
  esac

  output=$(printf '%s' "$input" | CLAUDE_PROJECT_DIR="$fixture_root" bash "$hook")
  rm -rf "$fixture_root"

  if [ -z "$output" ]; then
    actual_decision="allow"
    actual_reason=""
  else
    actual_decision=$(printf '%s' "$output" | jq -r '.hookSpecificOutput.permissionDecision // "allow"')
    actual_reason=$(printf '%s' "$output" | jq -r '.hookSpecificOutput.permissionDecisionReason // ""')
  fi

  if [ "$actual_decision" != "$expected" ]; then
    echo "FAIL  $name"
    echo "      expected decision: $expected"
    echo "      actual   decision: $actual_decision"
    echo "      reason: $actual_reason"
    failures=$((failures + 1))
    return
  fi

  if [ "$reason" != "-" ] && ! printf '%s' "$actual_reason" | grep -qF -- "$reason"; then
    echo "FAIL  $name"
    echo "      expected reason to contain: $reason"
    echo "      actual reason: $actual_reason"
    failures=$((failures + 1))
    return
  fi

  echo "ok    $name"
}

while IFS=$'\t' read -r name tool relpath old_b64 arg1_b64 arg2_b64 replace_all expected reason; do
  [ -z "$name" ] && continue
  case "$name" in "#"*) continue ;; esac
  run_case "$name" "$tool" "$relpath" "$old_b64" "$arg1_b64" "$arg2_b64" "$replace_all" "$expected" "$reason"
done < "$here/doc-guard.cases"

# Hook off: without a docGuard object in the config, even a denied-looking edit passes silently.
off_check() {
  local name="$1" setup="$2" tool="$3" project payload output
  project=$(mktemp -d)
  mkdir -p "$project/docs/reference" "$project/.claude"
  printf '%s' "$budgets_content" > "$project/docs/doc-budgets.txt"
  [ "$setup" = "none" ] || printf '%s\n' "$setup" > "$project/.claude/rhaydus.json"
  if [ "$tool" = "Write" ]; then
    payload=$(jq -n --arg fp "$project/fixture/tiny.md" --arg c "$(printf 'x%.0s' $(seq 1 60))" \
      '{tool_name: "Write", tool_input: {file_path: $fp, content: $c}}')
  else
    payload=$(jq -n --arg cmd "echo hi > docs/reference/foo.md" '{tool_name: "Bash", tool_input: {command: $cmd}}')
  fi
  output=$(printf '%s' "$payload" | CLAUDE_PROJECT_DIR="$project" bash "$hook")
  rm -rf "$project"
  if [ -z "$output" ]; then
    echo "ok    $name"
  else
    echo "FAIL  $name (expected the hook to stay silent, got: $output)"
    failures=$((failures + 1))
  fi
}
off_check "no config file turns the hook off (budget write)" none Write
off_check "no config file turns the hook off (bash markdown write)" none Bash
off_check "config without docGuard key turns the hook off" '{"slowGradle": {}}' Write
off_check "non-object docGuard turns the hook off" '{"docGuard": true}' Bash

exit "$failures"
