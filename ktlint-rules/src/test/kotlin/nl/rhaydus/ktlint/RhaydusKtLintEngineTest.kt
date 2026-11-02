package nl.rhaydus.ktlint

import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.LintError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class RhaydusKtLintEngineTest {
    private val engine = rhaydusKtLintEngine()

    @Nested
    inner class IndentEnforcement {
        @TempDir
        lateinit var tempDir: File

        private fun misIndentedFile(): File {
            File(
                tempDir,
                ".editorconfig",
            ).writeText(
                """
                root = true

                [*.{kt,kts}]
                ktlint_standard = disabled
                """.trimIndent(),
            )

            return File(
                tempDir,
                "Sample.kt",
            ).apply {
                writeText(
                    """
                    fun f() {
                      val a = 1
                    }

                    """.trimIndent(),
                )
            }
        }

        @Test
        fun `reports an indent violation although the scanned tree disables the standard rules`() {
            // ----- Arrange -----
            val file = misIndentedFile()
            val errors = mutableListOf<LintError>()

            // ----- Act -----
            engine.lint(Code.fromFile(file)) { errors.add(it) }

            // ----- Assert -----
            assertTrue(errors.any { it.ruleId.value == "standard:indent" })
        }

        @Test
        fun `re-indents the file although the scanned tree disables the standard rules`() {
            // ----- Arrange -----
            val file = misIndentedFile()

            // ----- Act -----
            val formatted = engine.format(Code.fromFile(file))

            // ----- Assert -----
            assertEquals(
                "fun f() {\n    val a = 1\n}\n",
                formatted,
            )
        }
    }

    @Nested
    inner class Convergence {
        private fun assertConverges(
            code: String,
            expected: String,
        ) {
            val once = engine.format(Code.fromSnippet(code))
            val twice = engine.format(Code.fromSnippet(once))

            assertEquals(
                expected,
                once,
            )

            assertEquals(
                once,
                twice,
            )
        }

        @Test
        fun `settles a call wrapped inside a lambda after one format`() {
            // ----- Arrange -----
            val code = """
                fun f() {
                    items.forEach { item ->
                        call(item, other)
                    }
                }

            """.trimIndent()

            val expected = """
                fun f() {
                    items.forEach { item ->
                        call(
                            item,
                            other,
                        )
                    }
                }

            """.trimIndent()

            // ----- Act & Assert -----
            assertConverges(
                code,
                expected,
            )
        }

        @Test
        fun `settles a call wrapped inside an if condition after one format`() {
            // ----- Arrange -----
            val code = """
                fun f() {
                    if (check(a, b)) {
                        run()
                    }
                }

            """.trimIndent()

            val expected = """
                fun f() {
                    if (check(
                            a,
                            b,
                        )) {
                        run()
                    }
                }

            """.trimIndent()

            // ----- Act & Assert -----
            assertConverges(
                code,
                expected,
            )
        }
    }
}
