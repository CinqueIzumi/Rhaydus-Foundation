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
}

val foundationVersion: String = providers.gradleProperty("foundation.version").getOrElse("0.0.0-SNAPSHOT")

allprojects {
    group = "nl.rhaydus"
    version = foundationVersion
}
