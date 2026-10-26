# Architecture

The `nl.rhaydus` apps follow **Clean Architecture** with the **TOAD** presentation framework
([`toad-architecture.md`](toad-architecture.md)). This doc covers layering, module / package structure and
dependency direction.

Two physical shapes, one architecture:

- **Multi-module Gradle build** (`:app -> :orchestration -> :feature:* -> :core:*`): the layering is the
  internal structure of each module; the tier graph is the cross-module dependency rule. This is the
  canonical target.
- **Single Gradle module** (`:app`): the package tree under `nl.rhaydus.<app>` mirrors the same
  `core/feature` structure, and package boundaries take the role of the module graph. Lay it out so that
  extraction into Gradle modules is a move-and-rename.

"Module" means "module or its mirror package" unless a section is about the Gradle-module axis.

---

## 1. The two axes

Every file is classified on two independent axes.

| Axis | Question it answers | Values |
|------|---------------------|--------|
| **Layer** | *What kind of code is this?* | `domain` / `data` / `presentation` / `di` |
| **Tier** | *Who is allowed to depend on it?* | `core` / `feature` / `orchestration` |

- Layer governs the internal structure of a module; tier governs cross-module dependencies.
- The axes are orthogonal: a `domain` use case may live in a `core` or a `feature` module.
- Choose both layer and tier for every type; never inherit them from where the first caller sits.
- The rules apply at package level whether or not the concept is already its own Gradle module.

---

## 2. Layered architecture

Every feature and every `core/*` operation service is organised into **data / domain / presentation**
layers, each with sub-folders per component type. Create only the layers and folders an area needs: a
presentation-only feature has just `presentation/`; a UI-less operation service has just `data/` + `domain/`.

```
<feature-or-core-area>/
├── domain/                 # Pure business layer - depends on nothing
│   ├── model/              # Domain models, state classifications, value types
│   ├── repository/         # Repository interfaces (contracts)
│   └── usecase/            # Business-logic use cases (added when there is real logic)
├── data/                   # Implements the domain contracts
│   ├── model/              # DTOs (*Dto / *Response), entities (*Entity)
│   ├── datasource/         # *RemoteDataSource / *LocalDataSource - interface + Impl colocated
│   ├── repository/         # Repository implementations (*RepositoryImpl)
│   └── mapper/             # Data <-> domain mappers
├── presentation/           # TOAD - see toad-architecture.md
│   └── state/ event/ action/ collector/ screenmodel/ screen/
└── di/                     # The area's Koin module
```

### Layer rules

- **Domain** depends on nothing. It owns domain models and repository interfaces.
- **Data** implements the domain interfaces. It owns DTOs, entities, data sources, mappers and
  `*RepositoryImpl`. Data may reference domain, never the reverse.
- **Presentation** depends on **domain** only, through **use cases**, never a repository or data-source
  type directly. A use case may be a thin pass-through to a repository.
- Keep platform-only and framework-only types out of `domain` packages.

### Contract-first repositories and data sources

Repositories and data sources are both contract-first: an interface plus an `*Impl`.

- A data source's interface and its `*Impl` live in the **same file**, named after the interface
  (`RemoteThingDataSource.kt` holds `RemoteThingDataSource` and `RemoteThingDataSourceImpl`).
- The repository **interface** lives in `domain/repository/`; its `*RepositoryImpl` in `data/repository/`.
- DI always binds the **interface** (`single<ThingRepository> { ThingRepositoryImpl(...) }`,
  `single<ThingLocalDataSource> { ThingLocalDataSourceImpl(...) }`).

### Error handling across layers

- **Data sources / repositories throw.** A failed network, IO or parsing operation propagates its
  exception. No sentinel return values for real failures, no failure-policy side effects (clearing bad
  state, retry-and-give-up). A `try` / `catch` / `runCatching` here only encodes a deliberate business
  decision (for example falling back to a placeholder when a secondary fetch fails, keeping a valid
  primary result). Never catch only to hide an error from the caller.
- **Use cases return `Result<T>` and own failure policy.** Wrap the repository call in the
  cancellation-aware `runCatching` helper (rethrows `CancellationException`) and return `Result<T>`, never
  the bare domain type. Cross-operation reactions to a failure live here, not in the repository (for
  example dropping unusable cached credentials when a dependent fetch fails, then returning the original
  failure).
- **Presentation (TOAD actions) folds the `Result`** with `.onSuccess { }` / `.onFailure { }` (not
  `.fold()`), translating both into UI state through the action scope. Actions hold no `try` / `catch`.
  Worked pattern: [`toad-architecture.md`](toad-architecture.md).

