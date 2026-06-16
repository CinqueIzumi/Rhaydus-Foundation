# CLAUDE.md

Guidance for Claude working in **rhaydus-foundation** — a stand-alone, general-purpose library
(published `nl.rhaydus:*` artifacts, Gradle convention plugins, a ktlint ruleset, conventions docs, and
the `rhaydus-kotlin` Claude Code plugin) consumed by Kotlin Multiplatform apps.

## Start here

**Read [`docs/CAPABILITIES.md`](docs/CAPABILITIES.md) first** — the index of every module, component, and
public API, so you reuse what exists. The canonical conventions live in [`docs/`](docs/): `architecture`,
`toad-architecture`, `code-style`, `design-system-foundations`. Read the one that governs what you touch.

## Maintenance rules (enforced in review)

- A change that **adds, removes, or renames a published module, component, or public API updates
  `docs/CAPABILITIES.md` in the same change** — plus the doc that governs it (e.g. `design-system-foundations.md`
  for a design-system change). Code, docs, and the capabilities index must not drift.
- A change to the `rhaydus-kotlin` plugin (its agents, skill, or hook) updates the plugin `README.md` and
  this file as needed.

## Versioning

`foundation.version` in `gradle.properties` is the single source for all seven published artifacts. The
Claude plugin's `plugin.json` version is **locked** to it by the `verifyPluginVersion` gate (wired into
`check`) — bump them together. A `v*` tag publishes to Maven Central via CI.

## Conventions

- **Stand-alone:** no references to specific consuming app names anywhere; the repo reads as a
  general-purpose library. Brand tokens and editorial *values* stay in each consuming app — the foundation
  ships only the neutral skeleton plus the opt-in editorial / image layers.
- **Docs are em-dash-free** (use `-`).
- Follow [`docs/code-style.md`](docs/code-style.md): one declaration per file, multi-arg one-per-line, and
  **comments are the exception** (default to none; only a non-obvious edge case, a workaround/gotcha, or a
  rationale not derivable from the code — public-API KDoc is welcome but must not narrate the implementation).

## Build & verify

```bash
./gradlew build                       # all targets (jvm/android/ios) + tests + gates (verifyPluginVersion, ktlint)
./gradlew :ktlint-rules:ktlintFormat  # auto-fix mechanizable style
./gradlew publishToMavenLocal         # smoke-test the real artifacts before a release
```

## Delegate

- After a substantial Kotlin change, run the **`code-reviewer`** agent before reporting the work done.
- **Never write unit tests inline — delegate to the `unit-test-writer` agent.**

## Commits

Single short headline, no body (match the git log).
