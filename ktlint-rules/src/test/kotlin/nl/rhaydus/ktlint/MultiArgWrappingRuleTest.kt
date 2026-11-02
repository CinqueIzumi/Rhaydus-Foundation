package nl.rhaydus.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MultiArgWrappingRuleTest {
    private val ruleAssertThat = assertThatRule { MultiArgWrappingRule() }

    @Nested
    inner class Wrapping {
        @Test
        fun `wraps a call with two arguments one per line with a trailing comma`() {
            // ----- Arrange -----
            val code = """
                fun f() {
                    call(a, b)
                }
            """.trimIndent()

            val formatted = """
                fun f() {
                    call(
                        a,
                        b,
                    )
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    2,
                    9,
                    LIST_MESSAGE,
                )
                .isFormattedAs(formatted)
        }

        @Test
        fun `wraps a call on a Modifier chain with two arguments`() {
            // ----- Arrange -----
            val code = """
                fun f() {
                    val m = Modifier.padding(horizontal = 8, vertical = 4)
                }
            """.trimIndent()

            val formatted = """
                fun f() {
                    val m = Modifier.padding(
                        horizontal = 8,
                        vertical = 4,
                    )
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    2,
                    29,
                    LIST_MESSAGE,
                )
                .isFormattedAs(formatted)
        }

        @Test
        fun `wraps the outer call as well when a nested call wraps`() {
            // ----- Arrange -----
            val code = """
                fun f() {
                    outer(inner(a, b))
                }
            """.trimIndent()

            val formatted = """
                fun f() {
                    outer(
                        inner(
                            a,
                            b,
                        ),
                    )
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    2,
                    16,
                    LIST_MESSAGE,
                )
                .isFormattedAs(formatted)
        }

        @Test
        fun `wraps a function declaration with two parameters`() {
            // ----- Arrange -----
            val code = """
                fun f(a: Int, b: Int) {
                }
            """.trimIndent()

            val formatted = """
                fun f(
                    a: Int,
                    b: Int,
                ) {
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    1,
                    6,
                    LIST_MESSAGE,
                )
                .isFormattedAs(formatted)
        }

        @Test
        fun `expands the braces of an enclosing lambda`() {
            // ----- Arrange -----
            val code = """
                fun f() {
                    run { call(a, b) }
                }
            """.trimIndent()

            val formatted = """
                fun f() {
                    run {
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
                    15,
                    LIST_MESSAGE,
                )
                .isFormattedAs(formatted)
        }
    }

    @Nested
    inner class HuggingRepair {
        @Test
        fun `moves a hugging inner call onto its own lines`() {
            // ----- Arrange -----
            val code = """
                fun f() {
                    set(write(
                        a,
                        b,
                    ))
                }
            """.trimIndent()

            val formatted = """
                fun f() {
                    set(
                        write(
                            a,
                            b,
                        ),
                    )
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    2,
                    8,
                    HUGGING_MESSAGE,
                )
                .isFormattedAs(formatted)
        }

        @Test
        fun `repairs the closer left hugging by a comma pass`() {
            // ----- Arrange -----
            val code = """
                fun f() {
                    set(write(
                        a,
                        b,
                    ),)
                }
            """.trimIndent()

            val formatted = """
                fun f() {
                    set(
                        write(
                            a,
                            b,
                        ),
                    )
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    2,
                    8,
                    HUGGING_MESSAGE,
                )
                .isFormattedAs(formatted)
        }
    }

    @Nested
    inner class Exemptions {
        @Test
        fun `keeps a collection factory inline`() {
            // ----- Arrange -----
            val code = """
                fun f() {
                    val l = listOf(a, b)
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `keeps a call with a trailing lambda inline`() {
            // ----- Arrange -----
            val code = """
                fun f() {
                    Effect(a, b) { use() }
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `keeps a single-argument call inline`() {
            // ----- Arrange -----
            val code = """
                fun f() {
                    call(a)
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `keeps lambda parameters inline`() {
            // ----- Arrange -----
            val code = """
                val f = { a: Int, b: Int -> a + b }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }
    }

    @Nested
    inner class Idempotence {
        @Test
        fun `formatting an already wrapped nested call is a no-op`() {
            // ----- Arrange -----
            val code = """
                fun f() {
                    outer(
                        inner(
                            a,
                            b,
                        ),
                    )
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }
    }

    private companion object {
        const val LIST_MESSAGE = "Multi-argument list must break one-per-line with a trailing comma"
        private const val HUGGING_MESSAGE = "A multi-line argument list must break one-per-line with a trailing comma"
    }
}
