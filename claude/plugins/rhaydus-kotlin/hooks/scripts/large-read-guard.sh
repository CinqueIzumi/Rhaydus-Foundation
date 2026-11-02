#!/usr/bin/env bash
# PreToolUse(Read) hook (bundled in the rhaydus-kotlin plugin): deny a whole-file Read of a large text
# file from the main conversation, and point at grep plus offset/limit or the implementer agent.
# Subagents, Reads that already pass a limit, binary files and paths outside the project pass through.
#
# Threshold from $CLAUDE_PROJECT_DIR/.claude/rhaydus.json, key "largeReadGuard.maxLines"
# (default 600). A missing file or key keeps the default.
set -u

input=$(cat)
root="${CLAUDE_PROJECT_DIR:-$(pwd)}"
config="$root/.claude/rhaydus.json"
max_lines=600
if [ -f "$config" ]; then
  configured=$(jq -r '.largeReadGuard.maxLines // empty' "$config" 2>/dev/null)
  case "$configured" in
    '' | *[!0-9]*) ;;
    *) max_lines="$configured" ;;
  esac
fi

[ -n "$(printf '%s' "$input" | jq -r '.agent_id // ""')" ] && exit 0
[ -n "$(printf '%s' "$input" | jq -r '.tool_input.limit // ""')" ] && exit 0

path=$(printf '%s' "$input" | jq -r '.tool_input.file_path // ""')
case "$path" in
  "$root"/*) ;;
  *) exit 0 ;;
esac
[ -f "$path" ] || exit 0

case "$(printf '%s' "$path" | tr '[:upper:]' '[:lower:]')" in
  *.png | *.jpg | *.jpeg | *.gif | *.webp | *.bmp | *.ico | *.pdf | *.ipynb) exit 0 ;;
esac
grep -Iq . "$path" 2>/dev/null || exit 0

lines=$(wc -l < "$path" | tr -d ' ')
[ "$lines" -gt "$max_lines" ] || exit 0

relative="${path#"$root"/}"
reason="$relative has $lines lines. grep for the symbol and Read with offset/limit, or delegate the work (the rhaydus-kotlin implementer agent / Explore)."
jq -n --arg reason "$reason" '{hookSpecificOutput: {hookEventName: "PreToolUse", permissionDecision: "deny", permissionDecisionReason: $reason}}'
exit 0
