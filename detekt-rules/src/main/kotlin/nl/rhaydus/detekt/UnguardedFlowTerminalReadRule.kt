package nl.rhaydus.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.api.internal.RequiresTypeResolution
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull

/**
 * code-style §Error Handling: a terminal read of a cold `Flow` must never be able to crash the app.
 * `Flow.first()` / `Flow.single()` throw on an empty flow, and any terminal operator re-throws an
 * upstream error (DataStore / network / repository). Guard it (`firstOrNull()` + default + `catch` /
 * a cancellation-aware `runCatching`) or consume the flow reactively through a TOAD `Collector`.
 *
 * Type-resolved so it fires only on the `kotlinx.coroutines.flow` terminals - the identically named
 * `Collection` / `Iterable` `first()` / `single()` are left alone. The guarded `firstOrNull()` /
 * `singleOrNull()` forms are different functions and never match. Test sources are excluded via the
 * shared config; the crash risk is a production concern.
 */
@RequiresTypeResolution
class UnguardedFlowTerminalReadRule(config: Config) : Rule(config) {
    override val issue = Issue(
        id = "UnguardedFlowTerminalRead",
        severity = Severity.Defect,
        description =
            "A terminal Flow read (first()/single()) throws on an empty or erroring flow - guard it " +
                "(firstOrNull() + default + catch / runCatchingCancellable) or consume the flow via a Collector.",
        debt = Debt.FIVE_MINS,
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val fqName = expression
            .getResolvedCall(bindingContext)
            ?.resultingDescriptor
            ?.fqNameOrNull()
            ?.asString()
            ?: return

        if (fqName != FLOW_FIRST && fqName != FLOW_SINGLE) return

        report(
            CodeSmell(
                issue = issue,
                entity = Entity.from(expression),
                message =
                    "Terminal flow read ($fqName) can crash on an empty or erroring flow - guard it " +
                        "(firstOrNull() + default + catch / runCatchingCancellable) or consume via a Collector.",
            ),
        )
    }

    private companion object {
        const val FLOW_FIRST = "kotlinx.coroutines.flow.first"
        const val FLOW_SINGLE = "kotlinx.coroutines.flow.single"
    }
}
