#!/usr/bin/env bash
# Case runner for ../scripts/agent-brief-check.sh. Builds throwaway fixture projects (one with a
# rhaydus.json, one without) in a temp dir; never touches a real checkout.
set -u

here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
hook="$here/../scripts/agent-brief-check.sh"
failures=0

tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
with_config="$tmp/with-config"
no_config="$tmp/no-config"
no_agents="$tmp/no-agents"
mkdir -p "$with_config/.claude" "$no_config" "$no_agents/.claude"
cat > "$with_config/.claude/rhaydus.json" <<'JSON'
{
  "briefCheck": {
    "briefDoc": "docs/briefs.md",
    "agents": {
      "my-implementer": "implementer",
      "my-tester": "testWriter",
      "my-reviewer": "reviewer"
    },
    "verify": {
      "implementer": "make impl-check",
      "testWriter": "make test-check"
    }
  }
}
JSON
echo '{"briefCheck": {"briefDoc": "docs/briefs.md"}}' > "$no_agents/.claude/rhaydus.json"

run_hook() {
  local project=$1 agent=$2 prompt=$3
  jq -n --arg a "$agent" --arg p "$prompt" \
    '{tool_name: "Agent", tool_input: {description: "d", prompt: $p, subagent_type: $a}}' |
    CLAUDE_PROJECT_DIR="$project" bash "$hook"
}

decision_of() {
  if [ -z "$1" ]; then echo allow; else printf '%s' "$1" | jq -r '.hookSpecificOutput.permissionDecision'; fi
}

check() {
  local name=$1 project=$2 agent=$3 prompt=$4 expected=$5 actual
  actual=$(decision_of "$(run_hook "$project" "$agent" "$prompt")")
  if [ "$actual" = "$expected" ]; then
    echo "ok    $name"
  else
    echo "FAIL  $name (expected $expected, got $actual)"
    failures=$((failures + 1))
  fi
}

check_reason() {
  local name=$1 project=$2 agent=$3 prompt=$4 needle=$5 reason
  reason=$(run_hook "$project" "$agent" "$prompt" | jq -r '.hookSpecificOutput.permissionDecisionReason // ""')
  if printf '%s' "$reason" | grep -qF -- "$needle"; then
    echo "ok    $name"
  else
    echo "FAIL  $name (reason lacks: $needle)"
    failures=$((failures + 1))
  fi
}

check_reason_absent() {
  local name=$1 project=$2 agent=$3 prompt=$4 needle=$5 reason
  reason=$(run_hook "$project" "$agent" "$prompt" | jq -r '.hookSpecificOutput.permissionDecisionReason // ""')
  if [ -n "$reason" ] && ! printf '%s' "$reason" | grep -qF -- "$needle"; then
    echo "ok    $name"
  else
    echo "FAIL  $name (reason should be present and lack: $needle)"
    failures=$((failures + 1))
  fi
}

full_impl=$'## Goal\nx\n\n## Files\n- a.kt\n\n## Verify\nrun it\n\n## Report\n150 words'
full_test=$'## Files\n- a.kt\n\n## Verify\nx\n\n## Report\ny'

# With a config: agents map by briefCheck.agents.
check "config: implementer bare prompt denied" "$with_config" my-implementer "add a button" deny
check "config: implementer full brief allowed" "$with_config" my-implementer "$full_impl" allow
check "config: implementer missing Report denied" "$with_config" my-implementer $'## Goal\nx\n## Files\ny\n## Verify\nz' deny
check "config: implementer missing Goal denied" "$with_config" my-implementer $'## Files\ny\n## Verify\nz\n## Report\nr' deny
check "config: inline mention is not a heading" "$with_config" my-implementer "needs ## Goal ## Files ## Verify ## Report" deny
check "config: heading with trailing spaces counts" "$with_config" my-implementer $'## Goal  \nx\n## Files\ny\n## Verify\nz\n## Report\nr' allow
check "config: test-writer full brief allowed" "$with_config" my-tester "$full_test" allow
check "config: test-writer missing Verify denied" "$with_config" my-tester $'## Files\na\n## Report\nb' deny
check "config: test-writer needs no Goal" "$with_config" my-tester "$full_test" allow
check "config: reviewer with Scope allowed" "$with_config" my-reviewer $'## Scope\nmain..HEAD' allow
check "config: reviewer bare prompt denied" "$with_config" my-reviewer "review the last commit" deny
check "config: unknown agent passes" "$with_config" Explore "find things" allow
check "config: empty subagent_type passes" "$with_config" "" "anything" allow
check "config: plugin agent names unchecked once agents are configured" "$with_config" rhaydus-kotlin:implementer "add a button" allow
check_reason "config: denial names briefDoc" "$with_config" my-implementer "x" "(docs/briefs.md)"
check_reason "config: implementer denial shows its verify line" "$with_config" my-implementer "x" "make impl-check"
check_reason "config: test-writer denial shows its verify line" "$with_config" my-tester "x" "make test-check"
check_reason "config: denial lists the missing sections" "$with_config" my-tester $'## Files\na' "my-tester brief is missing: ## Verify, ## Report"
check_reason "config: reviewer denial embeds the template" "$with_config" my-reviewer "x" "Commit range"
check_reason_absent "config: implementer verify line does not leak into test-writer" "$with_config" my-tester "x" "make impl-check"

# Without a config file: the plugin's own agents are checked.
check "no config: plugin implementer bare prompt denied" "$no_config" rhaydus-kotlin:implementer "add a button" deny
check "no config: plugin implementer full brief allowed" "$no_config" rhaydus-kotlin:implementer "$full_impl" allow
check "no config: plugin test-writer bare prompt denied" "$no_config" rhaydus-kotlin:test-writer "write tests" deny
check "no config: plugin test-writer full brief allowed" "$no_config" rhaydus-kotlin:test-writer "$full_test" allow
check "no config: plugin reviewer bare prompt denied" "$no_config" rhaydus-kotlin:reviewer "review it" deny
check "no config: plugin reviewer with Scope allowed" "$no_config" rhaydus-kotlin:reviewer $'## Scope\nstaged' allow
check "no config: unknown agent passes" "$no_config" Explore "find things" allow
check "no config: project-named agent passes" "$no_config" my-implementer "add a button" allow
check_reason "no config: denial uses the placeholder verify line" "$no_config" rhaydus-kotlin:implementer "x" "<one narrow build or test command>"
check_reason_absent "no config: denial names no brief doc" "$no_config" rhaydus-kotlin:implementer "x" "docs/briefs.md"

# A config without briefCheck.agents falls back to the plugin agents too.
check "no agents key: plugin implementer denied" "$no_agents" rhaydus-kotlin:implementer "add a button" deny
check_reason "no agents key: briefDoc still named" "$no_agents" rhaydus-kotlin:implementer "x" "(docs/briefs.md)"

exit "$failures"
