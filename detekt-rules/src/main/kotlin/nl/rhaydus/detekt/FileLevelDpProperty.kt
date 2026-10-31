package nl.rhaydus.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtProperty

/**
 * Reports a file-level property that hoists a `Dp` value: one typed `Dp`, one initialised by a `N.dp`
 * literal, or a holder built only from `N.dp` arguments (`BarSpec(width = 32.dp, height = 8.dp)`).
 *
 * A dp value reads at the use site, where the reader sees the element it sizes. A local inside a function
 * and a member of a class stay allowed.
 */
class FileLevelDpProperty(config: Config) :
    Rule(
        config,
        "A dp value must stay inline at the use site, not in a file-level property.",
    ) {
    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)

        if (property.isTopLevel.not()) return

        if (property.hoistsDp().not()) return

        report(
            Finding(
                entity = Entity.atName(property),
                message = "Keep the dp value inline at the use site instead of a file-level property.",
            ),
        )
    }

    private fun KtProperty.hoistsDp(): Boolean {
        if (typeReference?.text?.removeSuffix("?") == DP_TYPE) return true

        val initializer = initializer ?: return false

        return initializer.isDpLiteral() || initializer.isDpOnlyHolder()
    }

    private fun KtExpression.isDpOnlyHolder(): Boolean {
        val call = this as? KtCallExpression ?: return false

        val arguments = call.valueArguments.mapNotNull { it.getArgumentExpression() }

        return arguments.isNotEmpty() &&
            arguments.size == call.valueArguments.size &&
            arguments.all { it.isDpLiteral() }
    }

    private fun KtExpression.isDpLiteral(): Boolean {
        val dp = this as? KtDotQualifiedExpression ?: return false

        val unit = dp.selectorExpression as? KtNameReferenceExpression ?: return false

        return unit.getReferencedName() == DP_UNIT && dp.receiverExpression is KtConstantExpression
    }

    private companion object {
        const val DP_TYPE = "Dp"
        const val DP_UNIT = "dp"
    }
}
