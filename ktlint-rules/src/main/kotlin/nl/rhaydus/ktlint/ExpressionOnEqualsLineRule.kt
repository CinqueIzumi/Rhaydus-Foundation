package nl.rhaydus.ktlint

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BINARY_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EQ
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OPERATION_REFERENCE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PROPERTY_ACCESSOR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.firstChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

/**
 * code-style §Argument and Property Layout: a multi-line expression after `=` opens on the `=` line, so
 * `val listed =` / newline / `isListed(` becomes `val listed = isListed(`. This covers a property
 * initializer, a function or accessor expression body, a default parameter value, a named value argument and an
 * assignment.
 *
 * The fix joins the expression onto the `=` line and shifts its continuation lines one indent level left,
 * so the result is right in one pass without relying on `indent`. A single-line expression, an expression
 * whose first line does not end in `(`, `{`, `[` or a lambda parameter arrow, a comment after the `=`, and a join
 * that would pass [MAX_LINE_LENGTH] columns are left alone.
 */
class ExpressionOnEqualsLineRule :
    Rule(
        ruleId = RuleId("rhaydus:expression-on-equals-line"),
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
        if (node.elementType != EQ || node.isInitializerEquals().not()) return

        val anchor = node.treeParent.takeIf { it.elementType == OPERATION_REFERENCE } ?: node

        val whitespace = anchor.treeNext?.takeIf { it.elementType == WHITE_SPACE && it.textContains('\n') } ?: return

        val expression = whitespace.treeNext ?: return

        if (expression.canJoinOnto(node).not()) return

        emit(
            node.startOffset,
            "A multi-line expression after = opens on the = line",
            true,
        )
            .ifAutocorrectAllowed {
                expression.dedentContinuationLines()
                (whitespace as LeafPsiElement).rawReplaceWithText(" ")
            }
    }
}

private fun ASTNode.isInitializerEquals(): Boolean {
    val owner = treeParent ?: return false

    return when (owner.elementType) {
        PROPERTY, PROPERTY_ACCESSOR, FUN, VALUE_ARGUMENT, VALUE_PARAMETER -> true
        OPERATION_REFERENCE -> owner.treeParent?.elementType == BINARY_EXPRESSION
        else -> false
    }
}

private fun ASTNode.canJoinOnto(equals: ASTNode): Boolean {
    if (textContains('\n').not()) return false

    val firstLine = text.substringBefore('\n').trimEnd()

    if (firstLine.lastOrNull() !in OPENERS && firstLine.endsWith("->").not()) return false

    return equals.columnAfter() + 1 + firstLine.length <= MAX_LINE_LENGTH
}

private fun ASTNode.columnAfter(): Int {
    var column = text.length
    var leaf = prevLeaf()

    while (leaf != null) {
        val newlineIndex = leaf.text.lastIndexOf('\n')

        if (newlineIndex >= 0) return column + leaf.text.length - newlineIndex - 1

        column += leaf.text.length
        leaf = leaf.prevLeaf()
    }

    return column
}

private fun ASTNode.dedentContinuationLines() {
    leavesBetween(
        firstChildLeafOrSelf(),
        lastChildLeafOrSelf(),
    )
        .filter { it.elementType == WHITE_SPACE && it.textContains('\n') && it.trailingIndent().startsWith(INDENT) }
        .forEach { (it as LeafPsiElement).rawReplaceWithText(it.text.dropIndent()) }
}

private fun String.dropIndent(): String = substringBeforeLast('\n') + "\n" + substringAfterLast('\n').removePrefix(INDENT)

private val OPENERS = setOf('(', '{', '[')

private const val INDENT = "    "

private const val MAX_LINE_LENGTH = 140
