package nl.rhaydus.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private val ruleAssertThat = assertThatRule { InlineFullyQualifiedReferenceRule() }

class InlineFullyQualifiedReferenceRuleTest {
    @Nested
    inner class Fires {
        private val message = "Inline fully-qualified reference - add an import and use the short name (§Imports)"

        @Test
        fun `fires for kotlinx fully-qualified type in property declaration`() {
            // ----- Arrange -----
            val code = """
                val x: kotlinx.coroutines.flow.MutableStateFlow<Int>? = null
            """.trimIndent()

            // ----- Act & Assert -----
            // "val x: " = 7 chars, so kotlinx starts at col 8
            ruleAssertThat(code).hasLintViolationWithoutAutoCorrect(
                1,
                8,
                message,
            )
        }

        @Test
        fun `fires for androidx fully-qualified type in property declaration`() {
            // ----- Arrange -----
            val code = """
                val x: androidx.compose.ui.Modifier = TODO()
            """.trimIndent()

            // ----- Act & Assert -----
            // "val x: " = 7 chars, so androidx starts at col 8
            ruleAssertThat(code).hasLintViolationWithoutAutoCorrect(
                1,
                8,
                message,
            )
        }

        @Test
        fun `fires for androidx fragment type - only nl_rhaydus generated fragments are exempt`() {
            // ----- Arrange -----
            val code = """
                val x: androidx.fragment.app.Fragment? = null
            """.trimIndent()

            // ----- Act & Assert -----
            // "val x: " = 7 chars, so androidx starts at col 8
            ruleAssertThat(code).hasLintViolationWithoutAutoCorrect(
                1,
                8,
                message,
            )
        }
    }

    @Nested
    inner class Clean {
        @Test
        fun `clean when short name is used with an import`() {
            // ----- Arrange -----
            val code = """
                import kotlinx.coroutines.flow.MutableStateFlow

                val x: MutableStateFlow<Int>? = null
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `clean for import lines themselves`() {
            // ----- Arrange -----
            val code = """
                import kotlinx.coroutines.flow.MutableStateFlow
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `clean for package declaration`() {
            // ----- Arrange -----
            val code = """
                package nl.rhaydus.core.example
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `clean for a generated nl_rhaydus fragment type left fully-qualified`() {
            // ----- Arrange -----
            val code = """
                val x: nl.rhaydus.softcover.fragment.BookListFragment? = null
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `clean for all-lowercase qualified chain`() {
            // ----- Arrange -----
            val code = """
                val x = kotlinx.coroutines.flow.first
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }
    }
}
