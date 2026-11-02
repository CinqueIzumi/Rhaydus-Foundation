package nl.rhaydus.detekt

import dev.detekt.api.Config
import dev.detekt.test.lint
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NoRawLoggingTest {
    private val rule = NoRawLogging(Config.empty)

    @Nested
    inner class Flagged {
        @Test
        fun `flags println`() {
            // ----- Arrange -----
            val code = """
            fun f() {
                println("x")
            }
            """.trimIndent()

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `flags System out println`() {
            // ----- Arrange -----
            val code = """
            fun f() {
                System.out.println("x")
            }
            """.trimIndent()

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `flags Log d`() {
            // ----- Arrange -----
            val code = """
            fun f() {
                Log.d("T", "x")
            }
            """.trimIndent()

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `flags android util Log e`() {
            // ----- Arrange -----
            val code = """
            fun f() {
                android.util.Log.e("T", "x")
            }
            """.trimIndent()

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }
    }

    @Nested
    inner class Allowed {
        @Test
        fun `allows println on another receiver`() {
            // ----- Arrange -----
            val code = """
            fun f() {
                logger.println("x")
            }
            """.trimIndent()

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }

        @Test
        fun `allows the AppLog facade`() {
            // ----- Arrange -----
            val code = """
            fun f() {
                AppLog.i("x")
            }
            """.trimIndent()

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }

        @Test
        fun `allows a receiver ending in Log without a dot`() {
            // ----- Arrange -----
            val code = """
            fun f() {
                BackLog.d("x")
            }
            """.trimIndent()

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }
    }
}
