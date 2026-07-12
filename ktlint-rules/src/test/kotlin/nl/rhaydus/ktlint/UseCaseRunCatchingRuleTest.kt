package nl.rhaydus.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private val ruleAssertThat = assertThatRule { UseCaseRunCatchingRule() }

class UseCaseRunCatchingRuleTest {
    @Nested
    inner class Fires {
        private val message =
            "Use runCatchingCancellable (or runCatchingLogged to also log at the source) instead of bare runCatching (§Error Handling)"

        @Test
        fun `fires for runCatching in production UseCase file`() {
            // ----- Arrange -----
            val code = """
                fun invoke() = runCatching { 1 }
            """.trimIndent()

            // ----- Act & Assert -----
            // "fun invoke() = " = 15 chars, runCatching starts at col 16
            ruleAssertThat(code)
                .asFileWithPath("/x/src/commonMain/kotlin/LoadBookUseCase.kt")
                .hasLintViolationWithoutAutoCorrect(
                    1,
                    16,
                    message,
                )
        }
    }

    @Nested
    inner class Clean {
        @Test
        fun `clean for runCatching in non-UseCase main file`() {
            // ----- Arrange -----
            val code = """
                fun invoke() = runCatching { 1 }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .asFileWithPath("/x/src/commonMain/kotlin/SomeHelper.kt")
                .hasNoLintViolations()
        }

        @Test
        fun `clean for runCatching in test-path UseCase file`() {
            // ----- Arrange -----
            val code = """
                fun invoke() = runCatching { 1 }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .asFileWithPath("/x/src/commonTest/kotlin/LoadBookUseCaseTest.kt")
                .hasNoLintViolations()
        }
    }
}
