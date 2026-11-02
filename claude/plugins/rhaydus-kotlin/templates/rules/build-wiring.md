---
paths:
  - "**/build.gradle.kts"
  - "build-logic/**"
  - "gradle/**"
  - "settings.gradle.kts"
---

# Build wiring

- All versions live in `gradle/libs.versions.toml`; build files reference them as `libs.<alias>`, never a
  literal coordinate or version.
- The `nl.rhaydus:*` catalog entries are version-less; they are substitution coordinates for the foundation
  `includeBuild`.
- The app and the foundation build on the same AGP and Kotlin versions, because Gradle refuses two AGP
  versions in one build; bump both together.
- Apply the smallest set of `rhaydus.*` convention plugins a module needs and do not re-declare what they
  provide.
- dependency-analysis cannot credit a `commonMain` declaration of an included-build KMP library, so the
  `nl.rhaydus` coordinates are excluded from unused-dependency advice.
- Every CI checkout that runs Gradle sets `submodules: true`.

## Project lines

<!-- The app's own build gotchas: gated tasks, root-only tasks, resource packaging, file-size gates. -->
