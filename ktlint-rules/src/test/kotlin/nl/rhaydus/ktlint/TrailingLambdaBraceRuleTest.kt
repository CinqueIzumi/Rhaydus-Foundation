package nl.rhaydus.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TrailingLambdaBraceRuleTest {
    private val ruleAssertThat = assertThatRule { TrailingLambdaBraceRule() }

    @Nested
    inner class Expansion {
        @Test
        fun `expands braces around a multi-line call body and re-indents it`() {
            // ----- Arrange -----
            val code = """
                fun f() {
                    x { call(
                        a,
                        b,
                    ) }
                }
            """.trimIndent()

            val formatted = """
                fun f() {
                    x {
                        call(
                            a,
                            b,
                        )
                    }
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    2,
                    7,
                    MESSAGE,
                )
                .isFormattedAs(formatted)
        }

        @Test
        fun `moves only the closing brace when the opening one already sits alone`() {
            // ----- Arrange -----
            val code = """
                fun f() {
                    x {
                        call(
                            a,
                            b,
                        ) }
                }
            """.trimIndent()

            val formatted = """
                fun f() {
                    x {
                        call(
                            a,
                            b,
                        )
                    }
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    2,
                    7,
                    MESSAGE,
                )
                .isFormattedAs(formatted)
        }

        @Test
        fun `expands a multi-line lambda passed as a named argument`() {
            // ----- Arrange -----
            val code = """
                fun f() {
                    x(onClick = { call(
                        a,
                        b,
                    ) })
                }
            """.trimIndent()

            val formatted = """
                fun f() {
                    x(onClick = {
                        call(
                            a,
                            b,
                        )
                    })
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    2,
                    17,
                    MESSAGE,
                )
                .isFormattedAs(formatted)
        }
    }

    @Nested
    inner class LeftAlone {
        @Test
        fun `leaves a single-line body inline`() {
            // ----- Arrange -----
            val code = """
                fun f() {
                    x { call(a) }
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }
    }

    @Nested
    inner class Idempotence {
        @Test
        fun `formatting an already expanded lambda is a no-op`() {
            // ----- Arrange -----
            val code = """
                fun f() {
                    x {
                        call(
                            a,
                            b,
                        )
                    }
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }
    }

    private companion object {
        const val MESSAGE = "A multi-line lambda body puts its { and } on their own lines"
    }
}
