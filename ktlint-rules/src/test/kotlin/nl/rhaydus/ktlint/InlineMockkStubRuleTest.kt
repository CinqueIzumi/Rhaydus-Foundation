package nl.rhaydus.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private val ruleAssertThat = assertThatRule { InlineMockkStubRule() }

class InlineMockkStubRuleTest {
    @Nested
    inner class Fires {
        @Test
        fun `fires for single-line coEvery stub in test source`() {
            // ----- Arrange -----
            val code = """
                fun test() {
                coEvery { repo.load() } returns 5
                }
            """.trimIndent()

            // ----- Act & Assert -----
            // Line 2: "coEvery " = 8 chars, lambda { starts at col 9
            ruleAssertThat(code)
                .asFileWithPath("/x/src/commonTest/kotlin/FooTest.kt")
                .hasLintViolationWithoutAutoCorrect(
                    2,
                    9,
                    "Open the coEvery block onto its own line, blank line after its closing } (§Whitespace)",
                )
        }

        @Test
        fun `fires for single-line every stub in test source`() {
            // ----- Arrange -----
            val code = """
                fun test() {
                every { repo.name } returns "foo"
                }
            """.trimIndent()

            // ----- Act & Assert -----
            // Line 2: "every " = 6 chars, lambda { starts at col 7
            ruleAssertThat(code)
                .asFileWithPath("/x/src/commonTest/kotlin/FooTest.kt")
                .hasLintViolationWithoutAutoCorrect(
                    2,
                    7,
                    "Open the every block onto its own line, blank line after its closing } (§Whitespace)",
                )
        }
    }

    @Nested
    inner class Clean {
        @Test
        fun `clean for multi-line coEvery stub in test source`() {
            // ----- Arrange -----
            val code = """
                fun test() {
                coEvery {
                    repo.load()
                } returns 5
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .asFileWithPath("/x/src/commonTest/kotlin/FooTest.kt")
                .hasNoLintViolations()
        }

        @Test
        fun `clean for single-line coEvery stub in main source`() {
            // ----- Arrange -----
            val code = """
                fun test() {
                coEvery { repo.load() } returns 5
                }
            """.trimIndent()

            // ----- Act & Assert -----
            // Rule is scoped to test sources only — must NOT fire in main source
            ruleAssertThat(code)
                .asFileWithPath("/x/src/commonMain/kotlin/SomeClass.kt")
                .hasNoLintViolations()
        }
    }
}
