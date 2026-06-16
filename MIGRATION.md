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
| 2 | Shared version catalog + convention plugins | **Foundation done; app adoption deferred** | `build-logic` (4 `rhaydus.*` plugins) compiles; `nl.rhaydus:catalog` publishes. Room/Apollo left app-specific |
| 3 | TOAD runtime library (`nl.rhaydus.toad`) | **Foundation done; app adoption deferred** | `:toad` KMP lib compiles (`Collector`); 345-file app import rewrites deferred |
| 4 | `designsystem-core` skeleton (no tokens) | **4a done; 4b in progress** | `:core-ui` + `:designsystem-core` compile (jvm + android + ios). Adaptive/desktop primitives + Tier 1 utilities synced from Softcover (Sessions 8-9). Editorial layer split into a new opt-in `:designsystem-editorial` module with the typography seam (Session 10). Remaining 4b components + app adoption deferred |
| 5 | Claude Code plugin (skills, agents, hooks, docs) | **Done (installable)** | `rhaydus-kotlin` plugin + `rhaydus` marketplace. Install is opt-in per project. Docs await Phase 6 |
| 6 | Docs consolidation | **Foundation done** | 4 canonical docs in `docs/`. App `CLAUDE.md` link-updates deferred (app adoption) |

**Recommended order:** 0 -> 1 -> 2 -> 3 -> (5 ∥ 6) -> 4.

### Resume here (next session)
Phases 0, 1, 2, 3 (foundation side) + 5 (plugin) + 6 (docs) + **4a (full designsystem skeleton)** done +
committed. **Every foundation-only phase is now complete.** All app adoption is paused by decision - do
not modify the Softcover/Nestbox builds yet. When resuming, options:
- **Phase 4b** (optional, foundation-only): neutral component primitives (button family, chip, top bar,
  image, dialog, list animators) + snapshot tests; pulls in Coil/Voyager. Decide if worth the version
  coupling.
- **Resume deferred app adoption** (the main remaining work): ktlint-rules (Phase 1), convention
  plugins/catalog (Phase 2), TOAD import rewrites (Phase 3), design-system adoption (Phase 4 - wrap
  `RhaydusTheme`, delete duplicated infra), CLAUDE.md doc links (Phase 6), plugin install (Phase 5).
  Softcover first (feature branch); Nestbox after `release/1.0.0`. Steps in each phase's detail.

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
  designsystem-core/             design-agnostic skeleton      (published)
  designsystem-editorial/        opt-in editorial language     (published; depends on designsystem-core)
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

**Done (foundation side):**
- [x] `build-logic` included build (`includeBuild("build-logic")` in pluginManagement) with 4
      conventions, renamed ids: `rhaydus.android.library`, `rhaydus.kmp.library`,
      `rhaydus.android.compose`, `rhaydus.kmp.compose` (+ `Catalog.kt`). Compiles green.
- [x] `:catalog` module publishes the shared catalog as `nl.rhaydus:catalog` (vanniktech
      `VersionCatalog`). Catalog expanded so it fully backs all 4 conventions (android-compose +
      coroutines-android/koin-android + backhandler aliases added).
- [x] `publishToMavenLocal` verified for `:catalog` + `:ktlint-rules` (signing auto-skipped locally).

**Decision:** Room/Apollo conventions were NOT moved — they are Softcover-only tech (GraphQL/DB). They
stay in Softcover's own `build-logic`; revisit only if a second app adopts the same tech. So when
Softcover adopts the foundation plugins it keeps its local Room/Apollo conventions alongside.

**Next (app adoption — deferred, modifies app repos):**
- [ ] Softcover: `includeBuild` the foundation; switch module build files from `softcover.*` ids to
      `rhaydus.*`; consume `nl.rhaydus:catalog`. Keep local Room/Apollo conventions.
- [ ] Nestbox: adopt the shared catalog (align Kotlin/Compose/Voyager/Koin versions, define the
      material3 expressive override once). Convention plugins only matter once it splits into modules.

### Phase 3 — TOAD runtime (the big one)

