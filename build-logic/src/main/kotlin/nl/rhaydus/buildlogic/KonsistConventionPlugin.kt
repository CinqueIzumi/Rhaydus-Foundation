package nl.rhaydus.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/**
 * `rhaydus.konsist`: a plain Kotlin/JVM test module for Konsist architecture tests on JUnit 5.
 *
 * The module also gets the `nl.rhaydus:konsist-rules` assertions, from the consuming catalog's
 * `rhaydus-konsistRules` alias.
 *
 * Konsist resolves the scanned project from the working directory, so the tests run from the root project
 * directory.
 */
class KonsistConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")

        dependencies {
            add(
                "testImplementation",
                libs.library("konsist"),
            )

            add(
                "testImplementation",
                libs.library("rhaydus-konsistRules"),
            )

            add(
                "testImplementation",
                libs.library("junit-api"),
            )

            add(
                "testRuntimeOnly",
                libs.library("junit-engine"),
            )

            add(
                "testRuntimeOnly",
                libs.library("junit-platform-launcher"),
            )
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            workingDir = rootProject.projectDir
        }
    }
}
