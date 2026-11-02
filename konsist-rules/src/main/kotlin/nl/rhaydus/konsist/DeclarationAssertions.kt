package nl.rhaydus.konsist

import com.lemonappdev.konsist.api.container.KoScope
import com.lemonappdev.konsist.api.declaration.KoBaseDeclaration
import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.declaration.KoInterfaceDeclaration
import com.lemonappdev.konsist.api.declaration.KoObjectDeclaration
import com.lemonappdev.konsist.api.provider.KoNameProvider
import com.lemonappdev.konsist.api.provider.KoParentProvider
import com.lemonappdev.konsist.api.provider.modifier.KoSealedModifierProvider
import com.lemonappdev.konsist.api.provider.modifier.KoVisibilityModifierProvider

/** The TOAD presentation and data-layer plumbing that stays inside its module. */
val TOAD_PLUMBING: List<PlumbingRule> = listOf(
    PlumbingRule(
        ".presentation.action",
        listOf("Action"),
    ),
    PlumbingRule(
        ".presentation.event",
        listOf("Event"),
    ),
    PlumbingRule(
        ".presentation.state",
        listOf("UiState", "LocalVariables"),
    ),
    PlumbingRule(
        ".presentation.screenmodel",
        listOf("ScreenModel", "Dependencies"),
    ),
    PlumbingRule(
        ".presentation.flows",
        listOf("Collector", "Initializer", "Loader"),
    ),
    PlumbingRule(
        ".data.repository",
        listOf("Impl"),
    ),
    PlumbingRule(
        ".data.datasource",
        listOf("Impl"),
    ),
)

/**
 * Fails when a file declares more than one top-level type (class, interface or object).
 *
 * Two co-locations stay allowed: one sealed type with its own variants, and one interface with its single
 * implementing class.
 */
fun KoScope.assertOneTypePerFile(excludedPaths: List<String> = listOf(FOUNDATION_PATH)) {
    val violations = sourceFiles(excludedPaths).mapNotNull { file ->
        val types = file.declarations(
            includeNested = false,
            includeLocal = false,
        )
            .filter { it is KoClassDeclaration || it is KoInterfaceDeclaration || it is KoObjectDeclaration }

        if (types.size <= 1 || types.isSealedHierarchy() || types.isInterfaceWithImplementation()) return@mapNotNull null

        "${file.relativePath}: ${types.size} top-level types (${types.joinToString { it.nameOrEmpty() }})"
    }

    failOnViolations(
        rule = "One type per file",
        violations = violations,
    )
}

/**
 * Fails when a top-level declaration matching a [PlumbingRule] is neither `internal` nor `private`.
 */
fun KoScope.assertPlumbingIsInternal(
    rules: List<PlumbingRule> = TOAD_PLUMBING,
    excludedPaths: List<String> = listOf(FOUNDATION_PATH),
) {
    val violations = sourceFiles(excludedPaths).flatMap { file ->
        val packageName = file.packagee?.name.orEmpty()

        file.declarations(
            includeNested = false,
            includeLocal = false,
        )
            .filterIsInstance<KoVisibilityModifierProvider>()
            .filter { it.hasInternalModifier.not() && it.hasPrivateModifier.not() }
            .filter { declaration ->
                val name = declaration.nameOrEmpty()

                rules.any { rule -> ".$packageName".contains(rule.packageFragment) && rule.nameSuffixes.any { name.endsWith(it) } }
            }
            .map { "${file.relativePath}: ${it.nameOrEmpty()} is module-internal plumbing; mark it internal" }
    }

    failOnViolations(
        rule = "Plumbing visibility",
        violations = violations,
    )
}

/**
 * Fails when a `@Test` function is not declared in a `@Nested inner class`.
 *
 * Only files under `/src/<set>/` for one of [testSourceSets] are checked.
 */
fun KoScope.assertTestsAreNested(
    testSourceSets: List<String> = listOf("test", "commonTest", "androidHostTest", "jvmTest", "androidUnitTest"),
    testAnnotations: List<String> = listOf("Test", "ParameterizedTest"),
    excludedPaths: List<String> = listOf(FOUNDATION_PATH),
) {
    val violations = sourceFiles(excludedPaths)
        .filter { file -> testSourceSets.any { "/src/$it/" in "/${file.relativePath}" } }
        .flatMap { file ->
            val topLevel = file.functions(
                includeNested = false,
                includeLocal = false,
            )
                .filter { function -> testAnnotations.any { function.hasAnnotationWithName(it) } }
                .map { "${it.location}: ${it.name} is a test outside a @Nested inner class" }

            val inClasses = file.classes(includeNested = true)
                .filter { it.hasInnerModifier.not() || it.hasAnnotationWithName("Nested").not() }
                .flatMap { owner ->
                    owner.functions(
                        includeNested = false,
                        includeLocal = false,
                    )
                        .filter { function -> testAnnotations.any { function.hasAnnotationWithName(it) } }
                        .map { "${it.location}: ${owner.name}.${it.name} is a test outside a @Nested inner class" }
                }

            topLevel + inClasses
        }

    failOnViolations(
        rule = "Nested tests",
        violations = violations,
    )
}

