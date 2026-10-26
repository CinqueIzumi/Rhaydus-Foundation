# Design System Foundations

Brand-agnostic skeleton for `nl.rhaydus` apps (Jetpack Compose, Material 3 Expressive, TOAD). Brand identity
(palette, fonts, tone, motion choreography, illustrations, concrete component catalog) lives in each app's own
`design-system.md` (or `DESIGN_SYSTEM.md`). "Each app decides" means that doc records the decision.

> **Maintenance rule.** A change that introduces, retires, or alters a foundation, component, or pattern in
> this skeleton updates this file in the same change. Doc and code never disagree at merge. The same rule
> binds each app's own design-system doc for its brand-specific surfaces.

## 1. Theme setup (Material 3 Expressive)

- The root theme composable wraps content in **`MaterialExpressiveTheme`**
  (`@OptIn(ExperimentalMaterial3ExpressiveApi::class)`), passing a `colorScheme`, a `typography`, and
  `motionScheme = MotionScheme.expressive()`. Screens consume the expressive motion scheme; no hand-authored
  durations or easings.
- The Expressive APIs (expressive theme entry point, expressive motion scheme, floating/expressive widgets)
  come from an **alpha `androidx.compose.material3:material3` pinned in `libs.versions.toml` over the BOM's
  version**. The pin carries a one-line comment (Expressive components) and is load-bearing. Remove it once
  the BOM catches up.
- **Dynamic color (Android 12+ Material You) is wired but off by default**: the theme composable takes
  `dynamicColor: Boolean = false`. The app's canonical scheme is the default look; dynamic color is opt-in
  only.
- Light and dark schemes are full `lightColorScheme(...)` / `darkColorScheme(...)` declarations mapping every
  Material slot to a named token. Token values (hex) live in the app.

## 2. Typography plumbing

Two type scales, both reachable from `MaterialTheme`:

- **Material typography.** The standard display/headline/title/body/label scale, passed as `typography` to
  the expressive theme and consumed by stock Material components. Do not override it for component internals.
- **App-specific typography scale.** An `@Immutable data class` of named `TextStyle` roles for text a screen
  composes itself (not text inside a stock Material component). Exposed as:
  - An internal `staticCompositionLocalOf { DefaultXTypography }` holding the default instance.
  - A `MaterialTheme.<x>Typography` extension property (`@Composable @ReadOnlyComposable`) reading `.current`.
  - The root theme provides it via
    `CompositionLocalProvider(LocalXTypography provides DefaultXTypography) { MaterialExpressiveTheme(...) }`.
- **Font families are isolated in one file**: one named `FontFamily` declaration per family (e.g. a reading
  face and an accent face). System generic families (`SansSerif`, `Monospace`) versus bundled/multiplatform
  font resources is an **app-level choice documented in the app's own doc**. Swapping the pairing changes
  only those declarations.
- **No ad-hoc `TextStyle` at a call site.** Pick the closest app-scale or Material role; `.copy(...)` only
  for a documented exception. Case transforms (e.g. `.uppercase()`) are applied at the call site, not baked
  into the style.
- **Shared editorial role contract (opt-in layer).** The **opt-in `designsystem-editorial` module** (not
  core) owns a fixed `EditorialTypography` role set (eyebrow, pageTitle, headline, title, body, statLarge /
  statHero, ...). An app provides it via `EditorialTheme(editorialTypography = ...)` nested inside
  `RhaydusTheme`; editorial components read `MaterialTheme.editorialTypography`. The module owns the role
  *names*; role *values* are a brand decision (`buildEditorialTypography(typography)` is a neutral starting
  point to `.copy(...)`, or build from scratch). An app without editorial components does not depend on the
  module; core neither defines nor requires it.

Role names, sizes/weights, and each role's family belong in the app's own typography table.

## 3. Color roles

Semantic roles map onto Material 3 Expressive slots; the palette is app-side.

- **Primary.** The single accent: text/chrome emphasis (section marks, active toggle/filter states, progress
  indicators, the filled primary button). It is also the surface fill of **at most one hero/stat card per
  scrollable region**. Inside that card content is `onPrimary`, demoted text is `onPrimary` at low alpha, and
  any call to action steps down a tier (tonal/elevated). **Otherwise primary is never a background**;
  ordinary cards, rows, and grouped regions use the page background or a `surfaceContainer*` shade.
