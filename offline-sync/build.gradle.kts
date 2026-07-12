import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("rhaydus.kmp.library")
    alias(libs.plugins.vanniktech.mavenPublish)
}

kotlin {
    androidLibrary {
        namespace = "nl.rhaydus.offlinesync"
    }

    sourceSets {
        commonMain.dependencies {
            // NetworkAvailabilityProvider (drain-on-return) + AppDispatchers / runCatching* appear on the
            // public drainer surface, so api. core-platform re-exports core-common via api.
            api(project(":core-platform"))
        }
    }
}

mavenPublishing {
    configure(
        KotlinMultiplatform(
            javadocJar = JavadocJar.Empty(),
            sourcesJar = true,
        ),
    )
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
    coordinates(
        groupId = project.group.toString(),
        artifactId = "offline-sync",
        version = project.version.toString(),
    )
    pom {
        name.set("offline-sync")
        description.set("Brand-agnostic offline optimistic-write queue skeleton: a pluggable pending-write store, a drain-and-reconcile engine (bounded backoff, transient-halt vs terminal-discard, per-entity reconciliation hints), triggered on network return. Shared across the nl.rhaydus apps.")
        url.set("https://github.com/CinqueIzumi/rhaydus-foundation")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("CinqueIzumi")
                name.set("Cinque")
            }
        }
        scm {
            url.set("https://github.com/CinqueIzumi/rhaydus-foundation")
            connection.set("scm:git:git://github.com/CinqueIzumi/rhaydus-foundation.git")
            developerConnection.set("scm:git:ssh://git@github.com/CinqueIzumi/rhaydus-foundation.git")
        }
    }
}
