package nl.rhaydus.detekt

import dev.detekt.api.Config
import dev.detekt.test.lint
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FileLevelDpPropertyTest {
    private val rule = FileLevelDpProperty(Config.empty)

    @Nested
    inner class Flagged {
        @Test
        fun `flags a top-level Dp typed property`() {
            // ----- Arrange -----
            val code = "val gap: Dp = Dp(4f)"

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `flags a nullable top-level Dp typed property`() {
            // ----- Arrange -----
            val code = "val gap: Dp? = null"

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `flags a top-level dp literal initializer`() {
            // ----- Arrange -----
            val code = "val gap = 12.dp"

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `flags a holder built only from dp arguments`() {
            // ----- Arrange -----
            val code = "val spec = BarSpec(width = 32.dp, height = 8.dp)"

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }
    }

    @Nested
    inner class Allowed {
        @Test
        fun `allows a local dp value`() {
            // ----- Arrange -----
            val code = """
            fun f() {
                val gap = 12.dp
            }
            """.trimIndent()

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }

        @Test
        fun `allows a class member dp value`() {
            // ----- Arrange -----
            val code = """
            class Foo {
                val gap = 12.dp
            }
            """.trimIndent()

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }

        @Test
        fun `allows a holder mixing dp and other arguments`() {
            // ----- Arrange -----
            val code = "val spec = BarSpec(width = 32.dp, color = red)"

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }

        @Test
        fun `allows a holder with no arguments`() {
            // ----- Arrange -----
            val code = "val spec = BarSpec()"

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }

        @Test
        fun `allows a non-dp unit literal`() {
            // ----- Arrange -----
            val code = "val size = 12.sp"

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }
    }
}