- **on-surface / on-background.** Default readable copy.
- **on-surface-variant.** Demoted copy: metadata, captions, secondary lines, inactive labels.
- **surface / background.** The plain page canvas.
- **surfaceContainer, -High, -Highest.** Elevated tiles, cards, grouped sections. Step the shade *up* for
  grouping or focus. **Elevation is tonal**; no drop-shadow cards (an app may carve a narrow brand exception,
  e.g. a physical-object motif, documented in its own doc).
- **surfaceContainerLowest.** Reserved for modal sheets and overlays above the page.
- **outline / outlineVariant.** Hairline dividers and the rare bordered control. Prefer whitespace and a tone
  change over a rule.
- **secondaryContainer / onSecondaryContainer.** Selected state of pill chips and similar facet toggles; idle
  state is `surfaceContainerHigh / onSurface`.
- **inversePrimary.** Small attention markers on chrome (notification dots).
- **error / onError.** Destructive and validation states only.

Roles outside the Material scheme (a fixed brand accent identical in light and dark, a redaction treatment,
syntax tinting, ...) are app extensions documented in the app's own doc, never here.

## 4. Shape, elevation, and the accent bar

- **Corners.** Small radii on cards/tiles, medium on buttons (Material defaults via the expressive theme),
  fully rounded (`RoundedCornerShape(percent = 50)`) for pill chips and active toggle states. Exact radii are
  app-tunable.
- **Elevation is tonal** (section 3): step the container shade, never add elevation.
- **Accent bar.** A short primary-colored bar leads a section label, in **two sizes picked by label role**: a
  larger section bar in the page gutter ahead of a full-size label-plus-headline pair, and a smaller inline
  hairline for a compact label inside a card or hero. Never mixed: a full label gets the section bar, a
  compact label gets the hairline or no bar. Dimensions are an app choice.

## 5. Layout primitives

### 5.1 Spacing scale

- Spacing comes from a small fixed set of dp steps, each roughly double the last: tight inline gaps,
  within-component padding, the page horizontal gutter, gaps between blocks in a region, gaps between major
  sections. Values are app-tunable within this doubling rhythm.
- One gutter value per page.
- Two adjacent section headers without a generous gap between them is a layout bug.

### 5.2 Page scaffold and bottom-bar padding

A page is, top to bottom: optional top app bar, scrolling content with a consistent horizontal gutter, bottom
navigation on a tab root. **Scrolling content reserves the trailing padding the bottom bar demands**; the last
item is never occluded.

- `LocalBottomBarPadding` (default `0.dp`) carries that padding; never pass it by hand.
- `BottomBarScaffold(bottomBar, placement, barSpacing, content)` is the host: the app passes its brand bar as
  a slot, and the host **provides** the padding via `CompositionLocalProvider(LocalBottomBarPadding provides
  ...)`. No per-app host boilerplate.
- **`placement`:** `BottomBarPlacement.OVERLAY` draws the bar over content; layout reserves nothing and the
  host provides the bar's measured footprint + `barSpacing`. `BottomBarPlacement.DOCKED` hosts the bar in a
  Material `Scaffold` whose `innerPadding` reserves it; the host provides `barSpacing` alone.
- **Screens never branch.** The provided value always means the trailing padding to reserve. A runtime
  docked/floating preference flips only `placement`. No screen or read-side helper branches on the app's
  bar-style enum.
- **Never re-add the inset.** In `OVERLAY` the host's `onSizeChanged` sits outside the bar's
  `windowInsetsPadding`, so the measured height already includes the `navigationBars` inset. Never add
  `WindowInsets.navigationBars` on top of it.
- Screens read it through `rememberBottomBarPadding()` and apply it as trailing content padding.
- `BottomNavigationSpacer()` emits a `Spacer` sized to the bottom navigation-bar inset, for screens that need
  the raw inset.
- Any bottom-anchored chrome overlaying content counts as a bottom bar: a persistent player or session strip
  on a layout without a tab bar uses `OVERLAY`, `barSpacing = 0.dp`.
- The **rendered bar and tab host stay app-side** (section 8). The foundation ships `BottomBarScaffold`, the
  window-size primitives (§5.7), `TwoPaneScaffold` (§5.8), and the cross-tab pulse (§7); the app composes its
  adaptive nav shell from them.
- **Root tabs use an in-page title**, rendered inside the scroll in a `pageTitle`-style role. A Material
  top-bar title is only for sub-screens pushed into the stack that need back navigation or trailing actions.

### 5.3 Section rhythm

