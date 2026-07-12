package nl.rhaydus.ktlint

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.REFERENCE_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * code-style §Error Handling: a use case wraps its body in `runCatchingLogged { … }`, never bare
 * `runCatching { … }`. Bare `runCatching` swallows `CancellationException` (breaking structured
 * concurrency) and logs nothing, so a failure dropped by the caller vanishes silently. The
 * cancellation-safe `runCatchingLogged` (and the pure `runCatchingCancellable` primitive) live in
 * `nl.rhaydus:core-common`.
 *
 * Detect-only (not auto-corrected): the right replacement (`runCatchingLogged` vs `runCatchingCancellable`)
 * is a judgement call. Scoped to production `*UseCase*.kt` source, matching the convention's reach.
 */
class UseCaseRunCatchingRule :
    Rule(
        ruleId = RuleId("rhaydus:use-case-run-catching"),
        about = About(
            maintainer = "rhaydus",
            repositoryUrl = "",
            issueTrackerUrl = "",
        ),
    ),
    RuleAutocorrectApproveHandler {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != CALL_EXPRESSION) return

        if (node.findChildByType(REFERENCE_EXPRESSION)?.text != "runCatching") return

        if (node.isInTestSource()) return

        if (node.fileName().contains("UseCase").not()) return

        emit(
            node.startOffset,
            "Use runCatchingCancellable (or runCatchingLogged to also log at the source) instead of bare runCatching (§Error Handling)",
            false,
        )
    }
}
