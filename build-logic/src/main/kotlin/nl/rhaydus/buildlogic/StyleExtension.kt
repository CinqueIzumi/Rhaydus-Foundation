package nl.rhaydus.buildlogic

import java.io.File

/**
 * Configuration for the `rhaydus.style` gates ([StyleConventionPlugin]). The mechanism is shared; each build
 * supplies where its rule jars come from and its own deltas on top of the shared baseline.
 *
 * - [detektRules] and [ktlintRules] are dependency notations for the `detekt-rules` and `ktlint-rules`
 *   artifacts: a version catalog entry or a `project(...)`. Required.
 * - [detektConfigFiles] layer, in order, over the shared detekt baseline in every module.
 * - [appDetektConfigFiles] layer after [detektConfigFiles] in every module except the [detektRuleProjects].
 * - [detektRuleProjects] are project paths that produce detekt rules. Each is added to the `detektPlugins` of
 *   every other module, and is exempt from [appDetektConfigFiles]; a module cannot analyse itself with the
 *   jar it produces.
 * - [extraStyleTasks] are task names, looked up in every module, that `styleCheck` also runs.
 * - [ktlintScanRoot] is the directory ktlint scans; `-Pktlint.root=<dir>` overrides it, and `null` means the
 *   root project directory.
 */
open class StyleExtension {
    var detektRules: Any? = null

    var ktlintRules: Any? = null

    var detektConfigFiles: List<File> = emptyList()

    var appDetektConfigFiles: List<File> = emptyList()

    var detektRuleProjects: Set<String> = emptySet()

    var extraStyleTasks: Set<String> = emptySet()

    var ktlintScanRoot: File? = null
}
