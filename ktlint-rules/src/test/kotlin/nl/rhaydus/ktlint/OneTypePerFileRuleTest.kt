package nl.rhaydus.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private val ruleAssertThat = assertThatRule { OneTypePerFileRule() }

class OneTypePerFileRuleTest {
    @Nested
    inner class Fires {
        @Test
        fun `fires when file contains two unrelated top-level classes`() {
            // ----- Arrange -----
            val code = """
                class Alpha

                class Beta
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasLintViolationWithoutAutoCorrect(
                3,
                1,
                "More than one top-level type in this file (2) - one type per file, named after it; only a single sealed type's own variants may co-locate (§Files)",
            )
        }
    }

    @Nested
    inner class Clean {
        @Test
        fun `clean when file contains a single class`() {
            // ----- Arrange -----
            val code = """
                class Alpha
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `clean for sealed hierarchy with subtypes in the same file`() {
            // ----- Arrange -----
            val code = """
                sealed interface Shape

                data class Circle(val radius: Int) : Shape

                data object Square : Shape
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `clean for one classifier plus a top-level function`() {
            // ----- Arrange -----
            val code = """
                class Alpha

                fun helper(): Unit = Unit
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `clean for one classifier plus a top-level property`() {
            // ----- Arrange -----
            val code = """
                class Alpha

                val CONSTANT = 42
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }
    }
}
