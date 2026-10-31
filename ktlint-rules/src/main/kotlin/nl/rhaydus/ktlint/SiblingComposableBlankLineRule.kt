package nl.rhaydus.ktlint

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LAMBDA_ARGUMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.REFERENCE_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * code-style §Compose: anywhere inside a layout scope (`Column`, `Row`, `Box`, …), at any depth, leave a
 * blank line between each pair of child composable calls, including `Spacer`. That reaches the blocks nested
 * under the scope: a content lambda of another composable, an `if` / `when` branch, an `items { }` body.
 *
 * Conservative by design: it only fires inside a lambda that sits under a known layout composable, and only
 * between two statements that are both composable calls (PascalCase callee). That avoids guessing at
 * arbitrary `@Composable content` lambdas without type resolution.
 */
class SiblingComposableBlankLineRule :
    Rule(
        ruleId = RuleId("rhaydus:sibling-composable-blank-line"),
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
        if (node.elementType != BLOCK) return

        if (isInsideLayoutScope(node).not()) return

        val statements = node.children().filter { it.elementType != WHITE_SPACE }.toList()

        statements
            .zipWithNext()
            .filter { (current, next) -> isComposableCall(current) && isComposableCall(next) }
            .filter { (current, _) -> current.treeNext?.elementType == WHITE_SPACE }
            .filter { (current, _) -> (current.treeNext?.newlineCount() ?: 0) == 1 }
            .forEach { (current, _) ->
                emit(
                    current.startOffset,
                    "Blank line required between sibling composables",
                    true,
                )
                    .ifAutocorrectAllowed { ensureBlankLineAfter(current) }
            }
    }

    /** True when a lambda argument of a layout composable encloses [block], however deeply. */
    private fun isInsideLayoutScope(block: ASTNode): Boolean {
        var ancestor = block.treeParent

        while (ancestor != null && ancestor.elementType != FUN) {
            if (ancestor.elementType == LAMBDA_ARGUMENT) {
                val callee = ancestor.treeParent?.children()?.firstOrNull { it.elementType == REFERENCE_EXPRESSION }?.text

                if (callee != null && callee in LAYOUT_SCOPES) return true
            }

            ancestor = ancestor.treeParent
        }

        return false
    }

    private fun isComposableCall(node: ASTNode): Boolean {
        if (node.elementType != CALL_EXPRESSION) return false

        val callee = node.children().firstOrNull { it.elementType == REFERENCE_EXPRESSION }?.text ?: return false

        return callee.isNotEmpty() && callee.first().isUpperCase()
    }

    private companion object {
        val LAYOUT_SCOPES =
            setOf(
                "Column", "Row", "Box", "BoxWithConstraints",
                "LazyColumn", "LazyRow", "LazyVerticalGrid", "LazyHorizontalGrid",
                "FlowRow", "FlowColumn",
            )
    }
}
