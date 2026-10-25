package nl.rhaydus.buildlogic

internal data class BudgetRule(
    val glob: String,
    val token: String,
    val limit: Long,
    val unit: BudgetUnit,
) {
    private val matcher = globToRegex(glob)

    fun matches(relativePath: String): Boolean = matcher.matches(relativePath)
}

/** `*` stays within a path segment, `**` crosses segments; everything else matches literally. */
private fun globToRegex(glob: String): Regex {
    val pattern = StringBuilder("^")
    var index = 0
    while (index < glob.length) {
        when {
            glob.startsWith(
                "**",
                index,
            ) -> {
                pattern.append(".*")
                index += 2
            }

            glob[index] == '*' -> {
                pattern.append("[^/]*")
                index += 1
            }

            glob[index] in ".^$+?()[]{}|\\" -> {
                pattern.append('\\').append(glob[index])
                index += 1
            }

            else -> {
                pattern.append(glob[index])
                index += 1
            }
        }
    }
    pattern.append("$")
    return Regex(pattern.toString())
}