Every region opens with: optional accent bar, short label (eyebrow/kicker), human-readable headline, then the
body (a list, a carousel/grid, or a single block), then a generous gap before the next section.
`EditorialSectionHeader` (`designsystem-editorial`) implements the accent-bar + eyebrow + headline +
optional-description opener.

### 5.4 Hero region

A hero opens a screen with one dominant element and sits on the page surface, not on a container. The single
primary-filled stat card (section 3) is the only hero with a fill.

### 5.5 Cards and rows / reserved-row rule

- Collections render as cards (fixed-size in a horizontal carousel) or rows (same anatomy laid horizontally).
- **Cards reserve the height of every optional row they may show; the list never reflows as data streams in.**
- Horizontal carousels paint no overflow affordance (no edge fade, chevron, or page indicator); the rightmost
  card clipped against the gutter is the cue.

### 5.6 Modal sheets

- Modal bottom sheets open fully expanded on `surfaceContainerLowest`.
- The first element is always the canonical section header (accent bar, label, headline, optional
  description), then the body. **No exemptions**: a stock Material `titleLarge`/`bodyMedium` sheet header is a
  regression.
- Sheets handle their own dismissal; no custom close button.
- Content too transient for this header is a `Dialog`, not a sheet.

### 5.7 Window size and breakpoints

Adapt to the **measured size of the rendering surface**, never a hardcoded device type. Breakpoints live in
one place; no screen defines its own threshold.

- `rememberWindowSizeClass()` buckets the window width into a `WindowWidthClass`: **`COMPACT` (`< 600dp`)**,
  **`MEDIUM` (`600-840dp`)**, **`EXPANDED` (`>= 840dp`)**. The bounds (`COMPACT_MAX_WIDTH`,
  `MEDIUM_MAX_WIDTH`) are constants on `WindowSizeClass`; `widthClassFor(...)` is the single function applying
  them, used by layout code and tests alike.
- **Branch on `widthClass`, not raw dp.** Use `widthDp` / `heightDp` only for a continuous decision (e.g. a
  hero height tracking the viewport).
- The class recomputes on container size changes (desktop window resizes, foldable posture changes).
  Breakpoint values are app-tunable; the three buckets and the single source are fixed.

### 5.8 Adaptive content width and the two-pane split

A list surface never stretches a single column across a desktop window.

- **`TwoPaneScaffold`** is a pure-layout split: a fixed-width `list` pane beside a flexible `detail` pane with
  a divider, with no knowledge of navigation or screen models. A `null` `detail` **collapses to a single
  pane** without divider. `list` is always the first child, so toggling `detail` preserves the list subtree's
  state (and any `movableContentOf` it hosts).
- Wiring: on **`EXPANDED`** a list-detail screen shows both panes; on **`COMPACT` / `MEDIUM`** the detail is
  pushed as a full screen by ordinary navigation. The list pane picks its own column width; the app caps
  overall content width.
- `Modifier.cappedContentWidth(max)` centers a scrolling content column and caps its width. Caps come from
  `ContentMaxWidth` (`Reading` for single-column copy/forms, `Pane` for a list/grid spread), never a
  per-screen number. Full-bleed media (covers, hero backdrops) stays outside the capped column.

### 5.9 Adaptive modal sheet (sheet to panel)

**`AdaptiveModalSheet`** resolves a modal by window width: a `ModalBottomSheet` on `COMPACT` / `MEDIUM`, a
centered, bordered, large-radius panel (a `Dialog`) on `EXPANDED`.

- Its `content` slot is **identical to `ModalBottomSheet`'s** (a `ColumnScope` body owning its padding and
  scroll); call sites swap one for the other without touching the body.
- Both forms open on `surfaceContainerLowest`, lead with the canonical section header (5.6), and carry no
  close button.
- An in-content dismiss (e.g. an "are you sure?" choice) calls `LocalModalSheetDismiss`, never drives sheet
  state (the sheet animates down; the panel closes immediately).
- A body adjusting density to its surface reads `LocalModalSheetForm` (`SHEET` vs `PANEL`), never re-derives
  the width class.

## 6. Iconography

- Material **outline** icons, consistent stroke weight. Never mix filled and outlined families on one surface
  (an app may sanction a single filled glyph as a deliberate state pair, e.g. an active toggle, and documents
  it).
- Icon-only controls always carry a content description.
- Icon size scales with the control's size token; an icon in body type sits on the baseline at a size
  proportional to the text.

