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
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.supertypes

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
 *
 * **`SharedFlow` / `StateFlow` receivers are exempt from `first`, but not from `single`.** A hot flow
 * never completes and never fails, so neither hazard exists: `first()` returns the current value and
 * `first { predicate }` suspends until one matches - which is precisely how `awaitOnline()` is written
 * against a `StateFlow<Boolean>`. `single()` is a different story: on a flow that never completes it
 * either suspends forever or throws on the second emission, so it stays flagged on every receiver.
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

        val resolvedCall = expression.getResolvedCall(bindingContext) ?: return

        val fqName = resolvedCall
            .resultingDescriptor
            .fqNameOrNull()
            ?.asString()
            ?: return

        if (fqName != FLOW_FIRST && fqName != FLOW_SINGLE) return

        if (fqName == FLOW_FIRST && resolvedCall.hasHotFlowReceiver()) return

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

    /**
     * Whether the terminal reads a `SharedFlow` (or its `StateFlow` subtype). A hot flow never completes
     * and never fails, so `first` on it can neither throw `NoSuchElementException` nor re-throw upstream.
     *
     * Deliberately written against a conservative slice of the stdlib. Rules run inside detekt's own
     * runtime, which embeds an older Kotlin stdlib than the one they compile against, so a newer API
     * (`sequenceOf(element)`, for one) links at compile time and then dies with a `NoSuchMethodError`
     * mid-analysis.
     */
    private fun ResolvedCall<*>.hasHotFlowReceiver(): Boolean {
        val receiverType = extensionReceiver?.type ?: return false

        if (receiverType.isSharedFlow()) return true

        return receiverType.supertypes().any { supertype -> supertype.isSharedFlow() }
    }

    private fun KotlinType.isSharedFlow(): Boolean =
        constructor.declarationDescriptor?.fqNameOrNull()?.asString() == SHARED_FLOW

    private companion object {
        const val FLOW_FIRST = "kotlinx.coroutines.flow.first"
        const val FLOW_SINGLE = "kotlinx.coroutines.flow.single"
        const val SHARED_FLOW = "kotlinx.coroutines.flow.SharedFlow"
    }
}
