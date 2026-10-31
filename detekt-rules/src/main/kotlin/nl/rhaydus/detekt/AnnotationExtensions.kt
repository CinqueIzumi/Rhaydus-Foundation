package nl.rhaydus.detekt

import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

internal fun KtAnnotated.isAnnotatedWith(name: String): Boolean {
    return annotationEntries.any { it.shortName?.asString() == name }
}

internal fun KtFile.declaresAnnotated(name: String): Boolean {
    return collectDescendantsOfType<KtAnnotated> { it.isAnnotatedWith(name) }.isNotEmpty()
}
