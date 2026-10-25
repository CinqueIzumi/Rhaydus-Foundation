#!/usr/bin/env bash
# PreToolUse(Agent) hook (bundled in the rhaydus-kotlin plugin): deny a subagent spawn whose brief
# lacks the sections its role needs, and show the template to re-issue with. Required sections per
# role are fixed here: implementer needs Goal/Files/Verify/Report, test-writer needs
# Files/Verify/Report, reviewer needs Scope.
#
# Project facts come from $CLAUDE_PROJECT_DIR/.claude/rhaydus.json, key "briefCheck":
#   briefDoc  project-relative path of the brief guide, named in the denial (optional)
#   agents    object mapping subagent_type -> role ("implementer" | "testWriter" | "reviewer")
#   verify    object mapping role -> the example "## Verify" line shown in the template
# A missing file or key falls back to the plugin's own agents and a placeholder Verify line.
set -u

input=$(cat)
agent=$(printf '%s' "$input" | jq -r '.tool_input.subagent_type // ""')
prompt=$(printf '%s' "$input" | jq -r '.tool_input.prompt // ""')

config="${CLAUDE_PROJECT_DIR:-.}/.claude/rhaydus.json"
cfg() {
  [ -f "$config" ] || return 0
  jq -r "$1 // empty" "$config" 2>/dev/null
}

role=$(cfg ".briefCheck.agents[\"$agent\"]")
if [ -z "$role" ] && [ -z "$(cfg '.briefCheck.agents')" ]; then
  case "$agent" in
    rhaydus-kotlin:implementer) role=implementer ;;
    rhaydus-kotlin:test-writer) role=testWriter ;;
    rhaydus-kotlin:reviewer) role=reviewer ;;
  esac
fi

verify_line=$(cfg ".briefCheck.verify[\"$role\"]")
[ -n "$verify_line" ] || verify_line='<one narrow build or test command>'
brief_doc=$(cfg '.briefCheck.briefDoc')

case "$role" in
  implementer)
    required=("## Goal" "## Files" "## Verify" "## Report")
    template="## Goal
What the change does and why, in one or two sentences.

## Files
- \`path/to/File.kt:120-180\` — what to change there

## Contract (optional)
UiState / UiAction / UiEvent shape or public signature, when decided.

## Constraints (optional)
What must not change; decisions taken; the governing design-system section.

## Verify
$verify_line

## Report
≤150 words: Done / Files changed / Verify result / Left over / Needs tests."
    ;;
  testWriter)
    required=("## Files" "## Verify" "## Report")
    template="## Files
- \`path/to/Unit.kt\` — the unit under test
- \`path/to/UnitTest.kt:40-95\` — construction sites to fix / where to add

## Verify
$verify_line

## Report
≤150 words: tests added / pass or fail with names and diagnosis."
    ;;
  reviewer)
    required=("## Scope")
    template='## Scope
Commit range (main..HEAD), staged, unstaged, or a file list.

## Gates (optional)
The gate result you already have.

## Verify (optional)
One narrow command the reviewer may run if no gate result is given.

## Focus (optional)
What the change is meant to do; areas to look at hardest.'
    ;;
  *) exit 0 ;;
esac

missing=()
for section in "${required[@]}"; do
  printf '%s\n' "$prompt" | grep -Eq "^${section}[[:space:]]*$" || missing+=("$section")
done
[ "${#missing[@]}" -eq 0 ] && exit 0

joined=$(printf '%s, ' "${missing[@]}")
doc_note=""
[ -n "$brief_doc" ] && doc_note=" ($brief_doc)"
reason="$agent brief is missing: ${joined%, }. Re-issue with this template${doc_note}:

$template"
jq -n --arg reason "$reason" '{hookSpecificOutput: {hookEventName: "PreToolUse", permissionDecision: "deny", permissionDecisionReason: $reason}}'
exit 0
