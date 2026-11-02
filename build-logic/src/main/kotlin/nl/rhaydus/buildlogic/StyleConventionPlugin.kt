package nl.rhaydus.buildlogic

import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.process.CommandLineArgumentProvider
import java.util.concurrent.Callable

/**
 * `rhaydus.style`: the shared style gates, applied to the root project. It
 * - applies `dev.detekt` to every subproject, layering the project's config files over the baseline that
 *   ships inside the `detekt-rules` jar, and adds the custom `rhaydus` ruleset to `detektPlugins`;
 * - creates the type-resolved detekt task set and points the KMP compilation tasks at hand-written sources,
 *   so generated code is never analysed;
 * - registers `ktlintCheck` and `ktlintFormat` over the `ktlint-rules` jar;
 * - registers `styleCheck`, and puts `ktlintCheck` and the type-resolved detekt tasks into every `check`.
 *
 * Only the type-resolved tasks can run `rhaydus:UnguardedFlowTerminalRead`, so the source-only `detekt` task
 * is disabled and a `detektMain` lifecycle that fans out to nothing fails instead of passing vacuously.
 *
 * The consuming build must declare `dev.detekt` `apply false` in its root `plugins { }` block and fill in
 * [StyleExtension] (`rhaydusStyle { … }`).
 */
class StyleConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        require(target == target.rootProject) {
            "rhaydus.style must be applied to the root project (it configures all subprojects)."
        }

        val style = target.extensions.create(
            "rhaydusStyle",
            StyleExtension::class.java,
        )

        val detektConfig = target.configurations.create(DETEKT_CONFIG) { isTransitive = false }
        val ktlintRules = target.configurations.create(KTLINT_RULES)

        detektConfig.dependencies.addLater(
            target.provider {
                target.rulesDependency(
                    style.detektRules,
                    "detektRules",
                )
            },
        )

        ktlintRules.dependencies.addLater(
            target.provider {
                target.rulesDependency(
                    style.ktlintRules,
                    "ktlintRules",
                )
            },
        )

        val extractDetektConfig = target.tasks.register<Sync>("extractRhaydusDetektConfig") {
            group = "verification"
            description = "Unpacks the shared nl.rhaydus detekt baseline from the detekt-rules jar."

            // Derived from `elements`, not `.singleFile`: the Provider carries the task dependencies that
            // build the jar, so editing the baseline in an included build re-runs this Sync instead of
            // silently extracting a stale config.
            from(detektConfig.elements.map { jars -> jars.map { target.zipTree(it.asFile) } }) {
                include("config/detekt.yml")
            }

            into(target.layout.buildDirectory.dir("rhaydus-detekt"))
        }

        registerKtlint(
            target,
            style,
            ktlintRules,
        )

        target.tasks.register("styleCheck") {
            group = "verification"
            description = "Runs ktlintCheck and type-resolved detekt (shared baseline + rhaydus ruleset) across all modules."

            dependsOn(target.tasks.named("ktlintCheck"))

            dependsOn(
                Callable {
                    target.subprojects.map { sub -> sub.tasks.matching { isTypeResolved(it) } }
                },
            )

            dependsOn(
                Callable {
                    target.subprojects.map { sub -> sub.tasks.matching { it.name in style.extraStyleTasks } }
                },
            )
        }

        // Deferred so the root build script has filled in the extension before any module is wired.
        target.afterEvaluate {
            subprojects {
                configureModule(
                    this,
                    style,
                    extractDetektConfig,
                )
            }
        }
    }

    private fun registerKtlint(
        target: Project,
        style: StyleExtension,
        ktlintRules: org.gradle.api.artifacts.Configuration,
    ) {
        val scanRoot = target.provider {
            (target.findProperty("ktlint.root") as String?)
                ?: (style.ktlintScanRoot ?: target.rootDir).absolutePath
        }

        target.tasks.register<JavaExec>("ktlintFormat") {
            group = "formatting"
            description = "Auto-fixes the mechanizable layout rules across the repo (custom ktlint ruleset)."
            classpath = ktlintRules
            mainClass.set(KTLINT_MAIN)
            argumentProviders.add(CommandLineArgumentProvider { listOf("format", scanRoot.get()) })
        }

        target.tasks.register<JavaExec>("ktlintCheck") {
            group = "verification"
            description = "Fails the build on custom-ruleset violations (layout and structural rules)."
            classpath = ktlintRules
            mainClass.set(KTLINT_MAIN)
            argumentProviders.add(CommandLineArgumentProvider { listOf("check", scanRoot.get()) })
        }
    }

    private fun configureModule(
        module: Project,
        style: StyleExtension,
        extractDetektConfig: org.gradle.api.tasks.TaskProvider<Sync>,
    ) {
        with(module) {
            pluginManager.apply("dev.detekt")

            val isRuleProject = path in style.detektRuleProjects

            // A module that produces the shared ruleset cannot analyse itself with the jar it produces.
            if ((style.detektRules as? ProjectDependency)?.path != path) {
                configurations.getByName("detektPlugins").dependencies.addLater(
                    provider {
                        rulesDependency(
                            style.detektRules,
                            "detektRules",
                        )
                    },
                )
            }

            if (isRuleProject.not()) {
                style.detektRuleProjects.forEach {
                    dependencies.add(
                        "detektPlugins",
                        dependencies.project(mapOf("path" to it)),
                    )
                }
            }

            extensions.configure<DetektExtension> {
                buildUponDefaultConfig.set(true)

                // Layered, later wins: the shared baseline, then the project's own deltas.
                config.setFrom(
                    listOf(rootProject.layout.buildDirectory.file("rhaydus-detekt/config/detekt.yml")) +
                        style.detektConfigFiles +
                        style.appDetektConfigFiles.takeIf { isRuleProject.not() }.orEmpty(),
                )

                parallel.set(true)
            }

            tasks.withType<Detekt>().configureEach {
                dependsOn(extractDetektConfig)

                reports {
                    html.required.set(true)
                    checkstyle.required.set(true)
                    markdown.required.set(false)
                    sarif.required.set(false)
                }
            }

            // Each KMP compilation task is pointed at the HAND-WRITTEN sources it belongs to, keeping the
            // compilation's classpath (what type resolution needs) untouched. The compilation's source sets
            // also carry GENERATED code (KSP, Apollo, Compose resource accessors) that is neither owned nor
            // fixable. `commonMain` is analysed once per compilation so platform files see its declarations.
            afterEvaluate {
                tasks.withType<Detekt>().configureEach {
                    when (name) {
                        "detektMainAndroid" -> setSource(
                            files(
                                "src/commonMain/kotlin",
                                "src/mobileMain/kotlin",
                                "src/androidMain/kotlin",
                            ),
                        )

                        "detektMainJvm" -> setSource(
                            files(
                                "src/commonMain/kotlin",
                                "src/jvmMain/kotlin",
                            ),
                        )

                        "detektHostTestAndroid" -> setSource(
                            files(
                                "src/commonTest/kotlin",
                                "src/androidHostTest/kotlin",
                            ),
                        )
                    }
                }
            }

            // A lifecycle `detektMain` that fans out to nothing would pass green with nothing scanned, so a
            // detekt/AGP change that stops registering the variant tasks must fail here instead.
            tasks.matching { it.name == "detektMain" && it !is Detekt }.configureEach {
                doFirst {
                    val variantTasks = taskDependencies.getDependencies(this).filterIsInstance<Detekt>()

                    check(variantTasks.isNotEmpty()) {
                        "No type-resolved detekt<Variant> task is registered for $path; " +
                            "the detekt gate would pass vacuously."
                    }
                }
            }

            // The source-only `detekt` task parses without a classpath, so every type-resolved rule is inert
            // on it and its findings are a strict subset of the type-resolved tasks'. Disabled so it neither
            // double-reports nor lends a false sense of coverage.
            tasks.matching { it.name == "detekt" }.configureEach {
                enabled = false
            }

            tasks.matching { it.name == "check" }.configureEach {
                dependsOn(tasks.matching { isTypeResolved(it) })
                dependsOn(rootProject.tasks.named("ktlintCheck"))
            }
        }
    }

    /** Turns the extension's notation into a dependency of this project, failing loudly when unset. */
    private fun Project.rulesDependency(
        notation: Any?,
        property: String,
    ): Dependency {
        val value = checkNotNull(notation) { "rhaydus.style: rhaydusStyle { $property } must be set." }

        return when (value) {
            // A ProjectDependency belongs to the project that created it; re-resolve it from this project.
            is ProjectDependency -> dependencies.project(mapOf("path" to value.path))
            is Provider<*> -> dependencies.create(checkNotNull(value.get()))
            else -> dependencies.create(value)
        }
    }

    /**
     * True for the type-resolved detekt tasks. `detektTest` counts only on a plain `kotlin("jvm")` module,
     * where it is the `test` source set; elsewhere it is a lifecycle over variant tasks that the Android and
     * KMP wiring already covers or deliberately leaves out.
     */
    private fun isTypeResolved(task: Task): Boolean {
        return task.name in TYPE_RESOLVED_TASKS ||
            (task.name == "detektTest" && task.project.pluginManager.hasPlugin(KOTLIN_JVM_PLUGIN))
    }

    private companion object {
        const val KOTLIN_JVM_PLUGIN = "org.jetbrains.kotlin.jvm"
        const val DETEKT_CONFIG = "rhaydusDetektConfig"
        const val KTLINT_RULES = "ktlintRules"
        const val KTLINT_MAIN = "nl.rhaydus.ktlint.MainKt"

        // The detekt tasks that carry TYPE RESOLUTION, by module shape: KMP libraries get one task per
        // compilation, the Android application gets a `detektMain` lifecycle over its variants, and plain
        // JVM modules get `detektMain` for the main compilation and `detektTest` for the `test` source set.
        val TYPE_RESOLVED_TASKS = setOf(
            "detektMainAndroid",
            "detektMainJvm",
            "detektMain",
            "detektHostTestAndroid",
        )
    }
}
