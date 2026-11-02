package nl.rhaydus.detekt

import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtCallExpression

/**
 * Reports a bare `runCatching` call. It swallows `CancellationException`, which breaks structured
 * concurrency, and logs nothing, so a failure the caller drops vanishes. `runCatchingLogged` or
 * `runCatchingCancellable` from `core-common` replace it.
 *
 * The rule has no layer knowledge: the consumer scopes it to the use-case files with `includes`.
 */
class UseCaseRunCatching(config: Config) :
    Rule(
        config,
        "Use runCatchingLogged or runCatchingCancellable, not bare runCatching.",
    ) {
    @Configuration("The call names that are forbidden.")
    private val forbiddenCalls: List<String> by config(listOf("runCatching"))

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callee = expression.calleeExpression?.text ?: return

        if (callee !in forbiddenCalls) return

        report(
            Finding(
                entity = Entity.from(expression),
                message = "Use runCatchingLogged (or runCatchingCancellable) instead of bare $callee.",
            ),
        )
    }
}
