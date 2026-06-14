import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("rhaydus.kmp.library")
    id("rhaydus.kmp.compose")
    alias(libs.plugins.vanniktech.mavenPublish)
}

kotlin {
    androidLibrary {
        namespace = "nl.rhaydus.designsystem"
    }
    // Compose deps (runtime/foundation/animation/ui/material3-expressive/preview) come from the
    // rhaydus.kmp.compose convention. No brand tokens or app domain modules - that boundary is what
    // keeps this reusable.
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
        artifactId = "designsystem-core",
        version = project.version.toString(),
    )
    pom {
        name.set("designsystem-core")
        description.set("Brand-agnostic Compose design-system skeleton (Material 3 Expressive theme scaffold, layout primitives, modifier helpers, motion seam) shared across the nl.rhaydus apps. Brand tokens stay per app.")
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
