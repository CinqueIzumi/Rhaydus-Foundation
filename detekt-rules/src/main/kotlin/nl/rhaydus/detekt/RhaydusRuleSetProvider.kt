package nl.rhaydus.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

/**
 * Registers the custom `rhaydus` detekt ruleset. Discovered via the `META-INF/services` entry so that
 * adding `nl.rhaydus:detekt-rules` to the `detektPlugins` configuration is all a consumer needs.
 */
class RhaydusRuleSetProvider : RuleSetProvider {
    override val ruleSetId = "rhaydus"

    override fun instance(config: Config): RuleSet =
        RuleSet(
            ruleSetId,
            listOf(
                UnguardedFlowTerminalReadRule(config),
            ),
        )
}
