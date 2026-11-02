#!/usr/bin/env bash
# PreToolUse hook (bundled in the rhaydus-kotlin plugin): write-time guard that keeps documentation from
# growing. On Edit/Write it denies a markdown edit that breaks a size budget or its ratchet (a file over
# budget may shrink but never grow), adds a history line to a reference doc, or cites a plan, decision
# number or plan step from a permanent doc; it denies a Kotlin (`.kt`/`.kts`) or YAML comment that cites
# working docs, a decision number or a plan step. On Bash it denies shell writes to markdown, which would
# bypass the checks above.
#
# Every project fact comes from the `docGuard` object in `$CLAUDE_PROJECT_DIR/.claude/rhaydus.json` (override
# the path with `RHAYDUS_CONFIG`); the `rhaydus.doc-budgets` Gradle plugin reads the same object, so the
# hook and the CI gate share one predicate source. A missing config file or `docGuard` key turns the hook
# off. Keys: budgetsFile, activeFile, referenceDocs, workingDocs, permanentDocs (project-relative paths; a
# trailing `/` marks a prefix), decisionPattern, stepPattern (ERE), routing ([{path, hint}]), defaultHint.
set -u

input=$(cat)
root="${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../../.." && pwd)}"
config_file="${RHAYDUS_CONFIG:-$root/.claude/rhaydus.json}"
[ -f "$config_file" ] || exit 0
jq -e '.docGuard | type == "object"' "$config_file" > /dev/null 2>&1 || exit 0

cfg() { jq -r "$1" "$config_file"; }

budgets_rel=$(cfg '.docGuard.budgetsFile // ""')
active_rel=$(cfg '.docGuard.activeFile // ""')
budgets_file=""
active_file=""
[ -n "$budgets_rel" ] && budgets_file="$root/$budgets_rel"
[ -n "$active_rel" ] && active_file="$root/$active_rel"
reference_docs=$(cfg '(.docGuard.referenceDocs // [])[]')
working_docs=$(cfg '(.docGuard.workingDocs // [])[]')
permanent_docs=$(cfg '(.docGuard.permanentDocs // [])[]')
routing_rules=$(cfg '(.docGuard.routing // [])[] | "\(.path)\t\(.hint)"')
default_hint=$(cfg '.docGuard.defaultHint // ""')
decision_pattern=$(cfg '.docGuard.decisionPattern // "\\bD[0-9]{1,2}\\b"')
step_pattern=$(cfg '.docGuard.stepPattern // "\\bS[0-9]{1,2}-[0-9A-Z]"')
history_pattern='\b(used to|previously|no longer|was (changed|replaced|renamed)|we decided|(that|this|which) replaced|originally)\b'

# Plan-directory citations match a directory under a working-docs prefix; a comment may not cite the prefix at all.
plan_dir_pattern=""
comment_working_pattern=""
while IFS= read -r prefix || [ -n "$prefix" ]; do
  [ -n "$prefix" ] || continue
  escaped=$(printf '%s' "$prefix" | sed 's/[]\\.[*^$(){}+?|]/\\&/g')
  plan_dir_pattern="${plan_dir_pattern:+$plan_dir_pattern|}${escaped}[^/[:space:]]+/"
  comment_working_pattern="${comment_working_pattern:+$comment_working_pattern|}${escaped}"
done <<< "$working_docs"

# True when $1 equals a listed path or sits under a listed prefix (an entry ending in `/`).
path_in_list() {
  local relpath="$1" list="$2" entry
  while IFS= read -r entry || [ -n "$entry" ]; do
    [ -n "$entry" ] || continue
    case "$entry" in
      */) case "$relpath" in "$entry"*) return 0 ;; esac ;;
      *) [ "$relpath" = "$entry" ] && return 0 ;;
    esac
  done <<< "$list"
  return 1
}

