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
    implementation(libs.ktlint.ruleEngine)
    api(libs.ktlint.ruleEngineCore)

    // ktlint's rule-engine logs via slf4j; provide the api + a no-op binding at runtime
    implementation("org.slf4j:slf4j-api:2.0.16")
    runtimeOnly("org.slf4j:slf4j-nop:2.0.16")

    testImplementation(libs.ktlint.test)
    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}

// Published as nl.rhaydus:ktlint-rules. Consumers resolve this jar onto a JavaExec classpath and run
// `nl.rhaydus.ktlint.MainKt` against their own source (see README / the convention plugin in phase 2).
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
        artifactId = "ktlint-rules",
        version = project.version.toString(),
    )
    pom {
        name.set("rhaydus ktlint-rules")
        description.set("Custom ktlint ruleset (multi-arg wrapping, blank-line, visibility, etc.) shared across the nl.rhaydus apps.")
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
