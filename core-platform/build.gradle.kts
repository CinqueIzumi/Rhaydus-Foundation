import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("rhaydus.kmp.library")
    alias(libs.plugins.vanniktech.mavenPublish)
}

kotlin {
    androidLibrary {
        namespace = "nl.rhaydus.platform"
    }

    sourceSets {
        commonMain.dependencies {
            // AppDispatchers / AppLog / runCatchingCancellable appear in the public seam surface, so api.
            api(project(":core-common"))
        }

        jvmMain.dependencies {
            // KSafe backs the desktop SecureStorage actual (OS secret store custody).
            implementation(libs.ksafe)
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
        artifactId = "core-platform",
        version = project.version.toString(),
    )
    pom {
        name.set("core-platform")
        description.set("Brand-agnostic platform-capability seams (secure secret storage, network availability) with per-target implementations, shared across the nl.rhaydus apps.")
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