### Placing a new type in the correct layer

Classify the type before choosing a package; never default to where the first consumer lives. In order:

1. **Business rule or state classification derived from domain data?** -> `domain/model/`. A
   classification such as "loading / disconnected / connected" or "on track / behind / expired" is domain
   even when only a `@Composable` renders it.
2. **Persisted row, DTO or remote payload?** -> `data/model/` (`*Entity`, `*Dto` / `*Response`).
3. **Display-only (colors, icons, composable argument shapes, routes, tabs)?** -> `presentation/`.

- **Heuristic:** if a headless use case, a CLI or a non-Compose client could consume the type without
  losing meaning, it belongs in `domain`, even when its only consumer is UI code.
- Attribute-bearing enums (for example one carrying a `label`) stay in `domain` when the category is a
  business concept; presentation adds mappings (colors, icons) at the call site.
- **Derived state:** a value that is purely a function of a domain type's fields is a computed property
  on that type (`val ThingProgress.status`), not a free-standing extension file.
- Never file a domain-shaped concept under `presentation/`.

---

## 3. Tiers and the module / package structure

**A module may depend only on modules in a lower tier - never sideways within the feature tier, never
upward.**

```
┌─────────────────────────────────────────────────────────────┐
│  T3  :app            thin application shell (Application +     │  depends on :orchestration
│                      manifest + Koin startup)                 │  (+ a few core modules it touches)
│      :orchestration  navigation host + cross-feature          │  may depend on everything below
│                      orchestration use cases + Koin aggregate │
├─────────────────────────────────────────────────────────────┤
│  T2  :feature:<aggregator>  features that compose several     │  may depend on T1, T0
│                             other features                    │
├─────────────────────────────────────────────────────────────┤
│  T1  :feature:*      leaf features (one user-facing surface)  │  may depend on T0 only
│                                                               │  NEVER on a sibling feature
├─────────────────────────────────────────────────────────────┤
│  T0  :core:*         shared kernels: domain model, operation  │  may depend only on other T0
│                      services, preferences, identity,         │
│                      design system, network, storage          │
└─────────────────────────────────────────────────────────────┘
```

In a single-module app, the same tiers exist as packages under `nl.rhaydus.<app>`:

```
nl/rhaydus/<app>/
├── core/            # Shared infrastructure + operation services consumed by features
│   ├── network/         # Client factory / API plumbing
│   ├── presentation/    # TOAD framework, theme, shared widgets, dispatchers, nav shell, nav contract
│   ├── common/          # Cross-cutting helpers (e.g. the cancellation-aware runCatching)
│   └── <operation>/     # UI-less operation services (data + domain), one folder each
└── feature/         # Leaf features, one folder each (each with its own data/domain/presentation/di)
```

- `core/*` packages are T0; `feature/*` packages are T1. A T2 aggregator package exists only when a
  feature composes another.
- The app shell and cross-feature orchestration are T3. Orchestration may be a set of packages rather
  than a separate module.

### T0 - `core/*`

`core` holds code consumed by **two or more features** that carries no single feature's identity. Split
it into focused units, never one grab-bag. Typical members:

| Unit | Holds |
|------|-------|
| `core:domain` | shared domain models, classification enums, config value types, and cross-feature use-case **contracts** whose impls live elsewhere |
| `core:<operation>` | an **operations service**: a repository plus the use cases every feature calls (and its data sources / mappers). One per shared capability |
| `core:network` | the API client / transport, interceptors, and the shared call-wrapping helpers |
| `core:<storage>` | the local persistence layer (database, migrations, all persisted entities + DAOs, or the key-value store) |
| `core:designsystem` (or `core/presentation`) | the TOAD framework, theme, reusable Compose components, modifiers, shared presentation models, the navigation contract, and any app-scoped controllers |
| `core:common` | cross-cutting helpers with no domain identity |

- `core` modules depend on each other **downward only**. `core:domain` depends on nothing.
- **Litmus test:** a type a second, unrelated feature would import to do its job is `core`. A type
  imported by two or more features is `core`, even a screen model or presentation model.
- **Vertical-slice rule:** a `core:<concept>` operations unit owns only the **repository + use cases**
  (and its data sources / mappers). Its **domain models** live in `core:domain`. Its **persisted entities
  + DAOs** live in the shared storage unit (one database sees every entity; DAOs may join across them).
  Never give a concept unit its own models or entities.

### T1 / T2 - `feature/*`

