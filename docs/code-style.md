# Code Style Guide

- Canonical, app-agnostic code style for `nl.rhaydus` Kotlin/Compose projects.
- Projects follow the [official Kotlin code style](https://kotlinlang.org/docs/coding-conventions.html)
  (`kotlin.code.style=official` in `gradle.properties`).
- TOAD presentation conventions live in the architecture docs; this guide covers everything else.

## Tooling

- `nl.rhaydus:ktlint-rules` (package `nl.rhaydus.ktlint`) enforces mechanizable style.
  `./gradlew ktlintFormat` auto-fixes; `./gradlew ktlintCheck` verifies.
- The ktlint ruleset gates: the uniformly-internal visibility categories, one-type-per-file, no inline
  fully-qualified references, project-import order, no inline mockk stubs, no bare `runCatching` in a
  `*UseCase*`, and `no-raw-logging` (use the `AppLog` facade from `nl.rhaydus:core-common`, never `println`
  or `android.util.Log.*`). Cases it does not gate (cross-module mappers, shared components, anything
  subtler) are caught in review.
- `nl.rhaydus:detekt-rules` (`./gradlew detektCheck`, plus the shared `config/detekt.yml` baseline) adds the
  type-resolution checks, notably `UnguardedFlowTerminalRead`: it flags an unguarded `Flow` `first()` /
  `single()` and does not flag the `Collection` operators of the same name.
  - `first` on a hot flow (`SharedFlow` / `StateFlow`) is exempt, so `isOnline.first { it }` is a legal
    `awaitOnline()`. `single()` stays flagged on hot flows.
- detekt's `formatting` ruleset stays off; `ktlint-rules` owns layout rules.

- The detekt rules fire only under type resolution (`detektAndroidMain` / `detektJvmMain` / `detektMain`)
  and are inert on a syntactic run. A consuming app must run those tasks.
- The shared baseline disables `UnreachableCode`, `IgnoredReturnValue` and `RedundantSuspendModifier`
  (false positives from detekt 1.23's Kotlin 1.9 frontend on Kotlin 2.x; see the config's comments).

## Naming Conventions

### Files and Classes

Files are named in **PascalCase**, matching their primary class.

| Type | Convention | Example |
|------|-----------|---------|
| Domain models | Plain nouns | `AccountState`, `Author`, `DeviceCodeResponse` |
| Data entities | `*Entity` suffix | `AccountEntity` |
| Data sources | `*DataSource` / `*DataSourceImpl` (both in one file, named after the interface) | `RemoteDataSource`, `LocalDataSourceImpl` |
| Repositories | `*Repository` / `*RepositoryImpl` | `AccountRepository`, `AccountRepositoryImpl` |
| Use cases | `*UseCase` | `MarkContentAsReadUseCase`, `SearchForNameUseCase` |
| Screens | `*Screen` | `ProfileScreen`, `HomeScreen` |
| Screen models | `*ScreenModel` | `ProfileScreenModel` |
| ViewModels | `*ViewModel` | `MainViewModel` |
| Actions | `*Action` (one per file) | `StartLinkAction`, `OnQueryChangeAction` |
| Events | `*Event` | `ProfileEvent`, `SearchEvent` |
| UI state | `*UiState` | `ProfileUiState`, `SearchUiState` |
| Local variables | `*LocalVariables` | `ProfileLocalVariables` |
| Dependencies | `*Dependencies` | `ProfileDependencies` |
| Mappers | `*Mapper` | `AccountMapper` |
| DI modules | `*Module` | `ProfileModule`, `authModule` |

### Functions and Variables

- Functions and variables use **camelCase**; functions are action-oriented (`startDeviceFlow`,
  `getToken`, `searchForName`, `markAsRead`).
- Boolean variables/properties are prefixed with `is`/`has` (`isLoading`, `isActive`,
  `isAuthenticated`).
- **Injected use cases keep their full `*UseCase` name** as the property/parameter, e.g.
  `val startLinkUseCase: StartLinkUseCase`, never abbreviated to `startLink`.

## Project Structure

- Every feature follows the `data` / `domain` / `presentation` layer split (directory layout in the
  architecture docs).
- A data source interface and its implementation live in the **same file**, named after the interface
  (`RemoteDataSource.kt` holds `RemoteDataSource` and `RemoteDataSourceImpl`). Never split them.
- Each feature has its own Koin DI module in a `di/` subdirectory.

## One declaration per file

- **One data class per file**, named after the class: models, DTOs, snapshots, result holders,
  wrappers, everything. A single-caller data class gets its own file next to that caller.
  - Never declare a `data class` locally inside a function, method or `init` block.
  - Never colocate multiple unrelated `data class` declarations in one file.
- **One enum per file**, named after the enum. Labels, icons, colors and other display data are
  constructor properties on the enum (`status.label`), never re-derived in a `when` / `if` at call sites.
- **One action per file.** `<Name>Action.kt` holds only the `sealed interface`; each concrete
  `data object` / `data class` action lives in its own file and carries the `Action` suffix. Concrete
  events carry the `Event` suffix; the same per-suffix rule applies across the naming table
  (`*UiState`, `*UseCase`, `*Dependencies`, etc.).
- **Exception - sealed hierarchy variants.** The `data class` / `data object` variants of a
  `sealed interface` / `sealed class` MAY be co-located in the sealed type's own file (named after the
  sealed type), or split one-per-file as top-level subtypes in the same package. Either is fine; pick
  what reads better. A co-located sealed variant is not a one-data-class-per-file violation.

