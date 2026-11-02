package nl.rhaydus.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class StatementParagraphRuleTest {
    private val ruleAssertThat = assertThatRule { StatementParagraphRule() }

    @Nested
    inner class MultiLineStatements {
        @Test
        fun `adds blank lines before and after a multi-line statement`() {
            // ----- Arrange -----
            val code = """
                fun f() {
                    val a = 1
                    call(
                        x,
                        y,
                    )
                    val b = 2
                }
            """.trimIndent()

            val formatted = """
                fun f() {
                    val a = 1

                    call(
                        x,
                        y,
                    )

                    val b = 2
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolations(
                    LintViolation(
                        3,
                        5,
                        MESSAGE,
                    ),
                    LintViolation(
                        7,
                        5,
                        MESSAGE,
                    ),
                )
                .isFormattedAs(formatted)
        }

        @Test
        fun `keeps adjacent single-line statements together`() {
            // ----- Arrange -----
            val code = """
                fun f() {
                    val a = 1
                    val b = 2
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `keeps a leading comment with the statement under it`() {
            // ----- Arrange -----
            val code = """
                fun f() {
                    val a = 1
                    // explain
                    call(
                        x,
                        y,
                    )
                }
            """.trimIndent()

            val formatted = """
                fun f() {
                    val a = 1

                    // explain
                    call(
                        x,
                        y,
                    )
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    4,
                    5,
                    MESSAGE,
                )
                .isFormattedAs(formatted)
        }
    }

    @Nested
    inner class GuardClauses {
        @Test
        fun `adds a blank line after an if-return guard`() {
            // ----- Arrange -----
            val code = """
                fun f(a: Int?) {
                    if (a == null) return
                    use(a)
                }
            """.trimIndent()

            val formatted = """
                fun f(a: Int?) {
                    if (a == null) return

                    use(a)
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    3,
                    5,
                    MESSAGE,
                )
                .isFormattedAs(formatted)
        }

        @Test
        fun `adds a blank line after an elvis-return guard`() {
            // ----- Arrange -----
            val code = """
                fun f(a: Int?) {
                    val b = a ?: return
                    use(b)
                }
            """.trimIndent()

            val formatted = """
                fun f(a: Int?) {
                    val b = a ?: return

                    use(b)
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    3,
                    5,
                    MESSAGE,
                )
                .isFormattedAs(formatted)
        }

        @Test
        fun `does not treat an if with an else as a guard`() {
            // ----- Arrange -----
            val code = """
                fun f(a: Int?) {
                    if (a == null) return else use(a)
                    use(a)
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }
    }

    @Nested
    inner class AfterSpacedCalls {
        @Test
        fun `adds a blank line after a super call`() {
            // ----- Arrange -----
            val code = """
                fun f() {
                    super.onCreate()
                    setup()
                }
            """.trimIndent()

            val formatted = """
                fun f() {
                    super.onCreate()

                    setup()
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    3,
                    5,
                    MESSAGE,
                )
                .isFormattedAs(formatted)
        }

        @Test
        fun `adds a blank line after an AppLog error log`() {
            // ----- Arrange -----
            val code = """
                fun f() {
                    AppLog.e("failed")
                    recover()
                }
            """.trimIndent()

            val formatted = """
                fun f() {
                    AppLog.e("failed")

                    recover()
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    3,
                    5,
                    MESSAGE,
                )
                .isFormattedAs(formatted)
        }

        @Test
        fun `does not space a statement before an AppLog error log`() {
            // ----- Arrange -----
            val code = """
                fun f() {
                    prepare()
                    AppLog.e("failed")
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `does not space other AppLog levels`() {
            // ----- Arrange -----
            val code = """
                fun f() {
                    AppLog.d("note")
                    recover()
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }
    }

    @Nested
    inner class RegionMarkers {
        @Test
        fun `skips the boundary next to a region marker`() {
            // ----- Arrange -----
            val code = """
                fun f() {
                    val a = 1
                    // region extras
                    call(
                        x,
                        y,
                    )
                    // endregion
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }
    }

    @Nested
    inner class Idempotence {
        @Test
        fun `formatting already spaced statements is a no-op`() {
            // ----- Arrange -----
            val code = """
                fun f(a: Int?) {
                    if (a == null) return

                    call(
                        a,
                        a,
                    )

                    super.onCreate()

                    done()
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }
    }

    private companion object {
        const val MESSAGE =
            "A multi-line statement, guard clause, super call or error log is its own paragraph - separate it with a blank line"
    }
}
