package nl.rhaydus.ktlint

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LAMBDA_ARGUMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.REFERENCE_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * code-style §Whitespace: a mockk `coEvery { … }` / `every { … }` stub is never a one-liner - open the
 * stub block onto its own line (and leave a blank line after each stub's closing `}`). A single-line
 * `coEvery { … } returns …` hides the arrange step in the noise.
 *
 * Detect-only (not auto-corrected): re-flowing the lambda body is a layout change better made by hand.
 * Scoped to test source sets, where the mockk stubs live.
 */
class InlineMockkStubRule :
    Rule(
        ruleId = RuleId("rhaydus:inline-mockk-stub"),
        about = About(
            maintainer = "rhaydus",
            repositoryUrl = "",
            issueTrackerUrl = "",
        ),
    ),
    RuleAutocorrectApproveHandler {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != CALL_EXPRESSION) return

        if (node.isInTestSource().not()) return

        val callee = node.findChildByType(REFERENCE_EXPRESSION)?.text ?: return
        if (callee != "coEvery" && callee != "every") return

        val lambda = node.findChildByType(LAMBDA_ARGUMENT) ?: return
        if (lambda.textContains('\n')) return

        emit(
            lambda.startOffset,
            "Open the $callee block onto its own line, blank line after its closing } (§Whitespace)",
            false,
        )
    }
}
