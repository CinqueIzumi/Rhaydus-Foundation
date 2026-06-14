// Root build for rhaydus-foundation.
//
// There are no subprojects yet (they arrive per phase — see MIGRATION.md). Group + version are set
// for all projects here. Publishing itself is wired per-module with the com.vanniktech.maven.publish
// plugin (Maven Central via the Central Portal, with PGP signing) starting in phase 1; in phase 2 the
// common POM/host config moves into a shared `rhaydus.publish` convention plugin in build-logic.

val foundationVersion: String = providers.gradleProperty("foundation.version").getOrElse("0.0.0-SNAPSHOT")

allprojects {
    group = "nl.rhaydus"
    version = foundationVersion
}
