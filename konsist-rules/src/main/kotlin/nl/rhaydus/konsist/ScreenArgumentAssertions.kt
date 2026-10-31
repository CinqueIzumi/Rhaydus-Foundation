package nl.rhaydus.konsist

import com.lemonappdev.konsist.api.container.KoScope
import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.declaration.KoInterfaceDeclaration
import com.lemonappdev.konsist.api.declaration.KoObjectDeclaration
import com.lemonappdev.konsist.api.declaration.KoParameterDeclaration
import com.lemonappdev.konsist.api.provider.KoParentProvider

private val SERIALIZABLE_BUILT_INS = setOf(
    "Boolean", "Byte", "Char", "Short", "Int", "Long", "Float", "Double", "String",
    "UInt", "ULong", "UShort", "UByte",
    "BooleanArray", "ByteArray", "CharArray", "ShortArray", "IntArray", "LongArray", "FloatArray", "DoubleArray",
    "UIntArray", "ULongArray", "UShortArray", "UByteArray",
)

/**
 * Fails when a constructor property of a Screen cannot survive Android's Java serialization of the back stack.
 *
 * It must be a primitive, an unsigned type, a primitive array, a String, an enum, or a type declared in the
 * scope that has one of [serializableSupertypes] among its supertypes, or carry [transientAnnotation]. A type
 * that does not resolve to one of these is reported, including collections and external types.
 *
 * @param screenSupertypes simple names of the Screen base types, matched against direct and indirect parents.
 */
fun KoScope.assertScreenArgumentsSurviveSerialization(
    screenSupertypes: List<String>,
    serializableSupertypes: List<String> = listOf("Serializable"),
    transientAnnotation: String = "TransientNavArg",
    excludedPaths: List<String> = listOf(FOUNDATION_PATH),
) {
    val violations = sourceFiles(excludedPaths)
        .flatMap { it.classes(includeNested = true) }
        .filter { screen -> screen.parents(indirectParents = true).any { it.name in screenSupertypes } }
        .flatMap { screen ->
            screen.primaryConstructor?.parameters.orEmpty()
                .filter { it.hasValModifier || it.hasVarModifier }
                .filter { it.hasAnnotationWithName(transientAnnotation).not() }
                .filter {
                    isSerializable(
                        parameter = it,
                        serializableSupertypes = serializableSupertypes,
                    ).not()
                }
                .map { "${it.location}: ${screen.name}.${it.name} is not serializable; annotate it with @$transientAnnotation" }
        }

    failOnViolations(
        rule = "Screen arguments",
        violations = violations,
    )
}

private fun isSerializable(
    parameter: KoParameterDeclaration,
    serializableSupertypes: List<String>,
): Boolean {
    val type = parameter.type

    if (type.name in SERIALIZABLE_BUILT_INS && type.typeArguments.isNullOrEmpty()) return true

    val source = type.sourceDeclaration ?: return false

    return when (source) {
        is KoClassDeclaration -> source.hasEnumModifier || source.hasSerializableParent(serializableSupertypes)
        is KoInterfaceDeclaration -> source.hasSerializableParent(serializableSupertypes)
        is KoObjectDeclaration -> source.hasSerializableParent(serializableSupertypes)
        else -> false
    }
}

private fun KoParentProvider.hasSerializableParent(serializableSupertypes: List<String>): Boolean {
    return parents(indirectParents = true).any { it.name in serializableSupertypes }
}
