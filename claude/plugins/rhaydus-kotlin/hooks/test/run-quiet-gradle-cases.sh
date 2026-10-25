#!/usr/bin/env bash
# Case runner for ../scripts/quiet-gradle.sh. Builds a fixture tree (a project with an executable
# gradlew and a nested directory, plus a sibling tree without one) in a temp dir.
# quiet-gradle.cases columns (tab separated): command, expected ("=" unchanged), cwd ("-" = project root).
# Placeholders: @Q@ the wrapper path, @ROOT@ the project root, @NG@ the tree without a gradlew.
set -u

here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
hook="$here/../scripts/quiet-gradle.sh"
failures=0

tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
root="$tmp/project"
ng="$tmp/plain"
plugin="$tmp/plugin"
mkdir -p "$root/modules/feature" "$ng/inner" "$plugin/scripts"
printf '#!/bin/sh\n' > "$root/gradlew"
chmod +x "$root/gradlew"
quiet="$plugin/scripts/gradle-quiet.sh"

while IFS=$'\t' read -r command expected cwd; do
  [ -z "$command" ] && continue
  case "$command" in "#"*) continue ;; esac
  [ "$expected" = "=" ] && expected="$command"
  [ "$cwd" = "-" ] && cwd="@ROOT@"
  command="${command//@Q@/$quiet}"
  command="${command//@ROOT@/$root}"
  command="${command//@NG@/$ng}"
  expected="${expected//@Q@/$quiet}"
  expected="${expected//@ROOT@/$root}"
  expected="${expected//@NG@/$ng}"
  cwd="${cwd//@ROOT@/$root}"
  cwd="${cwd//@NG@/$ng}"
  input=$(jq -n --arg c "$command" --arg d "$cwd" \
    '{cwd: $d, tool_input: {command: $c, description: "d", timeout: 5}}')
  output=$(printf '%s' "$input" | CLAUDE_PLUGIN_ROOT="$plugin" CLAUDE_PROJECT_DIR="$ng" bash "$hook")
  if [ -z "$output" ]; then
    actual="$command"
  else
    actual=$(printf '%s' "$output" | jq -r '.hookSpecificOutput.updatedInput.command')
    kept=$(printf '%s' "$output" | jq -r '.hookSpecificOutput.updatedInput | "\(.description)/\(.timeout)"')
    [ "$kept" = "d/5" ] || { echo "FAIL (fields dropped): $command"; failures=$((failures + 1)); continue; }
  fi
  if [ "$actual" = "$expected" ]; then
    echo "ok    $command  [cwd ${cwd#"$tmp"/}]"
  else
    echo "FAIL  $command  [cwd ${cwd#"$tmp"/}]"
    echo "      expected: $expected"
    echo "      actual:   $actual"
    failures=$((failures + 1))
  fi
done < "$here/quiet-gradle.cases"

exit "$failures"
