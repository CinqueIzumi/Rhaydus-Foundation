# rhaydus-foundation — extraction progress

Single source of truth for the effort to extract shared code, tooling, and Claude Code assets
out of **Nestbox** and **Softcover** into this reusable foundation. Update this file at the end of
every working session so progress is never lost. The status table near the top is the quick read;
the detail sections below explain the why.

- **Owner / GitHub:** `CinqueIzumi`
- **Source projects:** `../Nestbox` (Android-only), `../Softcover` (KMP: Android + iOS + desktop)
- **This repo lives at:** `../rhaydus-foundation` (sibling of the two apps)

## Status at a glance

| Phase | Scope | Status | Notes |
|---|---|---|---|
| 0 | Bootstrap repo + publish plumbing + CI | **Done** | Verified `./gradlew help` |
| 1 | `ktlint-rules` published, adopted by both apps | **Foundation done; app adoption deferred** | Module moved + builds + self-lints green. App adoption paused by decision (see Resume) |
| 2 | Shared version catalog + convention plugins | Not started | Renames plugin ids `softcover.*` -> `rhaydus.*` |
| 3 | TOAD runtime library (`nl.rhaydus.toad`) | Not started | 345 files touched across both apps |
| 4 | `designsystem-core` skeleton (no tokens) | Not started | Optional / can defer |
| 5 | Claude Code plugin (skills, agents, hooks, docs) | Not started | Can run parallel to 2-4 |
| 6 | Docs consolidation | Not started | Feeds phase 5 |

**Recommended order:** 0 -> 1 -> 2 -> 3 -> (5 ∥ 6) -> 4.

### Resume here (next session)
Phase 0 done; Phase 1 foundation done (committed). **App adoption of ktlint-rules is paused by
decision** — do not modify the Softcover/Nestbox builds yet. When resuming, options:
- **Phase 2** (foundation-only, safe): create `build-logic` convention plugins (`rhaydus.*`) + publish
  the version catalog. Does not touch the apps. Good next step.
- **Resume Phase 1 app adoption** when ready: Softcover first (clean swap, feature branch); Nestbox
  only after `release/1.0.0` ships (format churn — branch first). Snippet + steps in the Phase 1
  detail below.

## Why this exists (the findings that justify it)

- **TOAD runtime is byte-for-byte identical** between the two apps (8 primitives), differing only by
  package path and one rename: Nestbox `Collector` vs Softcover `Initializer`. That rename is already
  silent drift with no shared source. **Resolution: keep `Collector`** (so Softcover renames
  `Initializer` -> `Collector`; Nestbox keeps its name).
- **Custom ktlint rules exist only in Softcover** (10 rules in `nl.rhaydus.ktlint`). Nestbox has no
  ktlint, no detekt, no `.editorconfig` — the style rules the docs describe are unenforced there.
- **Convention plugins exist only in Softcover** (`build-logic`, 6 plugins). Nestbox is a single
  `:app` module today but its package layout already mirrors `core/feature`.
- **Claude assets are lopsided.** Softcover has project skills + `code-reviewer` / `unit-test-writer`
  agents with rich agent-memory; Nestbox has only a docs-first hook.
- **Shared third-party stack** (Voyager, Koin, Ktor, kotlinx.serialization, DataStore, Compose
  Material3 with the same expressive-alpha override) drifts in versions between the two catalogs.

## Target end state

```
rhaydus-foundation/
  gradle/libs.versions.toml      shared version catalog        (published as a catalog artifact)
  build-logic/                   convention plugins            (includeBuild + optionally published)
  ktlint-rules/                  custom lint ruleset           (published jar)
  toad/                          KMP runtime, nl.rhaydus.toad  (published)
  designsystem-core/             theme/typography skeleton     (published)
  docs/                          canonical conventions
  claude/                        Claude Code plugin + marketplace.json
```

### Consumption model: hybrid (Maven + includeBuild)

- **Registry:** the repo is **public/open-source**, published to **Maven Central via the Central
  Portal** (group `nl.rhaydus`, verified by a DNS TXT record on rhaydus.nl — no server needed).
  Chosen over GitHub Packages because GH Packages' Maven registry requires a read token even for
  public repos; Central allows anonymous consumption. Chosen over JitPack for clean KMP module
  metadata and canonical coordinates. Publishing uses the `com.vanniktech.maven.publish` plugin.
- **Default:** each app depends on published coordinates (e.g. `nl.rhaydus:toad:<v>`) resolved from
  `mavenCentral()` — no credentials. `mavenLocal()` works as a fast local fallback for smoke tests.
