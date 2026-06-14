---
name: style-check
description: Auto-fix mechanizable style, then run the project's verification gates (ktlint, lint, tests, and any extra gates the project defines) and report.
---

Auto-fix the mechanizable style issues, then run the project's full set of checks — including any
gates `./gradlew check` omits — and report. This skill adapts to whatever gates the project actually
defines; not every project has every gate.

Steps:

1. Use the Bash tool to auto-fix mechanizable layout/lint issues. Run the formatters the project
   actually has — try `./gradlew ktlintFormat` (the shared `nl.rhaydus:ktlint-rules` ruleset) and, if
   the project defines it, `./gradlew lintFix`. Skip a task that does not exist rather than failing.
   This may modify files.
2. Run the verification gate: `./gradlew check`. This typically covers lint, unit tests, and
   `ktlintCheck`; in a multiplatform project it also compiles every target (iOS Kotlin/Native compiles
   only on macOS). If the project defines extra gates — e.g. `buildHealth` (dependency analysis),
   `styleCheck` (mechanical style script), `checkModuleGraph` — run those too. Discover them with
   `./gradlew tasks --group=verification` if unsure.
3. If a gate fails, surface the failing task(s) and the relevant output. Do not attempt non-mechanical
   fixes automatically — `check`/`buildHealth` failures are blocking and need a human decision.
4. Report which auto-fixes were applied (mention `git diff` is available to review them) and the gate
   result. Treat advisory findings (e.g. `styleCheck`) as advisory; treat `check`/`buildHealth`
   failures as blocking.

Do not create a commit.
