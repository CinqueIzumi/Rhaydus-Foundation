package nl.rhaydus.ktlint

import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.ec4j.core.model.PropertyType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private val ruleAssertThat = assertThatRule { InlineFullyQualifiedReferenceRule() }

private val exemptSegments =
    EditorConfigProperty(
        type =
            PropertyType.LowerCasingPropertyType(
                "rhaydus_fully_qualified_exempt_package_segments",
                "Comma-separated package segments whose types may stay fully-qualified.",
                PropertyType.PropertyValueParser.IDENTITY_VALUE_PARSER,
                emptySet(),
            ),
        defaultValue = "",
    )

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
        fun `fires for androidx fragment type - an exempt segment covers only a type directly under it`() {
            // ----- Arrange -----
            val code = """
                val x: androidx.fragment.app.Fragment? = null
            """.trimIndent()

            // ----- Act & Assert -----
            // "val x: " = 7 chars, so androidx starts at col 8
            ruleAssertThat(code)
                .withEditorConfigOverride(exemptSegments to "fragment")
                .hasLintViolationWithoutAutoCorrect(
                    1,
                    8,
                    message,
                )
        }

        @Test
        fun `fires for a fragment type when no exemption is configured`() {
            // ----- Arrange -----
            val code = """
                val x: nl.rhaydus.sample.fragment.BookListFragment? = null
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasLintViolationWithoutAutoCorrect(
                1,
                8,
                message,
            )
        }

        @Test
        fun `fires for a type under a segment that is not exempted`() {
            // ----- Arrange -----
            val code = """
                val x: nl.rhaydus.sample.model.Book? = null
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .withEditorConfigOverride(exemptSegments to "fragment")
                .hasLintViolationWithoutAutoCorrect(
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
        fun `clean for a type under an exempted package segment`() {
            // ----- Arrange -----
            val code = """
                val x: nl.rhaydus.sample.fragment.BookListFragment? = null
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .withEditorConfigOverride(exemptSegments to "fragment")
                .hasNoLintViolations()
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
