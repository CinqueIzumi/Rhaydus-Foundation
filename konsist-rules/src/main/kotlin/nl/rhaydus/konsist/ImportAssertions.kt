package nl.rhaydus.konsist

import com.lemonappdev.konsist.api.container.KoScope

private val BLOCK_COMMENT = Regex(
    "/\\*.*?\\*/",
    RegexOption.DOT_MATCHES_ALL,
)
private val LINE_COMMENT = Regex("//[^\n]*")
private val DIRECTIVE = Regex(
    "^\\s*(package|import)\\s[^\n]*",
    RegexOption.MULTILINE,
)

/**
 * Fails when a file imports, aliases or fully qualifies a [ForbiddenImport] outside its allowed files.
 *
 * A wildcard import of the banned name's package counts when the file mentions the banned simple name in code.
 * Comments and the package and import lines are not scanned for fully-qualified references.
 */
fun KoScope.assertNoForbiddenImports(
    bans: List<ForbiddenImport>,
    excludedPaths: List<String> = listOf(FOUNDATION_PATH),
) {
    val violations = sourceFiles(excludedPaths).flatMap { file ->
        val code = file.text.replace(
            BLOCK_COMMENT,
            "",
        ).replace(
            LINE_COMMENT,
            "",
        ).replace(
            DIRECTIVE,
            "",
        )

        bans
            .filter { ban ->
                ban.allowedFiles.none {
                    matchesGlob(
                        it,
                        file.relativePath,
                    )
                }
            }
            .filter { ban ->
                val simple = ban.import.substringAfterLast('.')
                val packageName = ban.import.substringBeforeLast('.')

                file.imports.any { it.name == ban.import || it.isWildcard && it.name == packageName && code.mentions(simple) } ||
                    Regex("(?<![\\w.])${Regex.escape(ban.import)}(?!\\w)").containsMatchIn(code)
            }
            .map { ban -> "${file.relativePath}: ${ban.import} - ${ban.reason}" }
    }

    failOnViolations(
        rule = "Forbidden imports",
        violations = violations,
    )
}

/**
 * Fails when a file in a lower layer package imports a package of the upper layer.
 *
 * A layer is matched as a whole segment of the file's package or of the import, so `data` matches
 * `app.feature.data.source` but not `app.database`.
 */
fun KoScope.assertLowerLayersDoNotImportUpperLayer(
    lowerLayers: List<String> = listOf("data", "domain"),
    upperLayer: String = "presentation",
    excludedPaths: List<String> = listOf(FOUNDATION_PATH),
) {
    val violations = sourceFiles(excludedPaths).flatMap { file ->
        val segments = file.packagee?.name.orEmpty().split('.')

        val layer = lowerLayers.firstOrNull { it in segments } ?: return@flatMap emptyList()

        file.imports
            .filter { upperLayer in it.name.split('.') }
            .map { "${file.relativePath}: the $layer layer imports ${it.name}" }
    }

    failOnViolations(
        rule = "Layering ($lowerLayers must not import $upperLayer)",
        violations = violations,
    )
}

private fun String.mentions(simpleName: String): Boolean {
    return Regex("(?<!\\w)${Regex.escape(simpleName)}(?!\\w)").containsMatchIn(this)
}
