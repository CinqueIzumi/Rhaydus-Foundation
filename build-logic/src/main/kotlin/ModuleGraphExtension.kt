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
 */
open class ModuleGraphExtension {
    var tierOf: (String) -> String? = { null }

    var allowedTargetTiers: Map<String, Set<String>> = emptyMap()

    var dataAreaModules: Set<String> = emptySet()

    var allowedApiDataEdges: Set<Pair<String, String>> = emptySet()
}
