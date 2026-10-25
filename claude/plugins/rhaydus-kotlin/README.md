# rhaydus-kotlin (Claude Code plugin)

Shared Claude Code assets for Kotlin/Compose/TOAD apps built on the `nl.rhaydus` foundation.

## What it ships

| Component | Name | Notes |
|---|---|---|
| Subagent | `rhaydus-adopt` | Onboards a project: adds the foundation submodule and its `includeBuild`, wires the catalog / convention plugins, loads this plugin from the project's settings, writes the managed routing block into the project's `CLAUDE.md`. |
| Subagent | `implementer` | Builds one vertical slice: TOAD logic, domain, data, mapper and the stateless Compose render, reusing the foundation design system **and** the project's own brand doc. Needs a brief with `## Goal` / `## Files` / `## Verify`. |
| Subagent | `reviewer` | Correctness / security / architecture review of a `## Scope`, one review per stage. Builds project-scoped agent-memory. |
| Subagent | `test-writer` | Triage-first, edge-case-aware tests; knows the TOAD layering (Actions/Collectors, use cases, data sources). Needs `## Files` / `## Verify`. |
| Skill | `style-check` | Auto-fix mechanizable style, then run the project's verification gates. Adapts to whatever gates exist. |
| Hook | docs-first reminder | `PreToolUse` on `Agent`: once per session, nudges to read the project's docs before fanning out Explore/general-purpose agents. |

## Development model

The agents are specialists you route substantial work to (not every one-line edit):

- **`rhaydus-adopt`** runs once at onboarding (and when the foundation tag or deps change). It writes a managed block into the project's `CLAUDE.md` recording the consumed modules, the doc locations, the app's own design-system path, and the routing rules below.
- A **feature** goes to one **`implementer`** per vertical slice: logic, data and the stateless render together. When a slice stops at the TOAD seam, it returns a `## Contract` section (state, actions, events) for a later slice to bind to; the orchestrating assistant drives the handoff.
- Agents grep **`CAPABILITIES.md`** for a concept before hand-rolling it, so they reuse what the foundation already ships for the version a project is pinned to.

## Install

The foundation is a git submodule at `foundation/`; the plugin loads in place from a project-scoped
local-directory marketplace. Add this to the project's `.claude/settings.json`, or run `rhaydus-adopt`:

```json
{
  "extraKnownMarketplaces": {
    "rhaydus": { "source": { "source": "directory", "path": "./foundation" } }
  },
  "enabledPlugins": { "rhaydus-kotlin@rhaydus": true }
}
```

Each machine then registers it once, from the project root:

```bash
claude plugin marketplace add ./foundation --scope project
claude plugin install rhaydus-kotlin@rhaydus --scope project
```

The first command writes an absolute path into `.claude/settings.json`; set it back to `./foundation`
before committing. Edits to the plugin apply after `/reload-plugins` or in the next session. A user-scope `rhaydus`
marketplace that installs from GitHub conflicts with this one and ignores the pinned commit; remove it with
`claude plugin marketplace remove rhaydus`.

Skills are namespaced as `/rhaydus-kotlin:style-check`. The subagents become available to the Agent
tool. Each project accumulates its own per-agent agent-memory under `.claude/agent-memory/` — that
memory is per-project and is NOT shipped by this plugin.

## Conventions docs

The agents and the docs-first hook refer to "the project's conventions docs." The canonical, shared
copies live in this repo at [`../../../docs/`](../../../docs): [`CAPABILITIES.md`](../../../docs/CAPABILITIES.md)
(the index of what the foundation provides — every `rhaydus-*` agent reads it first), plus architecture,
TOAD, code-style, and design-system foundations. Consuming projects read them in place from `foundation/docs/` at the
pinned commit, never a vendored copy; `rhaydus-adopt` records that path in the project's `CLAUDE.md`; each app keeps only its app-specific deltas (brand tokens, platform set).

## Deliberately NOT included

Project-shaped automation stays in each app, because it hardcodes per-app layout or platform set:
`release`, `set-version-name`, `increment-version-code` (Android/iOS/desktop version files differ per
app), and `update-readme` (depends on the app's module layout and README structure). Generalize and
promote them here later if it proves worthwhile.
