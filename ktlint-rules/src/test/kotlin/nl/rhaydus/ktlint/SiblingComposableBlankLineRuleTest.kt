package nl.rhaydus.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SiblingComposableBlankLineRuleTest {
    private val ruleAssertThat = assertThatRule { SiblingComposableBlankLineRule() }

    @Nested
    inner class Siblings {
        @Test
        fun `separates sibling composables in a layout scope with a blank line`() {
            // ----- Arrange -----
            val code = """
                fun Screen() {
                    Column {
                        Title()
                        Body()
                    }
                }
            """.trimIndent()

            val formatted = """
                fun Screen() {
                    Column {
                        Title()

                        Body()
                    }
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    3,
                    9,
                    MESSAGE,
                )
                .isFormattedAs(formatted)
        }

        @Test
        fun `reaches siblings in a lambda nested under the layout scope`() {
            // ----- Arrange -----
            val code = """
                fun Screen() {
                    Column {
                        Card {
                            Title()
                            Body()
                        }
                    }
                }
            """.trimIndent()

            val formatted = """
                fun Screen() {
                    Column {
                        Card {
                            Title()

                            Body()
                        }
                    }
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    4,
                    13,
                    MESSAGE,
                )
                .isFormattedAs(formatted)
        }
    }

    @Nested
    inner class LeftAlone {
        @Test
        fun `ignores siblings outside a layout scope`() {
            // ----- Arrange -----
            val code = """
                fun Screen() {
                    Title()
                    Body()
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `ignores a composable next to a lowercase call`() {
            // ----- Arrange -----
            val code = """
                fun Screen() {
                    Column {
                        Title()
                        log()
                    }
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }
    }

    @Nested
    inner class Idempotence {
        @Test
        fun `formatting already separated siblings is a no-op`() {
            // ----- Arrange -----
            val code = """
                fun Screen() {
                    Column {
                        Title()

                        Body()
                    }
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }
    }

    private companion object {
        const val MESSAGE = "Blank line required between sibling composables"
    }
}
