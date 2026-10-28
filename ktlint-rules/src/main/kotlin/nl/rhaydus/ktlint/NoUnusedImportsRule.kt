package nl.rhaydus.ktlint

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ARRAY_ACCESS_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DESTRUCTURING_DECLARATION_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DIV
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DIVEQ
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EXCL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FILE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FOR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.GT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.GTEQ
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IDENTIFIER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IMPORT_DIRECTIVE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IMPORT_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IN_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.KDOC
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LTEQ
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MINUS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MINUSEQ
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MINUSMINUS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MUL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MULTEQ
import com.pinterest.ktlint.rule.engine.core.api.ElementType.NOT_IN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PACKAGE_DIRECTIVE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PERC
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PERCEQ
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PLUS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PLUSEQ
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PLUSPLUS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PROPERTY_DELEGATE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RANGE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RANGE_UNTIL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.psi.KtImportDirective

/**
 * code-style §Import Ordering: remove unused imports.
 *
 * Purely syntactic: an import is used when its simple name (or its alias) appears as an identifier
 * anywhere outside the import list, in a KDoc `[Name]` link or `@see` / `@throws` reference, or when
 * the language reaches it implicitly through an operator, `by` delegate, destructuring declaration,
 * `for` loop or call. An identifier counts wherever it appears, so an extension used as
 * `Dispatchers.IO` keeps its import. Wildcard imports are never reported.
 */
class NoUnusedImportsRule :
    Rule(
        ruleId = RuleId("rhaydus:no-unused-imports"),
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
        if (node.elementType != FILE) return

        val importList = node.findChildByType(IMPORT_LIST) ?: return
        val imports = importList.getChildren(null).filter { it.elementType == IMPORT_DIRECTIVE }
        if (imports.isEmpty()) return

        val usedNames = node.collectUsedNames()
        val unused =
            imports.filter { directive ->
                val psi = directive.psi as? KtImportDirective ?: return@filter false
                if (psi.isAllUnder) return@filter false

                val name = psi.aliasName ?: psi.importedFqName?.shortName()?.asString() ?: return@filter false

                name.removeSurrounding("`") !in usedNames
            }

        unused.forEach { directive ->
            emit(
                directive.startOffset,
                "Unused import",
                true,
            )
                .ifAutocorrectAllowed { directive.removeImport(importList) }
        }
    }

    private fun ASTNode.collectUsedNames(): Set<String> {
        val names = mutableSetOf<String>()
        collectInto(names)

        return names
    }

    private fun ASTNode.collectInto(names: MutableSet<String>) {
        when (elementType) {
            IMPORT_LIST, PACKAGE_DIRECTIVE -> return

            KDOC -> {
                KDOC_REFERENCE.findAll(text).forEach { match ->
                    IDENTIFIER_WORD.findAll(match.groupValues[1].ifEmpty { match.groupValues[2] }).forEach {
                        names += it.value
                    }
                }

                return
            }

            IDENTIFIER -> names += text.removeSurrounding("`")

            PROPERTY_DELEGATE -> names += DELEGATE_CONVENTIONS

            CALL_EXPRESSION -> names += "invoke"

            ARRAY_ACCESS_EXPRESSION -> names += INDEX_CONVENTIONS

            DESTRUCTURING_DECLARATION_ENTRY -> names += COMPONENT_NAMES

            FOR -> names += FOR_CONVENTIONS

            else -> OPERATOR_CONVENTIONS[elementType]?.let { names += it }
        }

        getChildren(null).forEach { it.collectInto(names) }
    }

    private fun ASTNode.removeImport(importList: ASTNode) {
        val previous = treePrev?.takeIf { it.elementType == WHITE_SPACE }
        val next = treeNext?.takeIf { it.elementType == WHITE_SPACE }
        val separator =
            when {
                previous == null -> next
                next == null -> previous
                next.newlineCount() <= previous.newlineCount() -> next
                else -> previous
            }

        val remaining = importList.getChildren(null).count { it.elementType == IMPORT_DIRECTIVE }
        if (remaining == 1) {
            val after = importList.treeNext?.takeIf { it.elementType == WHITE_SPACE }
            importList.treeParent.removeChild(importList)
            after?.let { it.treeParent.removeChild(it) }

            return
        }

        separator?.let { importList.removeChild(it) }
        importList.removeChild(this)
    }

    private companion object {
        val IDENTIFIER_WORD = Regex("[A-Za-z_][A-Za-z0-9_]*")

        val KDOC_REFERENCE = Regex("\\[([^\\]]*)]|@(?:see|throws|exception)\\s+([A-Za-z_][A-Za-z0-9_.]*)")

        val DELEGATE_CONVENTIONS = setOf("getValue", "setValue", "provideDelegate")

        val INDEX_CONVENTIONS = setOf("get", "set")

        val COMPONENT_NAMES = (1..MAX_COMPONENT).map { "component$it" }

        val FOR_CONVENTIONS = setOf("iterator", "next", "hasNext")

        val OPERATOR_CONVENTIONS: Map<IElementType, Set<String>> =
            mapOf(
                PLUS to setOf("plus", "unaryPlus"),
                MINUS to setOf("minus", "unaryMinus"),
                MUL to setOf("times"),
                DIV to setOf("div"),
                PERC to setOf("rem"),
                RANGE to setOf("rangeTo"),
                RANGE_UNTIL to setOf("rangeUntil"),
                PLUSEQ to setOf("plusAssign", "plus"),
                MINUSEQ to setOf("minusAssign", "minus"),
                MULTEQ to setOf("timesAssign", "times"),
                DIVEQ to setOf("divAssign", "div"),
                PERCEQ to setOf("remAssign", "rem"),
                PLUSPLUS to setOf("inc"),
                MINUSMINUS to setOf("dec"),
                EXCL to setOf("not"),
                LT to setOf("compareTo"),
                GT to setOf("compareTo"),
                LTEQ to setOf("compareTo"),
                GTEQ to setOf("compareTo"),
                IN_KEYWORD to setOf("contains"),
                NOT_IN to setOf("contains"),
            )
    }
}

private const val MAX_COMPONENT = 30
