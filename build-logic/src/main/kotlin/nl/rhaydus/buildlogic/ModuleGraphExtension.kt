package nl.rhaydus.buildlogic

/**
 * Configuration for the `rhaydus.module-graph` gate ([ModuleGraphConventionPlugin]). Each consuming build
 * fills these in with its own module layout; the enforcement mechanism (the `checkModuleGraph` task) is
 * shared.
 *
 * - [tierOf] maps a Gradle module path (`":core-common"`, `":feature:explore"`, …) to its tier name, or
 *   `null` to exclude the module from the check (e.g. build tooling).
 * - [allowedTargetTiers] is the tier DAG: for each tier, the set of tiers it may depend on. An edge whose
 *   target tier is not in the source tier's set is a violation.
 * - [dataAreaModules] are modules whose whole data/use-case layer must not be re-exported: an `api` edge to
 *   one is a violation unless the `(source → target)` pair is in [allowedApiDataEdges]. Leave both empty to
 *   disable the api-visibility half of the check.
 * - [allowedProjects] is an explicit per-module allowlist of project dependencies, tighter than the tier
 *   rule. An empty set means the module may declare no project dependency at all.
 * - [bannedEdges] are `(from → to)` project edges that are always a violation, even when the tiers allow
 *   them.
 * - [bannedExternalGroups] lists, per module, the Maven groups it may not declare in any configuration. Test
 *   configurations are included on purpose: the ban is about what the module depends on, not about leakage
 *   into consumers.
 * - [ignoredExternalCoordinates] are `group:name` coordinates that [bannedExternalGroups] skips because the
 *   convention plugins inject them into every module.
 */
open class ModuleGraphExtension {
    var tierOf: (String) -> String? = { null }

    var allowedTargetTiers: Map<String, Set<String>> = emptyMap()

    var dataAreaModules: Set<String> = emptySet()

    var allowedApiDataEdges: Set<Pair<String, String>> = emptySet()

    var allowedProjects: Map<String, Set<String>> = emptyMap()

    var bannedEdges: Set<Pair<String, String>> = emptySet()

    var bannedExternalGroups: Map<String, Set<String>> = emptyMap()

    var ignoredExternalCoordinates: Set<String> = emptySet()
}
