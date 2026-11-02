package nl.rhaydus.detekt

import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.KtValueArgument

/**
 * Reports a raw `N.dp` literal passed as spacing, so spacing reads a `Spacing` token instead.
 *
 * Spacing is an argument to `padding`, `PaddingValues`, `Arrangement.spacedBy`, `offset` or a
 * `contentPadding =` named argument, or the `height` / `width` / `size` of a `Spacer`'s modifier.
 * The literals in `allowedValues` stay literal. Component sizes, strokes and borders are not spacing and
 * are not inspected. Files named in `tokenFiles` hold the tokens and are exempt.
 */
class RawSpacingDp(config: Config) :
    Rule(
        config,
        "Spacing must read a Spacing token, not a raw dp literal.",
    ) {
    @Configuration("The dp values that may stay literal.")
    private val allowedValues: List<String> by config(listOf("0", "64", "96"))

    @Configuration("File names that hold the spacing tokens and are exempt.")
    private val tokenFiles: List<String> by config(listOf("Spacing.kt"))

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        if (expression.containingKtFile.name in tokenFiles) return

        val callee = expression.calleeExpression?.text ?: return

        when {
            callee in SPACING_CALLS -> reportRawDpArguments(expression.valueArguments)

            callee == SPACER -> expression.valueArguments
                .mapNotNull { it.getArgumentExpression() }
                .flatMap { it.chainCalls() }
                .filter { it.calleeExpression?.text in SIZE_CALLS }
                .forEach { reportRawDpArguments(it.valueArguments) }
        }
    }

    override fun visitArgument(argument: KtValueArgument) {
        super.visitArgument(argument)

        if (argument.getArgumentName()?.asName?.asString() == CONTENT_PADDING) {
            reportRawDpArguments(listOf(argument))
        }
    }

    private fun reportRawDpArguments(arguments: List<KtValueArgument>) {
        arguments
            .mapNotNull { it.getArgumentExpression() }
            .filter { it.isRawSpacingDp() }
            .forEach { reportRawDp(it) }
    }

    private fun reportRawDp(expression: KtExpression) {
        report(
            Finding(
                entity = Entity.from(expression),
                message = MESSAGE,
            ),
        )
    }

    private fun KtExpression.chainCalls(): List<KtCallExpression> {
        val calls = mutableListOf<KtCallExpression>()
        var link: KtExpression? = this

        while (link is KtDotQualifiedExpression) {
            (link.selectorExpression as? KtCallExpression)?.let { calls += it }
            link = link.receiverExpression
        }

        (link as? KtCallExpression)?.let { calls += it }

        return calls
    }

    private fun KtExpression.isRawSpacingDp(): Boolean {
        val literal = if (this is KtPrefixExpression && operationToken == KtTokens.MINUS) {
            baseExpression
        } else {
            this
        }

        val dp = literal as? KtDotQualifiedExpression ?: return false

        val unit = dp.selectorExpression as? KtNameReferenceExpression ?: return false

        val number = dp.receiverExpression as? KtConstantExpression ?: return false

        if (unit.getReferencedName() != DP) return false

        val digits = number.text.filter { it != '_' }.trimEnd { it == 'f' || it == 'F' }

        val value = digits.toDoubleOrNull() ?: return false

        return value !in allowedValues.mapNotNull { it.toDoubleOrNull() }
    }

    private companion object {
        const val MESSAGE = "Use a Spacing token instead of a raw dp literal."
        const val DP = "dp"
        const val SPACER = "Spacer"
        const val CONTENT_PADDING = "contentPadding"
        val SPACING_CALLS = setOf("padding", "PaddingValues", "spacedBy", "offset")
        val SIZE_CALLS = setOf("height", "width", "size")
    }
}
