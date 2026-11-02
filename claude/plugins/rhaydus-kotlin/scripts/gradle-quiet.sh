#!/usr/bin/env bash
# Quiet Gradle wrapper (bundled in the rhaydus-kotlin plugin): runs the nearest ./gradlew with its
# output sent to a log file and prints a short summary instead (result line, failed tasks, compiler
# errors, failing tests, style findings) followed by the path of the full log.
#
# The project root is the nearest ancestor of the current directory that holds a gradlew, so a run from
# a git worktree or a nested included project uses that checkout, not the main one. Logs go to
# <root>/build/claude-logs. Setting GRADLE_VERBOSE=1 on a command makes the quiet-gradle.sh hook skip
# this wrapper.
set -u

root=$PWD
while [ ! -x "$root/gradlew" ]; do
  if [ "$root" = "/" ]; then
    echo "gradle-quiet.sh: no gradlew found at or above $PWD" >&2
    exit 1
  fi
  root=$(dirname "$root")
done

log_dir="$root/build/claude-logs"
mkdir -p "$log_dir"
log="$log_dir/gradle-$(date -u +%Y%m%dT%H%M%SZ)-$$.log"
marker="$log_dir/.run-$$"
touch "$marker"

cd "$root" || exit 1
./gradlew "$@" > "$log" 2>&1
status=$?

python3 - "$log" "$marker" "$root" "$status" "$@" <<'PY'
import os
import re
import sys
import xml.etree.ElementTree as ET

log_path, marker, root, status = sys.argv[1:5]
tasks = sys.argv[5:]
lines = open(log_path, errors="replace").read().splitlines()
since = os.path.getmtime(marker)
out = []


def capped(title, items, cap=20):
    if not items:
        return
    out.append(f"{title} ({len(items)}):")
    out.extend(f"  {item}" for item in items[:cap])
    if len(items) > cap:
        out.append(f"  … {len(items) - cap} more in the log")


def unique(items):
    return list(dict.fromkeys(items))


result = next((l for l in reversed(lines) if re.match(r"BUILD (SUCCESSFUL|FAILED) in ", l)), None)
out.append(result or f"Gradle exited with {status} (no BUILD line)")
out.append("Tasks: " + " ".join(tasks))

failed = unique(l.strip() for l in lines if re.match(r"^> Task \S+ FAILED", l))
capped("Failed tasks", failed)
for i, line in enumerate(lines):
    if line.startswith("* What went wrong:"):
        out.append(line)
        for detail in lines[i + 1:i + 6]:
            if detail.startswith("* "):
                break
            if detail.strip():
                out.append(detail)

capped("Compiler errors", unique(l for l in lines if l.startswith("e: ")))

failing_tests = []
for dirpath, dirnames, filenames in os.walk(root):
    dirnames[:] = [d for d in dirnames if d not in (".git", ".gradle", "node_modules", "claude-logs")]
    if "test-results" not in dirpath:
        continue
    for name in filenames:
        path = os.path.join(dirpath, name)
        if not (name.startswith("TEST-") and name.endswith(".xml")) or os.path.getmtime(path) < since:
            continue
        try:
            cases = ET.parse(path).getroot().iter("testcase")
        except ET.ParseError:
            continue
        for case in cases:
            if case.find("failure") is not None or case.find("error") is not None:
                failing_tests.append(f"{case.get('classname')}.{case.get('name')}")
capped("Failing tests", unique(failing_tests))

finding = re.compile(r"^\S+\.kts?:\d+:\d+")
capped("Style findings", unique(l.strip() for l in lines if finding.match(l.strip())))

limit = 39
if len(out) > limit:
    out = out[:limit - 1] + [f"… {len(out) - limit + 1} more summary lines; see the log"]
out.append(f"Full log: {log_path}")
print("\n".join(out))
PY

rm -f "$marker"
exit "$status"
