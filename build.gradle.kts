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
}

val foundationVersion: String = providers.gradleProperty("foundation.version").getOrElse("0.0.0-SNAPSHOT")

allprojects {
    group = "nl.rhaydus"
    version = foundationVersion
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
