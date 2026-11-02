package nl.rhaydus.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NoUnusedImportsRuleTest {
    private val ruleAssertThat = assertThatRule { NoUnusedImportsRule() }

    @Nested
    inner class Reporting {
        @Test
        fun `reports and removes an unused import`() {
            // ----- Arrange -----
            val code = """
                import a.b.Used
                import a.b.Unused

                val x = Used()
            """.trimIndent()

            val formatted = """
                import a.b.Used

                val x = Used()
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    2,
                    1,
                    MESSAGE,
                )
                .isFormattedAs(formatted)
        }

        @Test
        fun `keeps an import used only as a type reference`() {
            // ----- Arrange -----
            val code = """
                import a.b.Used

                val x: Used? = null
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `does not count a name that only appears in a string or comment`() {
            // ----- Arrange -----
            val code = """
                import a.b.Ghost

                // Ghost
                val x = "Ghost"
            """.trimIndent()

            val formatted = """
                // Ghost
                val x = "Ghost"
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    1,
                    1,
                    MESSAGE,
                )
                .isFormattedAs(formatted)
        }

        @Test
        fun `reports every unused import`() {
            // ----- Arrange -----
            val code = """
                import a.b.One
                import a.b.Used
                import a.b.Two

                val x = Used()
            """.trimIndent()

            val formatted = """
                import a.b.Used

                val x = Used()
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolations(
                    LintViolation(
                        1,
                        1,
                        MESSAGE,
                    ),
                    LintViolation(
                        3,
                        1,
                        MESSAGE,
                    ),
                ).isFormattedAs(formatted)
        }
    }

    @Nested
    inner class Aliases {
        @Test
        fun `keeps an alias import when the alias is used`() {
            // ----- Arrange -----
            val code = """
                import a.b.Original as Short

                val x = Short()
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `reports an alias import when only the original name is used`() {
            // ----- Arrange -----
            val code = """
                import a.b.Original as Short

                val x = Original()
            """.trimIndent()

            val formatted = """
                val x = Original()
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    1,
                    1,
                    MESSAGE,
                )
                .isFormattedAs(formatted)
        }
    }

    @Nested
    inner class Kdoc {
        @Test
        fun `keeps an import referenced by a KDoc link`() {
            // ----- Arrange -----
            val code = """
                import a.b.Linked

                /** See [Linked]. */
                val x = 1
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `keeps an import referenced by a see tag`() {
            // ----- Arrange -----
            val code = """
                import a.b.Linked

                /**
                 * @see Linked
                 */
                val x = 1
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `reports an import mentioned in KDoc prose without a link`() {
            // ----- Arrange -----
            val code = """
                import a.b.Linked

                /** Mentions Linked in prose. */
                val x = 1
            """.trimIndent()

            val formatted = """
                /** Mentions Linked in prose. */
                val x = 1
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    1,
                    1,
                    MESSAGE,
                )
                .isFormattedAs(formatted)
        }
    }

    @Nested
    inner class ImplicitUsage {
        @Test
        fun `keeps getValue and setValue imports for a by delegate`() {
            // ----- Arrange -----
            val code = """
                import a.b.getValue
                import a.b.setValue

                var x by holder
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `reports getValue import when there is no delegate`() {
            // ----- Arrange -----
            val code = """
                import a.b.getValue

                val x = 1
            """.trimIndent()

            val formatted = """
                val x = 1
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    1,
                    1,
                    MESSAGE,
                )
                .isFormattedAs(formatted)
        }

        @Test
        fun `keeps an invoke import for a call`() {
            // ----- Arrange -----
            val code = """
                import a.b.invoke

                val x = thing(1)
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `keeps a plus import for the plus operator`() {
            // ----- Arrange -----
            val code = """
                import a.b.plus

                val x = left + right
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `reports a plus import when no operator is used`() {
            // ----- Arrange -----
            val code = """
                import a.b.plus

                val x = left
            """.trimIndent()

            val formatted = """
                val x = left
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    1,
                    1,
                    MESSAGE,
                )
                .isFormattedAs(formatted)
        }

        @Test
        fun `keeps a component1 import for destructuring`() {
            // ----- Arrange -----
            val code = """
                import a.b.component1

                fun f() {
                    val (first) = pair
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `keeps an iterator import for a for loop`() {
            // ----- Arrange -----
            val code = """
                import a.b.iterator

                fun f() {
                    for (item in source) println(item)
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `keeps an extension import used as a qualified member`() {
            // ----- Arrange -----
            val code = """
                import kotlinx.coroutines.IO

                val x = Dispatchers.IO
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }
    }

    @Nested
    inner class Wildcards {
        @Test
        fun `never reports a wildcard import`() {
            // ----- Arrange -----
            val code = """
                import a.b.*

                val x = 1
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }
    }

    @Nested
    inner class Formatting {
        @Test
        fun `removes the first import without leaving a blank line`() {
            // ----- Arrange -----
            val code = """
                import a.b.Unused
                import a.b.Used

                val x = Used()
            """.trimIndent()

            val formatted = """
                import a.b.Used

                val x = Used()
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    1,
                    1,
                    MESSAGE,
                )
                .isFormattedAs(formatted)
        }

        @Test
        fun `removes the last import and keeps one blank line before the code`() {
            // ----- Arrange -----
            val code = """
                import a.b.Used
                import a.b.Unused

                val x = Used()
            """.trimIndent()

            val formatted = """
                import a.b.Used

                val x = Used()
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    2,
                    1,
                    MESSAGE,
                )
                .isFormattedAs(formatted)
        }

        @Test
        fun `removes the only import and leaves the package followed by one blank line`() {
            // ----- Arrange -----
            val code = """
                package a.b

                import a.b.Unused

                val x = 1
            """.trimIndent()

            val formatted = """
                package a.b

                val x = 1
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    3,
                    1,
                    MESSAGE,
                )
                .isFormattedAs(formatted)
        }

        @Test
        fun `removes a middle import and keeps the remaining block contiguous`() {
            // ----- Arrange -----
            val code = """
                import a.b.First
                import a.b.Unused
                import a.b.Last

                val x = First(Last)
            """.trimIndent()

            val formatted = """
                import a.b.First
                import a.b.Last

                val x = First(Last)
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    2,
                    1,
                    MESSAGE,
                )
                .isFormattedAs(formatted)
        }
    }

    private companion object {
        const val MESSAGE = "Unused import"
    }
}