fun KoScope.assertNoModelFieldNamed(
    forbiddenName: String = "action",
    modelSuffix: String = "UiModel",
    excludedPaths: List<String> = listOf(FOUNDATION_PATH),
) {
    val violations = sourceFiles(excludedPaths)
        .flatMap { it.classes(includeNested = true) }
        .filter { it.name.endsWith(modelSuffix) }
        .flatMap { model ->
            model.properties()
                .filter { it.name == forbiddenName }
                .map { "${it.location}: ${model.name}.$forbiddenName is reserved; name the field after what it holds" }
        }

    failOnViolations(
        rule = "Model field names",
        violations = violations,
    )
}

/**
 * Fails when a [stateSuffix] class builds a [modelSuffix] class in one of its members: a `get()`-bodied
 * property, or a function that returns a model and constructs, converts or `copy()`s one.
 *
 * Constructor calls and conversion calls such as `toFooUiModel()` count, qualified or not; the declared type
 * does not. A function without a declared return type counts only for a construction or conversion. A
 * reducer that returns the state itself is allowed, and so is any member annotated with one of
 * [exemptAnnotations].
 *
 * Such a member runs the builder on every read; keep a stored field with a default and `copy()` into it.
 */
fun KoScope.assertNoUiModelBuiltInStateMember(
    stateSuffix: String = "UiState",
    modelSuffix: String = "UiModel",
    exemptAnnotations: List<String> = emptyList(),
    excludedPaths: List<String> = listOf(FOUNDATION_PATH),
) {
    val construction = Regex("\\w*$modelSuffix\\w*\\s*[(<]")
    val copy = Regex("\\.copy\\s*\\(")
    val signature = Regex("^get\\s*\\(\\s*\\)\\s*(:[^={]*)?")

    val violations = sourceFiles(excludedPaths)
        .flatMap { it.classes(includeNested = true) }
        .filter { it.name.endsWith(stateSuffix) }
        .flatMap { state ->
            val getters = state.properties()
                .filter { property -> exemptAnnotations.none { property.hasAnnotationWithName(it) } }
                .filter { property ->
                    val body = property.getter?.text?.replace(
                        signature,
                        "",
                    ).orEmpty()

                    construction.containsMatchIn(body)
                }
                .map { "${it.location}: ${state.name}.${it.name} builds a $modelSuffix in a getter" }

            val functions = state.functions(
                includeNested = false,
                includeLocal = false,
            )
                .filter { function -> exemptAnnotations.none { function.hasAnnotationWithName(it) } }
                .filter { function ->
                    val returnType = function.returnType?.text
                    val body = function.text

                    when {
                        returnType == null -> construction.containsMatchIn(body)
                        returnType.contains(modelSuffix) ->
                            construction.containsMatchIn(body) || copy.containsMatchIn(body)
                        else -> false
                    }
                }
                .map { "${it.location}: ${state.name}.${it.name}() builds a $modelSuffix" }

            getters + functions
        }

    failOnViolations(
        rule = "State members",
        violations = violations,
    )
}

private fun Any.nameOrEmpty(): String = (this as? KoNameProvider)?.name.orEmpty()

private fun List<KoBaseDeclaration>.isSealedHierarchy(): Boolean {
    val sealed = filter { (it as? KoSealedModifierProvider)?.hasSealedModifier == true }

    val parent = sealed.singleOrNull() ?: return false

    val parentName = parent.nameOrEmpty()

    return filter { it !== parent }.all { subtype ->
        (subtype as? KoParentProvider)?.parents()?.any { it.name == parentName } == true
    }
}

private fun List<KoBaseDeclaration>.isInterfaceWithImplementation(): Boolean {
    if (size != 2) return false

    val contract = filterIsInstance<KoInterfaceDeclaration>().singleOrNull() ?: return false

    val implementation = single { it !== contract }

    return (implementation as? KoParentProvider)?.parents()?.any { it.name == contract.name } == true
}