How icons are addressed (raw Material `Icons`, or a typed in-house icon/illustration catalog on multiplatform
resources) is an app decision in the app's own doc.

## 7. Motion (structure)

- Use the Material **expressive motion scheme** (section 1); hand-author durations or easings only for a
  custom property the scheme cannot express.
- **Press feedback on ripple-less surfaces.** Tappable cards and ripple-less surfaces scale to about 0.97
  while pressed and ease back on release, driven by an `InteractionSource`. Press scale is the *only*
  indication there: never strip a card's ripple without adding the scale. Material buttons keep their ripple
  and pressed-shape morph and add no scale.
- **Wavy progress.** Progress and indeterminate waits use the wavy/expressive progress indicator; flat bars
  are chrome only, never semantic progress. Pull-to-refresh is the common documented exception; an app names
  its own exception.
- **User-triggered list mutations animate; ambient changes do not.** User-driven add/remove animates (removal
  fades, neighbors close the gap); initial load, background refresh, and pagination tail appear without
  animation. `rememberLazyItemMutationAnimator(keys)` + `Modifier.mutationAnimated(scope, animator, key)`
  snapshot the first non-empty key set as the un-animated baseline and animate only later appearances (with a
  brief accent-bar pulse on the inserted item). The animator does not distinguish user-driven from ambient:
  apply it only to surfaces whose changes are user-driven; a background refetch or pagination of a wrapped
  list animates too.
- **Staggered entry is one-shot**: played only by items composed during the initial screen-entry window,
  never on scroll. `rememberStaggeredEntryCoordinator(key)` + `Modifier.staggeredEntry(coordinator, index)`
  provide it; first entry is tracked per key for the process lifetime, so a return visit renders statically.
- **Cross-tab nav pulse.** An event landing on a tab other than the current one briefly pulses that tab's
  icon. `NavPulse` is a keyed, instance-owned signal: the app fires `navPulse.pulse(key)` where the event
  originates (safe outside composition) and the nav chrome scales the icon with
  `rememberPulseScale(navPulse, key)`. The concrete key (which tab, what event) stays app-side.
- **All motion is suppressed under reduced motion** (system animations disabled): an instant, un-animated
  swap. Mandatory for every animation.

Commit choreography (haptic textures, celebration bursts, shared-element morphs, shake-on-error, ...) belongs
in each app's own doc.

## 8. Component API conventions

The concrete catalog lives in the app's own doc.

- `designsystem-core`: design-agnostic primitives (layout, modifiers, the button family).
- `designsystem-editorial` (opt-in): `EditorialSectionHeader`, `HeroStatNumberField` + `EditorialSuffix`,
  `DropCapText`, `EditorialSearchField`, `PullToRefreshEyebrow`, all reading the editorial type roles
  (section 2).
- `designsystem-image` (opt-in, brings Coil): `RhaydusImage` (plain), `RhaydusPlaceholderImage` (a
  caller-supplied placeholder slot while loading), `RhaydusShimmerImage` (shimmer placeholder). The network
  fetcher is an app choice on the app's Coil `ImageLoader`.

- **Exporting a composable as an image.** `ShareCardCapture` (`share/`, `designsystem-core`) captures a
  composable to a PNG and saves or shares it: wrap the card in `CapturableShareCard(capture) { … }` (the
  card design is the `@Composable` slot), then call `saveToGallery` / `saveToCache` / `share` on the
  `rememberShareCardCapture(config)` instance. Platform handling is inside the seam (Android
  MediaStore/scoped-storage save + `FileProvider` share `Intent`; desktop native save dialog + clipboard copy;
  iOS `UIActivityViewController`). Brand-specific inputs (file-name prefix, gallery album, the Android
  `FileProvider` authority the app declares in its manifest) come through `ShareCardCaptureConfig`. It is
  self-contained (kotlinx `Dispatchers`, no Koin); failures surface through the `SaveOutcome` /
  `ShareOutcome` return types, not a logger. `GalleryWritePermissionRequester` gates the legacy Android write
  permission (API ≤ 28) and reports granted immediately elsewhere.
- **The button family is shared.** `RhaydusButton`, `RhaydusToggleButton`, `RhaydusIconToggleButton`, and
  `RhaydusSplitButton` (`designsystem-core`) are the canonical buttons, parameterized by the `ButtonSize` /
  `ButtonStyle` / `ToggleButtonStyle` / `IconToggleButtonStyle` / `SplitButtonStyle` enums, with icons as
  `RhaydusIconResource`. Use them, not Material `Button` directly. The filled style carries press-scale
  feedback (section 7). They use Material typography (no editorial dependency).
