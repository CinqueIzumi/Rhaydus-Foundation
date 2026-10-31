import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    // No version: the Kotlin plugin is on the classpath via the root build's apply-false block, so the
    // version is inherited (declaring one here triggers a classpath-conflict error).
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.vanniktech.mavenPublish)
}

dependencies {
    // KoScope and the declaration types appear in the public signatures.
    api(libs.konsist)

    testImplementation(libs.junit.api)
    testImplementation(libs.kotest)
    testRuntimeOnly(libs.junit.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

// Published as nl.rhaydus:konsist-rules. A Konsist test module (rhaydus.konsist) calls the assertion
// functions with the project facts as arguments.
mavenPublishing {
    configure(
        JavaLibrary(
            javadocJar = JavadocJar.Empty(),
            sourcesJar = true,
        ),
    )
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
    coordinates(
        groupId = project.group.toString(),
        artifactId = "konsist-rules",
        version = project.version.toString(),
    )
    pom {
        name.set("rhaydus konsist-rules")
        description.set(
            "Konsist assertion functions for declaration and architecture rules of the nl.rhaydus apps.",
        )
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
