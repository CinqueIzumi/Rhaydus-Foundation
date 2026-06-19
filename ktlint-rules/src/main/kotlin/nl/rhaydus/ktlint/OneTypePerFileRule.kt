package nl.rhaydus.ktlint

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FILE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IDENTIFIER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OBJECT_DECLARATION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SUPER_TYPE_LIST
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.children
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * code-style §One declaration per file: one top-level type per file, named after it (models, DTOs,
 * enums, `*Action`/`*Event`/etc.). Co-located unrelated types drift and hide the file's subject.
 *
 * Detect-only (not auto-corrected): splitting a file is a structural move. Sanctioned exception (per the
 * doc): the `data class` / `data object` variants of a single `sealed` type MAY co-locate in that type's
 * own file, so a file holding exactly one sealed type plus its own subtypes is not flagged.
 */
class OneTypePerFileRule :
    Rule(
        ruleId = RuleId("rhaydus:one-type-per-file"),
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

        val types = node
            .children()
            .filter { it.elementType == CLASS || it.elementType == OBJECT_DECLARATION }
            .toList()
        if (types.size <= 1) return

        if (isSealedHierarchyFile(types)) return

        emit(
            types[1].startOffset,
            "More than one top-level type in this file (${types.size}) - one type per file, named " +
                "after it; only a single sealed type's own variants may co-locate (§Files)",
            false,
        )
    }

    private fun isSealedHierarchyFile(types: List<ASTNode>): Boolean {
        val sealedTypes = types.filter { isSealed(it) }
        if (sealedTypes.size != 1) return false

        val parent = sealedTypes.single()
        val parentName = identifier(parent) ?: return false
        val reference = Regex("\\b${Regex.escape(parentName)}\\b")

        return types
            .filter { it !== parent }
            .all { subtype ->
                val superTypes = subtype.findChildByType(SUPER_TYPE_LIST)
                superTypes != null && reference.containsMatchIn(superTypes.text)
            }
    }

    private fun isSealed(node: ASTNode): Boolean =
        node.findChildByType(MODIFIER_LIST)?.children()?.any { it.text == "sealed" } == true

    private fun identifier(node: ASTNode): String? = node.findChildByType(IDENTIFIER)?.text
}