- **Inner loop:** set `foundation.local=true` in an app's `local.properties`. Its
  `settings.gradle.kts` then `includeBuild("../rhaydus-foundation")`, and Gradle's composite-build
  dependency substitution transparently swaps the published coordinates for local source — no
  version bumps, instant cross-repo edits. Flip off to return to the pinned release.

### Namespace / id decisions (locked)

| From | To |
|---|---|
| `nl.rhaydus.nestbox.core.presentation.toad` | `nl.rhaydus.toad` |
| `nl.rhaydus.softcover.core.designsystem.presentation.toad` | `nl.rhaydus.toad` |
| `Collector` (Nestbox) / `Initializer` (Softcover) | `Collector` |
| convention plugin ids `softcover.*` | `rhaydus.*` |
| `nl.rhaydus.ktlint` (ktlint rules) | unchanged |
| group coordinate | `nl.rhaydus` |

## Open decisions / risks to revisit

- **Version baseline drift.** Foundation is seeded from Softcover's catalog (the more advanced
  project): Kotlin `2.3.21`, AGP `9.0.0`, Compose MP `1.11.0`. Nestbox currently runs Kotlin
  `2.2.10` / AGP `9.1.1`. Nestbox will likely need a Kotlin bump to consume published KMP artifacts.
  Confirm compatibility during phase 1/3 adoption.
- **Publishing auth (maintainers only).** Consuming needs nothing. Publishing to Maven Central needs a
  Central Portal user token (`ORG_GRADLE_PROJECT_mavenCentralUsername`/`...Password`) + a PGP signing
  key (`ORG_GRADLE_PROJECT_signingInMemoryKey*`), via env/`~/.gradle/gradle.properties`, never
  committed. CI reads them from repo secrets (see `.github/workflows/publish.yml`).
- **Catalog scope.** Foundation catalog carries only what foundation modules need. App-only libs
  (Apollo, Room, Coil, CameraX, MLKit, markdown, etc.) stay in each app's own catalog.
- **Design system:** share only the brand-agnostic skeleton; Nestbox "reading room" and Softcover
  book tokens stay local. Be conservative — when in doubt, leave it in the app.

## Per-phase detail

### Phase 0 — Bootstrap (in progress)
Repo skeleton + publish plumbing every later phase reuses.
- [x] `git init`, Gradle wrapper (9.1.0), `.gitignore`
- [x] `settings.gradle.kts`, root `build.gradle.kts` (publishing convention via `maven-publish`)
- [x] `gradle.properties` (`foundation.version`), seeded `gradle/libs.versions.toml`
- [x] GitHub Actions publish-on-tag workflow
- [x] **Verify:** `./gradlew help` -> BUILD SUCCESSFUL on Gradle 9.1.0; publishing wiring compiles
- **Acceptance:** met. Configuration resolves; publishing wiring compiles. Real `publishToMavenLocal`
  is first exercised by the ktlint-rules module in phase 1.
- **Remaining before a real release (not needed to build/test or to develop via includeBuild):**
  create a Central Portal account, register + DNS-verify the `nl.rhaydus` namespace, generate a PGP
  signing key, and add the Central + signing values as GitHub repo secrets. Push this repo public to
  `github.com/CinqueIzumi/rhaydus-foundation`. Phases 1-4 can be developed and validated entirely via
  the `foundation.local=true` includeBuild loop before the first publish.

### Phase 1 — ktlint-rules (quick win, do first)
Move Softcover `:ktlint-rules` verbatim (package already neutral) -> publish -> Softcover repoints at
published ruleset -> Nestbox adopts ktlint for the first time (+ `.editorconfig`, expect one-time
format churn). Acceptance: both apps `ktlintCheck` green.