A `feature` owns a **user-facing surface** (a screen or a coherent flow) and the domain / data /
presentation specific to it, in the section 2 layout:

```
feature/<name>/
├── domain/         repository interfaces, use cases, feature-local models - depends on core only
├── data/           repository impls, data sources, entities, mappers
├── presentation/   screens, screenmodels, actions, events, collectors, state
└── di/             the feature's Koin module
```

Module-axis rules:

- **A leaf feature (T1) imports `core` only - never a sibling feature.** Before writing
  `import nl.rhaydus.<app>.feature.<other>` from a leaf, apply one of the three resolutions below.
- **Feature-local stays feature-local.** A model, enum or component used by exactly one feature lives in
  that feature. Promote to `core` only on the *second* real consumer, never speculatively.
- **An aggregator (T2)** may depend downward on the leaf features it composes. A leaf never depends on an
  aggregator; it navigates to it via the navigation contract.

When a leaf seems to need something from a sibling, apply exactly one:

1. The shared thing is a kernel -> push it down to `core` (most common).
2. The feature aggregates the other -> it moves to T2 and may depend on it.
3. The feature only needs to *navigate* to the other's screen -> use the navigation contract
   (section 5), not a screen import.

### T3 - `:orchestration` + `:app`

T3 is the only tier that may know about many features at once.

**`:orchestration`** holds:

- The **navigation host** (`Navigator` / `TabNavigator` setup, root and bottom-bar screens, the
  navigation-contract implementations). Its manifest contributes the launcher Activity.
- **Cross-feature orchestration use cases**: logic that coordinates several features (for example wiping
  user data across features, or launch-time initialisation spanning identity plus content sync).
- The **Koin aggregate**: the list of every module's `module { }`.

**`:app`** holds only the `Application` class, the launcher resources, the manifest `<application>`
element and the Koin startup (`modules(<appName>Modules + appModule)`). It depends on `:orchestration`
plus the core modules its Application touches directly.

- A use case that reaches into two or more *features* is orchestration and lives here. One that reaches
  into two or more *core* modules only may live in `core`.
- **A leaf never imports `:app` or `:orchestration`.** A T1 screen model that triggers a cross-feature
  flow depends on a **contract interface in `core:domain`**; the `...UseCaseImpl` lives in
  `:orchestration` and is bound to the interface in the orchestration Koin module.
- In a single-module app, the app shell, navigation host and cross-feature use cases fill T3 from
  `core/presentation` and the application class; cross-feature coordination stays out of feature
  packages.

---

## 4. Dependency injection

Koin. Each feature and each `core/*` operation service owns exactly **one** `module { }` in its `di/`
folder, binding implementations to domain interfaces (`single<ThingRepository> { ThingRepositoryImpl(...) }`).

- Repositories and data sources are `single`. Use cases are `factory`. Screen models are `factory`
  (injected with `koinScreenModel`).
- All modules are aggregated into one list (`<appName>Modules`), owned by `:orchestration` in a
  multi-module app and by a single aggregator file in a single-module app. The `Application` starts Koin
  with `modules(<appName>Modules + appModule)`.
- Shared dependencies (storage, the API client, dispatchers) come from their owning `core/*` unit.
- Flow collectors are passed inline into the screen-model factory
  (`factory { XScreenModel(..., flows = listOf(SomeCollector)) }`), never bound in Koin. See
  [`toad-architecture.md`](toad-architecture.md).

---

## 5. Navigation

Voyager. `TabNavigator` drives bottom-bar tabs; `Navigator` handles push / pop stacks. The nav shell (root
screen, bottom-bar screen, bottom bar) lives at the orchestration tier in a multi-module app, or in
`core/presentation` in a single-module app.

### The cross-feature navigation contract

A feature **never** imports another feature's `Screen` or `Tab` class. Cross-feature navigation goes
through a contract in `core` (the design-system / presentation unit):

```kotlin
interface AppNavigator {
    fun screen(destination: ScreenDestination): Screen   // resolves "what" to navigate to
    fun tab(destination: TabDestination): Tab
}
```

- A feature injects the contract (`koinInject<AppNavigator>()`) and controls *how* it navigates
  (`navigator.push`, `navigator.parent?.push`, `tabNavigator.current = ...`); the contract resolves only
  *what*.
- The single implementation lives at the **orchestration tier**, bound `single<AppNavigator>` in the
  orchestration Koin module.
- A new externally reachable surface adds a `ScreenDestination` / `TabDestination` case wired in the
  implementation, never an import in the calling feature.
