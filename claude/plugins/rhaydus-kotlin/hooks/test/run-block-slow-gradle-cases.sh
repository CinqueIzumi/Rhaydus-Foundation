#!/usr/bin/env bash
# Case runner for ../scripts/block-slow-gradle.sh. Builds fixture projects (blocked/allowed task
# config, empty blockedTasks, no config) in a temp dir; never touches a real checkout.
set -u

here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
hook="$here/../scripts/block-slow-gradle.sh"
failures=0

tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
configured="$tmp/configured"
empty="$tmp/empty"
none="$tmp/none"
skipvalues="$tmp/skipvalues"
mkdir -p "$configured/.claude" "$empty/.claude" "$none" "$skipvalues/.claude"
echo '{"slowGradle": {"blockedTasks": ["build", "lint"]}}' > "$skipvalues/.claude/rhaydus.json"
cat > "$configured/.claude/rhaydus.json" <<'JSON'
{
  "slowGradle": {
    "blockedTasks": [
      "assemble.*",
      "build",
      "link.*",
      ".*releaseframework.*",
      ".*binaries.*",
      ".*embedandsign.*",
      ".*bundlerelease.*",
      ".*publish.*",
      ".*ios(arm64|simulatorarm64|x64).*",
      ".*iosmain.*"
    ],
    "allowedTasks": [
      "assembledebug.*",
      "compilekotlinios.*",
      "compileiosmainkotlinmetadata.*"
    ],
    "hint": "Use a lightweight task instead."
  }
}
JSON
echo '{"slowGradle": {"blockedTasks": []}}' > "$empty/.claude/rhaydus.json"

# check <name> <project> <command> <expected exit code>
check() {
  local name=$1 project=$2 command=$3 expected=$4 actual
  jq -n --arg c "$command" '{tool_input: {command: $c}}' |
    CLAUDE_PROJECT_DIR="$project" bash "$hook" > /dev/null 2>&1
  actual=$?
  if [ "$actual" -eq "$expected" ]; then
    echo "ok    $name"
  else
    echo "FAIL  $name (expected exit $expected, got $actual)"
    failures=$((failures + 1))
  fi
}

blocked() { check "blocked: $1" "$configured" "$1" 2; }
passed() { check "passed:  $1" "$configured" "$1" 0; }

blocked './gradlew assembleRelease'
blocked './gradlew assemble'
blocked './gradlew :app:build'
blocked './gradlew build -x test'
blocked './gradlew iosSimulatorArm64Test'
blocked './gradlew :shared:iosX64Test'
blocked './gradlew linkReleaseFrameworkIosArm64'
blocked './gradlew :x:linkDebugFrameworkIosArm64'
blocked './gradlew :x:embedAndSignAppleFrameworkForXcode'
blocked './gradlew publishToMavenLocal'
blocked 'scripts/gradle-quiet.sh :app:build'
blocked 'gradle build'
blocked './gradlew test && ./gradlew assembleRelease'
blocked './gradlew :x:compileKotlinIosSimulatorArm64 :y:iosSimulatorArm64Test'
blocked 'JAVA_HOME=/opt/jdk ./gradlew :app:ASSEMBLERELEASE'

passed './gradlew assembleDebug'
passed './gradlew :app:assembleDebugAndroidTest'
passed './gradlew :x:compileKotlinIosSimulatorArm64'
passed './gradlew :x:compileIosMainKotlinMetadata'
passed './gradlew :x:testAndroidHostTest'
passed './gradlew --build-cache test'
passed './gradlew test | grep build'
passed './gradlew test | grep assembleRelease; echo "build done"'
passed './gradlew test > build/out.txt'
passed 'ls ~/.gradle/caches'
passed 'echo assembleRelease'
passed 'git status'
passed 'ACCEPTANCE_BUILD=1 ./gradlew assembleRelease'
passed 'ACCEPTANCE_BUILD=1 ./gradlew :app:build'
passed "'ACCEPTANCE_BUILD=1' ./gradlew build"
passed 'env ACCEPTANCE_BUILD=1 ./gradlew linkReleaseFrameworkIosArm64'

check "empty blockedTasks passes a blocked-looking task" "$empty" './gradlew assembleRelease' 0
check "empty blockedTasks passes build" "$empty" './gradlew build' 0
check "no config passes a blocked-looking task" "$none" './gradlew assembleRelease' 0
check "no config passes build" "$none" './gradlew :app:build' 0

# An option's value token is not a task, so only the tasks around it are checked.
skipped() { check "passed:  $1" "$skipvalues" "$1" 0; }
skipped './gradlew check -x lint'
skipped './gradlew check --exclude-task lint'
skipped './gradlew -p build test'
skipped './gradlew --project-dir build test'
skipped './gradlew test --tests build'
skipped './gradlew --project-dir=build test'
check "blocked: ./gradlew -x lint build" "$skipvalues" './gradlew -x lint build' 2

# The denial names the blocked task and carries the configured hint.
msg=$(jq -n --arg c './gradlew :app:build' '{tool_input: {command: $c}}' | CLAUDE_PROJECT_DIR="$configured" bash "$hook" 2>&1 > /dev/null)
case "$msg" in
  *"runs 'build'"*"Use a lightweight task instead."*"ACCEPTANCE_BUILD=1"*) echo "ok    denial names task, hint and bypass" ;;
  *) echo "FAIL  denial names task, hint and bypass"; failures=$((failures + 1)) ;;
esac

exit "$failures"
