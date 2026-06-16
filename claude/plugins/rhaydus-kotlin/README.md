# rhaydus-kotlin (Claude Code plugin)

Shared Claude Code assets for Kotlin/Compose/TOAD apps built on the `nl.rhaydus` foundation.

## What it ships

| Component | Name | Notes |
|---|---|---|
| Subagent | `rhaydus-adopt` | Onboards a project: wires the catalog / convention plugins / `includeBuild`, makes the foundation docs reachable, writes the managed routing block into the project's `CLAUDE.md`. |
| Subagent | `rhaydus-logic` | Builds feature **logic**: the TOAD state/action contract, ScreenModel, Collectors, use cases, domain, data. Owns `UiState`/`UiAction`/`UiEvent` and hands the UI off via a contract spec. |
| Subagent | `rhaydus-ui` | Builds feature **UI**: the stateless Compose render, reusing the foundation design system **and** the project's own brand/editorial design doc. Consumes the TOAD contract; never invents state/actions. |
| Subagent | `code-reviewer` | Correctness / style / architecture review. Builds project-scoped agent-memory. |
| Subagent | `unit-test-writer` | Edge-case-first tests; knows the TOAD layering (Actions/Collectors, use cases, data sources). |
| Skill | `style-check` | Auto-fix mechanizable style, then run the project's verification gates. Adapts to whatever gates exist. |
| Hook | docs-first reminder | `PreToolUse` on `Agent`: once per session, nudges to read the project's docs before fanning out Explore/general-purpose agents. |

## Development model

The three `rhaydus-*` agents are specialists you route substantial work to (not every one-line edit):

- **`rhaydus-adopt`** runs once at onboarding (and when foundation deps change). It writes a managed block into the project's `CLAUDE.md` recording the consumed modules, the doc locations, the app's own design-system path, and the routing rules below.
- A **new feature** splits along TOAD's seam: **`rhaydus-logic`** builds everything behind `runAction` and defines the `UiState`/`UiAction`/`UiEvent` contract; that contract is handed to **`rhaydus-ui`**, which builds the stateless Compose render. The orchestrating assistant drives the handoff — the logic agent returns a UI spec, it does not spawn the UI agent itself.
- Every agent reads **`CAPABILITIES.md`** first, so it reuses what the foundation already ships and stays correct for the version a project is pinned to.

## Install

```
/plugin marketplace add CinqueIzumi/rhaydus-foundation
/plugin install rhaydus-kotlin@rhaydus
```

Skills are namespaced as `/rhaydus-kotlin:style-check`. The subagents become available to the Agent
tool. Each project accumulates its own per-agent agent-memory under `.claude/agent-memory/` — that
memory is per-project and is NOT shipped by this plugin.

## Conventions docs

The agents and the docs-first hook refer to "the project's conventions docs." The canonical, shared
copies live in this repo at [`../../../docs/`](../../../docs): [`CAPABILITIES.md`](../../../docs/CAPABILITIES.md)
(the index of what the foundation provides — every `rhaydus-*` agent reads it first), plus architecture,
TOAD, code-style, and design-system foundations. `rhaydus-adopt` makes them reachable from each consuming
project (an `includeBuild` sibling or a vendored, version-pinned snapshot) and records their location in
the project's `CLAUDE.md`; each app keeps only its app-specific deltas (brand tokens, platform set).

## Deliberately NOT included

Project-shaped automation stays in each app, because it hardcodes per-app layout or platform set:
`release`, `set-version-name`, `increment-version-code` (Android/iOS/desktop version files differ per
app), and `update-readme` (depends on the app's module layout and README structure). Generalize and
promote them here later if it proves worthwhile.