deny() {
  jq -n --arg reason "$1" \
    '{hookSpecificOutput: {hookEventName: "PreToolUse", permissionDecision: "deny", permissionDecisionReason: $reason}}'
  exit 0
}

file_bytes() {
  if [ -f "$1" ]; then wc -c < "$1" | tr -d ' '; else echo 0; fi
}

file_lines() {
  if [ -f "$1" ]; then grep -c '' "$1"; else echo 0; fi
}

glob_to_ere() {
  local glob="$1" pattern="^" i=0 len ch
  len=${#glob}
  while [ "$i" -lt "$len" ]; do
    if [ "${glob:$i:2}" = "**" ]; then
      pattern="${pattern}.*"
      i=$((i + 2))
      continue
    fi
    ch="${glob:$i:1}"
    case "$ch" in
      '*') pattern="${pattern}[^/]*" ;;
      '.' | '^' | '$' | '+' | '?' | '(' | ')' | '[' | ']' | '{' | '}' | '|' | '\')
        pattern="${pattern}\\${ch}" ;;
      *) pattern="${pattern}${ch}" ;;
    esac
    i=$((i + 1))
  done
  printf '%s$' "$pattern"
}

parse_limit() {
  case "$1" in
    *KB) printf '%s B' "$(( ${1%KB} * 1024 ))" ;;
    *B) printf '%s B' "${1%B}" ;;
    *L) printf '%s L' "${1%L}" ;;
  esac
}

find_budget() {
  local relpath="$1" line glob token regex
  [ -f "$budgets_file" ] || return 1
  while IFS= read -r line || [ -n "$line" ]; do
    case "$line" in
      "## Now"*) continue ;;
      "#"*) continue ;;
      "") continue ;;
    esac
    glob=$(printf '%s' "$line" | awk '{print $1}')
    token=$(printf '%s' "$line" | awk '{print $NF}')
    regex=$(glob_to_ere "$glob")
    if [[ "$relpath" =~ $regex ]]; then
      printf '%s %s' "$glob" "$token"
      return 0
    fi
  done < "$budgets_file"
  return 1
}

now_limit_token() {
  [ -f "$budgets_file" ] || return 0
  grep '^## Now' "$budgets_file" | awk '{print $NF}'
}

extract_now_section() {
  [ -f "$1" ] || return 0
  awk '
    /^## Now[[:space:]]*$/ { capture=1; print; next }
    capture && /^## / { exit }
    capture { print }
  ' "$1"
}

extract_section_body() {
  [ -f "$1" ] || return 0
  awk -v header="$2" '
    $0 ~ ("^" header "[[:space:]]*$") { capture=1; next }
    capture && /^## / { exit }
    capture { print }
  ' "$1"
}

routing_hint() {
  local path hint
  while IFS=$'\t' read -r path hint || [ -n "$path" ]; do
    [ -n "$path" ] || continue
    if path_in_list "$1" "$path"; then
      printf '%s' "$hint"
      return 0
    fi
  done <<< "$routing_rules"
  printf '%s' "$default_hint"
}

check_budget() {
  local relpath="$1" new_file="$2" old_file="$3" budget glob token value unit new_size old_size

  budget=$(find_budget "$relpath") || return 0
  glob="${budget% *}"
  token="${budget##* }"
  read -r value unit <<< "$(parse_limit "$token")"

  if [ "$unit" = "L" ]; then
    new_size=$(file_lines "$new_file")
    old_size=$(file_lines "$old_file")
  else
    new_size=$(file_bytes "$new_file")
    old_size=$(file_bytes "$old_file")
  fi

  if [ "$new_size" -gt "$value" ] && [ "$new_size" -gt "$old_size" ]; then
    deny "$relpath exceeds its budget ($glob $token): ${old_size}${unit} → ${new_size}${unit} (limit ${value}${unit}). $(routing_hint "$relpath")"
  fi
}

