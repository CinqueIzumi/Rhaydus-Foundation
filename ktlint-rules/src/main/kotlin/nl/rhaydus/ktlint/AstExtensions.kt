package nl.rhaydus.ktlint

import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.firstChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

/** Number of line breaks contained in this node's text (0 for a same-line whitespace). */
internal fun ASTNode.newlineCount(): Int = text.count { it == '\n' }

/** Indentation (whitespace after the last newline) of a whitespace node, e.g. "\n        " -> "        ". */
internal fun ASTNode.trailingIndent(): String = text.substringAfterLast('\n')

/**
 * Ensure exactly one blank line *after* [node] — unless it is the last element before a closing brace.
 * No-op if [node] is not followed by a single-newline whitespace.
 */
internal fun ensureBlankLineAfter(node: ASTNode) {
    val ws = node.treeNext ?: return

    if (ws.elementType != WHITE_SPACE) return

    if (ws.treeNext?.elementType == RBRACE) return

    if (ws.newlineCount() != 1) return

    node.upsertWhitespaceAfterMe("\n\n${ws.trailingIndent()}")
}

/** Collapse a blank line *after* [node] (e.g. right after an opening brace) down to a single newline. */
internal fun removeBlankLineAfter(node: ASTNode) {
    val ws = node.treeNext ?: return

    if (ws.elementType != WHITE_SPACE) return

    if (ws.newlineCount() < 2) return

    node.upsertWhitespaceAfterMe("\n${ws.trailingIndent()}")
}

/** Collapse a blank line *before* [node] (e.g. right before a closing brace) down to a single newline. */
internal fun removeBlankLineBefore(node: ASTNode) {
    val ws = node.treePrev ?: return

    if (ws.elementType != WHITE_SPACE) return

    if (ws.newlineCount() < 2) return

    node.upsertWhitespaceBeforeMe("\n${ws.trailingIndent()}")
}

/** Leading whitespace (indentation) of the line on which this node starts. */
internal fun ASTNode.lineIndent(): String {
    var leaf = prevLeaf()

    while (leaf != null) {
        val newlineIndex = leaf.text.lastIndexOf('\n')

        if (newlineIndex >= 0) {
            return if (leaf.text.isBlank()) leaf.text.substring(newlineIndex + 1) else ""
        }

        leaf = leaf.prevLeaf()
    }

    return ""
}

/** Pushes every line break inside this node [indent] further right, so the node can move one level deeper. */
internal fun ASTNode.indentContinuationLines(indent: String) {
    leavesBetween(
        firstChildLeafOrSelf(),
        lastChildLeafOrSelf(),
    ).indentLineBreaks(indent)
}

/** The leaves from [first] to [last] inclusive, in document order. */
internal fun leavesBetween(
    first: ASTNode,
    last: ASTNode,
): List<ASTNode> {
    val leaves = mutableListOf<ASTNode>()
    var leaf: ASTNode? = first

    while (leaf != null) {
        leaves += leaf

        if (leaf == last) break

        leaf = leaf.nextLeaf()
    }

    return leaves
}

/** Appends [indent] to every line-break whitespace among these leaves. */
internal fun List<ASTNode>.indentLineBreaks(indent: String) {
    filter { it.elementType == WHITE_SPACE && it.textContains('\n') }
        .forEach { (it as LeafPsiElement).rawReplaceWithText("${it.text}$indent") }
}
