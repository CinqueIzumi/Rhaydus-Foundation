package nl.rhaydus.buildlogic

import com.autonomousapps.DependencyAnalysisExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `rhaydus.dependency-analysis`: the shared `buildHealth` policy, applied to the root project. It applies
 * `com.autonomousapps.dependency-analysis` to the root and to every subproject, makes each subproject's
 * `check` depend on its `projectHealth` (when it has one), and fails the build on unused dependencies and on
 * wrong `api` / `implementation` exposure.
 *
 * Transitive-completeness advice, runtime-only splitting and redundant-plugin advice are ignored. The
 * exclusion lists cover exactly the dependencies the convention plugins inject into every module, so they
 * never surface as per-module findings. A consuming build adds its own exclusions in a later
 * `dependencyAnalysis { issues { all { … } } }` block.
 *
 * The consuming build must have the dependency-analysis plugin on its classpath (declared `apply false` in
 * the root `plugins { }` block).
 */
class DependencyAnalysisConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        require(target == target.rootProject) {
            "rhaydus.dependency-analysis must be applied to the root project (it configures all subprojects)."
        }

        target.pluginManager.apply(DEPENDENCY_ANALYSIS_ID)

        target.subprojects {
            pluginManager.apply(DEPENDENCY_ANALYSIS_ID)

            // A project with no dependency graph to analyse (a `version-catalog` project) gets no
            // `projectHealth` task, and a `check` dependency on a missing task fails the task graph.
            afterEvaluate {
                if (tasks.names.contains(PROJECT_HEALTH)) {
                    tasks.matching { it.name == "check" }.configureEach {
                        dependsOn(PROJECT_HEALTH)
                    }
                }
            }
        }

        target.extensions.configure(DependencyAnalysisExtension::class.java) {
            issues {
                all {
                    onUnusedDependencies {
                        severity("fail")
                        exclude(*UNUSED_EXCLUSIONS)
                    }
                    onIncorrectConfiguration {
                        severity("fail")
                        exclude(*INCORRECT_CONFIGURATION_EXCLUSIONS)
                    }
                    onUsedTransitiveDependencies {
                        severity("ignore")
                    }
                    onRuntimeOnly {
                        severity("ignore")
                    }
                    onRedundantPlugins {
                        severity("ignore")
                    }
                }
            }
        }
    }

    private companion object {
        const val DEPENDENCY_ANALYSIS_ID = "com.autonomousapps.dependency-analysis"
        const val PROJECT_HEALTH = "projectHealth"

        val UNUSED_EXCLUSIONS = arrayOf(
            // Test bundle (Android/Kmp library convention plugins).
            "org.junit.jupiter:junit-jupiter-api",
            "org.junit.jupiter:junit-jupiter-params",
            "io.kotest:kotest-assertions-core",
            "app.cash.turbine:turbine",
            "org.jetbrains.kotlinx:kotlinx-coroutines-test",
            "io.mockk:mockk",
            // Runtime bundle (the library convention plugins).
            "io.insert-koin:koin-core",
            "io.insert-koin:koin-android",
            "org.jetbrains.kotlinx:kotlinx-coroutines-core",
            "org.jetbrains.kotlinx:kotlinx-coroutines-android",
            // Compose Multiplatform bundle (KmpComposeConventionPlugin commonMain).
            "org.jetbrains.compose.runtime:runtime",
            "org.jetbrains.compose.foundation:foundation",
            "org.jetbrains.compose.animation:animation",
            "org.jetbrains.compose.ui:ui",
            "org.jetbrains.androidx.navigationevent:navigationevent-compose",
            "org.jetbrains.compose.components:components-ui-tooling-preview",
            "org.jetbrains.compose.components:components-resources",
            "org.jetbrains.compose.material3:material3",
            // Added to every Compose module by the stability analyzer (applyComposeStabilityGate).
            "com.github.skydoves:compose-stability-runtime",
            // Android Compose extras (KmpComposeConventionPlugin androidMain).
            "androidx.compose.ui:ui-tooling-preview",
            "androidx.compose.ui:ui-tooling",
            "androidx.activity:activity-compose",
            "androidx.core:core-ktx",
            // Compose desktop host runtimes + Hot Reload, auto-injected for every jvm() target.
            "org.jetbrains.compose.desktop:desktop-jvm-macos-arm64",
            "org.jetbrains.compose.desktop:desktop-jvm-macos-x64",
            "org.jetbrains.compose.desktop:desktop-jvm-linux-x64",
            "org.jetbrains.compose.desktop:desktop-jvm-linux-arm64",
            "org.jetbrains.compose.desktop:desktop-jvm-windows-x64",
            "org.jetbrains.compose.hot-reload:hot-reload-runtime-api",
        )

        val INCORRECT_CONFIGURATION_EXCLUSIONS = arrayOf(
            "io.insert-koin:koin-core",
            "org.jetbrains.kotlinx:kotlinx-coroutines-core",
            "org.jetbrains.compose.components:components-resources",
            "org.jetbrains.compose.material3:material3",
            "androidx.compose.ui:ui",
            "androidx.compose.ui:ui-graphics",
            "androidx.compose.material3:material3",
        )
    }
}
