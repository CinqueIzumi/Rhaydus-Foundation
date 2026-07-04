// Root build for rhaydus-foundation.
//
// The convention plugins in build-logic apply AGP/Kotlin/Compose plugins by id; their versions are
// pinned here `apply false` (from the catalog) so every module resolves them consistently. Group +
// version for all modules are set below. Publishing is wired per-module with the
// com.vanniktech.maven.publish plugin (Maven Central via the Central Portal, with PGP signing).

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    // Applied (apply false) at the root so the vanniktech plugin - and its shared Sonatype release
    // build service - load in ONE classloader scope. Without this, the per-module applications load
    // the build service under different classloaders and `publishAndReleaseToMavenCentral` fails the
    // task graph with a SonatypeRepositoryBuildService type mismatch across sibling modules.
    alias(libs.plugins.vanniktech.mavenPublish) apply false
    // Applied (not `apply false`): the module-graph gate operates on the root + subprojects.
    id("rhaydus.module-graph")
    // Root-applied: dependency-analysis auto-configures every subproject and adds the `buildHealth`
    // aggregate task.
    alias(libs.plugins.dependency.analysis)
}

val foundationVersion: String = providers.gradleProperty("foundation.version").getOrElse("0.0.0-SNAPSHOT")

allprojects {
    group = "nl.rhaydus"
    version = foundationVersion
}

// dependency-analysis (buildHealth): the root application (plugins block) registers the aggregate task;
// each subproject needs the plugin too so it produces a project-health report, and its `check` depends on
// that per-module `projectHealth` so the policy actually gates (not just a manually-invoked report).
subprojects {
    apply(plugin = "com.autonomousapps.dependency-analysis")

    tasks.matching { it.name == "check" }.configureEach {
        dependsOn("projectHealth")
    }
}

// buildHealth policy. Gate on the high-value categories - genuinely unused dependencies and wrong
// api/implementation exposure - while excluding the uniform runtime + test + Compose bundle that the
// convention plugins inject centrally (so it is never a per-module "unused" finding). Transitive-
// completeness advice and compile-vs-runtime splitting are convention/BOM noise, so they stay
// informational (ignored), not gates. The exclusions mirror exactly what build-logic's convention
// plugins provide; a genuinely-unused app library would still fail.
dependencyAnalysis {
    issues {
        all {
            onUnusedDependencies {
                severity("fail")
                exclude(
                    // Test bundle (Android/Kmp library convention plugins).
                    "org.junit.jupiter:junit-jupiter-api",
                    "org.junit.jupiter:junit-jupiter-params",
                    "io.kotest:kotest-assertions-core",
                    "app.cash.turbine:turbine",
                    "org.jetbrains.kotlinx:kotlinx-coroutines-test",
                    "io.mockk:mockk",
                    // Runtime bundle (the library convention plugins).
                    "io.insert-koin:koin-core",
                    "io.insert-koin:koin-android",
                    "org.jetbrains.kotlinx:kotlinx-coroutines-core",
                    "org.jetbrains.kotlinx:kotlinx-coroutines-android",
                    // Compose Multiplatform bundle (KmpComposeConventionPlugin commonMain).
                    "org.jetbrains.compose.runtime:runtime",
                    "org.jetbrains.compose.foundation:foundation",
                    "org.jetbrains.compose.animation:animation",
                    "org.jetbrains.compose.ui:ui",
                    "org.jetbrains.compose.ui:ui-backhandler",
                    "org.jetbrains.compose.components:components-ui-tooling-preview",
                    "org.jetbrains.compose.components:components-resources",
                    "org.jetbrains.compose.material3:material3",
                    // Android Compose extras (KmpComposeConventionPlugin androidMain).
                    "androidx.compose.ui:ui-tooling-preview",
                    "androidx.compose.ui:ui-tooling",
                    "androidx.activity:activity-compose",
                    "androidx.core:core-ktx",
                    // Compose desktop host runtimes + Hot Reload, auto-injected for every jvm() target.
                    "org.jetbrains.compose.desktop:desktop-jvm-macos-arm64",
                    "org.jetbrains.compose.desktop:desktop-jvm-macos-x64",
                    "org.jetbrains.compose.desktop:desktop-jvm-linux-x64",
                    "org.jetbrains.compose.desktop:desktop-jvm-linux-arm64",
                    "org.jetbrains.compose.desktop:desktop-jvm-windows-x64",
                    "org.jetbrains.compose.hot-reload:hot-reload-runtime-api",
                )
            }

            onIncorrectConfiguration {
                severity("fail")
                exclude(
                    // api/implementation of convention-provided deps is managed centrally, not per module.
                    "org.jetbrains.kotlinx:kotlinx-coroutines-core",
                    "org.jetbrains.compose.components:components-resources",
                    "org.jetbrains.compose.material3:material3",
                    "androidx.compose.ui:ui",
                    "androidx.compose.ui:ui-graphics",
                    "androidx.compose.material3:material3",
                    // KSafe backs the desktop SecureStorage actual and is intentionally implementation
                    // (no KSafe type leaks into SecureStorage's public surface).
                    "eu.anifantakis:ksafe",
                )
            }

            // Transitive-completeness advice ("declare X directly") is BOM/convention noise.
            onUsedTransitiveDependencies {
                severity("ignore")
            }

            // Compile-vs-runtime splitting is a micro-optimisation entangled with the convention bundle.
            onRuntimeOnly {
                severity("ignore")
            }

            onRedundantPlugins {
                severity("ignore")
            }
        }
    }
}

