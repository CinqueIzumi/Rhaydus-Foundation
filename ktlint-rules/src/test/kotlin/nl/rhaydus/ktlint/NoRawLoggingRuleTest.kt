package nl.rhaydus.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private val ruleAssertThat = assertThatRule { NoRawLoggingRule() }

class NoRawLoggingRuleTest {
    @Nested
    inner class Fires {
        private val messagePrintln =
            "Use the AppLog facade (AppLog.i/w/e), not raw println (§Error Handling)"
        private val messageLog =
            "Use the AppLog facade (AppLog.i/w/e), not android.util.Log.* (§Error Handling)"

        @Test
        fun `fires for unqualified println call`() {
            // ----- Arrange -----
            val code = """
                fun test() {
                println("x")
                }
            """.trimIndent()

            // ----- Act & Assert -----
            // Line 2: no receiver, println starts at col 1
            ruleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(
                    2,
                    1,
                    messagePrintln,
                )
        }

        @Test
        fun `fires for System-out println call`() {
            // ----- Arrange -----
            val code = """
                fun test() {
                System.out.println("x")
                }
            """.trimIndent()

            // ----- Act & Assert -----
            // Line 2: "System.out." = 11 chars, println starts at col 12
            ruleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(
                    2,
                    12,
                    messagePrintln,
                )
        }

        @Test
        fun `fires for System-err println call`() {
            // ----- Arrange -----
            val code = """
                fun test() {
                System.err.println("x")
                }
            """.trimIndent()

            // ----- Act & Assert -----
            // Line 2: "System.err." = 11 chars, println starts at col 12
            ruleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(
                    2,
                    12,
                    messagePrintln,
                )
        }

        @Test
        fun `fires for Log-d call`() {
            // ----- Arrange -----
            val code = """
                fun test() {
                Log.d("tag", "msg")
                }
            """.trimIndent()

            // ----- Act & Assert -----
            // Line 2: "Log." = 4 chars, d starts at col 5
            ruleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(
                    2,
                    5,
                    messageLog,
                )
        }

        @Test
        fun `fires for Log-e call`() {
            // ----- Arrange -----
            val code = """
                fun test() {
                Log.e("tag", "msg")
                }
            """.trimIndent()

            // ----- Act & Assert -----
            // Line 2: "Log." = 4 chars, e starts at col 5
            ruleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(
                    2,
                    5,
                    messageLog,
                )
        }

        @Test
        fun `fires for android-util-Log-w call`() {
            // ----- Arrange -----
            val code = """
                fun test() {
                android.util.Log.w("tag", "msg")
                }
            """.trimIndent()

            // ----- Act & Assert -----
            // Line 2: "android.util.Log." = 17 chars, w starts at col 18
            ruleAssertThat(code)
                .hasLintViolationWithoutAutoCorrect(
                    2,
                    18,
                    messageLog,
                )
        }
    }

    @Nested
    inner class Clean {
        @Test
        fun `clean for AppLog-e call with a throwable`() {
            // ----- Arrange -----
            val code = """
                fun test(throwable: Throwable) {
                AppLog.e(throwable, "msg")
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `clean for AppLog-i call`() {
            // ----- Arrange -----
            val code = """
                fun test() {
                AppLog.i("msg")
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `clean for a println call on a non-System receiver`() {
            // ----- Arrange -----
            val code = """
                fun test(writer: java.io.PrintWriter) {
                writer.println("x")
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `clean for println and Log text inside a comment and a string literal`() {
            // ----- Arrange -----
            val code = """
                fun test() {
                // println("debug") and Log.d("tag", "msg") should not trigger
                val message = "println(x) or Log.e(x) as plain text"
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `clean for a property and a class merely named log`() {
            // ----- Arrange -----
            val code = """
                val log = 5

                class Log
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }
    }
}
