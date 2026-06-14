# rhaydus-kotlin (Claude Code plugin)

Shared Claude Code assets for the `nl.rhaydus` Kotlin/Compose/TOAD apps (Nestbox, Softcover).

## What it ships

| Component | Name | Notes |
|---|---|---|
| Subagent | `code-reviewer` | Correctness / style / architecture review. Builds project-scoped agent-memory. |
| Subagent | `unit-test-writer` | Edge-case-first tests; knows the TOAD layering (Actions/Collectors, use cases, data sources). |
| Skill | `style-check` | Auto-fix mechanizable style, then run the project's verification gates. Adapts to whatever gates exist. |
| Hook | docs-first reminder | `PreToolUse` on `Agent`: once per session, nudges to read the project's docs before fanning out Explore/general-purpose agents. |

## Install

```
/plugin marketplace add CinqueIzumi/rhaydus-foundation
/plugin install rhaydus-kotlin@rhaydus
```

Skills are namespaced as `/rhaydus-kotlin:style-check`. The two subagents become available to the
Agent tool. Each project accumulates its own `code-reviewer` / `unit-test-writer` agent-memory under
`.claude/agent-memory/` — that memory is per-project and is NOT shipped by this plugin.

## Conventions docs

The agents and the docs-first hook refer to "the project's conventions docs." The canonical, shared
copies live in this repo at [`../../../docs/`](../../../docs) (architecture, TOAD, code-style,
design-system foundations). They are not duplicated into the plugin - each consuming project links to
them from its own `CLAUDE.md` and keeps only app-specific deltas.

## Deliberately NOT included

Project-shaped automation stays in each app, because it hardcodes per-app layout or platform set:
`release`, `set-version-name`, `increment-version-code` (Android/iOS/desktop version files differ per
app), and `update-readme` (depends on the app's module layout and README structure). Generalize and
promote them here later if it proves worthwhile.
