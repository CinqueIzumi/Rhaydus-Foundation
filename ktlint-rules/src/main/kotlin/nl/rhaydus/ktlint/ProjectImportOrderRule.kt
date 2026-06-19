package nl.rhaydus.ktlint

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IMPORT_DIRECTIVE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IMPORT_LIST
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.children
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * code-style §Import Ordering: project imports (`nl.rhaydus.*`) are alphabetical within their group. An
 * out-of-order project import block is hard to scan and produces noisy diffs.
 *
 * Detect-only (not auto-corrected): import sorting is owned by the IDE/formatter; the gate keeps a
 * mis-sorted block from landing. Checks alphabetical order across the `nl.rhaydus.*` imports only (the
 * project group); the Android / third-party groups are left to the IDE.
 */
class ProjectImportOrderRule :
    Rule(
        ruleId = RuleId("rhaydus:project-import-order"),
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
        if (node.elementType != IMPORT_LIST) return

        val projectImports = node
            .children()
            .filter { it.elementType == IMPORT_DIRECTIVE }
            .map { it to importPath(it) }
            // Star imports (`nl.rhaydus.*`) sort before any named import, so exclude them from the order check.
            .filter { (_, path) -> path.startsWith("nl.rhaydus.") && path.endsWith(".*").not() }
            .toList()

        projectImports
            .zipWithNext()
            .firstOrNull { (previous, current) -> current.second < previous.second }
            ?.let { (_, current) ->
                emit(
                    current.first.startOffset,
                    "Project import block out of alphabetical order - sort nl.rhaydus.* imports (§Import Ordering)",
                    false,
                )
            }
    }

    private fun importPath(directive: ASTNode): String =
        directive.text
            .removePrefix("import ")
            .substringBefore(" as ")
            .trim()
}
