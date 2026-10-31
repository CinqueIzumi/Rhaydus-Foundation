package nl.rhaydus.buildlogic

import org.gradle.api.GradleException

/** The parsed budgets file: first-match-wins [rules] plus the optional `## Now` line limit. */
internal data class DocBudgets(
    val rules: List<BudgetRule>,
    val nowLimit: Long?,
) {
    companion object {
        fun parse(text: String): DocBudgets {
            val rules = mutableListOf<BudgetRule>()
            var nowLimit: Long? = null

            text.lineSequence().forEach { rawLine ->
                val line = rawLine.trim()

                when {
                    line.isBlank() -> Unit
                    line.startsWith("## Now") -> nowLimit = parseLimit(line.split(WHITESPACE).last()).first
                    line.startsWith("#") -> Unit
                    else -> {
                        val fields = line.split(WHITESPACE)
                        val (limit, unit) = parseLimit(fields.last())

                        rules += BudgetRule(
                            fields.first(),
                            fields.last(),
                            limit,
                            unit,
                        )
                    }
                }
            }

            return DocBudgets(
                rules,
                nowLimit,
            )
        }

        private val WHITESPACE = Regex("\\s+")
    }
}

private fun parseLimit(token: String): Pair<Long, BudgetUnit> = when {
    token.endsWith("KB") -> (token.removeSuffix("KB").toLong() * 1024) to BudgetUnit.BYTES
    token.endsWith("B") -> token.removeSuffix("B").toLong() to BudgetUnit.BYTES
    token.endsWith("L") -> token.removeSuffix("L").toLong() to BudgetUnit.LINES
    else -> throw GradleException("Unrecognised doc budgets unit in token '$token'")
}
