package nl.rhaydus.buildlogic

import com.autonomousapps.DependencyAnalysisExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.withType

/**
 * `rhaydus.konsist`: a plain Kotlin/JVM test module for Konsist architecture tests on JUnit 5.
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

        // The module declares the architecture-test stack before any test exists, so the unused-dependency
        // advice would fail `check` on an empty module.
        rootProject.extensions.findByType<DependencyAnalysisExtension>()?.issues {
            project(path) {
                onUnusedDependencies {
                    exclude(KONSIST_COORDINATE)
                }
            }
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            workingDir = rootProject.projectDir
        }
    }

    private companion object {
        const val KONSIST_COORDINATE = "com.lemonappdev:konsist"
    }
}
