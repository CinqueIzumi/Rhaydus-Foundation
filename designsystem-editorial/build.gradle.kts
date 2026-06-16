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
        namespace = "nl.rhaydus.designsystem.editorial"
    }
    sourceSets {
        commonMain.dependencies {
            // `api`: editorial public APIs expose core types (the `MaterialTheme.editorialTypography`
            // extension, components taking core models), so consumers need core on their compile classpath.
            api(project(":designsystem-core"))
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
        artifactId = "designsystem-editorial",
        version = project.version.toString(),
    )
    pom {
        name.set("designsystem-editorial")
        description.set("Opt-in editorial design language (shared editorial typography roles and components) layered on designsystem-core. Brand tokens and an app's own type scale stay per app; an app that wants a different design depends on designsystem-core alone.")
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
