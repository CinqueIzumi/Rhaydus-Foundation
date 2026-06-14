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
}

kotlin {
    jvmToolchain(17)
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

// Repo-wide tasks for linting the FOUNDATION's own sources. `ktlintFormat` autofixes; `ktlintCheck`
// gates. Both drive the custom ruleset via ktlint's rule-engine — no Spotless/ktlint-gradle plugin,
// so the ktlint version stays fully under our control. `-Pktlint.root=<dir>` scopes the scan;
// defaults to the whole repo. Consumers register their own equivalents against the published jar.
val ktlintScanRoot = (project.findProperty("ktlint.root") as String?) ?: rootDir.absolutePath

tasks.register<JavaExec>("ktlintFormat") {
    group = "formatting"
    description = "Auto-wraps multi-arg calls/declarations across the repo (custom ktlint ruleset)."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("nl.rhaydus.ktlint.MainKt")
    args("format", ktlintScanRoot)
}

tasks.register<JavaExec>("ktlintCheck") {
    group = "verification"
    description = "Fails the build on custom-ruleset violations (multi-arg one-per-line wrapping)."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("nl.rhaydus.ktlint.MainKt")
    args("check", ktlintScanRoot)
}
