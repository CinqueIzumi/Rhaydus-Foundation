package nl.rhaydus.ktlint

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUNCTION_LITERAL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LAMBDA_ARGUMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LAMBDA_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.REFERENCE_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * code-style §Whitespace: a mockk `coEvery { … }` / `every { … }` stub is never a one-liner - open the
 * stub block onto its own line. A single-line `coEvery { … } returns …` hides the arrange step in the
 * noise.
 *
 * Auto-corrected: re-flows the single-line stub lambda onto its own line — a newline (and the body
 * indented one level) after `{`, and the closing `}` back on the call's own indent. The re-flow is a
 * pure whitespace insertion, so the stub body is untouched. Scoped to test source sets, where the mockk
 * stubs live.
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
            "Open the $callee block onto its own line (§Whitespace)",
            true,
        ).ifAutocorrectAllowed {
            reflow(
                node,
                lambda,
            )
        }
    }

    private fun reflow(
        call: ASTNode,
        lambda: ASTNode,
    ) {
        val functionLiteral =
            lambda
                .findChildByType(LAMBDA_EXPRESSION)
                ?.findChildByType(FUNCTION_LITERAL) ?: return
        val open = functionLiteral.findChildByType(LBRACE) ?: return
        val close = functionLiteral.findChildByType(RBRACE) ?: return

        var content = open.treeNext
        while (content != null && content.elementType == WHITE_SPACE) {
            content = content.treeNext
        }
        // Empty lambda (`every { }`) — nothing to re-flow.
        if (content == null || content == close) return

        val baseIndent = call.lineIndent()
        val bodyIndent = "$baseIndent    "

        content.upsertWhitespaceBeforeMe("\n$bodyIndent")
        close.upsertWhitespaceBeforeMe("\n$baseIndent")
    }

    /** Leading whitespace (indentation) of the line on which [this] node starts. */
    private fun ASTNode.lineIndent(): String {
        var leaf = prevLeaf()
        while (leaf != null) {
            val newlineIndex = leaf.text.lastIndexOf('\n')
            if (newlineIndex >= 0) {
                return if (leaf.text.isBlank()) leaf.text.substring(newlineIndex + 1) else ""
            }
            leaf = leaf.prevLeaf()
        }
        return ""
    }
}
