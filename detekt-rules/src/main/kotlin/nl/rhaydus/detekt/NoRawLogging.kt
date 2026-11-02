package nl.rhaydus.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression

/**
 * Reports raw logging: an unqualified `println(...)`, a `System.out` or `System.err` `.println(...)`, and
 * any `Log.*` or `android.util.Log.*` call. The shared `AppLog` facade is the logger, because `println`
 * writes to an invisible stdout and `Log.*` is Android-only and skips the debug-gated facade.
 *
 * Pure PSI, matched on the call shape: a member call on another receiver (`logger.println(...)`) and a
 * `Log` reached through an import alias are left alone.
 */
class NoRawLogging(config: Config) :
    Rule(
        config,
        "Log through the AppLog facade, not println or android.util.Log.",
    ) {
    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callee = expression.calleeExpression?.text ?: return

        val receiver = (expression.parent as? KtDotQualifiedExpression)
            ?.takeIf { it.selectorExpression == expression }
            ?.receiverExpression
            ?.text

        val message = when {
            callee == PRINTLN && (receiver == null || receiver in SYSTEM_STREAMS) -> MESSAGE_PRINTLN
            receiver == LOG || receiver?.endsWith(".$LOG") == true -> MESSAGE_LOG
            else -> return
        }

        report(
            Finding(
                entity = Entity.from(expression),
                message = message,
            ),
        )
    }

    private companion object {
        const val PRINTLN = "println"
        const val LOG = "Log"
        const val MESSAGE_PRINTLN = "Use the AppLog facade (AppLog.i/w/e), not raw println."
        const val MESSAGE_LOG = "Use the AppLog facade (AppLog.i/w/e), not android.util.Log."
        val SYSTEM_STREAMS = setOf("System.out", "System.err")
    }
}
