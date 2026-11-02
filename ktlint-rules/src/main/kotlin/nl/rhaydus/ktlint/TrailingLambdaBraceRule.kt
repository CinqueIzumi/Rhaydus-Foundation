package nl.rhaydus.ktlint

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUNCTION_LITERAL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.firstChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * code-style §Argument and Property Layout: when a lambda's body (trailing or passed as a value) spans more
 * than one line, its opening `{` and closing `}` each sit on their own line - never `) }` or `{ call(`. A
 * single-line body stays inline.
 *
 * The fix moves the body onto its own lines and shifts its continuation lines one indent level.
 * [MultiArgWrappingRule] calls [expandLambdaBraces] after wrapping a call inside a lambda, so the two
 * rules reach the same layout in one pass and never undo each other's edits.
 */
class TrailingLambdaBraceRule :
    Rule(
        ruleId = RuleId("rhaydus:trailing-lambda-brace"),
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
        if (node.elementType != FUNCTION_LITERAL) return

        if (node.needsBraceExpansion().not()) return

        emit(
            node.startOffset,
            "A multi-line lambda body puts its { and } on their own lines",
            true,
        )
            .ifAutocorrectAllowed { expandLambdaBraces(node) }
    }
}

private fun ASTNode.bodyLeaves(): Pair<ASTNode, ASTNode>? {
    val block = children().firstOrNull { it.elementType == BLOCK } ?: return null

    var first: ASTNode? = block.firstChildLeafOrSelf()
    while (first != null && first.elementType == WHITE_SPACE && first.isInside(block)) first = first.nextLeaf()

    var last: ASTNode? = block.lastChildLeafOrSelf()
    while (last != null && last.elementType == WHITE_SPACE && last.isInside(block)) last = last.prevLeaf()

    if (first == null || last == null) return null

    if (first.isBodyLeaf(block).not() || last.isBodyLeaf(block).not()) return null

    return first to last
}

private fun ASTNode.isBodyLeaf(block: ASTNode): Boolean = elementType != WHITE_SPACE && isInside(block)

private fun ASTNode.isInside(ancestor: ASTNode): Boolean {
    var current: ASTNode? = this

    while (current != null) {
        if (current == ancestor) return true

        current = current.treeParent
    }

    return false
}

private fun ASTNode.isNewlineWhitespace(): Boolean = elementType == WHITE_SPACE && textContains('\n')

private fun ASTNode.bodySpansLines(): Boolean {
    val (first, last) = bodyLeaves() ?: return false
    var leaf: ASTNode? = first

    while (leaf != null) {
        if (leaf.textContains('\n')) return true

        if (leaf == last) return false

        leaf = leaf.nextLeaf()
    }

    return false
}

private fun ASTNode.needsBraceExpansion(): Boolean {
    if (bodySpansLines().not()) return false

    val (first, last) = bodyLeaves() ?: return false

    return first.prevLeaf()?.isNewlineWhitespace() != true || last.nextLeaf()?.isNewlineWhitespace() != true
}

internal fun expandLambdaBraces(literal: ASTNode) {
    if (literal.needsBraceExpansion().not()) return

    val (first, last) = literal.bodyLeaves() ?: return

    val rbrace = literal.children().lastOrNull { it.elementType == RBRACE } ?: return

    val baseIndent = literal.lineIndent()
    val opensInline = first.prevLeaf()?.isNewlineWhitespace() != true
    val closesInline = last.nextLeaf()?.isNewlineWhitespace() != true

    if (opensInline) {
        if (first != last) leavesBetween(
            first,
            last,
        ).indentLineBreaks(INDENT)

        first.upsertWhitespaceBeforeMe("\n$baseIndent$INDENT")
    }

    if (closesInline) rbrace.upsertWhitespaceBeforeMe("\n$baseIndent")
}

private const val INDENT = "    "
