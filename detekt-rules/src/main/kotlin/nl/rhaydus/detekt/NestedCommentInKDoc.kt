package nl.rhaydus.detekt

import com.intellij.psi.PsiElement
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.kdoc.psi.api.KDoc

/**
 * Reports a KDoc that contains the characters that open a block comment. Kotlin block comments nest, so
 * the stray opener leaves the comment unclosed and swallows the code after it.
 */
class NestedCommentInKDoc(config: Config) :
    Rule(
        config,
        "A KDoc must not contain a block-comment opener.",
    ) {
    override fun visitElement(element: PsiElement) {
        super.visitElement(element)

        if (element !is KDoc) return

        if (element.text.removePrefix(KDOC_OPENER).contains(COMMENT_OPENER).not()) return

        report(
            Finding(
                entity = Entity.from(element),
                message = "Kotlin block comments nest; remove the block-comment opener from this KDoc.",
            ),
        )
    }

    private companion object {
        const val KDOC_OPENER = "/**"
        const val COMMENT_OPENER = "/*"
    }
}
