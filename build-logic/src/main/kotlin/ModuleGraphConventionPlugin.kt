import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

/**
 * `rhaydus.module-graph`: a build-time gate on the module-dependency DAG, applied to the root project. It
 * fails the build on any project dependency that breaks the tier rules configured via [ModuleGraphExtension]
 * (`moduleGraph { … }`) — replacing manual import audits with a gate wired into every module's `check`.
 *
 * Two checks per edge:
 * 1. **Tier legality** — a module may depend only on a tier listed in its `allowedTargetTiers` entry.
 * 2. **api-visibility** — an `api` edge to a `dataAreaModules` member must be allowlisted in
 *    `allowedApiDataEdges`, so re-exporting a data/use-case layer is always a deliberate, reviewed decision
 *    rather than a silent leak. (Inert when `dataAreaModules` is empty.)
 *
 * The mechanism is brand-agnostic; the concrete tiers, module→tier mapping, and allowlists are the
 * consuming build's data (see the `moduleGraph { }` block in its root build script).
 */
class ModuleGraphConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        require(target == target.rootProject) {
            "rhaydus.module-graph must be applied to the root project (it inspects all subprojects)."
        }

        val extension = target.extensions.create(
            "moduleGraph",
            ModuleGraphExtension::class.java,
        )

        val checkModuleGraph = target.tasks.register("checkModuleGraph") {
            group = "verification"
            description = "Fails on any module dependency that breaks the configured tier DAG."

            // Note: this doLast reads the live `subprojects` / `Configuration` graph at execution time, so
            // it is not configuration-cache-safe (unlike verifyPluginVersion, which snapshots plain values).
            // Config cache is not enabled in this build; a config-cache-safe rewrite would need to serialize
            // the declared edges — and the `moduleGraph` extension's `tierOf` lambda — which is deferred
            // until config cache is actually adopted.
            doLast {
                val violations = mutableListOf<String>()
                var edges = 0

                target.subprojects.forEach { module ->
                    val fromTier = extension.tierOf(module.path) ?: return@forEach
                    val allowed = extension.allowedTargetTiers[fromTier].orEmpty()

                    module.configurations.forEach { configuration ->
                        configuration.dependencies
                            .filterIsInstance<ProjectDependency>()
                            .forEach forEachDependency@{ dependency ->
                                if (dependency.path == module.path) return@forEachDependency

                                val targetTier = extension.tierOf(dependency.path) ?: return@forEachDependency
                                edges++

                                if (targetTier !in allowed) {
                                    violations += "${module.path} → ${dependency.path}  " +
                                        "($fromTier may depend only on $allowed)"
                                }

                                // api-visibility: a declared `api` edge to a data-area module must be
                                // allowlisted. Match every declarable api bucket - plain `api` and the
                                // KMP/variant `<sourceSet>Api` configs (`commonMainApi`, …) - but never the
                                // test ones, where an api edge can't leak into a production consumer's graph.
                                val configName = configuration.name.lowercase()
                                val isApiEdge = configName.endsWith("api") && configName.contains("test").not()

                                if (
                                    isApiEdge &&
                                    dependency.path in extension.dataAreaModules &&
                                    (module.path to dependency.path) !in extension.allowedApiDataEdges
                                ) {
                                    violations += "${module.path} → api(${dependency.path})  " +
                                        "(data modules must be implementation-depended unless allowlisted)"
                                }
                            }
                    }
                }

                if (violations.isNotEmpty()) {
                    throw GradleException(
                        "Illegal module dependencies:\n" +
                            violations.distinct().sorted().joinToString("\n") { "  - $it" },
                    )
                }

                logger.lifecycle("checkModuleGraph: $edges project dependencies validated, DAG intact.")
            }
        }

        target.subprojects {
            tasks.matching { it.name == "check" }.configureEach {
                dependsOn(checkModuleGraph)
            }
        }
    }
}
