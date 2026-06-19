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
    // detekt-api is provided by the host detekt process at runtime; compile against it only.
    compileOnly(libs.detekt.api)

    testImplementation(libs.detekt.test)
    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    // Type resolution in the rule tests needs the coroutines Flow API on the analysis classpath.
    testImplementation(libs.kotlinx.coroutines.core)
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}

// Repo-wide detekt over the FOUNDATION's own sources, mirroring :ktlint-rules' `ktlintCheck`. Runs the
// detekt CLI syntactically (no type resolution) with the shared baseline config + the custom `rhaydus`
// ruleset (discovered via ServiceLoader from this module's output). The `@RequiresTypeResolution` rule
// is inert on this untyped run and ships for consumers that run type-resolution detekt. `-Pdetekt.root=<dir>`
// scopes the scan; defaults to the whole repo.
val detektCli: Configuration by configurations.creating

dependencies {
    detektCli(libs.detekt.cli)
}

val detektScanRoot = (project.findProperty("detekt.root") as String?) ?: rootDir.absolutePath

tasks.register<JavaExec>("detektCheck") {
    group = "verification"
    description = "Runs the shared detekt baseline + rhaydus ruleset over the repo (syntactic)."
    classpath = detektCli + sourceSets["main"].output
    mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
    args(
        "--input",
        detektScanRoot,
        "--config",
        file("src/main/resources/config/detekt.yml").absolutePath,
        "--build-upon-default-config",
        "--excludes",
        // Exclude generated output, Gradle build scripts, and convention-plugin plumbing - the shared
        // config targets library/app Kotlin source, not build configuration. Test sources are excluded too.
        "**/build/**,**/.gradle/**,**/build-logic/**,**/*.gradle.kts,**/src/*[Tt]est*/**",
    )
}

// Published as nl.rhaydus:detekt-rules. Consumers add it to the `detektPlugins` configuration and point
// detekt at the bundled `config/detekt.yml` (the shared foundation baseline) via `config.from(...)`.
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
        artifactId = "detekt-rules",
        version = project.version.toString(),
    )
    pom {
        name.set("rhaydus detekt-rules")
        description.set("Custom detekt ruleset (type-resolved crash-safety rules) plus the shared detekt baseline config for the nl.rhaydus apps.")
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
