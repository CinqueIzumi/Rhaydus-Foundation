package nl.rhaydus.detekt

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

/**
 * Registers the custom `rhaydus` detekt ruleset. Discovered via the `META-INF/services` entry so that
 * adding `nl.rhaydus:detekt-rules` to the `detektPlugins` configuration is all a consumer needs.
 */
class RhaydusRuleSetProvider : RuleSetProvider {
    override val ruleSetId = RuleSetId("rhaydus")

    override fun instance(): RuleSet =
        RuleSet(
            ruleSetId,
            listOf(
                ::UnguardedFlowTerminalRead,
                ::NoReflection,
                ::FileLevelDpProperty,
                ::RawSpacingDp,
                ::RawCornerDp,
                ::UiModelBuiltInComposable,
                ::InlineMockkStub,
                ::UseCaseRunCatching,
                ::NoRawLogging,
                ::InlineFullyQualifiedReference,
                ::NestedCommentInKDoc,
                ::TestMissingArrangeActAssert,
            ),
        )
}
