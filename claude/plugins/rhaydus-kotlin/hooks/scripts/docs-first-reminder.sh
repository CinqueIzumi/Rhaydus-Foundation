#!/usr/bin/env bash
# PreToolUse hook (bundled in the rhaydus-kotlin plugin): before fanning out code-exploration agents,
# remind to read the project's docs first. Fires once per session, and only for Explore /
# general-purpose subagents (the ones used to map the codebase). Other agent types (claude-code-guide,
# Plan, etc.) pass through untouched.
#
# additionalContext is not honored on PreToolUse, so the only way to surface a model-visible note is
# exit 2 (which blocks). To avoid blocking every spawn forever, this blocks just the first matching
# spawn per session, drops a marker, then stays out of the way.

input=$(cat)

subagent=$(printf '%s' "$input" | jq -r '.tool_input.subagent_type // empty' 2>/dev/null)
session=$(printf '%s' "$input" | jq -r '.session_id // "nosession"' 2>/dev/null)

case "$subagent" in
  Explore | general-purpose) ;;
  *) exit 0 ;;
esac

marker="${TMPDIR:-/tmp}/rhaydus-docs-reminder-${session}"

if [ -f "$marker" ]; then
  exit 0
fi

touch "$marker"

echo "Docs-first reminder: before exploring the code with agents, read this project's conventions docs. If this project consumes the nl.rhaydus foundation, start with its capabilities index (CAPABILITIES.md — the modules/components/APIs available, so you reuse rather than reinvent) and the 'Rhaydus foundation' block in CLAUDE.md (it records the doc locations, this app's design-system path, and which work routes to the rhaydus-adopt / rhaydus-logic / rhaydus-ui agents). Then the architecture, TOAD, code-style, and design-system docs (see CLAUDE.md for exact locations). They cover the data/domain/presentation layering, the TOAD pattern and add-a-feature checklist, the code style, and the design system, so most architecture questions are answered there. For a new feature, prefer the rhaydus-logic (logic) and rhaydus-ui (UI) agents over hand-rolling. Reserve Explore/general-purpose agents for what the docs genuinely do not cover (exact dependency versions, current assets state, verbatim base-class signatures). If you have already read them, re-issue this call to proceed. This reminder fires once per session." >&2

exit 2
