package nl.rhaydus.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtCallExpression

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
class UnguardedFlowTerminalRead(config: Config) :
    Rule(
        config,
        "A terminal Flow read (first()/single()) throws on an empty or erroring flow - guard it " +
            "(firstOrNull() + default + catch / runCatchingCancellable) or consume the flow via a Collector.",
    ),
    RequiresAnalysisApi {
    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val terminal = analyze(expression) {
            val call = expression.resolveToCall()?.successfulFunctionCallOrNull() ?: return@analyze null
            val callableId = call.symbol.callableId ?: return@analyze null
            val name = callableId.asSingleFqName().asString()

            if (name != FLOW_FIRST && name != FLOW_SINGLE) return@analyze null

            val receiverType: KaType? = call.extensionReceiver?.type
            val hotReceiver = receiverType?.isSubtypeOf(SHARED_FLOW) == true

            if (name == FLOW_FIRST && hotReceiver) return@analyze null

            name
        } ?: return

        report(
            Finding(
                entity = Entity.from(expression),
                message =
                    "Terminal flow read ($terminal) can crash on an empty or erroring flow - guard it " +
                        "(firstOrNull() + default + catch / runCatchingCancellable) or consume via a Collector.",
            ),
        )
    }

    private companion object {
        const val FLOW_FIRST = "kotlinx.coroutines.flow.first"
        const val FLOW_SINGLE = "kotlinx.coroutines.flow.single"
        val SHARED_FLOW = ClassId.fromString("kotlinx/coroutines/flow/SharedFlow")
    }
}
