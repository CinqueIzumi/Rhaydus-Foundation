# Foundation capabilities

The single index of **what the `nl.rhaydus` foundation makes available** to a consuming project: the
published modules, the components/APIs each exposes, the dependency graph, the tooling, and which
canonical doc governs what. The `rhaydus-*` agents read this **first**, every time, so they reach for
what already exists instead of reinventing it, and so their knowledge tracks the version a project is on
rather than a memorized snapshot.

> **Reuse-first rule.** Before hand-rolling a component, modifier, util, theme seam, or build wiring,
> check this index for an existing foundation equivalent. Reinventing something listed here is a defect,
> not a style nit.

> **Maintenance rule.** Adding, removing, or renaming a published module / component / public API updates
> this file in the same change (the `reviewer` enforces it, alongside the per-doc maintenance rules).

## How a project consumes it

- **Default:** a git submodule at `foundation/`, pinned on a `v*` tag, built through an unconditional
  `includeBuild("foundation")`. The `nl.rhaydus:*` catalog entries are version-less substitution
  coordinates. The app builds on the same AGP and Kotlin versions as the pinned foundation. Artifacts are
  also published to Maven Central.
- **Editing:** change `foundation/` on a branch; the app builds the edit directly, with no version bump.
- **Catalog and convention plugins:** `from(files("foundation/gradle/libs.versions.toml"))` for the shared
  catalog, `includeBuild("foundation/build-logic")` in `pluginManagement` for the `rhaydus.*` plugins.
  App-only libraries stay in the app's own catalog.
- **Plugin and docs:** the `rhaydus-kotlin` plugin loads from the project's `.claude/settings.json` through a
  local-directory marketplace at `./foundation`; docs are read from `foundation/docs/` at the pinned commit.
  `rhaydus-adopt` wires all of it.

## Published modules

