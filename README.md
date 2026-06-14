# rhaydus-foundation

Shared foundation for the `nl.rhaydus` apps (Nestbox, Softcover): the TOAD presentation runtime,
Gradle convention plugins, custom ktlint rules, a shared version catalog, the design-system skeleton,
and the reusable Claude Code assets (skills, agents, hooks, conventions docs).

> **Status:** bootstrapping. See [`MIGRATION.md`](MIGRATION.md) for the full plan, phase status, and
> the decisions log. Update that file at the end of every session.

## Layout (filled in per phase)

| Path | What | Distribution |
|---|---|---|
| `gradle/libs.versions.toml` | shared version catalog | published catalog artifact |
| `build-logic/` | convention plugins (`rhaydus.*`) | includeBuild / published |
| `ktlint-rules/` | custom lint ruleset | published jar |
| `toad/` | TOAD runtime (`nl.rhaydus.toad`) | published KMP lib |
| `designsystem-core/` | theme/typography skeleton | published KMP lib |
| `docs/` | canonical conventions | bundled in the Claude plugin |
| `claude/` | Claude Code plugin + marketplace | plugin install |

## Consuming from an app (hybrid model)

Published artifacts go to **Maven Central** (group `nl.rhaydus`), so consumers need **no credentials**
— just `mavenCentral()`, which both apps already declare. While actively developing the foundation,
source-link it instead:

1. In the app's `local.properties`: `foundation.local=true`
2. The app's `settings.gradle.kts` reads that flag and `includeBuild("../rhaydus-foundation")`.
   Gradle substitutes the published coordinates for local source automatically — no version bumps.
3. Set it back to `false` (or remove it) to return to the pinned published version.

### Publishing credentials (maintainers only)

Consuming needs nothing. Publishing to Maven Central needs a Central Portal user token + a PGP signing
key, supplied via env/Gradle properties (never committed) — see the secret names in
`.github/workflows/publish.yml`. The `nl.rhaydus` namespace is verified with a DNS TXT record on
rhaydus.nl (no server required). Releases are normally done by CI on a `v*` tag.

## Build

```
./gradlew help                          # configuration check (no modules yet)
./gradlew build                         # once modules exist
./gradlew publishToMavenLocal           # local smoke test of publishing
./gradlew publishAndReleaseToMavenCentral   # release (normally done by CI on a v* tag)
```