```kotlin
// Good - label lives on the enum.
enum class DeadlineStatus(val label: String) {
    OnTrack(label = "On track"),
    Behind(label = "Behind"),
    Expired(label = "Expired"),
}

Text(text = status.label)

// Bad - label duplicated at every call site.
val label = when (status) {
    DeadlineStatus.OnTrack -> "On track"
    DeadlineStatus.Behind -> "Behind"
    DeadlineStatus.Expired -> "Expired"
}
```

```kotlin
// Good - HoursMinutesSeconds.kt
data class HoursMinutesSeconds(
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
)

// Bad - declared locally inside a function.
fun onLaunch(...) {
    data class ProgressSnapshot(val itemId: Int?, val total: Int?)
    ...
}

// Bad - second data class tacked onto a file alongside helper functions.
fun secondsToHm(seconds: Int): String { ... }
data class HoursMinutesSeconds(...)
```

## Comments

- **Default: no comment.** First try to make the code say it (a clearer name, an extracted function, a
  named constant). Delete comments that fail the bar below.
- A comment is allowed only for:
  - a **non-obvious edge case** (a value that must be created on every recomposition, a first-frame
    guard, an off-by-one boundary),
  - a **workaround or gotcha** (why the non-obvious API was chosen, a framework footgun, a deliberate
    `runCatching` swallow),
  - a **reason the code cannot show** (why a magic constant has this value, a spec or design
    requirement the code satisfies).