- Data passed across a navigation boundary is a **shared presentation model in `core`**.
- Non-Compose deep links (for example a notification opening a screen) use an analogous `AppEntryPoint`
  contract that builds intents targeting the launcher Activity.

### Pitfall: per-item nested navigators need a unique `key`

When a list-detail screen hosts the detail pane in its **own nested `Navigator`** (one per selected item,
e.g. the expanded pane of a two-pane layout - see design-system-foundations.md section 5.8), every pane
renders the **first** selected item: Voyager caches a `Navigator` by composition position, and a bare
`Screen`'s identity defaults to its class name. Give the detail `Screen` an id-derived key:

```kotlin
class BookDetailScreen(val id: String) : Screen {
    override val key = "book-detail-$id"   // without this, every pane shows the first id
}
```

Give every `Screen` whose identity depends on a runtime argument an explicit `key`.

---

## 6. Dispatchers

- An `AppDispatchers` abstraction provides `Main` / `IO` / `Default` dispatchers via DI.
- TOAD actions run on `Main`; use cases, data sources and repositories switch to `IO` internally for
  network and disk work.
- `AppDispatchers` lives in `core-common`, the module of non-visual shared primitives (dispatchers,
  logging, result helpers, formatting).

`core-platform` (depends on `core-common`) holds the platform-capability seams, each an interface in
`commonMain` with a public per-platform implementation the app wires into its own DI:

- **`SecureStorage`** - a keyed read/write/delete secret store backed by hardware-backed storage
  (`AndroidSecureStorage` over the Keystore, `IosSecureStorage` over the Keychain, `JvmSecureStorage`
  over the desktop OS secret store).
- **`NetworkAvailabilityProvider`** - reactive connectivity (`isOnline: StateFlow<Boolean>`,
  `awaitOnline()`) with per-platform providers (Android `ConnectivityManager`, iOS `nw_path_monitor`,
  desktop reachability polling). The `NetworkAvailability` singleton exposes the same state as an
  instant, non-suspending check for guard clauses; install the provider once at startup. The desktop
  provider is `AutoCloseable`; the app must `close()` it on shutdown.

### Offline write queue (`offline-sync`)

`offline-sync` (depends on `core-platform`) is the generic optimistic-write / drain-and-reconcile engine:
a mutation that cannot reach the server is enqueued locally and replayed when connectivity returns. The
app supplies write shapes, persistence and remote dispatch as injected seams.

- **`PendingWriteStore<P>`** (extends **`WriteQueue<P>`**) is the persistence seam the app implements
  (Room / SQLite / DataStore): `enqueue(payload)`, `getPending(maxAttempts)` (oldest-first, poison-capped),
  `delete(localId)`, `incrementAttempts(localId)`. The concrete table, the payload-row codec and any
  coalescing (upsert-replacing same-kind writes for an entity) stay app-side. `P` is the app's opaque
  write payload; a `PendingWrite<P>` row carries `localId` + `attempts`.
- **`DefaultOfflineWriteDrainer<P, I, K>`** (implements **`OfflineWriteDrainer<I, K>`**) is the engine.
  `start(scope)` drains once at startup and on every connectivity return (via
  `NetworkAvailabilityProvider`); `drain()` runs one pass on demand and returns the **reconciliation
  hints** `Map<I, Set<K>>` (per entity id, the write kinds that synced). When merging a fresh fetch, the
  caller preserves only the fields each replayed write owns.
- The app supplies the `replay: suspend (P) -> ReplayOutcome` dispatch, a `hintKey: (P) -> Pair<I, K>?`
  extractor, an `isTransient: (Throwable) -> Boolean` classifier and a `DrainPolicy` (poison cap +
  in-drain exponential backoff).
- A **transient** replay failure bumps the row's attempts and halts the pass; a **terminal** failure
  discards the row. `isTransient` defaults to transient for every failure; override it to discard writes
  the server always rejects.
- The app owns the drain -> fetch -> reconcile composition; the module ships only the drainer.

---

## 7. Build setup and convention plugins

Module build files use the **Gradle convention plugins** in `build-logic/`. Apply the smallest set that
fits:

| Plugin | Apply to |
|--------|----------|
| `rhaydus.android.library` | base for an Android-only `core` / `feature` / `orchestration` module (SDK / JDK, the shared coroutines / Koin / logging runtime, and the test stack) |
| `rhaydus.android.compose` | any Android-only module with Compose UI |
| `rhaydus.kmp.library` | base for a multiplatform module |
| `rhaydus.kmp.compose` | a multiplatform module with Compose UI |

