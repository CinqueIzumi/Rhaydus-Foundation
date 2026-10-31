package nl.rhaydus.ktlint

import com.pinterest.ktlint.rule.engine.core.api.ElementType.BINARY_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BREAK
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CONTINUE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ELSE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ELVIS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EOL_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IF
import com.pinterest.ktlint.rule.engine.core.api.ElementType.KDOC
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OPERATION_REFERENCE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.REFERENCE_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RETURN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SUPER_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.THEN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.THROW
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.children
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

internal fun ASTNode.isComment(): Boolean = elementType == EOL_COMMENT || elementType == BLOCK_COMMENT || elementType == KDOC

internal fun ASTNode.isRegionMarker(): Boolean {
    if (elementType != EOL_COMMENT) return false

    val text = this.text.trimStart(
        '/',
        ' ',
    )

    return text.startsWith("region") || text.startsWith("endregion")
}

internal fun ASTNode.isGuardClause(): Boolean = when (elementType) {
    IF -> isJumpGuardIf()
    PROPERTY -> children().any { it.elementType == BINARY_EXPRESSION && it.isElvisJump() }
    BINARY_EXPRESSION -> isElvisJump()
    else -> false
}

/** `if (…) <jump>` or `if (…) { <jump> }` without an `else`. */
private fun ASTNode.isJumpGuardIf(): Boolean {
    if (children().any { it.elementType == ELSE }) return false

    val then = children().firstOrNull { it.elementType == THEN } ?: return false

    val body = then.children().firstOrNull { it.elementType != WHITE_SPACE } ?: return false

    if (body.isJump()) return true

    if (body.elementType != BLOCK) return false

    val statements = body.children().filter { it.elementType != WHITE_SPACE && it.isBraceToken().not() }.toList()

    return statements.size == 1 && statements.single().isJump()
}

internal fun ASTNode.isBraceToken(): Boolean =
    elementType == LBRACE || elementType == RBRACE

private fun ASTNode.isElvisJump(): Boolean {
    val operator = children().firstOrNull { it.elementType == OPERATION_REFERENCE } ?: return false

    if (operator.firstChildNode?.elementType != ELVIS) return false

    return lastChildNode?.isJump() == true
}

private fun ASTNode.isJump(): Boolean =
    elementType == RETURN || elementType == THROW || elementType == CONTINUE || elementType == BREAK

private fun ASTNode.isSuperCall(): Boolean =
    elementType == DOT_QUALIFIED_EXPRESSION && firstChildNode?.elementType == SUPER_EXPRESSION

private fun ASTNode.isAppLogErrorLog(): Boolean {
    if (elementType != DOT_QUALIFIED_EXPRESSION) return false

    val receiver = firstChildNode

    if (receiver?.elementType != REFERENCE_EXPRESSION || receiver.text != "AppLog") return false

    val selector = children().lastOrNull { it.elementType == CALL_EXPRESSION } ?: return false

    return selector.children().firstOrNull { it.elementType == REFERENCE_EXPRESSION }?.text == "e"
}

internal fun ASTNode.isAfterSpaced(): Boolean = isSuperCall() || isAppLogErrorLog()
