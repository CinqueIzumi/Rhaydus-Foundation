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
import java.io.File

/**
 * Reports a raw corner radius passed to a corner shape or `CornerSize`, so corners read a `Corners`
 * token instead.
 *
 * A raw radius is a number or `N.dp` literal outside `allowedValues`, or a named `percent` argument
 * outside it. Files named in `tokenFiles` hold the tokens and are exempt.
 */
class RawCornerDp(config: Config) :
    Rule(
        config,
        "Corners must read a Corners token, not a raw corner radius.",
    ) {
    @Configuration("The radii that may stay literal.")
    private val allowedValues: List<String> by config(listOf("0"))

    @Configuration("File names that hold the corner tokens and are exempt.")
    private val tokenFiles: List<String> by config(listOf("Corners.kt"))

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        if (File(expression.containingKtFile.name).name in tokenFiles) return

        if (expression.calleeExpression?.text !in CORNER_CALLS) return

        expression.valueArguments
            .filter { it.isRawRadius() }
            .forEach { reportRawRadius(it) }
    }

    private fun reportRawRadius(argument: KtValueArgument) {
        report(
            Finding(
                entity = Entity.from(argument),
                message = MESSAGE,
            ),
        )
    }

    private fun KtValueArgument.isRawRadius(): Boolean {
        val expression = getArgumentExpression() ?: return false

        val value = expression.literalValue()
        val allowed = allowedValues.mapNotNull { it.toDoubleOrNull() }

        if (getArgumentName()?.asName?.asString() == PERCENT) return value !in allowed

        return value != null && value !in allowed
    }

    private fun KtExpression.literalValue(): Double? {
        val literal = if (this is KtPrefixExpression && operationToken == KtTokens.MINUS) {
            baseExpression
        } else {
            this
        }

        val number = when (literal) {
            is KtConstantExpression -> literal
            is KtDotQualifiedExpression -> literal.dpReceiver() ?: return null
            else -> return null
        }

        val digits = number.text.filter { it != '_' }.trimEnd { it == 'f' || it == 'F' }

        return digits.toDoubleOrNull()
    }

    private fun KtDotQualifiedExpression.dpReceiver(): KtConstantExpression? {
        val unit = selectorExpression as? KtNameReferenceExpression ?: return null

        if (unit.getReferencedName() != DP) return null

        return receiverExpression as? KtConstantExpression
    }

    private companion object {
        const val MESSAGE = "Use a Corners token instead of a raw corner radius."
        const val DP = "dp"
        const val PERCENT = "percent"
        val CORNER_CALLS = setOf(
            "RoundedCornerShape",
            "CutCornerShape",
            "AbsoluteRoundedCornerShape",
            "AbsoluteCutCornerShape",
            "CornerSize",
        )
    }
}
