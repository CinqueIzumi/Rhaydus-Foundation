package nl.rhaydus.konsist

import com.lemonappdev.konsist.api.container.KoScope
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import java.nio.file.FileSystems
import java.nio.file.Paths

/** The glob of the foundation submodule, which `scopeFromProject()` also scans. */
const val FOUNDATION_PATH: String = "foundation/**"

internal fun KoScope.sourceFiles(excludedPaths: List<String>): List<KoFileDeclaration> {
    return files.filter { file ->
        excludedPaths.none {
            matchesGlob(
                it,
                file.relativePath,
            )
        }
    }
}

/**
 * A glob that opens with a double-star directory segment also matches when that directory is absent, so it
 * reaches a root-level module too.
 */
internal fun matchesGlob(
    glob: String,
    path: String,
): Boolean {
    val target = Paths.get(path)
    val patterns = listOf(glob, glob.removePrefix("**/")).distinct()

    return patterns.any { FileSystems.getDefault().getPathMatcher("glob:$it").matches(target) }
}

internal val KoFileDeclaration.relativePath: String
    get() = projectPath.trimStart('/')

internal fun failOnViolations(
    rule: String,
    violations: List<String>,
) {
    if (violations.isEmpty()) return

    throw AssertionError(
        "$rule: ${violations.size} violation(s)\n" + violations.distinct().sorted().joinToString("\n") { "  $it" },
    )
}