| Coordinate | Purpose | Key public surface | Governing doc |
|---|---|---|---|
| `nl.rhaydus:toad` | TOAD presentation runtime (KMP, `commonMain`) | `ToadScreenModel`, `UiState`, `UiAction`, `UiEvent`, `Collector`, `ActionDependencies`, `ActionScope`, `LocalVariables`, `@TransientNavArg` (`nl.rhaydus.toad.navigation`: a Voyager `Screen` argument Android's back-stack serialization skips) | `toad-architecture.md` |
| `nl.rhaydus:core-common` | Non-visual shared primitives | `AppDispatchers`, `AppLog`, `runCatchingCancellable`, `runCatchingLogged`, `TimeFormat`, `CurrentDate`, `HoursMinutesSeconds`, `NumberFormat` | `architecture.md` |
| `nl.rhaydus:core-platform` | Platform-capability seams (depends on core-common) | `SecureStorage` (Android/iOS/JVM), `NetworkAvailabilityProvider` + `NetworkAvailability` (Android/iOS/JVM); ships a manifest declaring `ACCESS_NETWORK_STATE` | `architecture.md` |
| `nl.rhaydus:offline-sync` | Offline optimistic-write queue skeleton (depends on core-platform) | `WriteQueue` / `PendingWriteStore` (app-implemented persistence seam), `PendingWrite`, `OfflineWriteDrainer` + `DefaultOfflineWriteDrainer`, `ReplayOutcome`, `DrainPolicy` | `architecture.md` |
| `nl.rhaydus:designsystem-core` | Design-agnostic Compose skeleton (no brand tokens) | see **designsystem-core surface** below | `design-system-foundations.md`, `code-style.md` |
| `nl.rhaydus:designsystem-editorial` | Opt-in editorial design language (depends on core) | `EditorialTypography`, `buildEditorialTypography`, `EditorialTheme`, `MaterialTheme.editorialTypography`; components `EditorialSectionHeader`, `HeroStatNumberField` + `EditorialSuffix`, `PullToRefreshEyebrow`, `DropCapText`, `EditorialSearchField` | `design-system-foundations.md` §2 |
| `nl.rhaydus:designsystem-image` | Opt-in async images on Coil (depends on core) | `RhaydusImage` (plain), `RhaydusPlaceholderImage` (placeholder slot), `RhaydusShimmerImage` (shimmer) | `design-system-foundations.md` §8 |
| `nl.rhaydus:catalog` | Shared version catalog | `libs.*` aliases for the shared third-party stack | - |
| `nl.rhaydus:ktlint-rules` | Custom ktlint formatter (10 auto-fixing rules) | `ktlintCheck` / `ktlintFormat` gates | `code-style.md` |
| `nl.rhaydus:detekt-rules` | Custom detekt 2 ruleset (`dev.detekt`, analysis-API crash-safety) + shared baseline config | `rhaydus` ruleset for call sites and bodies (see **detekt-rules** below), bundled `config/detekt.yml` | `code-style.md` |
| `nl.rhaydus:konsist-rules` | Konsist assertions for declaration and architecture rules, parameterised with project facts | see **konsist-rules** below; called from a `rhaydus.konsist` test module | `code-style.md`, `architecture.md` |

### designsystem-core surface

- **theme/** - `RhaydusTheme(colorScheme, typography, motionScheme, shapes)` (the Material 3 Expressive scaffold; the app supplies tokens; `shapes` defaults to Material's; resolves reduced motion to a snapping scheme and provides `LocalHaptics` and `Motion`), `StandardPreview`.
- **theme/ tokens** - `Spacing` (2dp hairline, 4dp grid to 48dp, `gutter`, `section`); `Corners` / `CornerRadius` (the corner-radius scale as shapes and as dp, plus `pill`); `Motion` read through `MaterialTheme.motion` (`valueChange`, `celebration()`, `ambientLoop()`; snaps under reduced motion).
- **layout/** - `rememberWindowSizeClass()` + `WindowSizeClass` / `WindowWidthClass` (the single breakpoint source: COMPACT < 600dp, MEDIUM 600-840dp, EXPANDED >= 840dp), `TwoPaneScaffold`, `ContentMaxWidth` + `Modifier.cappedContentWidth`, `BottomBarScaffold` + `BottomBarPlacement` (the bottom-bar host for both placements: `OVERLAY` measures a caller-supplied bar slot and provides `footprint + barSpacing`, `DOCKED` hosts it in a Material `Scaffold` and provides `barSpacing` alone), `LocalBottomBarPadding` + `rememberBottomBarPadding()` (the paired live read - always "the trailing padding scrolling content reserves", so screens never branch on the placement), `BottomNavigationSpacer`, `ExpandableFlowRow(itemCount, itemContent)` (a lazy wrapping row that composes only the items it places and collapses to N lines behind a "show more" affordance; `FlowRowExpansion.Progressive` reveals a few more lines per tap, `FlowRowExpansion.Full` reveals everything at once, and `collapsible` adds a "show less" back - both affordances are replaceable slots), spacers.
- **nav/** - `NavPulse` (a keyed, instance-owned cross-tab pulse signal; the app's keys implement `NavPulseKey`) + `rememberPulseScale(pulse, key)`, `rememberPulseScale(trigger: Int)` and `rememberPulseScaleProvider(trigger)` (the animated scale that plays on each pulse or trigger change, specs from the motion scheme, gated by `playDecorativeMotion()`).
- **modifier/** - `pressScale` (motion-scheme fast spatial spec, no decorative-motion gate), `pressScaleClickable` / `pressScaleCombinedClickable` / `pressScaleSelectable` / `pressScaleToggleable` (take `role`, `enabled`, `hoverShape`, optional shared `interactionSource`; bundle press scale, `hoverHighlight` and the hand cursor; selectable and toggleable play the select haptic), `noRippleClickable`, `pointerHandCursor`, `hoverHighlight`, `platformModifierClick` (commonMain expect; desktop Ctrl/Cmd-toggle + Shift-range modifier-click selection, no-op on touch), `shimmer`, `skeletonMask` / `clippedSkeletonMask` (skeleton shimmer over loading content: grace delay, minimum-visible hold, crossfade to content; `SkeletonRevealGroup` skips the grace for masks that start after one has shown content), `grayscale`, `conditional`, `shakeOnError` (takes a Boolean or a `ShakeTrigger` from `rememberShakeTrigger()`; motion-scheme timing, `reject` haptic still fires under reduced motion); jvm-only `dismissOnEscape` (takes focus) and `onEscape` (key handler only, safe to gate on a text-field-driven condition).
- **component/** - `AdaptiveModalSheet` (bottom sheet, or an animated scale-and-fade panel on expanded widths with a `panelShape` param; sends `onDismissRequest` once after the exit; swipe follows `dismissOnTapOutside`; `overlay` slot composed inside the sheet window; + `LocalModalSheetForm` / `LocalModalSheetDismiss` / `LocalModalSheetClosing`), `DesktopTooltip`, `DesktopContextMenu` (+ `DesktopContextMenuItem`), jvm-only `DesktopBackStrip`, jvm-only `DesktopVerticalScrollbar` (themed vertical scrollbar with `LazyGridState` / `LazyListState` / `ScrollState` overloads; thumb keyed to `onSurface`), `StaggeredEntry` (`rememberStaggeredEntryCoordinator` + `Modifier.staggeredEntry`), `LazyItemMutationAnimator` (`rememberLazyItemMutationAnimator` + `Modifier.mutationAnimated`), `StarRatingInput` (interactive half-star rating with tap + drag-scrub and haptics; the star glyph is a `RhaydusIconResource`, the fill/empty tints are params), `RhaydusDatePickerDialog` (single-day calendar dialog taking and reporting a `LocalDate`; keeps the Material UTC-millis contract internal), `InlineErrorState` (in-content load/submit-failure message in the `error` role + a `RhaydusButton` retry; the render side of the TOAD error-slot convention, `toad-architecture.md`), the **button family** `RhaydusButton` (a `label` string or a `content` slot) / `RhaydusToggleButton` / `RhaydusIconToggleButton` / `RhaydusSplitButton`.
- **model/** - `ButtonSize`, `ButtonStyle`, `ToggleButtonStyle`, `IconToggleButtonStyle`, `IconToggleButtonShape`, `SplitButtonStyle`, `ModalSheetForm`, `RhaydusMenuItem`.
- **motion/** - `playDecorativeMotion()` (reduced-motion gate; every animation routes through it).
- **haptics/** - `Haptics`, `rememberHaptics()`, `LocalHaptics` (provided by `RhaydusTheme`).
- **util/** - `ObserveAsEvents`, `SnackBarManager`, `SkeletonCrossfade`, `htmlToAnnotatedString`, `ClipboardReader`.
- **icon/** - `RhaydusIconResource` (the brand-agnostic icon wrapper; the app supplies its own icon catalog).
- **share/** - `ShareCardCapture` (`rememberShareCardCapture(config)`) captures a composable to a PNG and saves/shares it (Android MediaStore + share `Intent`, desktop file dialog + clipboard, iOS share sheet; `SaveOutcome.Cancelled` and `ShareOutcome.Cancelled` / `CopiedToClipboard` report a dismissed dialog and a desktop clipboard copy); `CapturableShareCard` is the capture host taking the app's card as a `@Composable` slot; `ShareCardCaptureConfig` supplies the file-name prefix / gallery album / Android `FileProvider` authority; `SaveOutcome` / `ShareOutcome` are the result types; `GalleryWritePermissionRequester` gates the Android API ≤ 28 write permission. Self-contained (no Koin/`AppDispatchers`); the Android target pulls `androidx.core` + `activity-compose`.

## Dependency graph

```
designsystem-editorial ──► designsystem-core
designsystem-image     ──► designsystem-core   (+ Coil: coil-compose; the network fetcher is an app choice)
offline-sync           ──► core-platform ──► core-common
core-platform          ──► core-common
core-common, toad      (standalone)
```

Adding a foundation dependency: declare the coordinate (or its catalog alias) in the module's
`build.gradle.kts`. The `includeBuild("foundation")` substitutes it to the submodule source automatically. The
editorial and image modules are **opt-in**: a project that wants neither the editorial look nor remote
images depends only on `designsystem-core`.

## Tooling

- **Convention plugins** (`build-logic`, applied by id): `rhaydus.android.library`, `rhaydus.kmp.library`, `rhaydus.android.compose`, `rhaydus.kmp.compose`; `rhaydus.konsist` (a plain JVM test module on JUnit 5 with Konsist for architecture tests, reached by the root `check`). The KMP library convention also declares the `mobileMain` / `mobileTest` (Android+iOS shared, desktop branches to `jvmMain`) seam and the shared Android lint policy (`warningsAsErrors` + the root `lint.xml`, with version-freshness checks held `informational`), applied to the `androidMain` and JVM sources of every KMP library.
- **Build gates** (`build-logic` + root): `rhaydus.module-graph` (a root-applied convention plugin - the `checkModuleGraph` task fails on any `project(...)` edge breaking the tier DAG configured via `moduleGraph { }`, plus an `api`-visibility allowlist, per-module `allowedProjects` allowlists, `bannedEdges`, and `bannedExternalGroups` with `ignoredExternalCoordinates`; edges are snapshotted at configuration time); `rhaydus.doc-budgets` (a root-applied convention plugin - the `checkDocBudgets` task holds docs to the budgets and citation rules of `doc-hygiene.md`, reading the `docGuard` object of `.claude/rhaydus.json` that the plugin's doc-guard hook also reads); `rhaydus.dependency-analysis` (a root-applied convention plugin - applies `com.autonomousapps.dependency-analysis` to every module and carries the shared policy: fail on unused deps / wrong api-vs-implementation, with the convention bundle excluded; each module's `projectHealth` runs in its `check`, aggregated by `buildHealth`). `rhaydus.style` (a root-applied convention plugin - applies `dev.detekt` to every module over the baseline shipped in the `detekt-rules` jar, creates the type-resolved detekt tasks with a vacuous-pass guard, registers `ktlintCheck` / `ktlintFormat` over the `ktlint-rules` jar plus `styleCheck`, and puts both into every `check`; the build supplies its rule jars, extra config files and rule projects through `rhaydusStyle { }`). `rhaydus.resource-packaging` (a root-applied convention plugin - the `checkResourcePackaging` task fails a module that declares Compose resources but leaves `androidResources.enable` off). `rhaydus.android.r8-keep-contract` (applied to the application, configured via `r8KeepContract { }` - `checkR8KeepContract` fails when R8 renames or drops a class reached by name; the `keepRules/` consumer-rule convention is in `architecture.md`). The Compose conventions (`rhaydus.kmp.compose`, `rhaydus.android.compose`) apply the Compose stability gate (`stabilityCheck` against checked-in baselines). These gates wire into every module's `check`.
- **ktlint-rules** (`nl.rhaydus:ktlint-rules`): formats only; every rule auto-fixes. Multi-arg one-per-line wrapping (`Modifier` calls included, nested calls wrap the outer call), trailing-lambda braces on their own lines over a multi-line body, trailing commas, statement paragraphs (multi-line statements, guard clauses), brace blank lines, region flushing, sibling-composable spacing, boolean `.not()`, unused imports, and the import layout from `ij_kotlin_imports_layout`. `ktlintFormat` fixes, `ktlintCheck` gates.
- **detekt-rules** (`nl.rhaydus:detekt-rules`, ruleset `rhaydus`): `UnguardedFlowTerminalRead`, `NoReflection`, `FileLevelDpProperty`, `RawSpacingDp`, `RawCornerDp`, `UiModelBuiltInComposable`, `InlineMockkStub`, `UseCaseRunCatching`, `NoRawLogging`, `InlineFullyQualifiedReference`, `NestedCommentInKDoc`, `TestMissingArrangeActAssert`; the baseline enables `ForbiddenSuppress` for the contract rules. Scoping comes from config.
- **konsist-rules** (`nl.rhaydus:konsist-rules`): `KoScope` assertions `assertNoForbiddenImports` (per-file allowlists, wildcard and fully-qualified matches), `assertLowerLayersDoNotImportUpperLayer`, `assertOneTypePerFile`, `assertPlumbingIsInternal`, `assertTestsAreNested`, `assertNoModelFieldNamed`, `assertNoUiModelBuiltInStateMember` (a `*UiState` getter or function that builds a model; exempt annotations are a parameter), `assertScreenArgumentsSurviveSerialization`. Each takes `excludedPaths` globs over the project-relative path and excludes the `foundation/` checkout by default.
- **Claude plugin** (`rhaydus-kotlin`): the `rhaydus-adopt` agent, the generic `implementer` / `reviewer` / `test-writer` agents, the `style-check` and `handoff` skills, the quiet-Gradle and token-usage scripts, and the hooks (docs-first, brief check, slow-Gradle blocker, quiet Gradle, large-read guard, doc guard, context budget) configured by `.claude/rhaydus.json`.

## Which doc governs what

- `architecture.md` - Clean Architecture layering, the `core`/`feature`/orchestration tiers, navigation, dispatchers, build setup.
- `toad-architecture.md` - the TOAD pattern: the five type parameters, data flow, the add-a-feature checklist.
- `code-style.md` - naming, one-declaration-per-file, layout, comments, Compose formatting, visibility, test structure. Mechanizable rules are enforced by `ktlint-rules`.
- `working-docs.md` - the session-loop convention: active-tracker pointer, the `## Now` block, one step per session, `/handoff` then `/clear`.
- `doc-hygiene.md` - the doc-type contract: size budgets and their ratchet, no history in reference docs, no plan citations in permanent docs or code comments.
- `design-system-foundations.md` - the brand-agnostic design skeleton: theme/typography plumbing, color roles, layout primitives (incl. window size, two-pane, adaptive modal), motion, desktop affordances, the editorial role contract (§2), the shared component catalog (§8). **Brand tokens, the editorial tone, and concrete palettes live in each app's own design-system doc, not here.**