- **Shared interactive widgets take params, never brand values.**
  - `StarRatingInput` (`component/`): half-star tap/drag-scrub rating; star glyph is a `RhaydusIconResource`,
    filled/empty tints are color params defaulting to Material roles.
  - `ExpandableFlowRow` (`layout/`): a `FlowRow` collapsed to N lines behind a replaceable "show more"
    affordance. The caller's `FlowRowExpansion` sets the reveal: `Progressive` adds a few lines per tap,
    `Full` opens the whole set. `collapsible = true` adds a matching "show less" back to the collapsed
    height; required for a `Full` reveal over an unbounded set.
  - `InlineErrorState` (`component/`): the in-content load/submit-failure surface, a message in the Material
    `error` role above a `RhaydusButton` retry, with retry label and text style as params (app copy and
    editorial role). It renders the TOAD error slot (`toad-architecture.md`).
  - Use these before hand-rolling a rating row, a collapsing chip/tag set, or an error-and-retry block.
- **Reusable building blocks live in `core/presentation/widget/` (or the app's design-system module); theming
  lives in `core/presentation/theme/`.** Components that name concrete features (bottom navigation and the
  tab host, which enumerate app tabs) live at the shell/orchestration tier, not generic `core`; the pieces
  they use (icon toggles, color roles, `BottomBarScaffold` and its padding contract §5.2, the `NavPulse`
  cross-tab pulse §7) stay in `core`.
- **One declaration per file**, named after the declaration. One action per file in TOAD features (section 9).
- **Slot APIs over flags where content varies.** A pill chip takes an optional `onClick` (interactive when
  present, read-only tag when omitted), not a boolean mode. Use the canonical shared primitive over a
  hand-rolled `Surface`-and-`Text`.
- **Visibility.** `public` only for the cross-module surface (domain contracts, use cases, `*Screen`/`*Tab`,
  the aggregated `*Module`, shared design-system components); everything else is module-private. For a
  screen, only `override fun Content()` and the stateless `XScreen(state, runAction)` render function are
  public; every sub-component composable is `private`.
- **Compose formatting** (enforced by the style gates): a blank line between sibling composables in any
  layout scope (including `Spacer`); a call with two or more arguments breaks one-argument-per-line with a
  trailing comma; single-argument forms stay inline; property/parameter annotations go on their own line
  above the declaration.
- **Previews** drive the public stateless `XScreen(state, runAction)` render function without a screen model,
  never the `Content()` entry point.

## 9. TOAD to Compose glue

TOAD ([`toad-architecture.md`](toad-architecture.md)) connects to Compose at exactly one seam.

- A screen is a Voyager `Screen` object. `Content()` obtains its screen model via
  `koinScreenModel<XScreenModel>()`, collects state with `collectAsState()`, and hands `state` plus
  `screenModel::runAction` to a **stateless** `XScreen(state, runAction)` render function. The render
  function and every sub-component are otherwise side-effect-free.
- **The screen model's only public entry point is `runAction(action)`.** UI never touches the dispatcher,
  repositories, or the action scope; it only dispatches actions.
- **`UiState` is rendered; `LocalVariables` is not.** Transient state actions need between dispatches
  (in-flight ids, scratch values) lives in `LocalVariables` and triggers no recomposition.
- **One-off effects go through `UiEvent`, collected once at the screen's top level.** Off-app handoffs (open a
  URL, share, launch the browser) dispatch a `UiEvent` that the top-level `Content()` collects and hands to
  the platform handler. **Never call a `LocalUriHandler`, share sheet, or other platform handler from a leaf
  composable.**
- Collectors push external state (a repository `StateFlow`) into the same action scope, so external changes
  reach the UI without a user action.

## 10. Decision rules (shared core)

Walk this before reaching for novelty. Brand-specific rules extend this list in each app's own doc.

- **Need a primary action?** Filled button. One per region.
- **Need to introduce a region?** The section header (accent bar, label, headline).
- **Need to mark a region as elevated relative to its neighbor?** Step the container shade. No shadow.
- **Need a divider?** First try a vertical gap or a tone change; a hairline only when neither works.
- **Need a custom font weight, size, or family?** Pick a different role from the app or Material scale. Never
  inline a `TextStyle`.
