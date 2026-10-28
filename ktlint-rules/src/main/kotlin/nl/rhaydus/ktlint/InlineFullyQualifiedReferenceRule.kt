package nl.rhaydus.ktlint

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IMPORT_DIRECTIVE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PACKAGE_DIRECTIVE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.USER_TYPE
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import org.ec4j.core.model.PropertyType
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
 *
 * Exemption: the `.editorconfig` property `rhaydus_fully_qualified_exempt_package_segments` lists package
 * segments (comma-separated, e.g. `fragment`) whose types may stay fully-qualified, for generated code
 * whose names collide. A reference is exempt when it reaches a type directly under a listed segment. The
 * list is empty by default, so nothing is exempt unless configured.
 */
class InlineFullyQualifiedReferenceRule :
    Rule(
        ruleId = RuleId("rhaydus:no-fully-qualified-reference"),
        about = About(
            maintainer = "rhaydus",
            repositoryUrl = "",
            issueTrackerUrl = "",
        ),
        usesEditorConfigProperties = setOf(EXEMPT_PACKAGE_SEGMENTS_PROPERTY),
    ),
    RuleAutocorrectApproveHandler {
    private var exemptReference: Regex? = null

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        val segments =
            editorConfig[EXEMPT_PACKAGE_SEGMENTS_PROPERTY]
                .split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        exemptReference =
            segments
                .takeIf { it.isNotEmpty() }
                ?.let { Regex("\\.(${it.joinToString("|") { segment -> Regex.escape(segment) }})\\.[A-Z]") }
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val type = node.elementType
        if (type != USER_TYPE && type != DOT_QUALIFIED_EXPRESSION) return

        if (node.treeParent?.elementType == type) return

        if (node.isInsideImportOrPackage()) return

        if (FULLY_QUALIFIED.containsMatchIn(node.text).not()) return

        if (exemptReference?.containsMatchIn(node.text) == true) return

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

        val EXEMPT_PACKAGE_SEGMENTS_PROPERTY =
            EditorConfigProperty(
                type =
                    PropertyType.LowerCasingPropertyType(
                        "rhaydus_fully_qualified_exempt_package_segments",
                        "Comma-separated package segments whose types may stay fully-qualified.",
                        PropertyType.PropertyValueParser.IDENTITY_VALUE_PARSER,
                        emptySet(),
                    ),
                defaultValue = "",
            )
    }
}