**Done (foundation side):**
- [x] `:toad` KMP library under `nl.rhaydus.toad` via `rhaydus.kmp.library` + vanniktech publishing
      (`nl.rhaydus:toad`). 8 primitives moved to `commonMain`, settled on `Collector`.
- [x] Dep: `api(libs.voyager.screenModel)` — `ScreenModel`/`screenModelScope` live in
      **voyager-screenmodel**, NOT voyager-navigator (the obvious guess fails to resolve). coroutines
      come from the convention.
- [x] Verified `:toad:compileKotlinJvm` green. All TOAD code is in `commonMain` with no expect/actual,
      so the JVM compile validates the code every target shares.
- [x] Repo plumbing this required (kept for later phases): root `build.gradle.kts` now declares the
      convention-applied plugins (`kotlin.multiplatform`, `android.kotlin.multiplatform.library`,
      `kotlin.compose`, `compose.multiplatform`) `apply false` so modules resolve them; `lint.xml`
      copied in (conventions reference it); gitignored `local.properties` with `sdk.dir`. Side effect:
      `:ktlint-rules` now applies `org.jetbrains.kotlin.jvm` WITHOUT a version (inherits from the root
      classpath; declaring a version triggers a classpath-conflict error).

**Next (app adoption — deferred):**
New `:toad` KMP library (`commonMain`, deps: Voyager + coroutines), under `nl.rhaydus.toad`, settle on
`Collector`. Scripted import rewrite: **Softcover 308 files**, **Nestbox 37 files** (+ rename
`Initializer`->`Collector` at Softcover's interface + its 11 screen-model uses; Nestbox keeps its name). Do the whole migration with
`foundation.local=true`, then publish + pin. Run ktlint import-ordering after (see Softcover's
`code-reviewer` memory `feedback_import_rewrite_ordering`). Consumers: Softcover 11 screen models,
Nestbox 4. Acceptance: both compile + screen-model tests pass; old packages gone.

### Phase 4 - designsystem-core skeleton (4a in progress)
Confirmed scope (see the approved plan): **4a skeleton only, two modules** (`:core-ui` non-visual +
`:designsystem-core` Compose). Component primitives (4b) and app adoption are deferred. Full findings +
the keystone parameterization seam design are in the approved plan; the canonical design skeleton is
`docs/design-system-foundations.md`.

**Done (4a, foundation side):**
- [x] `:core-ui` (`nl.rhaydus:core-ui`, package `nl.rhaydus.ui.common`): `AppDispatchers`, `TimeFormat`,
      `CurrentDate`, `HoursMinutesSeconds`, `NumberFormat` (expect + android/ios/jvm actuals). Visibility
      widened to public for reuse. `compileKotlinJvm` green.
- [x] `:designsystem-core` (`nl.rhaydus:designsystem-core`, package `nl.rhaydus.designsystem`):
      - `theme/RhaydusTheme` - the KEYSTONE parameterized scaffold: `RhaydusTheme(colorScheme,
        typography, motionScheme = expressive(), content)`. App supplies its own tokens + dynamic-color
        choice + its own custom-typography CompositionLocal inside `content`.
      - `theme/StandardPreview`, `layout/LocalBottomBarPadding` + `Spacers`,
        `modifier/ModifierExtensions` (pressScale/shimmer/grayscale/noRipple/conditional) + `ShakeOnError`,
        `motion/ReducedMotion` (expect + 3 actuals), `haptics/Haptics` (interface + `LocalHaptics` +
        `rememberHaptics` expect + 3 actuals), `util/` (`ClipboardReader` expect + 3 actuals,
        `ObserveAsEvents`, `SnackBarManager`, `SkeletonCrossfade`, `HtmlToAnnotatedString`),
        `icon/RhaydusIconResource` (mechanism generalized off the brand enum to wrap `DrawableResource`),
        `model/` enums (Button/IconToggle/Toggle/Split styles + sizes).
      - Added `androidx-lifecycle-runtime-compose` to the catalog (for `ObserveAsEvents`).
      - `compileKotlinJvm` green. No app domain dep; does NOT depend on `nl.rhaydus:toad`.

**4a is complete** (foundation side). The icon ASSETS, brand tokens, and domain components stay per app
(Layers 3-4 in the plan). Verified on jvm only (as with `:toad`); android/iOS targets compile at adoption.

**4b adaptive/desktop layer done (Session 8).** The window-size + desktop affordance layer Softcover grew
after the last sync (commit `8185e8a`) was ported in and generalized: `layout/WindowSizeClass` +
`WindowWidthClass` + `TwoPaneScaffold`, `component/AdaptiveModalSheet` (+ `model/ModalSheetForm`),
`component/DesktopTooltip` (expect + mobile/jvm actuals), `component/DesktopBackStrip` (generalized off the
brand icon enum to take a `RhaydusIconResource`), `modifier/DesktopKeyboard.dismissOnEscape` (jvmMain), and
`pointerHandCursor` + `hoverHighlight` merged into `modifier/ModifierExtensions`. This was the first code in
`designsystem-core` to need a **mobile (Android + iOS) shared source set**, so the `mobileMain`/`mobileTest`
seam was added to `rhaydus.kmp.library` (Phase 2 convention plugin). Verified `compileKotlinJvm` +
`compileAndroidMain` + `compileKotlinIosArm64` + the mobile/ios metadata all green.

**4b utility/animation/desktop primitives done (Session 9).** Brand-agnostic pieces that needed no
typography seam: `layout/ContentMaxWidth` (+ `editorialContentWidth`), `component/DesktopContextMenu`
(expect + mobile/jvm actuals), `component/StaggeredEntry`, `component/LazyItemMutationAnimator`. Each
closes a design-system-foundations gap (5.8 content width, 11 right-click menu, 7 staggered entry + list
mutation) that the doc described without shipping code.

**Editorial layer started as its own module (Session 10).** Gate (1) is cleared, but the editorial
typography was placed in a NEW opt-in **`designsystem-editorial`** module, not in core. Decision (raised by
review): editorial is a *design language*, not part of the neutral skeleton, so wiring it into the
design-agnostic `RhaydusTheme` would force the editorial vocabulary on every consuming app. Instead:
`designsystem-editorial` (`nl.rhaydus.designsystem.editorial`, depends `api` on `:designsystem-core`) holds
`EditorialTypography` (role vocabulary) + `buildEditorialTypography(typography)` neutral factory +
`EditorialTheme` provider + `MaterialTheme.editorialTypography`. `designsystem-core` / `RhaydusTheme` stay
design-agnostic and know nothing about editorial. An app opts in by nesting `EditorialTheme` inside
`RhaydusTheme`; an app that wants a different design depends on `:designsystem-core` alone. Documented in
design-system-foundations section 2.

**Deferred (4b, remaining):** the editorial/button component tier. The **editorial** components (section
header, hero stat, eyebrow, etc.) go into `:designsystem-editorial` (now unblocked by the typography seam).
The **button** suite is design-neutral enough to stay in `:designsystem-core` (its style enums + the
`RhaydusIconResource` wrapper already live there and it uses Material typography, not the editorial roles).
Remaining gate: the version-coupling decision for Coil (async image only).
**Deferred (app adoption):** each app wraps `RhaydusTheme`, deletes duplicated infra, points at the
shared modules. Brand tokens + domain components stay. Verified only on jvm (as with `:toad`); android/
iOS targets compile at adoption time.

### Phase 5 — Claude Code plugin
`claude/` plugin + `marketplace.json` bundling: `code-reviewer` + `unit-test-writer` agents, generic
skills (`style-check`, `update-readme`), generalised docs-first hook, and the canonical docs (phase
6). Parameterise project-shaped skills (`release`, version bumps differ: Softcover Android+iOS,
Nestbox Android-only). Fold durable agent-memory into agent defs/docs. Install into both repos.

**Done:**
- [x] Marketplace `rhaydus` at repo root `.claude-plugin/marketplace.json` -> plugin
      `./claude/plugins/rhaydus-kotlin`. Plugin manifest at the plugin's `.claude-plugin/plugin.json`;
      components at the plugin root (confirmed schema via claude-code-guide).
- [x] Bundled: `code-reviewer` + `unit-test-writer` agents (genericised the `CODE_STYLE_GUIDE.md`
      hardcode; unit-test-writer was already stack-generic). `style-check` skill **generalised** to run
      whatever gates a project defines (Softcover's `buildHealth`/`styleCheck`/`detekt`/iOS were
      hardcoded). docs-first `PreToolUse` hook (matcher `Agent`) generalised off Nestbox's, using
      `${CLAUDE_PLUGIN_ROOT}`.
- [x] Validated: all JSON parses, hook script executable + `bash -n` clean, agent frontmatter intact.
- **Install (opt-in, per project — not a repo change):**
  `/plugin marketplace add CinqueIzumi/rhaydus-foundation` then `/plugin install rhaydus-kotlin@rhaydus`.

**Deliberately excluded (kept app-local, hardcode per-app layout/platform):** `update-readme`,
`release`, `set-version-name`, `increment-version-code`. Agent-memory is per-project, not bundled.
**Pending:** add the canonical conventions docs to the plugin once Phase 6 consolidates them.

### Phase 6 - Docs consolidation
Merge each duplicated pair into one canonical doc under `docs/` (`architecture`, `code-style`,
`toad-architecture`, `design-system-foundations` + Softcover's module-structure guidelines). Each
app `CLAUDE.md` links out and keeps only app-specific deltas. Honor no-emdashes + commit-style
conventions in the merged docs.

**Done (foundation side):** 4 canonical docs in `docs/` (+ `docs/README.md` index), merged from both
apps, app-agnostic, emdash-free (verified `grep -c "emdash"` = 0):
- [x] `architecture.md` - layering + module/package structure (handles multi-module Softcover vs
      single-module Nestbox via the "module or its mirror package" framing); references `rhaydus.*` plugins.
- [x] `toad-architecture.md` - canonical TOAD doc pointing at the `nl.rhaydus:toad` runtime, `Collector`.
- [x] `code-style.md` - merged style guide; mechanizable rules enforced by `nl.rhaydus:ktlint-rules`.
- [x] `design-system-foundations.md` - the brand-agnostic skeleton ONLY; brand tokens stay per app.
- Plugin README points at `docs/` (not duplicated into the plugin; canonical home is here).
- Synthesis of the 3 big docs was delegated to parallel agents reading both apps' versions; toad doc
  written directly. Note: the Nestbox docs-first hook (active this session) blocked one agent once -
  re-running cleared it.
- **Deferred (app adoption):** each app's `CLAUDE.md` links to these + deletes its old per-app doc
  copies. That edits the app repos, so it waits.

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
- **Session 3:** Phase 2 foundation side — created `build-logic` (4 `rhaydus.*` conventions, verbatim
  classes) + `:catalog` module publishing `nl.rhaydus:catalog`. Expanded the catalog to back all 4
  conventions. Verified build-logic compiles + `publishToMavenLocal` for catalog + ktlint-rules.
  Decided Room/Apollo stay Softcover-only. App adoption still deferred.
- **Session 4:** Phase 3 foundation side — `:toad` KMP library (`nl.rhaydus.toad`, `Collector`), 8
  primitives in commonMain, vanniktech publishing. Verified `:toad:compileKotlinJvm`. Learnings:
  ScreenModel is in voyager-screenmodel (not -navigator); root needs convention plugins `apply false`;
  ktlint-rules now applies kotlin.jvm without a version. Added lint.xml + gitignored local.properties.
- **Session 5:** Phase 5 — `rhaydus-kotlin` Claude Code plugin + `rhaydus` marketplace. Bundled the 2
  agents + generalised `style-check` skill + generalised docs-first hook; excluded project-shaped
  skills. Confirmed plugin/marketplace schema via claude-code-guide; validated JSON + hook. Foundation
  only (install is opt-in per project).
- **Session 6:** Phase 6 - 4 canonical docs in `docs/` (architecture, toad-architecture, code-style,
  design-system-foundations) + index. Synthesized the 3 big ones via parallel agents reading both
  apps' versions; wrote toad doc directly. Verified app-agnostic + emdash-free. Plugin README points
  at docs. App CLAUDE.md link-updates deferred.
- **Session 7:** Phase 4 planned (plan mode: full extract/not findings + the parameterization-seam
  recommendation, approved) then 4a-core implemented - `:core-ui` (dispatchers + date/time/number
  seams) and `:designsystem-core` (RhaydusTheme scaffold, layout primitives, modifiers + ReducedMotion
  seam, model enums, StandardPreview). Both `compileKotlinJvm` green. Then finished 4a: added the
  Haptics + ClipboardReader seams (expect + 3 actuals each), ShakeOnError, ObserveAsEvents (+ lifecycle
  dep), SnackBarManager, SkeletonCrossfade, HtmlToAnnotatedString, the generalized RhaydusIconResource
  mechanism, and the Toggle/Split enums. 4a complete. 4b + app adoption deferred.
- **Session 8:** Sync of Softcover's post-`8185e8a` reusable additions (the last sync was that commit, so
  everything after it was checked). The payload was the adaptive/desktop layer; ported into
  `:designsystem-core` and generalized to `nl.rhaydus.designsystem.*` (no brand tokens): WindowSizeClass +
  WindowWidthClass + TwoPaneScaffold (`layout/`), AdaptiveModalSheet + ModalSheetForm, DesktopTooltip
  (expect/mobile/jvm), DesktopBackStrip (parameterized on `RhaydusIconResource` instead of the brand icon
  enum), dismissOnEscape (jvmMain), and `pointerHandCursor` + `hoverHighlight` merged into
  ModifierExtensions. Added the `mobileMain`/`mobileTest` seam to `rhaydus.kmp.library` (first module here
  needing an Android+iOS-only set, for DesktopTooltip's pass-through actual). Docs: added breakpoints (5.7),
  two-pane (5.8), adaptive modal (5.9), and a desktop-affordances section (11) to
  `design-system-foundations.md`, plus the Voyager per-item nested-navigator `key` pitfall to
  `architecture.md` section 5. Verified designsystem-core compiles on jvm + android + ios + mobile metadata.
  WindowSizeClass round-trip test ported to `androidHostTest`. Checked Nestbox too: nothing net-new (its
  docs-first hook is already in the plugin; its error-handling model is already in `architecture.md`).
  Foundation-only; app adoption still deferred.
- **Session 9:** Tier 1 of the broader 4b component extraction - the brand-agnostic utility/animation/
  desktop primitives that need no typography seam: `ContentMaxWidth` + `editorialContentWidth` (layout),
  `DesktopContextMenu` (expect + mobile/jvm), `StaggeredEntry`, `LazyItemMutationAnimator`. Generalized to
  `nl.rhaydus.designsystem.*`, brand-doc/roadmap cross-refs dropped, narration comments removed per the
  sharpened comment rule. Documented each in design-system-foundations (5.8, 7, 11). Verified jvm + android
  + ios compile and ktlintCheck green. Editorial/button tier deferred behind the typography-seam decision
  (see Phase 4 detail).
- **Session 10:** Typography seam - the gate for the editorial component tier. First built in core
  (`RhaydusTheme` gained an `editorialTypography` param), then **reworked after review into a new opt-in
  `designsystem-editorial` module** so the design-agnostic core is not forced to carry an editorial design
  choice. The module (`nl.rhaydus.designsystem.editorial`, `api`-depends on `:designsystem-core`) holds
  `EditorialTypography` (focused 9-role vocabulary, not Softcover's full 15-role brand scale), the neutral
  `buildEditorialTypography(typography)` factory, the `EditorialTheme` provider, and the
  `MaterialTheme.editorialTypography` extension. `RhaydusTheme` reverted to design-agnostic. Added
  `include(":designsystem-editorial")`. Documented in design-system-foundations section 2. jvm + android +
  ios compile + ktlintCheck green.