- **Need to show progress?** The wavy/expressive progress indicator (bar and number move as one), except the
  documented pull-to-refresh exception.
- **Need a modal interaction?** Bottom sheet first (with its canonical header), full screen second, dialog
  only for an unavoidable blocking moment.
- **Need an off-app handoff?** Dispatch a `UiEvent` collected at the screen top; never call the platform
  handler from a leaf.
- **Need to adapt to window width?** `rememberWindowSizeClass()` and branch on `widthClass` (5.7); never read
  a raw dp threshold at the call site.
- **Need a modal that should not read as a phone sheet on a wide window?** `AdaptiveModalSheet` (5.9), not a
  bare `ModalBottomSheet`.
- **Building a desktop (jvm) layout?** Use the section 11 affordances before hand-rolling pointer or keyboard
  behaviour.

## 11. Desktop affordances (pointer and keyboard)

Desktop-only affordances live in `jvmMain`. The `commonMain` modifiers (`pointerHandCursor`, `hoverHighlight`,
`platformModifierClick`) are inert on touch; shared composables apply them unconditionally.

- **Hand cursor.** `Modifier.pointerHandCursor()` shows the platform hand cursor while the pointer hovers a
  clickable surface lacking a built-in cursor affordance. No-op on touch.
- **Hover highlight.** `Modifier.hoverHighlight(interactionSource)` paints a subtle translucent wash on hover,
  cross-fading in and out; the hover sibling of `pressScale` (7). Pair it with a `clickable` / `hoverable`
  that **shares the same `InteractionSource`**. Inert on touch; suppressed under reduced motion.
- **Tooltip.** `DesktopTooltip(text) { control }` wraps a pointer-only control in a hover tooltip on desktop
  and is a pure pass-through on touch (no extra layout node). Use it on **every icon-only desktop control**.
  `contentDescription` stays the accessibility label; `text` is the hover label.
- **Back strip.** `DesktopBackStrip(...)` is the static leading-gutter back button a pushed desktop screen
  carries instead of a scroll-collapsing top bar. The app supplies its arrow glyph (as `RhaydusIconResource`);
  the shared module bakes in no icon asset.
- **Scrollbar.** `DesktopVerticalScrollbar(state)` overlays a persistent, themed vertical scrollbar on the
  trailing edge of a scrolling region. It takes a `LazyGridState`, `LazyListState`, or `ScrollState`; use the
  `ScrollState` overload when items vary widely in height (exact extent instead of a lazy estimate). The thumb
  uses `MaterialTheme.colorScheme.onSurface`. jvm-only.
- **Esc dismisses in-app overlays.** `Modifier.dismissOnEscape(enabled) { onDismiss() }` closes an in-app
  surface over the page (a full-screen viewer, a transient selection mode) on Esc. Only for surfaces that are
  **not** a `Dialog` / `Popup`; a `Dialog` (including `AdaptiveModalSheet`'s expanded panel) already maps
  `dismissOnBackPress` to Esc. It runs in the key **preview** phase (catches Esc while a descendant text field
  holds focus) and never grabs focus while disabled, so it can gate on a transient mode.
- **Selection on desktop.** Where touch enters multi-select by long-press, desktop also honors pointer idioms
  (right-click to open the selection, modifier-click to extend it). Long-press is never the only entry on
  desktop; the concrete gestures are an app decision.
- **Modifier-click selection.** `Modifier.platformModifierClick(onCtrlClick, onShiftClick)`: on desktop a
  primary click with **Ctrl/Cmd** toggles this item's selection and with **Shift** range-selects to it; Shift
  wins when both are held. It intercepts in the pointer **Initial** phase and consumes the press only when a
  modifier is held, so a plain click still reaches the surface's own `combinedClickable` (no double-fire).
  `commonMain`, pure pass-through on touch.
- **Right-click menu.** `DesktopContextMenu(items) { content }` wraps a surface in a desktop secondary-click
  menu (the pointer counterpart to long-press). Pure pass-through on touch and for an empty `items` list;
  wrap unconditionally and populate items only where a menu applies.

**Tap-to-dismiss scrims use `detectTapGestures`, not `clickable`.** A full-screen scrim dismissing an overlay
on tap takes its tap through `Modifier.pointerInput { detectTapGestures { ... } }`. `clickable` binds Space /
Enter and makes the node a focusable semantic button, turning the scrim into a desktop focus stop;
`detectTapGestures` is pointer-only.
