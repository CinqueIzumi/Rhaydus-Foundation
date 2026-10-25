---
paths:
  - "**/*.kt"
---

# Kotlin style gates

`kotlin.code.style=official`. The mechanical rules are gated by tooling through `check`; the review-only rules
are in the app's code-style doc.

- **ktlint** (`nl.rhaydus:ktlint-rules`): `ktlintFormat` auto-fixes, `ktlintCheck` gates.
- **detekt** is type-resolved and gates from zero, with no baseline. Without type resolution, rules such as
  `rhaydus:UnguardedFlowTerminalRead` are silently inert.
- **Never add a per-file `@Suppress` to quiet a finding.** Either fix it, or change the config and write down
  the reason there.
- A new mechanical rule becomes a ktlint or detekt rule.
- detekt's `MatchingDeclarationName` fails a file whose one top-level type is named differently from the file,
  even when the rest of the file is functions; give such a type its own file.
- Follow `foundation/docs/code-style.md`.

## Project lines

<!-- The app's own style deltas: detekt config location, source sets covered, exemptions. -->
