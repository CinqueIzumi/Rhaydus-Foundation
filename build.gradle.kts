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
    // Root-applied: the shared buildHealth policy for the root and every subproject.
    id("rhaydus.dependency-analysis")
    // Root-applied: ktlint, type-resolved detekt and `styleCheck` over every module, on the foundation's own rules.
    id("rhaydus.style")
    // Root-applied: fails a module that declares Compose resources but does not package them on Android.
    id("rhaydus.resource-packaging")
    // On the root classpath for the convention plugin above, which applies it by id.
    alias(libs.plugins.dependency.analysis) apply false
    // On the root classpath for rhaydus.style, which applies it to every module by id.
    alias(libs.plugins.detekt) apply false
    // On the root classpath for the Compose convention plugins, which apply it by id for the stability gate.
    alias(libs.plugins.stability.analyzer) apply false
}

val foundationVersion: String = providers.gradleProperty("foundation.version").getOrElse("0.0.0-SNAPSHOT")

allprojects {
    group = "nl.rhaydus"
    version = foundationVersion
}

// Foundation-specific buildHealth exclusions on top of the shared policy.
dependencyAnalysis {
    issues {
        all {
            onIncorrectConfiguration {
                // KSafe backs the desktop SecureStorage actual and is intentionally implementation
                // (no KSafe type leaks into SecureStorage's public surface).
                exclude("eu.anifantakis:ksafe")
            }
        }
    }
}

// The foundation checks itself with the rules it ships: the jars come straight from the modules.
rhaydusStyle {
    detektRules = project(":detekt-rules")
    ktlintRules = project(":ktlint-rules")
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
