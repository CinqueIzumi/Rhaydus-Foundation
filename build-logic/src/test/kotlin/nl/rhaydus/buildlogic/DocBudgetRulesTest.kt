package nl.rhaydus.buildlogic

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.api.GradleException
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DocBudgetRulesTest {
    @Nested
    inner class Parse {
        @Test
        fun `reads bytes, kilobytes and lines units into limits`() {
            // ----- Arrange -----
            val text = """
                a.md        10B
                b.md        2KB
                c.md        30L
            """.trimIndent()

            // ----- Act -----
            val rules = DocBudgets.parse(text).rules

            // ----- Assert -----
            rules.map { it.limit to it.unit } shouldBe listOf(
                10L to BudgetUnit.BYTES,
                2048L to BudgetUnit.BYTES,
                30L to BudgetUnit.LINES,
            )
        }

        @Test
        fun `skips blank lines and comment lines`() {
            // ----- Arrange -----
            val text = "# heading\n\n   \na.md 10B\n# trailing note\n"

            // ----- Act -----
            val rules = DocBudgets.parse(text).rules

            // ----- Assert -----
            rules.map { it.glob } shouldBe listOf("a.md")
        }

        @Test
        fun `reads the Now section limit apart from the file rules`() {
            // ----- Arrange -----
            val text = "## Now      12L\na.md 10B\n"

            // ----- Act -----
            val budgets = DocBudgets.parse(text)

            // ----- Assert -----
            budgets.nowLimit shouldBe 12L
            budgets.rules.map { it.glob } shouldBe listOf("a.md")
        }

        @Test
        fun `leaves the Now limit unset when the file has no Now line`() {
            // ----- Arrange -----
            val text = "a.md 10B\n"

            // ----- Act -----
            val budgets = DocBudgets.parse(text)

            // ----- Assert -----
            budgets.nowLimit shouldBe null
        }

        @Test
        fun `keeps rules in file order so the first match wins`() {
            // ----- Arrange -----
            val text = "docs/a.md 5B\ndocs/*.md 500B\n"

            // ----- Act -----
            val winner = DocBudgets.parse(text).rules.first { it.matches("docs/a.md") }

            // ----- Assert -----
            winner.limit shouldBe 5L
        }

        @Test
        fun `rejects an unrecognised unit`() {
            // ----- Arrange -----
            val text = "a.md 10X\n"

            // ----- Act & Assert -----
            val exception = shouldThrow<GradleException> { DocBudgets.parse(text) }
            exception.message shouldContain "10X"
        }
    }

    @Nested
    inner class Matches {
        @Test
        fun `keeps a single star inside one path segment`() {
            // ----- Arrange -----
            val rule = BudgetRule("docs/*.md", "1B", 1, BudgetUnit.BYTES)

            // ----- Act -----
            val flat = rule.matches("docs/a.md")
            val nested = rule.matches("docs/sub/a.md")

            // ----- Assert -----
            flat.shouldBeTrue()
            nested.shouldBeFalse()
        }

        @Test
        fun `lets a double star cross path segments`() {
            // ----- Arrange -----
            val rule = BudgetRule("docs/**/*.md", "1B", 1, BudgetUnit.BYTES)

            // ----- Act -----
            val nested = rule.matches("docs/sub/deeper/a.md")

            // ----- Assert -----
            nested.shouldBeTrue()
        }

        @Test
        fun `treats a dot as a literal character`() {
            // ----- Arrange -----
            val rule = BudgetRule("docs/a.md", "1B", 1, BudgetUnit.BYTES)

            // ----- Act -----
            val lookalike = rule.matches("docs/aXmd")

            // ----- Assert -----
            lookalike.shouldBeFalse()
        }
    }
}