- **Never narrate.** Never write a comment that restates what the code does ("opens the sheet", "loops
  over items", "the back button").
- **KDoc** (`/** ... */`) is expected on the public, cross-module surface of a shared module (components,
  modifiers, contracts). It describes the contract and the why, never the implementation. A private
  member gets KDoc only when it meets the comment bar above.

## Code Organization Within Files

- Use **region comments** to group related code sections in production code:
  ```kotlin
  // region Search Logic
  ...
  // endregion
  ```
- `// region` and `// endregion` lines sit flush against the code they wrap. Do **not** add a blank
  line directly before or after them.
- Separate logical sections with blank lines.
- Region comments are for production code only; test files use `@Nested inner class` grouping instead
  (see *Test Class Organization*).

## Boolean Negation

- Never use the `!` prefix operator for boolean negation; use `.not()`. Applies to all booleans: locals,
  properties, call results and complex expressions.
- The non-null assertion `!!` is outside this rule.

```kotlin
// Good
if (isLoading.not()) { ... }
if (state.hasFocus.not()) { ... }
(items.isEmpty()).not()

// Bad
if (!isLoading) { ... }
if (!state.hasFocus) { ... }
!items.isEmpty()
```

## If / Else

- A single-line `if` / `else` expression (the whole statement fits on one line) may omit braces:
  `val x = if (a) b else c`.
- Once the `if` / `else` spans multiple lines (condition, a branch body, or total length), **every branch
  uses an explicit `{ ... }` block**. Never mix braced and brace-less branches; never span a brace-less
  branch across lines.

```kotlin
// Good - single line, no braces needed.
val dayLabel = if (daysRemaining == 1L) "day" else "days"

// Good - multi-line, both branches use blocks.
val message = if (progress.isOnTrack) {
    "You're on track."
} else {
    "You're ${progress.pagesBehindSchedule} pages behind schedule."
}

// Bad - brace-less branch spans multiple lines.
val message = if (progress.isOnTrack) "You're on track."
    else "You're ${progress.pagesBehindSchedule} pages behind schedule."
```

## Visibility

- **A top-level declaration referenced only within its own module is `internal`** (or `private`), never
  public-by-default. Public is only for a deliberate cross-module surface; a public declaration no other
  module imports is wrong.

**Default to `internal`:**

- **TOAD plumbing** in a feature's `presentation/`: `*ScreenModel`, the sealed `*Action` and every
  subclass, `*UiState`, the `*Event` type and its subclasses, `*Dependencies`, `*LocalVariables`,
  and the `flows/` initializers/collectors. Mark a whole sealed hierarchy consistently (parent +
  all variants).
- **Data-layer implementations**: `*RepositoryImpl`, `*DataSourceImpl`, `*StorageImpl`, `*QueueImpl`,
  DataStore serializers/entities, and `*Mapper` functions/classes used only within their own module.
- **Module-private helpers**: top-level helper functions, extension functions, and `@Composable`
  helper components not referenced from another module.
- **The app shell** (the `Application`, `appModule`, version-provider impl) and **navigation-host
  internals** (the navigator impl, root/bottom-bar screens, cross-feature use-case impls bound to a
  domain contract).

**Stays `public` (the cross-module surface):**

- Domain contracts: `*Repository` / `*DataSource` **interfaces**, all `*UseCase` classes.
- Domain models, enums, value types, and sealed result types in the shared domain module (and
  feature-local models exposed through a public use case's signature).
- `*Screen` / `*Tab` navigation classes and navigation contracts.
- The aggregated Koin `val *Module`.
- Shared design-system components/theme/models/TOAD contracts, and any impl that realises a shared
  contract consumed elsewhere.
- A mapper or other helper genuinely called from another module (confirm with a cross-module
  reference before keeping it public).

**Cascade rule.** A `public` member that exposes an `internal` type becomes `internal` itself (e.g. a
`Screen(state: FooUiState, …)` member composable whose `FooUiState` is internal). Fix it at the member;
never re-widen the type.

- The `nl.rhaydus.ktlint` visibility-modifier rule (`ktlintCheck` / `check`) gates the
  uniformly-internal categories (the `*Action` / `*Event` / `*UiState` / `*LocalVariables` /
  `*ScreenModel` / `*Dependencies` / `flows` / `*Impl` suffixes in their packages). Other cases are
  caught in review.

> Single-module projects: use `private` for local helpers and keep the public surface intentional.
> Adopt internal-by-default for the categories above once the project splits into modules.

## Compose

- Screens follow the project's presentation architecture (Voyager `Screen` objects or top-level
  `@Composable` functions). For Voyager screens the core `XScreen(state, runAction)` render function is
  `public` (for `@Preview`) and every sub-component composable is `private`. See the TOAD/architecture
  docs.
- Reusable UI components live in the shared design-system / presentation component package; custom
  modifiers in the modifier package; theming in the theme package.
- **Blank line between sibling composables.** Inside any layout scope (`Column`, `Row`, `Box`, etc.),
  leave a blank line between each child composable call, including `Spacer`. Never stack two
  composable calls back-to-back.
- **Multi-argument composables break across lines.** Any composable invocation (or any function call,
  see *Argument and Property Layout*) with two or more arguments puts each argument on its own line
  with a trailing comma, even short calls like
  `Text(text = ":", modifier = Modifier.padding(horizontal = 4.dp))`. Single-argument composables
  stay inline.

```kotlin
// Good
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Center,
) {
    TimeField(value = hours, ...)

    Text(
        text = ":",
        modifier = Modifier.padding(horizontal = 4.dp),
    )

    TimeField(value = minutes, ...)
}

// Bad - siblings touching, inline multi-arg Text.
Row(...) {
    TimeField(value = hours, ...)
    Text(text = ":", modifier = Modifier.padding(horizontal = 4.dp))
    TimeField(value = minutes, ...)
}
```

## Argument and Property Layout

- One-argument calls and declarations stay on a single line.
- With **two or more** arguments or properties, each sits on its own line: the opening `(` stays on the
  name's line, the closing `)` sits on its own line, and every argument, **including the last**, has a
  trailing comma.
- This applies to:
  - function declarations (including abstract/member signatures in `interface` and `abstract class`
    bodies, even without a body),
  - function call sites,
  - constructor invocations,
  - class, `sealed class` and `sealed interface` declarations: both the primary constructor parameter
    list and the supertype invocation (`: Parent(arg1, arg2)`),
  - data-class instantiations,
  - `apply { ... }` / `copy(...)` invocations on data classes,
  - any other parenthesised parameter list with two or more entries.
- A trailing lambda (`fn(arg) { ... }`) does not count as an argument: `runTest(dispatcher) { ... }`
  stays on one line.
- **Property/parameter annotations go on their own line** above the declaration, never inline. When a
  class's properties carry such annotations, separate **every** property in that list with a blank
  line, including unannotated ones mixed in.

```kotlin
// Single argument - inline.
repository.fetchById(id = itemId)

// Function declaration with two arguments - one per line, trailing comma, closing paren on its own line.
fun updateProgress(
    item: Item,
    newPage: Int,
): Item

// Function call with two arguments.
remoteDataSource.updateProgress(
    item = item,
    newPage = newPage,
)

// Data-class instantiation with two or more properties - one property per line.
Item(
    id = id,
    title = title,
    rating = rating,
)

// Single-property data class - still inline.
Author(name = name)

// Interface member with two or more parameters - one per line, trailing comma.
interface ItemDao {
    suspend fun setStatusEnabled(
        code: Int,
        enabled: Boolean,
    )
}

// Sealed class with a multi-argument primary constructor, and subclasses whose
// supertype invocation also takes multiple arguments - both lists break apart.
sealed class LibraryTab(
    val id: String,
    val label: String,
) {
    data object All : LibraryTab(
        id = "all",
        label = "All",
    )

    data class CustomList(
        val listId: Int,
        val listName: String,
    ) : LibraryTab(
        id = "list-$listId",
        label = listName,
    )
}

// Annotated DTO property - annotation on its own line, blank line between properties.
@Serializable
data class DeviceCodeResponse(
    @SerialName("device_code")
    val deviceCode: String,

    @SerialName("user_code")
    val userCode: String,

    val interval: Int,
)
```

## Code Block Whitespace

Every multi-line construct is a paragraph: a blank line before and after it.

### Opening and closing braces

- **No blank line after an opening `{`.** Exception: sealed class / sealed interface bodies have a
  blank line after `{`.
- **No blank line before a closing `}`.**
- A blank line follows the closing `}` of a code block, unless it is the last line of its enclosing
  block.

### Paragraph rule for multi-line constructs

Every multi-line construct (code block, multi-line call, multi-line assignment, `when` / `if`
expression) gets a blank line before and after it. This includes:

- `val`/`var` assignments where the right-hand side spans multiple lines (`if/else`, `when`,
  multi-line lambda, multi-line constructor/function call).
- A single-line `val` followed by a multi-line `val` has a blank line between them.
- mockk's `coEvery { ... }` / `every { ... }` stubs are never one-liners: open the block onto its own
  line and leave a blank line after each stub's closing `}`.

### Consecutive single-line statements

- Blank line **between logically unrelated groups**.
- **No blank line within a related group** (e.g. related property declarations, consecutive guard
  clause bodies that belong to one extraction).

### Guard clauses

- Blank line **after** a `val`/`var` extraction before the first guard clause.
- Blank line **between** each guard clause.
- Blank line **after** the last guard clause before the main logic.
- Every guard clause is its own paragraph.
- This applies to **every** safeguard, including single-line ones (`if (ids.isEmpty()) return emptyList()`,
  `val x = foo ?: return null`, `val x = foo ?: return@map other`), whether it exits with `return`,
  `return <value>`, `return@label`, `throw` or `continue`.

### `if` statements that consume an extracted variable

- An `if` that references a `val`/`var` declared on the line directly above it has a blank line between
  the assignment and the `if`. Applies to single-line and multi-line `if` bodies, guard or branching.

### Between declarations

- **Functions / methods**: always one blank line between them, in both classes and interfaces.
- **Interface members**: always one blank line between method signatures.
- **Sealed class variants**: always one blank line between variants.
- **Data class properties**: no blank lines between properties. DTO properties annotated with
  `@SerialName` / `@SerializedName` (or similar): blank line between each property.
- **Enum entries**: no blank lines between entries. One blank line before `companion object`.
- **Property groups in classes**: no blank lines within a logical group; one blank line between
  different groups.

### `when` expressions

- `when` inside `onEvent()`-style dispatchers: single-line branches grouped without blanks; blank
  line before the first block-body branch; blank lines between all block-body branches.
- `when` where **all** branches have block bodies: blank lines between each branch.

### Coroutine launches

- Blank lines between sequential `launch { }` blocks (e.g. in `init`).

### Super calls and error logs

- Blank line between a `super.*()` inheritance call (e.g. `super.onResume()`, `super.onCreate(...)`)
  and the following code.
- Blank line between an error log call and the following code.

```kotlin
class Example {
    fun doWork() {
        coEvery {
            repository.fetch()
        } returns value

        every {
            mapper.map(any())
        } returns mapped

        val result = useCase()
    }
}
```

## Import Ordering

1. Android / AndroidX imports
2. Third-party libraries (Kotlin stdlib, Coroutines, Ktor, Koin, Compose, etc.)
3. Project-specific imports (`nl.rhaydus.*`)

Remove unused imports.

### No fully-qualified references

- Never reference anything by its fully-qualified name inline: types, top-level functions, enum
  entries, object members. Import it and use the short name.
- Applies to production and test code, including parameter types, return types, generic arguments,
  lambda receiver types, `mockk()` type witnesses and top-level function calls.
- **Strictest for project-own code** (`nl.rhaydus.*`): always add the import.

```kotlin
// Bad
additionalActions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},

// Good
import androidx.compose.foundation.layout.RowScope

additionalActions: @Composable RowScope.() -> Unit = {},
```

## Data Flow

- UI state is always exposed as `StateFlow` (immutable from the UI's perspective).
- One-time events (navigation, toasts) are sent via `Channel`.
- Repository data is exposed as `Flow` and collected in the screen model's initializers/collectors.
- Actions execute on the `Main` dispatcher; network/DB/IO operations run on `IO`.

## Error Handling & Logging

- **Data sources and repositories throw freely.** Never swallow a genuine failure (network, IO, parsing)
  into a sentinel return; let the exception propagate. Repository-level `try`/`catch`/`runCatching` is
  only for a deliberate business decision (e.g. falling back to a placeholder), never to hide an error
  the caller should see or to run failure-policy side effects.
- **Every use case returns `Result<T>` and owns failure policy.** Wrap the repository call in
  `runCatchingCancellable` (`nl.rhaydus:core-common`), which rethrows `CancellationException`. Never use
  bare `runCatching` around a suspend call. Failure reactions spanning operations belong in the use case,
  not the repository. To also log the failure, use `runCatchingLogged` (same module):
  `runCatchingCancellable` plus one `AppLog.e`, using the tag passed to `AppLog.install(...)`.
- **Screen-model actions unpack the `Result` with `.onSuccess { }` / `.onFailure { }`** and fold the
  outcome into UI state. **Never use `.fold()`.** Actions carry no `try`/`catch` of their own.

```kotlin
// Use case - domain layer.
class StartAuthorizationUseCase(private val accountRepository: AccountRepository) {
    suspend operator fun invoke(): Result<DeviceAuthorization> =
        runCatchingCancellable { accountRepository.startDeviceAuthorization() }
}

// Action - presentation layer.
dependencies.startAuthorizationUseCase()
    .onSuccess { authorization -> scope.setState { it.copy(link = Connecting(...)) } }
    .onFailure { scope.setState { it.copy(link = Disconnected(error = "Couldn't reach the server.")) } }
```

- Plain `try`/`catch` is allowed in data/repository code where control flow needs it: retry loops, or
  rethrowing `CancellationException` before catching `Exception` so a recovery never fires on
  cancellation.
- Network calls use the project's shared HTTP client.
- Log errors through `AppLog` (`nl.rhaydus:core-common`), never `println` or `android.util.Log.*`.

## Dependencies

- Dependency versions live in `gradle/libs.versions.toml`; reference them via the version catalog
  (`libs.<alias>`) in `build.gradle.kts`. Never hard-code a version in a module build file.

## Test Class Organization

- Group all tests for one function of the unit under test in a JUnit 5 `@Nested inner class`. Test files
  use no `// region` / `// endregion` blocks.
- Each nested class targets exactly **one** function (or property) on the unit under test.
- Name the nested class in **PascalCase** after the function (`initializeItems` →
  `inner class InitializeItems`), or after the property for property wiring tests (`items` →
  `inner class Items`).
- Annotate it with `@Nested` and declare it `inner class`.
- The outer test class holds shared mocks, the `@BeforeEach` setup and private helper functions. Do
  **not** redeclare these inside the nested classes.
- Every test lives inside a `@Nested inner class`, even a function's only test; no `@Test` functions
  directly on the outer class.

```kotlin
class RepositoryImplTest {

    private lateinit var remoteDataSource: RemoteDataSource
    private lateinit var localDataSource: LocalDataSource
    private lateinit var repository: RepositoryImpl

    @BeforeEach
    fun setUp() {
        remoteDataSource = mockk()
        localDataSource = mockk()
        repository = RepositoryImpl(
            remoteDataSource = remoteDataSource,
            localDataSource = localDataSource,
        )
    }

    @Nested
    inner class InitializeItems {

        @Test
        fun `caches fetched items on first call`() = runTest {
            // ----- Arrange -----
            ...
        }
    }

    @Nested
    inner class RefreshItems {

        @Test
        fun `does not reset the session flag`() = runTest {
            // ----- Arrange -----
            ...
        }
    }
}
```

## Unit Test Structure

Unit tests follow **Arrange-Act-Assert (AAA)**, with three marked sections:

```kotlin
@Test
fun `does the thing`() = runTest {
    // ----- Arrange -----
    val input = stubInput()

    coEvery {
        repository.fetch()
    } returns input

    // ----- Act -----
    val result = useCase()

    // ----- Assert -----
    result.isSuccess shouldBe true
    result.getOrNull() shouldBe input
}
```

- Use `// ----- Arrange -----`, `// ----- Act -----` and `// ----- Assert -----` exactly as shown: five
  dashes on each side, capitalized section name in the middle.
- Every unit test contains all three markers, in order, even if a section holds a single line.
- A blank line precedes each marker, except the first, which directly follows the function's opening
  `{`. Each section's body starts on the line directly under its marker, with no blank line.
- When act and assertion are the same expression with no separable act step (asserting on a `Flow` inside
  a Turbine `test { }` block, or a call wrapped in `shouldThrow { ... }`), use a single
  `// ----- Act & Assert -----` marker.
- AAA markers are not region blocks; the region no-blank-line rule does not apply to them.
