package nl.rhaydus.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ExpressionOnEqualsLineRuleTest {
    private val ruleAssertThat = assertThatRule { ExpressionOnEqualsLineRule() }

    @Nested
    inner class Joining {
        @Test
        fun `joins a property initializer onto the equals line and dedents it`() {
            // ----- Arrange -----
            val code = """
                val items =
                    listOf(
                        1,
                        2,
                    )
            """.trimIndent()

            val formatted = """
                val items = listOf(
                    1,
                    2,
                )
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    1,
                    11,
                    MESSAGE,
                )
                .isFormattedAs(formatted)
        }

        @Test
        fun `joins a function expression body onto the equals line and dedents it`() {
            // ----- Arrange -----
            val code = """
                fun items() =
                    listOf(
                        1,
                        2,
                    )
            """.trimIndent()

            val formatted = """
                fun items() = listOf(
                    1,
                    2,
                )
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    1,
                    13,
                    MESSAGE,
                )
                .isFormattedAs(formatted)
        }

        @Test
        fun `joins a named argument onto the equals line and dedents it`() {
            // ----- Arrange -----
            val code = """
                val result = call(
                    items =
                        listOf(
                            1,
                            2,
                        ),
                )
            """.trimIndent()

            val formatted = """
                val result = call(
                    items = listOf(
                        1,
                        2,
                    ),
                )
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    2,
                    11,
                    MESSAGE,
                )
                .isFormattedAs(formatted)
        }

        @Test
        fun `joins an assignment onto the equals line and dedents it`() {
            // ----- Arrange -----
            val code = """
                fun f() {
                    items =
                        listOf(
                            1,
                            2,
                        )
                }
            """.trimIndent()

            val formatted = """
                fun f() {
                    items = listOf(
                        1,
                        2,
                    )
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    2,
                    11,
                    MESSAGE,
                )
                .isFormattedAs(formatted)
        }

        @Test
        fun `joins a chain whose first line ends in a lambda brace`() {
            // ----- Arrange -----
            val code = """
                val first =
                    listOf(a).firstOrNull {
                        it > 1
                    }
            """.trimIndent()

            val formatted = """
                val first = listOf(a).firstOrNull {
                    it > 1
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    1,
                    11,
                    MESSAGE,
                )
                .isFormattedAs(formatted)
        }

        @Test
        fun `joins a chain whose first line ends in a lambda parameter arrow`() {
            // ----- Arrange -----
            val code = """
                val found =
                    listOf(a).firstOrNull { x ->
                        x > 1
                    }
            """.trimIndent()

            val formatted = """
                val found = listOf(a).firstOrNull { x ->
                    x > 1
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    1,
                    11,
                    MESSAGE,
                )
                .isFormattedAs(formatted)
        }

        @Test
        fun `joins a default parameter value onto the equals line and dedents it`() {
            // ----- Arrange -----
            val code = """
                fun f(
                    x: Foo =
                        Foo(
                            1,
                            2,
                        ),
                )
            """.trimIndent()

            val formatted = """
                fun f(
                    x: Foo = Foo(
                        1,
                        2,
                    ),
                )
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    2,
                    12,
                    MESSAGE,
                )
                .isFormattedAs(formatted)
        }

        @Test
        fun `joins a property accessor body onto the equals line and dedents it`() {
            // ----- Arrange -----
            val code = """
                val items: List<Int>
                    get() =
                        listOf(
                            1,
                            2,
                        )
            """.trimIndent()

            val formatted = """
                val items: List<Int>
                    get() = listOf(
                        1,
                        2,
                    )
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    2,
                    11,
                    MESSAGE,
                )
                .isFormattedAs(formatted)
        }

        @Test
        fun `keeps nested content at its relative indentation after the dedent`() {
            // ----- Arrange -----
            val code = """
                val result =
                    call(
                        first,
                        block {
                            inner()
                        },
                    )
            """.trimIndent()

            val formatted = """
                val result = call(
                    first,
                    block {
                        inner()
                    },
                )
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    1,
                    12,
                    MESSAGE,
                )
                .isFormattedAs(formatted)
        }

        @Test
        fun `joins a when expression and keeps its branch arrows and bodies at their relative layout`() {
            // ----- Arrange -----
            val code = """
                val x =
                    when (y) {
                        1 ->
                            first()
                        else ->
                            second()
                    }
            """.trimIndent()

            val formatted = """
                val x = when (y) {
                    1 ->
                        first()
                    else ->
                        second()
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    1,
                    7,
                    MESSAGE,
                )
                .isFormattedAs(formatted)
        }

        @Test
        fun `joins a call with a raw string argument and leaves the raw string content untouched`() {
            // ----- Arrange -----
            val quotes = "\"\"\""

            val code = """
                val x =
                    call(
                        $quotes
                flush text
                          deeper text
                        $quotes,
                    )
            """.trimIndent()

            val formatted = """
                val x = call(
                    $quotes
                flush text
                          deeper text
                        $quotes,
                )
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    1,
                    7,
                    MESSAGE,
                )
                .isFormattedAs(formatted)
        }
    }

    @Nested
    inner class LeftAlone {
        @Test
        fun `leaves a single-line expression on its own line`() {
            // ----- Arrange -----
            val code = """
                val items =
                    listOf(1)
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `leaves a receiver followed by chain lines`() {
            // ----- Arrange -----
            val code = """
                val names =
                    items
                        .map { it.name }
                        .distinct()
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `leaves an expression after a comment on the equals line`() {
            // ----- Arrange -----
            val code = """
                val items = // seed
                    listOf(
                        1,
                        2,
                    )
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `leaves an expression whose join would pass 140 columns`() {
            // ----- Arrange -----
            val longName = "a".repeat(130)

            val code = """
                val items =
                    $longName(
                        1,
                        2,
                    )
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }
    }

    private companion object {
        const val MESSAGE = "A multi-line expression after = opens on the = line"
    }
}
