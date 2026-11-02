---
paths:
  - "feature/**"
  - "core/**"
---

# Architecture quick reference

Detail: `foundation/docs/architecture.md` and `toad-architecture.md`, then the app's architecture doc.

- **A feature never imports a sibling feature.** Cross-feature wiring lives in the layer above the features.
- **Layers per feature:** `domain/` (repository interfaces + use cases, depends on nothing) -> `data/` (impls,
  data sources, mappers) -> `presentation/` (screens, ScreenModels, actions, events, state; depends on domain
  only) -> `di/` (the DI module).
- **TOAD:** immutable `UiState` as a `StateFlow`; a sealed `UiAction`, one per interaction; one-time `UiEvent`
  via `Channel`; `LocalVariables`; `ActionDependencies`; per-feature `*Collector` interfaces. Flow:
  `UiAction.execute()` -> use cases -> `setState()` -> recompose.
- **Always:** `AppDispatchers` for Main/IO/Default; network and disk failures return
  `Result<T>` with `.onSuccess()` / `.onFailure()`; `AppLog` for logging, never `println` / `Log.*`.
- **Naming:** domain models are plain nouns. Suffixes mark the role: `*Entity`, `*DataSource(Impl)`,
  `*Repository(Impl)`, `*UseCase`, `*Screen`, `*ScreenModel`, `*Action`, `*Event`, `*UiState`,
  `*LocalVariables`, `*Dependencies`, `*Collector`.

## Project lines

<!-- The app's own architecture rules: module roster, persistence, networking, DI, navigation. -->
