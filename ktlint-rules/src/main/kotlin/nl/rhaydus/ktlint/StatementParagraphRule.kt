package nl.rhaydus.ktlint

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SEMICOLON
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * code-style §Paragraph rule and §Guard clauses: inside a block, a statement that is its own paragraph is
 * separated from its neighbours by a blank line. That covers
 *   - a multi-line statement (call, assignment, `if` / `when` expression, nested block), before and after,
 *   - a guard clause (`if (…) return`, `val x = foo ?: return`, …), before and after,
 *   - a `super.*()` call or an `AppLog.e(...)` error log, after.
 *
 * A leading comment travels with the statement under it, and a trailing same-line comment with the
 * statement it follows. A `// region` / `// endregion` marker boundary is left alone because
 * [RegionCommentFlushRule] keeps those flush against the code.
 */
class StatementParagraphRule :
    Rule(
        ruleId = RuleId("rhaydus:statement-paragraph"),
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

        paragraphs(node)
            .zipWithNext()
            .forEach { (previous, next) ->
                val gap = previous.last().treeNext

                if (gap?.elementType != WHITE_SPACE || gap.newlineCount() != 1) return@forEach

                if (gap.treeNext != next.first()) return@forEach

                if (previous.last().isRegionMarker() || next.first().isRegionMarker()) return@forEach

                val previousStatement = previous.statement()
                val nextStatement = next.statement()
                val needsBlankLine = previousStatement.isAfterSpaced() || previousStatement.isParagraph() || nextStatement.isParagraph()

                if (needsBlankLine.not()) return@forEach

                emit(
                    nextStatement.startOffset,
                    "A multi-line statement, guard clause, super call or error log is its own paragraph - separate it with a blank line",
                    true,
                )
                    .ifAutocorrectAllowed { ensureBlankLineAfter(previous.last()) }
            }
    }

    /**
     * Groups a block's children into units: optional leading comments, one statement, and the comments or
     * semicolons that trail it on the same line.
     */
    private fun paragraphs(block: ASTNode): List<List<ASTNode>> {
        val units = mutableListOf<MutableList<ASTNode>>()
        val pendingComments = mutableListOf<ASTNode>()

        block.children().forEach { child ->
            when {
                child.elementType == WHITE_SPACE || child.isBraceToken() -> Unit

                child.isTrailingOnItsLine() && units.isNotEmpty() && pendingComments.isEmpty() -> units.last() += child

                child.isComment() -> pendingComments += child

                else -> {
                    units += (pendingComments + child).toMutableList()
                    pendingComments.clear()
                }
            }
        }

        if (pendingComments.isNotEmpty()) units += pendingComments.toMutableList()

        return units
    }

    private fun ASTNode.isTrailingOnItsLine(): Boolean {
        val before = treePrev
        val sameLine = before == null || before.elementType != WHITE_SPACE || before.newlineCount() == 0
        val trailingKind = isComment() || elementType == SEMICOLON

        return trailingKind && sameLine
    }

    private fun List<ASTNode>.statement(): ASTNode =
        firstOrNull { it.isComment().not() && it.elementType != SEMICOLON }
            ?: first()

    private fun ASTNode.isParagraph(): Boolean = isComment().not() && (textContains('\n') || isGuardClause())
}
