package nl.rhaydus.buildlogic

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency

/**
 * `rhaydus.module-graph`: a build-time gate on the module-dependency DAG, applied to the root project. It
 * fails the build on any project dependency that breaks the tier rules configured via [ModuleGraphExtension]
 * (`moduleGraph { … }`) — replacing manual import audits with a gate wired into every module's `check`.
 *
 * Checks per project edge:
 * 1. **Tier legality** — a module may depend only on a tier listed in its `allowedTargetTiers` entry.
 * 2. **api-visibility** — an `api` edge to a `dataAreaModules` member must be allowlisted in
 *    `allowedApiDataEdges`, so re-exporting a data/use-case layer is always a deliberate, reviewed decision
 *    rather than a silent leak. (Inert when `dataAreaModules` is empty.)
 * 3. **Explicit allowlist** - a module in `allowedProjects` may depend only on the listed projects (none, when
 *    the set is empty).
 * 4. **Banned edges** - a pair in `bannedEdges` is always a violation.
 *
 * Per external dependency, `bannedExternalGroups` forbids a module from declaring the listed Maven groups in
 * any configuration, except the `ignoredExternalCoordinates`.
 *
 * Edges are snapshotted as plain data in `projectsEvaluated`, so the task action reads no live project state.
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

        // Edges are snapshotted as plain data once every project is evaluated, so the task body never touches
        // `Project` / `Configuration` state.
        val projectEdges = mutableListOf<ProjectEdge>()
        val externalEdges = mutableListOf<ExternalEdge>()

        target.gradle.projectsEvaluated {
            target.subprojects.forEach { module ->
                module.configurations.forEach forEachConfiguration@{ configuration ->
                    // The Kotlin plugin aggregates every project into its SwiftPM lock-file classpath.
                    if (configuration.name.startsWith("swiftPMDependencies")) return@forEachConfiguration

                    configuration.dependencies.forEach { dependency ->
                        when (dependency) {
                            is ProjectDependency ->
                                projectEdges += ProjectEdge(
                                    module.path,
                                    dependency.path,
                                    configuration.name,
                                )

                            is ExternalModuleDependency ->
                                externalEdges += ExternalEdge(
                                    from = module.path,
                                    configurationName = configuration.name,
                                    group = dependency.group.orEmpty(),
                                    name = dependency.name,
                                )
                        }
                    }
                }
            }
        }

        val checkModuleGraph = target.tasks.register("checkModuleGraph") {
            group = "verification"
            description = "Fails on any module dependency that breaks the configured module graph rules."

            doLast {
                val violations = mutableListOf<String>()
                var edges = 0

                projectEdges.forEach edge@{ (fromPath, toPath, configurationName) ->
                    if (toPath == fromPath) return@edge

                    val fromTier = extension.tierOf(fromPath) ?: return@edge

                    val targetTier = extension.tierOf(toPath) ?: return@edge

                    edges++

                    val allowedTiers = extension.allowedTargetTiers[fromTier].orEmpty()

                    if (targetTier !in allowedTiers) {
                        violations += "$fromPath → $toPath  ($fromTier may depend only on $allowedTiers)"
                    }

                    // api-visibility: a declared `api` edge to a data-area module must be allowlisted. Match
                    // every declarable api bucket - plain `api` and the KMP/variant `<sourceSet>Api` configs
                    // (`commonMainApi`, …) - but never the test ones, where an api edge can't leak into a
                    // production consumer's graph.
                    val isApiEdge = configurationName.endsWith(
                        "api",
                        ignoreCase = true,
                    ) &&
                        configurationName.contains(
                            "test",
                            ignoreCase = true,
                        ).not()

                    if (
                        isApiEdge &&
                        toPath in extension.dataAreaModules &&
                        (fromPath to toPath) !in extension.allowedApiDataEdges
                    ) {
                        violations += "$fromPath → api($toPath)  " +
                            "(data modules must be implementation-depended unless allowlisted)"
                    }

                    if ((fromPath to toPath) in extension.bannedEdges) {
                        violations += "$fromPath → $toPath  (wrong direction)"
                    }

                    val allowedTargets = extension.allowedProjects[fromPath]

                    if (allowedTargets != null && toPath !in allowedTargets) {
                        violations += if (allowedTargets.isEmpty()) {
                            "$fromPath → $toPath  ($fromPath must declare no project dependency)"
                        } else {
                            "$fromPath → $toPath  ($fromPath may depend only on $allowedTargets)"
                        }
                    }
                }

                // Deliberately not restricted to non-test configurations, unlike the api-visibility rule:
                // that one is about leakage into consumers, this one about what the module itself depends on.
                externalEdges.forEach external@{ (fromPath, _, group, name) ->
                    if (extension.tierOf(fromPath) == null) return@external

                    val bannedGroups = extension.bannedExternalGroups[fromPath] ?: return@external

                    val coordinate = "$group:$name"

                    if (coordinate in extension.ignoredExternalCoordinates) return@external

                    if (group in bannedGroups) {
                        violations += "$fromPath → $coordinate  ($fromPath may not depend on group $group)"
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
