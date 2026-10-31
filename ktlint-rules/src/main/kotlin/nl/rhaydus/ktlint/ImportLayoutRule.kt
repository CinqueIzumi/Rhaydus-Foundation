package nl.rhaydus.ktlint

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IMPORT_DIRECTIVE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IMPORT_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl

/**
 * code-style §Import Ordering: the whole import block follows the layout in the `.editorconfig` property
 * `ij_kotlin_imports_layout` (default `*,java.**,javax.**,kotlin.**,^`), the same value IntelliJ's
 * Optimize Imports reads, so the IDE and this rule agree.
 *
 * Layout entries, comma-separated and matched in order:
 *   - `*` - every import no other entry claims,
 *   - `a.b.**` - `a.b` and its sub-packages, `a.b.*` - exactly the package `a.b`,
 *   - `^` - aliased imports (`import a.B as C`), also `^a.b.**` to restrict them to a package,
 *   - `|` - a blank line between the neighbouring buckets.
 *
 * Imports sort ASCII-wise by path inside each bucket, with no blank lines unless the layout asks for them.
 * Aliased imports match the `^` entry first, so the default layout gives them their own trailing bucket. A
 * block that holds a comment is left alone.
 */
class ImportLayoutRule :
    Rule(
        ruleId = RuleId("rhaydus:import-layout"),
        about = About(
            maintainer = "rhaydus",
            repositoryUrl = "",
            issueTrackerUrl = "",
        ),
        usesEditorConfigProperties = setOf(IMPORT_LAYOUT_PROPERTY),
    ),
    RuleAutocorrectApproveHandler {
    private var layout: List<LayoutEntry> = parseLayout(DEFAULT_LAYOUT)

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        layout = parseLayout(editorConfig[IMPORT_LAYOUT_PROPERTY])
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != IMPORT_LIST) return

        val directives = node.children().filter { it.elementType == IMPORT_DIRECTIVE }.toList()

        if (directives.isEmpty()) return

        val onlyImports = node.children().all { it.elementType == IMPORT_DIRECTIVE || it.elementType == WHITE_SPACE }

        if (onlyImports.not()) return

        val expected = layoutText(directives)
        val actual = node.text.trim()

        if (expected == actual) return

        emit(
            directives.first().startOffset,
            "Imports must follow the layout '${layout.joinToString(",") { it.source }}' (§Import Ordering)",
            true,
        )
            .ifAutocorrectAllowed {
                rewrite(
                    node,
                    directives,
                )
            }
    }

    private fun rewrite(
        importList: ASTNode,
        directives: List<ASTNode>,
    ) {
        val separators = separators(directives)
        val ordered = ordered(directives)

        importList.children().toList().forEach { importList.removeChild(it) }

        ordered.forEachIndexed { index, directive ->
            if (index > 0) importList.addChild(
                PsiWhiteSpaceImpl(separators[index - 1]),
                null,
            )

            importList.addChild(
                directive,
                null,
            )
        }
    }

    private fun layoutText(directives: List<ASTNode>): String {
        val separators = separators(directives)

        return ordered(directives)
            .mapIndexed { index, directive -> (if (index > 0) separators[index - 1] else "") + directive.text }
            .joinToString("")
    }

    private fun separators(directives: List<ASTNode>): List<String> {
        val bucketOf = ordered(directives).map { bucketIndex(it) }

        return bucketOf.zipWithNext { previous, current ->
            val blank = blankLineBetween(
                previous,
                current,
            )

            if (blank) "\n\n" else "\n"
        }
    }

    private fun ordered(directives: List<ASTNode>): List<ASTNode> = directives.sortedWith(
        compareBy<ASTNode> { bucketIndex(it) }
            .thenBy { importPath(it) }
            .thenBy { it.text },
    )

    private fun blankLineBetween(
        previousBucket: Int,
        currentBucket: Int,
    ): Boolean =
        previousBucket != currentBucket &&
            layout.subList(
                previousBucket + 1,
                currentBucket,
            ).any { it.kind == Kind.BLANK_LINE }

    private fun bucketIndex(directive: ASTNode): Int {
        val path = importPath(directive)
        val aliased = directive.text.contains(" as ")

        val claiming = layout.indexOfFirst { entry ->
            entry.kind == Kind.IMPORTS && entry.claims(
                path,
                aliased,
            )
        }

        if (claiming >= 0) return claiming

        return layout.indexOfFirst { it.kind == Kind.IMPORTS && it.isWildcard }.takeIf { it >= 0 } ?: layout.size
    }

    private fun importPath(directive: ASTNode): String =
        directive.text
            .removePrefix("import")
            .substringBefore(" as ")
            .trim()

    private enum class Kind { IMPORTS, BLANK_LINE }

    private class LayoutEntry(
        val source: String,
        val kind: Kind,
        val aliasOnly: Boolean = false,
        val isWildcard: Boolean = false,
        private val pattern: String = "",
    ) {
        fun claims(
            path: String,
            aliased: Boolean,
        ): Boolean {
            if (isWildcard) return false

            if (aliasOnly && aliased.not()) return false

            if (aliasOnly.not() && aliased) return false

            if (pattern.isEmpty()) return true

            val pkg = path.substringBeforeLast(
                '.',
                "",
            )

            return when {
                pattern.endsWith(".**") -> path.startsWith(pattern.removeSuffix("**"))
                pattern.endsWith(".*") -> pkg == pattern.removeSuffix(".*")
                else -> path == pattern
            }
        }
    }

    internal companion object {
        private const val DEFAULT_LAYOUT = "*,java.**,javax.**,kotlin.**,^"

        private fun parseLayout(value: String): List<LayoutEntry> =
            value
                .split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { entry ->
                    when {
                        entry == "|" -> LayoutEntry(
                            entry,
                            Kind.BLANK_LINE,
                        )
                        entry == "*" -> LayoutEntry(
                            entry,
                            Kind.IMPORTS,
                            isWildcard = true,
                        )
                        entry.startsWith("^") ->
                            LayoutEntry(
                                entry,
                                Kind.IMPORTS,
                                aliasOnly = true,
                                pattern = entry.removePrefix("^"),
                            )
                        else -> LayoutEntry(
                            entry,
                            Kind.IMPORTS,
                            pattern = entry,
                        )
                    }
                }

        val IMPORT_LAYOUT_PROPERTY = EditorConfigProperty(
            type = PropertyType.LowerCasingPropertyType(
                "ij_kotlin_imports_layout",
                "Comma-separated import layout, as IntelliJ's Optimize Imports reads it.",
                PropertyType.PropertyValueParser.IDENTITY_VALUE_PARSER,
                emptySet(),
            ),
            defaultValue = DEFAULT_LAYOUT,
        )
    }
}
