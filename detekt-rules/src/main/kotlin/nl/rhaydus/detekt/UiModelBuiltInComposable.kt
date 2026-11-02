package nl.rhaydus.detekt

import com.intellij.psi.PsiElement
import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parents

/**
 * Reports a UI model built in render code: a constructor, `copy` or project function returning a `*UiModel`
 * inside a composable, a composable-typed lambda, or a property of a file that declares a composable.
 * Composition re-runs the build on every recomposition, so the model belongs in the mapper or the
 * ScreenModel.
 *
 * Type-resolved. A call counts when it returns a type whose name ends in `uiModelSuffix` and is declared
 * under one of `packagePrefixes`; a member of a `*UiState` class is not a build. A declaration annotated
 * with one of `exemptAnnotations`, or a function annotated with a name ending in one of
 * `exemptFunctionAnnotationSuffixes`, is skipped.
 */
class UiModelBuiltInComposable(config: Config) :
    Rule(
        config,
        "A UI model must be built outside render code.",
    ),
    RequiresAnalysisApi {
    @Configuration("The name suffix that marks a UI model type.")
    private val uiModelSuffix: String by config("UiModel")

    @Configuration("The name suffix that marks a UiState class, whose members are not builds.")
    private val uiStateSuffix: String by config("UiState")

    @Configuration("The package prefixes whose declarations count as project code.")
    private val packagePrefixes: List<String> by config(listOf("nl.rhaydus."))

    @Configuration("Annotations, matched on their short name, that exempt the declaration they mark.")
    private val exemptAnnotations: List<String> by config(listOf("ModelBuiltInComposition"))

    @Configuration("Name suffixes of annotations that exempt the function they mark, such as a preview.")
    private val exemptFunctionAnnotationSuffixes: List<String> by config(listOf("Preview"))

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val builtInRender = analyze(expression) {
            buildsUiModel(expression) && isBuiltInRender(expression)
        }

        if (builtInRender.not()) return

        report(
            Finding(
                entity = Entity.from(expression),
                message = "Build UI models in the mapper or ScreenModel, not in render code.",
            ),
        )
    }

    private fun KaSession.buildsUiModel(call: KtCallExpression): Boolean {
        val symbol = call.resolveToCall()?.successfulFunctionCallOrNull()?.symbol ?: return false

        if (isUiModel(symbol.returnType).not()) return false

        if (symbol.isUiStateMember()) return false

        return symbol is KaConstructorSymbol || symbol.callableId.isProjectCode()
    }

    private fun KaCallableSymbol.isUiStateMember(): Boolean {
        val owner = if (this is KaConstructorSymbol) containingClassId else callableId?.classId

        return owner != null &&
            owner.relativeClassName.shortName().asString().endsWith(uiStateSuffix) &&
            owner.asSingleFqName().asString().isProjectName()
    }

    private fun CallableId?.isProjectCode(): Boolean {
        return this?.asSingleFqName()?.asString()?.isProjectName() == true
    }

    private fun String.isProjectName(): Boolean = packagePrefixes.any { startsWith(it) }

    private fun KaSession.isBuiltInRender(call: KtCallExpression): Boolean {
        val ancestors = call.parents.toList()

        if (ancestors.any { it.isExempt() }) return false

        val inComposable = isInComposableScope(ancestors)

        val inRenderProperty = ancestors.any {
            it is KtProperty && it.isLocal.not() && call.containingKtFile.declaresAnnotated(COMPOSABLE)
        }

        return inComposable || inRenderProperty
    }

    /**
     * Walks up to the nearest lambda that runs later than composition (an event handler), which ends the
     * render scope. An inline lambda such as `map { }` or `remember { }` runs in place, so the walk goes on.
     */
    private fun KaSession.isInComposableScope(ancestors: List<PsiElement>): Boolean {
        for (ancestor in ancestors) {
            if (isComposable(ancestor)) return true

            if (ancestor is KtLambdaExpression && isInlineLambda(ancestor).not()) return false
        }

        return false
    }

    private fun KaSession.isInlineLambda(lambda: KtLambdaExpression): Boolean {
        val argument = lambda.parent as? KtValueArgument ?: return false

        val call = argument.getStrictParentOfType<KtCallExpression>() ?: return false

        val resolved = call.resolveToCall()?.successfulFunctionCallOrNull() ?: return false

        val function = resolved.symbol as? KaNamedFunctionSymbol ?: return false

        val parameter = resolved.argumentMapping[lambda]?.symbol ?: return false

        return function.isInline && parameter.isNoinline.not() && parameter.isCrossinline.not()
    }

    private fun KaSession.isComposable(element: PsiElement): Boolean {
        return when (element) {
            is KtLambdaExpression -> element.expectedType?.annotations?.any {
                it.classId?.shortClassName?.asString() == COMPOSABLE
            } == true

            is KtProperty -> element.isAnnotatedWith(COMPOSABLE) ||
                element.typeReference?.isAnnotatedWith(COMPOSABLE) == true

            is KtAnnotated -> element.isAnnotatedWith(COMPOSABLE)
            else -> false
        }
    }

    private fun PsiElement.isExempt(): Boolean {
        if (this !is KtAnnotated) return false

        return annotationEntries.any { entry ->
            val name = entry.shortName?.asString().orEmpty()

            name in exemptAnnotations ||
                (this is KtNamedFunction && exemptFunctionAnnotationSuffixes.any { name.endsWith(it) })
        }
    }

    private fun KaSession.isUiModel(type: KaType): Boolean {
        return (sequenceOf(type) + type.allSupertypes).any {
            (it as? KaClassType)?.classId?.relativeClassName?.shortName()?.asString()?.endsWith(uiModelSuffix) == true
        }
    }

    private companion object {
        const val COMPOSABLE = "Composable"
    }
}