- Apply the Compose plugin only on modules with UI, on top of the base library plugin.
- The application module uses the Android application plugin and owns build-type-conditional wiring.
- Never re-declare what a convention plugin provides.
- **Each module declares the `project(...)` dependencies its own code imports** - never rely on a
  transitive dependency. A library whose type appears in a module's *public API* is `api`; all other
  dependencies are `implementation`.
- **Manifests merge upward.** Library modules contribute their components via their own
  `AndroidManifest.xml`; the application module owns the `<application>` element, permissions and
  launcher icons. Shared resources (strings, drawables, the theme) live in the design-system unit.
- **One Koin `module { }` per module**, aggregated as in section 4.
- A single-module app keeps the same package boundaries and declare-what-you-import discipline.

The multi-module shape gates three things, each wired into every module's `check`:

- **Module-graph tiers** - the root-applied `rhaydus.module-graph` convention plugin registers
  `checkModuleGraph`, which derives each module's tier from its path and fails on any `project(...)`
  dependency pointing sideways or upward. An `api` edge to a designated data-area module must be
  allowlisted. Each build supplies its tiers, module-to-tier mapping and allowlists in the
  `moduleGraph { }` block of its root build script.
- **Dependency health** - the `com.autonomousapps.dependency-analysis` policy fails on an unused
  dependency or a wrong `api`-vs-`implementation` exposure; each module's `projectHealth` is wired into
  its `check` (`buildHealth` aggregates them). The uniform runtime / test / Compose bundle the convention
  plugins inject is excluded from "unused" findings; transitive-completeness and compile-vs-runtime advice
  stay informational.
- **Android lint** - every module runs lint with `warningsAsErrors` + `abortOnError` against the shared
  root `lint.xml`; its version-freshness checks (`NewerVersionAvailable` / `GradleDependency` /
  `AndroidGradlePluginVersion`) are `informational`.

---

## 8. Placing a new thing - decision flow

Decide every new class, enum, screen or use case's **layer** (section 2) and **tier** (section 3).

1. **User-facing surface or surface-specific logic?** -> a `feature` (T1, or T2 if it composes other
   features): the feature whose surface it serves.
2. **Will a second, unrelated feature import it to do its job?** -> `core` (T0), in the focused unit for
   what it is: a domain model or enum -> `core:domain`; an operation -> the matching `core:<operation>`
   service; a reusable component / theme / TOAD / nav contract -> `core:designsystem`.
3. **Does it coordinate two or more *features*?** -> the orchestration tier (T3).
4. **Default:** feature-local. Promote to `core` only when the second real consumer appears, never
   speculatively.

**Tie-breaker (`core` vs `feature`):** a type a headless use case, a CLI or a second feature could
consume without losing meaning leans `core`; a type that only makes sense for one screen stays in that
feature.

---

## 9. Review checklist

A change is structurally correct when:

- [ ] No leaf feature (T1) imports another feature.
- [ ] No module depends sideways or upward - only on lower tiers.
- [ ] A type imported by two or more features lives in `core`, not in a feature.
- [ ] Cross-feature navigation goes through the `core` contract, not a `Screen` import.
- [ ] Cross-feature coordination lives in the orchestration tier, not inside a single feature.
- [ ] Repositories / data sources throw; use cases return `Result<T>`; actions fold with
      `.onSuccess` / `.onFailure`.
- [ ] The new type's **layer** and **tier** were both chosen deliberately, not inherited from where
      the first caller happened to sit.

---

## 10. Naming and folder conventions

- Module path mirrors tier and name (`:core:domain`, `:core:<operation>`, `:feature:<name>`). **One
  Gradle module per feature, not one per layer.** The `domain` / `data` / `presentation` split stays
  as **packages inside** the module. A single-module app expresses the same split as
  `core.<unit>.<layer>` / `feature.<name>.<layer>` packages.
- Suffixes: `*Repository` / `*RepositoryImpl`, `*UseCase`, `*DataSource` / `*DataSourceImpl`,
  `*Entity`, `*Dto`, `*Screen`, `*ScreenModel`, `*Action`, `*Event`, `*UiState`, `*Dependencies`,
  `*LocalVariables`.
- One declaration per file. A data source's interface and its `*Impl` are the sanctioned exception
  (colocated in one file named after the interface).
- See [`code-style.md`](code-style.md) for the full naming, file-layout, comment, and Compose
  formatting rules, and [`toad-architecture.md`](toad-architecture.md) for the presentation pattern.