// The foundation's own module-tier DAG (rhaydus.module-graph gate). The core stack (core-common ←
// core-platform ← offline-sync) may depend only on itself; the design-system stack may depend on itself
// and the non-visual core; toad is standalone. Tooling modules (ktlint-rules, detekt-rules, catalog) are
// excluded. The foundation ships no re-exported data-area modules, so the api-visibility half is inert here.
moduleGraph {
    tierOf = { path ->
        when (path) {
            ":core-common", ":core-platform", ":offline-sync" -> "core"
            ":designsystem-core", ":designsystem-editorial", ":designsystem-image" -> "designsystem"
            ":toad" -> "toad"
            else -> null
        }
    }
    allowedTargetTiers = mapOf(
        "core" to setOf("core"),
        "designsystem" to setOf("designsystem", "core"),
        "toad" to emptySet(),
    )
}

// The rhaydus-kotlin Claude plugin carries its own `version` in plugin.json (it is not a Gradle module,
// so it cannot read `foundation.version`). Lock the two together: this gate fails the build if they
// diverge. It is wired into every module's `check` below, so the CI `./gradlew build` and any local
// `check` enforce it - on a release bump, plugin.json and gradle.properties must move together.
val pluginManifest = layout.projectDirectory.file("claude/plugins/rhaydus-kotlin/.claude-plugin/plugin.json")

val verifyPluginVersion = tasks.register("verifyPluginVersion") {
    group = "verification"
    description = "Fails if the rhaydus-kotlin plugin version diverges from foundation.version."
    val manifestFile = pluginManifest.asFile
    val expected = foundationVersion
    inputs.file(manifestFile)
    inputs.property("expected", expected)
    doLast {
        val actual = Regex("\"version\"\\s*:\\s*\"([^\"]+)\"").find(manifestFile.readText())?.groupValues?.get(1)
            ?: error("No \"version\" field found in ${manifestFile.path}")
        if (actual != expected) {
            error(
                "Plugin version ($actual in plugin.json) must match foundation.version ($expected). " +
                    "Bump plugin.json and gradle.properties together.",
            )
        }
    }
}

subprojects {
    tasks.matching { it.name == "check" }.configureEach {
        dependsOn(verifyPluginVersion)
    }
}