check_now_section() {
  local relpath="$1" new_file="$2" old_file="$3" path
  [ -n "$active_file" ] && [ -f "$active_file" ] || return 0
  grep -qxF -- "$relpath" "$active_file" || return 0

  local now_limit new_now old_now new_tmp old_tmp
  now_limit=$(now_limit_token)
  [ -n "$now_limit" ] || return 0
  now_limit="${now_limit%L}"

  new_tmp=$(mktemp)
  old_tmp=$(mktemp)
  extract_now_section "$new_file" > "$new_tmp"
  extract_now_section "$old_file" > "$old_tmp"
  new_now=$(file_lines "$new_tmp")
  old_now=$(file_lines "$old_tmp")
  rm -f "$new_tmp" "$old_tmp"

  if [ "$new_now" -gt "$now_limit" ] && [ "$new_now" -gt "$old_now" ]; then
    deny "$relpath ## Now section exceeds its budget (## Now ${now_limit}L): ${old_now}L → ${new_now}L (limit ${now_limit}L). $(routing_hint "$relpath")"
  fi
}

check_history() {
  local relpath="$1" new_file="$2" old_file="$3" line
  path_in_list "$relpath" "$reference_docs" || return 0

  while IFS= read -r line || [ -n "$line" ]; do
    [ -n "$line" ] || continue
    grep -qxF -- "$line" "$old_file" 2>/dev/null && continue
    case "$line" in
      *'<!-- history-ok -->'*) continue ;;
    esac
    if printf '%s' "$line" | grep -iEq "$history_pattern"; then
      deny "$relpath adds a history line: \"$line\" — rewrite to present tense, or mark the line \`<!-- history-ok -->\`."
    fi
  done < "$new_file"
}

is_permanent_doc() {
  path_in_list "$1" "$permanent_docs"
}

check_plan_refs() {
  local relpath="$1" new_file="$2" old_file="$3" line
  is_permanent_doc "$relpath" || return 0

  while IFS= read -r line || [ -n "$line" ]; do
    [ -n "$line" ] || continue
    grep -qxF -- "$line" "$old_file" 2>/dev/null && continue
    if [ -n "$plan_dir_pattern" ] && printf '%s' "$line" | grep -Eq "$plan_dir_pattern"; then
      deny "$relpath cites a plan directory: \"$line\" — plans are deleted when they finish; link only a top-level working-docs file."
    fi
    if printf '%s' "$line" | grep -Eq "$decision_pattern"; then
      deny "$relpath cites a decision number: \"$line\" — plans are deleted when they finish; the reason goes in the PR description."
    fi
    if printf '%s' "$line" | grep -Eq "$step_pattern"; then
      deny "$relpath cites a plan step: \"$line\" — plans are deleted when they finish; the reason goes in the PR description."
    fi
  done < "$new_file"
}

check_decisions() {
  local relpath="$1" new_file="$2" old_file="$3" line in_bullet=false section_tmp
  path_in_list "$relpath" "$working_docs" || return 0

  section_tmp=$(mktemp)
  extract_section_body "$new_file" "## Decisions" > "$section_tmp"

  while IFS= read -r line || [ -n "$line" ]; do
    case "$line" in
      "#"* | "") in_bullet=false; continue ;;
    esac
    if [[ "$line" == "- "* ]]; then
      if [ "${#line}" -gt 200 ] && ! grep -qxF -- "$line" "$old_file" 2>/dev/null; then
        rm -f "$section_tmp"
        deny "$relpath: a decision is one line; a step's approved shape goes in its step file's ## Approved shape"
      fi
      in_bullet=true
      continue
    fi
    if [ "$in_bullet" = true ] && [[ "$line" =~ ^[[:space:]]+[^[:space:]] ]]; then
      if ! grep -qxF -- "$line" "$old_file" 2>/dev/null; then
        rm -f "$section_tmp"
        deny "$relpath: a decision is one line; a step's approved shape goes in its step file's ## Approved shape"
      fi
      continue
    fi
    in_bullet=false
  done < "$section_tmp"

  rm -f "$section_tmp"
}

