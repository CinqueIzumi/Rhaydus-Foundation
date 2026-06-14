import com.vanniktech.maven.publish.SonatypeHost
import com.vanniktech.maven.publish.VersionCatalog

plugins {
    `version-catalog`
    alias(libs.plugins.vanniktech.mavenPublish)
}

// Publishes the foundation version catalog as nl.rhaydus:catalog. Consumers wire it in their own
// settings with: versionCatalogs { create("libs") { from("nl.rhaydus:catalog:<v>") } }, giving both
// apps one source of truth for shared dependency versions.
catalog {
    versionCatalog {
        from(files(rootDir.resolve("gradle/libs.versions.toml")))
    }
}

mavenPublishing {
    configure(VersionCatalog())
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
    coordinates(
        groupId = project.group.toString(),
        artifactId = "catalog",
        version = project.version.toString(),
    )
    pom {
        name.set("rhaydus catalog")
        description.set("Shared Gradle version catalog for the nl.rhaydus apps.")
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
