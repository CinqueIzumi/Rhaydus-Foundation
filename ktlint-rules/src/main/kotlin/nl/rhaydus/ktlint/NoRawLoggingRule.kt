package nl.rhaydus.ktlint

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.REFERENCE_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * code-style §Error Handling: the shared `AppLog` facade (`nl.rhaydus:core-common`) is the logger — never
 * raw `println` or `android.util.Log.*`. `println` writes to stdout (invisible on device, unstructured on
 * desktop) and `Log.*` is Android-only and bypasses the debug-gated, multiplatform facade.
 *
 * Detect-only (not auto-corrected): the replacement (`AppLog.i` / `AppLog.w` / `AppLog.e`, choosing a
 * level) is a judgement call, not a mechanical rewrite. Pure-AST — it matches on call shape: an
 * unqualified `println(...)` (or a `System.out` / `System.err` `.println(...)`), and any `Log.*` /
 * `android.util.Log.*` call. A member call on some other receiver (`logger.println(...)`) is left alone.
 *
 * Pure-AST limitations (accepted): the `Log` branch matches on receiver text, so a call on an app class
 * literally named `Log` would also flag (such a class is itself a smell, colliding with `android.util.Log`);
 * conversely a `Log` reached via an import alias or a `Log?.d()` safe-call is not matched. A fully-qualified
 * `kotlin.io.println(...)` is caught by the sibling inline-fully-qualified-reference rule instead.
 */
class NoRawLoggingRule :
    Rule(
        ruleId = RuleId("rhaydus:no-raw-logging"),
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

        val callee = node.findChildByType(REFERENCE_EXPRESSION)?.text ?: return
        val qualifierReceiver = node.treeParent
            ?.takeIf { it.elementType == DOT_QUALIFIED_EXPRESSION && it.lastChildNode == node }
            ?.firstChildNode
            ?.text

        val isRawPrintln = callee == "println" &&
            (qualifierReceiver == null || qualifierReceiver == "System.out" || qualifierReceiver == "System.err")

        if (isRawPrintln) {
            emit(
                node.startOffset,
                MESSAGE_PRINTLN,
                false,
            )

            return
        }

        if (qualifierReceiver == "Log" || qualifierReceiver?.endsWith(".Log") == true) {
            emit(
                node.startOffset,
                MESSAGE_LOG,
                false,
            )
        }
    }

    private companion object {
        const val MESSAGE_PRINTLN = "Use the AppLog facade (AppLog.i/w/e), not raw println (§Error Handling)"
        const val MESSAGE_LOG = "Use the AppLog facade (AppLog.i/w/e), not android.util.Log.* (§Error Handling)"
    }
}
