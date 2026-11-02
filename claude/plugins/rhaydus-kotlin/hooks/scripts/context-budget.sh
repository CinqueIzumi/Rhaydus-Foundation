#!/usr/bin/env bash
# UserPromptSubmit hook (bundled in the rhaydus-kotlin plugin): once the session's context passes a token
# budget, tell the model to finish the current step and ask the user to run /handoff and /clear.
#
# The budget is `session.contextBudget` in $CLAUDE_PROJECT_DIR/.claude/rhaydus.json. A missing file, key or
# non-numeric value falls back to 150000. A missing transcript or usage figure leaves the hook silent.

set -u

input=$(cat)
transcript=$(printf '%s' "$input" | jq -r '.transcript_path // ""' 2>/dev/null)
[ -n "$transcript" ] && [ -f "$transcript" ] || exit 0

config="${CLAUDE_PROJECT_DIR:-.}/.claude/rhaydus.json"
budget=""
[ -f "$config" ] && budget=$(jq -r '.session.contextBudget // empty' "$config" 2>/dev/null)
case "$budget" in
  '' | *[!0-9]*) budget=150000 ;;
esac

used=$(
  grep '"type":"assistant"' "$transcript" 2>/dev/null |
    tail -n 50 |
    jq -R 'fromjson? | select(.type == "assistant" and .message.usage != null) | .message.usage | (.input_tokens // 0) + (.cache_creation_input_tokens // 0) + (.cache_read_input_tokens // 0)' 2>/dev/null |
    tail -n 1
)
case "$used" in
  '' | *[!0-9]*) exit 0 ;;
esac

[ "$used" -lt "$budget" ] && exit 0

used_k=$(((used + 500) / 1000))
budget_k=$(((budget + 500) / 1000))
notice="Context is ~${used_k}K tokens (budget ${budget_k}K). Finish the current step only, then ask the user to run /handoff and /clear."
jq -n --arg ctx "$notice" '{hookSpecificOutput: {hookEventName: "UserPromptSubmit", additionalContext: $ctx}}'
exit 0