compute_new_content() {
  local old_file="$1" out_file="$2" err_file="$3"
  printf '%s' "$input" | jq -j --rawfile old "$old_file" '
    .tool_input as $ti
    | ($old) as $o
    | if .tool_name == "Write" then
        ($ti.content // "")
      else
        ($ti.old_string // "") as $olds
        | ($ti.new_string // "") as $news
        | ($ti.replace_all // false) as $all
        | ($o | split($olds)) as $parts
        | if ($parts|length) < 2 then error("DOC_GUARD_NOT_FOUND")
          elif $all then ($parts | join($news))
          else ($parts[0] + $news + ($parts[1:] | join($olds)))
          end
      end
  ' > "$out_file" 2> "$err_file"
}

handle_markdown_write() {
  local path old_file rc relpath
  path=$(printf '%s' "$input" | jq -r '.tool_input.file_path // ""')
  case "$path" in
    "$root"/*) ;;
    *) return 0 ;;
  esac
  case "$(printf '%s' "$path" | tr '[:upper:]' '[:lower:]')" in
    *.md) ;;
    *) return 0 ;;
  esac

  if [ -f "$path" ]; then old_file="$path"; else old_file="/dev/null"; fi

  tmp_new=$(mktemp)
  tmp_err=$(mktemp)
  trap 'rm -f "$tmp_new" "$tmp_err"' EXIT
  compute_new_content "$old_file" "$tmp_new" "$tmp_err"
  rc=$?
  if [ "$rc" -ne 0 ]; then
    grep -q DOC_GUARD_NOT_FOUND "$tmp_err" && return 0
    deny "doc-guard could not evaluate the edit: $(tr '\n' ' ' < "$tmp_err")"
  fi

  relpath="${path#"$root"/}"
  check_budget "$relpath" "$tmp_new" "$old_file"
  check_now_section "$relpath" "$tmp_new" "$old_file"
  check_history "$relpath" "$tmp_new" "$old_file"
  check_plan_refs "$relpath" "$tmp_new" "$old_file"
  check_decisions "$relpath" "$tmp_new" "$old_file"
}

extract_code_comment() {
  awk '
  BEGIN { sq = sprintf("%c", 39) }
  {
    n = length($0)
    in_str = 0
    in_chr = 0
    for (i = 1; i <= n; i++) {
      c = substr($0, i, 1)
      if (in_str) {
        if (c == "\\") { i++; continue }
        if (c == "\"") in_str = 0
        continue
      }
      if (in_chr) {
        if (c == "\\") { i++; continue }
        if (c == sq) in_chr = 0
        continue
      }
      if (c == "\"") { in_str = 1; continue }
      if (c == sq) { in_chr = 1; continue }
      if (c == "/") {
        nc = substr($0, i + 1, 1)
        if (nc == "/" || nc == "*") {
          print substr($0, i + 2)
          exit
        }
      }
    }
  }' <<< "$1"
}

extract_yaml_comment() {
  awk '
  BEGIN { sq = sprintf("%c", 39) }
  {
    n = length($0)
    quote = ""
    for (i = 1; i <= n; i++) {
      c = substr($0, i, 1)
      if (quote == sq) {
        if (c == sq) {
          if (substr($0, i + 1, 1) == sq) { i++; continue }
          quote = ""
        }
        continue
      }
      if (quote == "\"") {
        if (c == "\\") { i++; continue }
        if (c == "\"") quote = ""
        continue
      }
      if (c == sq) { quote = sq; continue }
      if (c == "\"") { quote = "\""; continue }
      if (c == "#") {
        prevc = (i == 1) ? "" : substr($0, i - 1, 1)
        if (i == 1 || prevc == " " || prevc == "\t") {
          print substr($0, i + 1)
          exit
        }
      }
    }
  }' <<< "$1"
}

check_kotlin_comments() {
  local relpath="$1" new_file="$2" old_file="$3" line trimmed comment
  while IFS= read -r line || [ -n "$line" ]; do
    [ -n "$line" ] || continue
    trimmed="${line#"${line%%[![:space:]]*}"}"
    case "$trimmed" in
      '//'* | '*'* | '/**'*) comment="$trimmed" ;;
      *)
        comment=$(extract_code_comment "$line")
        [ -n "$comment" ] || continue
        ;;
    esac
    grep -qxF -- "$line" "$old_file" 2>/dev/null && continue
    if [ -n "$comment_working_pattern" ] && printf '%s' "$comment" | grep -Eq "$comment_working_pattern"; then
      deny "$relpath adds a comment citing working docs: \"$comment\" — plans are deleted when they finish; cite nothing from the working docs. The reason goes in the PR description."
    fi
    if printf '%s' "$comment" | grep -Eq "$decision_pattern"; then
      deny "$relpath adds a comment citing a decision number: \"$comment\" — plans are deleted when they finish; cite nothing from the working docs. The reason goes in the PR description."
    fi
    if printf '%s' "$comment" | grep -Eq "$step_pattern"; then
      deny "$relpath adds a comment citing a plan step: \"$comment\" — plans are deleted when they finish; cite nothing from the working docs. The reason goes in the PR description."
    fi
  done < "$new_file"
}

handle_kotlin_write() {
  local path old_file rc relpath
  path=$(printf '%s' "$input" | jq -r '.tool_input.file_path // ""')
  case "$path" in
    "$root"/*) ;;
    *) return 0 ;;
  esac
  case "$path" in
    *.kt | *.kts) ;;
    *) return 0 ;;
  esac

  if [ -f "$path" ]; then old_file="$path"; else old_file="/dev/null"; fi

  tmp_new=$(mktemp)
  tmp_err=$(mktemp)
  trap 'rm -f "$tmp_new" "$tmp_err"' EXIT
  compute_new_content "$old_file" "$tmp_new" "$tmp_err"
  rc=$?
  if [ "$rc" -ne 0 ]; then
    grep -q DOC_GUARD_NOT_FOUND "$tmp_err" && return 0
    deny "doc-guard could not evaluate the edit: $(tr '\n' ' ' < "$tmp_err")"
  fi

  relpath="${path#"$root"/}"
  check_kotlin_comments "$relpath" "$tmp_new" "$old_file"
}

check_yaml_comments() {
  local relpath="$1" new_file="$2" old_file="$3" line comment
  while IFS= read -r line || [ -n "$line" ]; do
    [ -n "$line" ] || continue
    comment=$(extract_yaml_comment "$line")
    [ -n "$comment" ] || continue
    grep -qxF -- "$line" "$old_file" 2>/dev/null && continue
    if [ -n "$comment_working_pattern" ] && printf '%s' "$comment" | grep -Eq "$comment_working_pattern"; then
      deny "$relpath adds a comment citing working docs: \"$comment\" — plans are deleted when they finish; cite nothing from the working docs. The reason goes in the PR description."
    fi
    if printf '%s' "$comment" | grep -Eq "$decision_pattern"; then
      deny "$relpath adds a comment citing a decision number: \"$comment\" — plans are deleted when they finish; cite nothing from the working docs. The reason goes in the PR description."
    fi
    if printf '%s' "$comment" | grep -Eq "$step_pattern"; then
      deny "$relpath adds a comment citing a plan step: \"$comment\" — plans are deleted when they finish; cite nothing from the working docs. The reason goes in the PR description."
    fi
  done < "$new_file"
}

handle_yaml_write() {
  local path old_file rc relpath
  path=$(printf '%s' "$input" | jq -r '.tool_input.file_path // ""')
  case "$path" in
    "$root"/*) ;;
    *) return 0 ;;
  esac
  case "$path" in
    *.yml | *.yaml) ;;
    *) return 0 ;;
  esac

  if [ -f "$path" ]; then old_file="$path"; else old_file="/dev/null"; fi

  tmp_new=$(mktemp)
  tmp_err=$(mktemp)
  trap 'rm -f "$tmp_new" "$tmp_err"' EXIT
  compute_new_content "$old_file" "$tmp_new" "$tmp_err"
  rc=$?
  if [ "$rc" -ne 0 ]; then
    grep -q DOC_GUARD_NOT_FOUND "$tmp_err" && return 0
    deny "doc-guard could not evaluate the edit: $(tr '\n' ' ' < "$tmp_err")"
  fi

  relpath="${path#"$root"/}"
  check_yaml_comments "$relpath" "$tmp_new" "$old_file"
}

handle_bash() {
  local command
  command=$(printf '%s' "$input" | jq -r '.tool_input.command // ""')

  if printf '%s' "$command" | grep -qE '>{1,2}[[:space:]]*[^[:space:]&][^[:space:]]*\.md([[:space:]]|[;&|]|$)'; then
    deny "edit markdown with Edit/Write so doc-guard sees the change"
  fi

  if printf '%s' "$command" | grep -qE '(^|[^[:alnum:]_])tee([[:space:]]|$)'; then
    printf '%s' "$command" | grep -qE '\.md([[:space:]]|[;&|]|$)' &&
      deny "edit markdown with Edit/Write so doc-guard sees the change"
  fi

  if printf '%s' "$command" | grep -qE '(^|[^[:alnum:]_])(sed|perl)([^;&|]*)[[:space:]]-i([[:space:]]|$)'; then
    printf '%s' "$command" | grep -qE '\.md([[:space:]]|[;&|]|$)' &&
      deny "edit markdown with Edit/Write so doc-guard sees the change"
  fi

  if printf '%s' "$command" | grep -qE '(^|[^[:alnum:]_])(python3?|node|ruby|perl)([[:space:]].*)?[[:space:]](-c|-e)([[:space:]]|$)'; then
    printf '%s' "$command" | grep -q '\.md' &&
      deny "edit markdown with Edit/Write so doc-guard sees the change"
  fi

  if printf '%s' "$command" | grep -qE '(^|[^[:alnum:]_])(python3?|node|ruby|perl)[[:space:]][^|;&]*<<'; then
    printf '%s' "$command" | grep -q '\.md' &&
      deny "edit markdown with Edit/Write so doc-guard sees the change"
  fi

  if printf '%s' "$command" | grep -qE '(^|[^[:alnum:]_])cp([[:space:]]|$)'; then
    case "$(printf '%s' "$command" | awk '{print $NF}')" in
      *.md) deny "edit markdown with Edit/Write so doc-guard sees the change" ;;
    esac
  fi

  if printf '%s' "$command" | grep -qE '(^|[^[:alnum:]_])dd([[:space:]]|$)'; then
    printf '%s' "$command" | grep -qE '(^|[[:space:]])of=[^[:space:]]*\.md([[:space:]]|$)' &&
      deny "edit markdown with Edit/Write so doc-guard sees the change"
  fi

  if printf '%s' "$command" | grep -qE '(^|[^[:alnum:]_])rsync([[:space:]]|$)'; then
    case "$(printf '%s' "$command" | awk '{print $NF}')" in
      *.md) deny "edit markdown with Edit/Write so doc-guard sees the change" ;;
    esac
  fi

  if printf '%s' "$command" | grep -qE '(^|[^[:alnum:]_])awk([[:space:]].*)?[[:space:]]-i[[:space:]]+inplace'; then
    printf '%s' "$command" | grep -q '\.md' &&
      deny "edit markdown with Edit/Write so doc-guard sees the change"
  fi
}

tool_name=$(printf '%s' "$input" | jq -r '.tool_name // ""')
case "$tool_name" in
  Edit | Write)
    handle_markdown_write
    handle_kotlin_write
    handle_yaml_write
    ;;
  Bash) handle_bash ;;
esac
exit 0
