package nl.rhaydus.detekt

import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

/**
 * Reports a type, top-level function, enum entry or object member reached by its fully-qualified name
 * inline. An `import` and the short name replace it.
 *
 * Conservative: only a reference rooted at one of `rootPackages` and reaching an uppercase segment counts,
 * with or without a package in between (`kotlin.String` and `kotlin.collections.List`), so a plain lowercase
 * chain such as `kotlinx.coroutines.flow.first` is left alone. A reference whose own package prefix ends
 * directly under one of `exemptPackageSegments` is allowed, for generated code whose names
 * collide. Imports, packages, strings and KDoc are never matched.
 */
class InlineFullyQualifiedReference(config: Config) :
    Rule(
        config,
        "Import the type and use its short name instead of a fully-qualified reference.",
    ) {
    @Configuration("The top-level packages a reference must be rooted at to count.")
    private val rootPackages: List<String> by config(
        listOf("androidx", "android", "java", "javax", "kotlin", "kotlinx", "nl.rhaydus"),
    )

    @Configuration("Package segments whose types may stay fully-qualified.")
    private val exemptPackageSegments: List<String> by config(emptyList<String>())

    private val fullyQualified: Regex by lazy {
        Regex("^(${rootPackages.joinToString("|") { Regex.escape(it) }})(\\.[a-z][A-Za-z0-9_.]*)?\\.[A-Z]")
    }

    private val exemptReference: Regex? by lazy {
        exemptPackageSegments
            .takeIf { it.isNotEmpty() }
            ?.let { Regex("\\.(${it.joinToString("|") { segment -> Regex.escape(segment) }})\\.[A-Z]$") }
    }

    override fun visitUserType(type: KtUserType) {
        super.visitUserType(type)

        if (type.parent is KtUserType) return

        check(type)
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)

        if (expression.parent is KtDotQualifiedExpression) return

        check(expression)
    }

    private fun check(element: KtElement) {
        if (element.getStrictParentOfType<KtImportDirective>() != null) return

        if (element.getStrictParentOfType<KtPackageDirective>() != null) return

        val prefix = fullyQualified.find(element.text)?.value ?: return

        if (exemptReference?.containsMatchIn(prefix) == true) return

        report(
            Finding(
                entity = Entity.from(element),
                message = "Inline fully-qualified reference; add an import and use the short name.",
            ),
        )
    }
}
