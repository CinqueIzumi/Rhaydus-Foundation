#!/usr/bin/env bash
# PreToolUse hook (bundled in the rhaydus-kotlin plugin), matcher Bash: rewrites a bare `./gradlew ...`
# in a command to the plugin's scripts/gradle-quiet.sh, which sends Gradle output to a log file and
# prints a short summary plus the log path.
#
# The wrapper resolves the project root at run time from the nearest gradlew above the shell's cwd, so
# worktrees and nested projects work. This hook only decides whether to rewrite: it computes the
# command's effective directory (the payload's .cwd, moved by a leading `cd <dir> &&`) and skips the
# rewrite when no gradlew exists at or above it. A command containing GRADLE_VERBOSE=1, or already
# naming gradle-quiet.sh, passes through untouched.
set -u

input=$(cat)
plugin_root="${CLAUDE_PLUGIN_ROOT:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)}"

QUIET_GRADLE="$plugin_root/scripts/gradle-quiet.sh" python3 -c '
import json
import os
import re
import shlex
import sys

data = json.load(sys.stdin)
tool_input = data.get("tool_input") or {}
cmd = tool_input.get("command") or ""
if "GRADLE_VERBOSE=1" in cmd or "gradle-quiet.sh" in cmd:
    sys.exit(0)

directory = data.get("cwd") or os.environ.get("CLAUDE_PROJECT_DIR") or os.getcwd()
leading_cd = re.match(r"\s*cd\s+(\"[^\"]*\"|\x27[^\x27]*\x27|[^\s;&|]+)\s*&&", cmd)
if leading_cd:
    target = os.path.expanduser(leading_cd.group(1).strip("\"\x27"))
    directory = os.path.normpath(os.path.join(directory, target))
while not os.access(os.path.join(directory, "gradlew"), os.X_OK):
    parent = os.path.dirname(directory)
    if parent == directory:
        sys.exit(0)
    directory = parent

token = "./gradlew"
boundary_before = " \t\n;&|()"
boundary_after = " \t\n;&|)"
quiet = shlex.quote(os.environ["QUIET_GRADLE"])
out = []
quote = None
i = 0
changed = False
while i < len(cmd):
    ch = cmd[i]
    if quote:
        if ch == "\\" and quote == "\"" and i + 1 < len(cmd):
            out.append(cmd[i:i + 2])
            i += 2
            continue
        if ch == quote:
            quote = None
        out.append(ch)
        i += 1
        continue
    if ch == "\\" and i + 1 < len(cmd):
        out.append(cmd[i:i + 2])
        i += 2
        continue
    if ch in "\"\x27":
        quote = ch
        out.append(ch)
        i += 1
        continue
    end = i + len(token)
    if (
        cmd.startswith(token, i)
        and (i == 0 or cmd[i - 1] in boundary_before)
        and (end == len(cmd) or cmd[end] in boundary_after)
    ):
        out.append(quiet)
        i = end
        changed = True
        continue
    out.append(ch)
    i += 1

if changed:
    updated = dict(tool_input, command="".join(out))
    print(json.dumps({"hookSpecificOutput": {"hookEventName": "PreToolUse", "updatedInput": updated}}))
' <<< "$input"
exit 0
