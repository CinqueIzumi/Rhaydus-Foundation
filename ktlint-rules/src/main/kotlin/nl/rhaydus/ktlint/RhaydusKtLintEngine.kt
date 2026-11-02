package nl.rhaydus.ktlint

import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.RULE_EXECUTION_PROPERTY_TYPE
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.RuleExecution
import com.pinterest.ktlint.ruleset.standard.rules.IndentationRule

private val ruleProviders: Set<RuleProvider> = setOf(
    RuleProvider { MultiArgWrappingRule() },
    RuleProvider { TrailingLambdaBraceRule() },
    RuleProvider { ExpressionOnEqualsLineRule() },
    RuleProvider { TrailingCommaRule() },
    RuleProvider { BooleanNotationRule() },
    RuleProvider { StatementParagraphRule() },
    RuleProvider { RegionCommentFlushRule() },
    RuleProvider { BraceBlankLineRule() },
    RuleProvider { SiblingComposableBlankLineRule() },
    RuleProvider { ImportLayoutRule() },
    RuleProvider { NoUnusedImportsRule() },
    RuleProvider { IndentationRule() },
)

private val indentExecutionProperty = EditorConfigProperty(
    type = RULE_EXECUTION_PROPERTY_TYPE,
    defaultValue = RuleExecution.enabled,
    name = "ktlint_standard_indent",
)

internal fun rhaydusKtLintEngine(): KtLintRuleEngine = KtLintRuleEngine(
    ruleProviders = ruleProviders,
    editorConfigOverride = EditorConfigOverride.from(
        indentExecutionProperty to RuleExecution.enabled,
        INDENT_STYLE_PROPERTY to IndentConfig.IndentStyle.SPACE,
        INDENT_SIZE_PROPERTY to 4,
    ),
)
