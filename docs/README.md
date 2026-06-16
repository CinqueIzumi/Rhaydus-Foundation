# Canonical conventions

The single source of truth for the conventions shared across apps built on this foundation. These
replace the per-app doc copies that would otherwise drift. Each consuming app's `CLAUDE.md` links here
and keeps only app-specific deltas (brand tokens, platform set, app-only tech).

| Doc | Covers |
|---|---|
| [`CAPABILITIES.md`](CAPABILITIES.md) | The index of what the foundation provides: published modules + coordinates, the components/APIs each exposes, the dependency graph, tooling, and which doc governs what. The `rhaydus-*` agents read it first. |
| [`architecture.md`](architecture.md) | Clean Architecture layering (data/domain/presentation), the `core`/`feature` module-or-package structure, dependency direction, where each type belongs. |
| [`toad-architecture.md`](toad-architecture.md) | The TOAD presentation pattern (the `nl.rhaydus:toad` runtime), its five type parameters, the add-a-feature checklist, and conventions. |
| [`code-style.md`](code-style.md) | Naming, one-declaration-per-file, file layout, comments, Compose formatting, whitespace, visibility, test structure. Mechanizable rules are enforced by `nl.rhaydus:ktlint-rules`. |
| [`design-system-foundations.md`](design-system-foundations.md) | The brand-agnostic design skeleton: theme/typography plumbing, color roles, Material 3 Expressive setup, layout primitives, component + TOAD-to-Compose conventions. Brand tokens stay in each app's own design-system doc. |

**Maintenance rule:** a change to a foundation, component, or pattern updates the relevant doc in the
same change. Do not let the docs and the code (or the `nl.rhaydus:*` libraries) drift apart.
