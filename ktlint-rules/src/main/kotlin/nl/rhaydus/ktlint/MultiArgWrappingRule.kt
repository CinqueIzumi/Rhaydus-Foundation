package nl.rhaydus.ktlint

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.COMMA
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUNCTION_LITERAL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LAMBDA_ARGUMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PRIMARY_CONSTRUCTOR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.REFERENCE_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SECONDARY_CONSTRUCTOR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

/**
 * Enforces CODE_STYLE_GUIDE §Argument and Property Layout: a value-argument list (call sites,
 * constructor invocations, `.copy(...)`) or a function/constructor value-parameter list with 2+
 * entries breaks one-per-line with a trailing comma — even when it would fit on one line.
 *
 * Only single-line lists are touched (lists already spanning multiple lines are left as-is), and the
 * documented exemptions are honoured so it matches the team's existing scope:
 *   - collection / vararg factories (`listOf`, `setOf`, `mapOf`, `arrayOf`, …) stay inline,
 *   - calls with a trailing lambda (`LaunchedEffect(a, b) { … }`) stay inline.
 *
 * Wrapping a list also wraps every single-line argument or parameter list that encloses it, so a nested
 * call never leaves its closer hugging the outer one (`),)`), and expands the braces of any enclosing
 * lambda so the wrapped call never ends in `) }` (see [TrailingLambdaBraceRule]).
 */
class MultiArgWrappingRule :
    Rule(
        ruleId = RuleId("rhaydus:multi-arg-wrapping"),
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
        val entryType =
            when (node.elementType) {
                VALUE_ARGUMENT_LIST -> if (shouldWrapArgumentList(node)) VALUE_ARGUMENT else return
                VALUE_PARAMETER_LIST -> if (shouldWrapParameterList(node)) VALUE_PARAMETER else return
                else -> return
            }

        val entries = node.children().filter { it.elementType == entryType }.toList()

        if (node.textContains('\n')) {
            repairHuggingCall(
                node,
                entries,
                emit,
            )

            return
        }

        if (entries.size < 2) return

        emit(
            node.startOffset,
            "Multi-argument list must break one-per-line with a trailing comma",
            true,
        )
            .ifAutocorrectAllowed {
                enclosingSingleLineLists(node).asReversed().forEach { wrap(it) }
                wrap(node)
                enclosingFunctionLiterals(node).asReversed().forEach { expandLambdaBraces(it) }
            }
    }

    /**
     * A multi-line list whose only argument is a call hugging both parentheses (`outer(inner(\n…\n))`, or
     * the `),)` an earlier comma pass left) moves that argument onto its own lines.
     */
    private fun repairHuggingCall(
        list: ASTNode,
        entries: List<ASTNode>,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val entry = entries.singleOrNull() ?: return

        if (list.elementType != VALUE_ARGUMENT_LIST || entry.elementType != VALUE_ARGUMENT) return

        if (entry.text.endsWith(")").not()) return

        val rpar = list.children().firstOrNull { it.elementType == RPAR } ?: return

        val huggingOpen = entry.treePrev?.elementType == LPAR
        val huggingClose = entry.treeNext == rpar || (entry.treeNext?.elementType == COMMA && entry.treeNext?.treeNext == rpar)

        if (huggingOpen.not() || huggingClose.not()) return

        emit(
            list.startOffset,
            "A multi-line argument list must break one-per-line with a trailing comma",
            true,
        )
            .ifAutocorrectAllowed {
                val baseIndent = list.lineIndent()
                entry.indentContinuationLines(INDENT)
                entry.upsertWhitespaceBeforeMe("\n$baseIndent$INDENT")

                if (entry.treeNext?.elementType != COMMA) {
                    list.addChild(
                        LeafPsiElement(
                            COMMA,
                            ",",
                        ),
                        entry.treeNext,
                    )
                }

                rpar.upsertWhitespaceBeforeMe("\n$baseIndent")
            }
    }

    /** A value-argument list is in scope only for real call expressions, minus the documented exemptions. */
    private fun shouldWrapArgumentList(argumentList: ASTNode): Boolean {
        val call = argumentList.treeParent ?: return false

        if (call.elementType != CALL_EXPRESSION) return false

        // trailing lambda → leave the value args inline
        if (call.children().any { it.elementType == LAMBDA_ARGUMENT }) return false

        val callee = call.children().firstOrNull { it.elementType == REFERENCE_EXPRESSION }?.text

        if (callee != null && callee in COLLECTION_FACTORIES) return false

        return true
    }

    /** Only function/constructor declarations — never lambda parameter lists. */
    private fun shouldWrapParameterList(parameterList: ASTNode): Boolean =
        parameterList.treeParent?.elementType in DECLARATION_OWNERS

    /** Single-line argument / parameter lists that contain [list], innermost first. */
    private fun enclosingSingleLineLists(list: ASTNode): List<ASTNode> {
        val lists = mutableListOf<ASTNode>()
        var ancestor = list.treeParent

        while (ancestor != null) {
            val isList = ancestor.elementType == VALUE_ARGUMENT_LIST || isDeclarationParameterList(ancestor)

            if (isList && ancestor.textContains('\n')) break

            if (isList) lists += ancestor

            ancestor = ancestor.treeParent
        }

        return lists
    }

    private fun isDeclarationParameterList(node: ASTNode): Boolean =
        node.elementType == VALUE_PARAMETER_LIST && shouldWrapParameterList(node)

    /** Lambda bodies that contain [node], innermost first. */
    private fun enclosingFunctionLiterals(node: ASTNode): List<ASTNode> {
        val literals = mutableListOf<ASTNode>()
        var ancestor = node.treeParent

        while (ancestor != null) {
            if (ancestor.elementType == FUNCTION_LITERAL) literals += ancestor

            ancestor = ancestor.treeParent
        }

        return literals
    }

    private fun wrap(node: ASTNode) {
        val entryType = if (node.elementType == VALUE_ARGUMENT_LIST) VALUE_ARGUMENT else VALUE_PARAMETER
        val entries = node.children().filter { it.elementType == entryType }.toList()

        if (entries.isEmpty()) return

        val baseIndent = node.lineIndent()
        val entryIndent = "$baseIndent$INDENT"

        val rpar = node.children().firstOrNull { it.elementType == RPAR } ?: return

        entries.forEach { entry -> entry.upsertWhitespaceBeforeMe("\n$entryIndent") }

        val lastEntry = entries.last()
        val afterLast = lastEntry.treeNext

        if (afterLast == null || afterLast.elementType != COMMA) {
            node.addChild(
                LeafPsiElement(
                    COMMA,
                    ",",
                ),
                lastEntry.treeNext,
            )
        }

        rpar.upsertWhitespaceBeforeMe("\n$baseIndent")
    }

    private companion object {
        const val INDENT = "    "

        val DECLARATION_OWNERS = setOf(FUN, PRIMARY_CONSTRUCTOR, SECONDARY_CONSTRUCTOR)

        val COLLECTION_FACTORIES =
            setOf(
                "listOf", "mutableListOf", "listOfNotNull", "arrayListOf",
                "setOf", "mutableSetOf", "sortedSetOf", "linkedSetOf", "hashSetOf",
                "mapOf", "mutableMapOf", "sortedMapOf", "linkedMapOf", "hashMapOf",
                "arrayOf", "intArrayOf", "longArrayOf", "doubleArrayOf", "floatArrayOf",
                "booleanArrayOf", "charArrayOf", "byteArrayOf", "shortArrayOf",
                "buildList", "buildSet", "buildMap", "persistentListOf", "persistentSetOf", "persistentMapOf",
            )
    }
}
