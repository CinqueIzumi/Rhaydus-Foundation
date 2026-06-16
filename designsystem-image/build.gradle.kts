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
        namespace = "nl.rhaydus.designsystem.image"
    }
    // Opt-in async-image components built on Coil. Isolated here so apps that never show a remote image
    // do not inherit the Coil dependency from designsystem-core. `coil-compose` only - the network fetcher
    // (okhttp/ktor) is an app choice, supplied through the app's Coil ImageLoader. Both deps are
    // `implementation`: no Coil or core type appears in the public API (models are passed as `Any?`), so
    // nothing needs to be on a consumer's compile classpath.
    sourceSets {
        commonMain.dependencies {
            implementation(project(":designsystem-core"))
            implementation(libs.coil.compose)
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
        artifactId = "designsystem-image",
        version = project.version.toString(),
    )
    pom {
        name.set("designsystem-image")
        description.set("Opt-in async-image components (plain, placeholder, shimmer) built on Coil, layered on designsystem-core. Apps that show no remote images depend on designsystem-core alone and avoid the Coil dependency.")
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
