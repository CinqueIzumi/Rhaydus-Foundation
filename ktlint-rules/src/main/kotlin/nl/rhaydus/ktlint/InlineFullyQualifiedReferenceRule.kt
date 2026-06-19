package nl.rhaydus.ktlint

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IMPORT_DIRECTIVE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PACKAGE_DIRECTIVE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.USER_TYPE
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * code-style §No fully-qualified references: never reference a type, top-level function, enum entry, or
 * object member by its fully-qualified name inline - add an `import` and use the short name. Especially
 * strict for project-own code (`nl.rhaydus.*`).
 *
 * Detect-only (not auto-corrected): hoisting the import + shortening the reference is a multi-site edit
 * left to the IDE. Working on the AST, this never matches inside imports, packages, strings, or KDoc -
 * it is more precise than the grep recipe it replaces. Conservative: only a reference rooted at a known
 * top-level package (`androidx`/`android`/`java`/`javax`/`kotlin`/`kotlinx`/`nl.rhaydus`) and reaching an
 * uppercase segment is flagged, so a plain lowercase chain (`kotlinx...flow.first`) is left alone.
 */
class InlineFullyQualifiedReferenceRule :
    Rule(
        ruleId = RuleId("rhaydus:no-fully-qualified-reference"),
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
        val type = node.elementType
        if (type != USER_TYPE && type != DOT_QUALIFIED_EXPRESSION) return

        if (node.treeParent?.elementType == type) return

        if (node.isInsideImportOrPackage()) return

        if (FULLY_QUALIFIED.containsMatchIn(node.text).not()) return

        emit(
            node.startOffset,
            "Inline fully-qualified reference - add an import and use the short name (§Imports)",
            false,
        )
    }

    private fun ASTNode.isInsideImportOrPackage(): Boolean {
        var ancestor = treeParent
        while (ancestor != null) {
            if (ancestor.elementType == IMPORT_DIRECTIVE || ancestor.elementType == PACKAGE_DIRECTIVE) return true
            ancestor = ancestor.treeParent
        }

        return false
    }

    private companion object {
        val FULLY_QUALIFIED =
            Regex("^(androidx|android|java|javax|kotlin|kotlinx|nl\\.rhaydus)\\.[a-z][A-Za-z0-9_.]*\\.[A-Z]")
    }
}
