#!/usr/bin/env bash
# PreToolUse(Bash) hook (bundled in the rhaydus-kotlin plugin): block Gradle tasks the project has
# declared too slow to run casually, such as aggregate builds or native release linking. It only
# inspects the segments of the command that invoke Gradle (gradlew, a gradle-quiet.sh wrapper or a
# bare `gradle`), so a `grep` or `echo` after a pipe never trips it.
#
# Bypass: prefix the command with ACCEPTANCE_BUILD=1 to allow anything through.
#
# Parameters come from $CLAUDE_PROJECT_DIR/.claude/rhaydus.json, key "slowGradle":
#   blockedTasks  array of case-insensitive ERE, each matched against a whole task name
#                 (any ":module:" prefix stripped); a match blocks the command
#   allowedTasks  array of the same shape; a match exempts the task from blockedTasks
#   hint          text appended to the denial, e.g. the lightweight tasks to use instead
# A missing file or an empty blockedTasks turns the hook off.
set -u -f

config="${CLAUDE_PROJECT_DIR:-.}/.claude/rhaydus.json"
[ -f "$config" ] || exit 0

input=$(cat)
cmd=$(printf '%s' "$input" | jq -r '.tool_input.command // ""')

blocked=()
while IFS= read -r pattern; do
  [ -n "$pattern" ] && blocked+=("$pattern")
done < <(jq -r '.slowGradle.blockedTasks[]? // empty' "$config" 2>/dev/null)
[ "${#blocked[@]}" -gt 0 ] || exit 0

allowed=()
while IFS= read -r pattern; do
  [ -n "$pattern" ] && allowed+=("$pattern")
done < <(jq -r '.slowGradle.allowedTasks[]? // empty' "$config" 2>/dev/null)
hint=$(jq -r '.slowGradle.hint // empty' "$config" 2>/dev/null)

gradle_pattern='gradlew|gradle-quiet\.sh|(^|[[:space:]])gradle[[:space:]]'

# Only act on Gradle invocations (not paths like ~/.gradle/).
printf '%s' "$cmd" | grep -Eq "$gradle_pattern" || exit 0

# Explicit bypass for the deliberate acceptance build.
printf '%s' "$cmd" | grep -q 'ACCEPTANCE_BUILD=1' && exit 0

# Everything downstream of a pipe or a command separator is someone's grep/echo, and matching task
# names in that text would block `... | grep FAILED; echo "build done"`. Split on separators and
# inspect only the segments that invoke Gradle.
gradle_segments=$(
  printf '%s' "$cmd" |
    sed -E 's/(\|\||&&|;|\|)/\n/g' |
    grep -E "$gradle_pattern"
)

matches_any() {
  local task="$1" pattern
  shift
  for pattern in "$@"; do
    printf '%s\n' "$task" | grep -Eixq "$pattern" && return 0
  done
  return 1
}

hit=""
while IFS= read -r seg; do
  [ -z "$seg" ] && continue
  skip_next=0
  for tok in $seg; do
    if [ "$skip_next" -eq 1 ]; then
      skip_next=0
      continue
    fi
    case "$tok" in
      -x | --exclude-task | -p | --project-dir | --tests | -b | --build-file | -c | --settings-file \
        | --include-build | -g | --gradle-user-home | -D | -P)
        skip_next=1
        continue
        ;;
      -*) continue ;;
    esac
    task="${tok##*:}"
    [ -z "$task" ] && continue
    if [ "${#allowed[@]}" -gt 0 ] && matches_any "$task" "${allowed[@]}"; then
      continue
    fi
    if matches_any "$task" "${blocked[@]}"; then
      hit="$task"
      break 2
    fi
  done
done <<< "$gradle_segments"

[ -n "$hit" ] || exit 0

cat >&2 <<MSG
BLOCKED: this Gradle command runs '$hit', which this project marks as too slow to run casually.
${hint:+$hint
}If you genuinely need it (e.g. before opening a PR), prefix it:
  ACCEPTANCE_BUILD=1 ./gradlew <task>
MSG
exit 2