**Mechanism note (discovered during the move):** this is NOT a standard ktlint RuleSetProvider jar.
By deliberate design (full control of the ktlint version, no Spotless/ktlint-gradle coupling) it is a
plain `kotlin("jvm")` library whose `Main.kt` drives ktlint's rule-engine directly, exposed as
`ktlintCheck`/`ktlintFormat` JavaExec tasks. So consumption is: each app declares a `ktlintRules`
configuration depending on `nl.rhaydus:ktlint-rules:<v>`, and registers its own JavaExec tasks
(classpath = that configuration, mainClass `nl.rhaydus.ktlint.MainKt`, scanning the app's rootDir),
wiring `check` to depend on `ktlintCheck`. With `foundation.local=true` includeBuild, that dependency
substitutes to the local project automatically. This ~15-line snippet is duplicated per app for now
and graduates into a `rhaydus.ktlint` convention plugin in phase 2.

**Done so far (foundation side):**
- [x] Moved the 10 sources verbatim to `ktlint-rules/` (package `nl.rhaydus.ktlint` unchanged)
- [x] `build.gradle.kts`: kotlin-jvm + vanniktech publishing (coordinates `nl.rhaydus:ktlint-rules`,
      Central Portal, signing, POM) + the self-lint JavaExec tasks
- [x] `include(":ktlint-rules")`; `:ktlint-rules:build` green; `:ktlint-rules:ktlintCheck` green
      (foundation lints its own sources)

**Next (app adoption — modifies the app repos, so on feature branches):**
- [ ] Softcover: add the `foundation.local` switch + includeBuild; replace the local `:ktlint-rules`
      module with the `ktlintRules` configuration + JavaExec snippet against `nl.rhaydus:ktlint-rules`;
      delete `Softcover/ktlint-rules/`. Verify `ktlintCheck` green.
- [ ] Nestbox (on a release branch — branch first): add the switch + snippet + `.editorconfig`; run
      `ktlintFormat` once (expect format churn commit); verify `ktlintCheck` green.

### Phase 2 — Catalog + convention plugins
Publish the catalog as a `version-catalog` artifact (`from(...)` in each app). Move `build-logic`,
rename ids `softcover.*` -> `rhaydus.*`. Softcover updates ids; Nestbox adopts the shared catalog to
stop version drift (define the `material3` expressive override once).

### Phase 3 — TOAD runtime (the big one)
New `:toad` KMP library (`commonMain`, deps: Voyager + coroutines), under `nl.rhaydus.toad`, settle on
`Collector`. Scripted import rewrite: **Softcover 308 files**, **Nestbox 37 files** (+ rename
`Initializer`->`Collector` at Softcover's interface + its 11 screen-model uses; Nestbox keeps its name). Do the whole migration with
`foundation.local=true`, then publish + pin. Run ktlint import-ordering after (see Softcover's
`code-reviewer` memory `feedback_import_rewrite_ordering`). Consumers: Softcover 11 screen models,
Nestbox 4. Acceptance: both compile + screen-model tests pass; old packages gone.

### Phase 4 — designsystem-core skeleton (optional / last)
Extract brand-agnostic primitives only: the `MaterialTheme.readerTypography` extension pattern,
typography-scale plumbing, layout primitives (Spacers, bottom-bar padding locals), expressive-M3
setup, TOAD<->Compose glue. Tokens stay per-app.

### Phase 5 — Claude Code plugin
`claude/` plugin + `marketplace.json` bundling: `code-reviewer` + `unit-test-writer` agents, generic
skills (`style-check`, `update-readme`), generalised docs-first hook, and the canonical docs (phase
6). Parameterise project-shaped skills (`release`, version bumps differ: Softcover Android+iOS,
Nestbox Android-only). Fold durable agent-memory into agent defs/docs. Install into both repos.

### Phase 6 — Docs consolidation
Merge each duplicated pair into one canonical doc under `docs/` (`architecture`, `code-style`,
`toad-architecture`, `design-system-foundations` + Softcover's module-structure guidelines). Each
app `CLAUDE.md` links out and keeps only app-specific deltas. Honor no-emdashes + commit-style
conventions in the merged docs.

## Verification (run after Phase 0 file changes)

```
cd ../rhaydus-foundation && ./gradlew help
```
Expect a clean BUILD SUCCESSFUL with no subprojects yet. First real publish is phase 1.

## Session log

- **Session 1:** Analysis of both repos + this plan. Phase 0 scaffolding created (repo skeleton,
  wrapper, settings/root build, seeded catalog, CI workflow, docs). Decisions during the session:
  TOAD name resolved to **`Collector`** (Softcover renames Initializer -> Collector); repo will be
  **public**, published to **Maven Central / Central Portal** (not GitHub Packages), group
  **`nl.rhaydus`** via DNS TXT. Phase 0 re-wired off GitHub Packages onto Maven Central accordingly.
- **Session 2:** Phase 1 foundation side — moved `ktlint-rules` in (verbatim), wired vanniktech
  publishing, verified `:ktlint-rules:build` + `:ktlint-rules:ktlintCheck` green. Discovered it's a
  self-driving JavaExec tool (not a RuleSetProvider jar) and recorded the per-app consumption snippet.
  App adoption paused by decision. Committed: bootstrap + ktlint-rules module.
