package nl.rhaydus.detekt

import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtCallExpression

/**
 * Reports a mockk stub whose block sits on one line. A single-line `coEvery { ... } returns ...` hides the
 * arrange step in the noise, so the block opens onto its own line.
 *
 * The rule has no source-set knowledge: the stubs live in test sources, so the consumer scopes it there
 * with `includes`.
 */
class InlineMockkStub(config: Config) :
    Rule(
        config,
        "Open a mockk stub block onto its own line.",
    ) {
    @Configuration("The stubbing calls whose block must span lines.")
    private val stubCalls: List<String> by config(listOf("coEvery", "every"))

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callee = expression.calleeExpression?.text ?: return

        if (callee !in stubCalls) return

        val lambda = expression.lambdaArguments.firstOrNull() ?: return

        if (lambda.textContains('\n')) return

        if (lambda.getLambdaExpression()?.bodyExpression?.statements.isNullOrEmpty()) return

        report(
            Finding(
                entity = Entity.from(lambda),
                message = "Open the $callee block onto its own line.",
            ),
        )
    }
}
