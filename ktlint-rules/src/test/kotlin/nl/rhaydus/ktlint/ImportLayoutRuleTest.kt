package nl.rhaydus.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ImportLayoutRuleTest {
    private val ruleAssertThat = assertThatRule { ImportLayoutRule() }

    @Nested
    inner class DefaultLayout {
        @Test
        fun `moves java, javax and kotlin imports after the rest and sorts each bucket`() {
            // ----- Arrange -----
            val code = """
                import kotlin.collections.List
                import javax.inject.Inject
                import java.util.Date
                import b.Zed
                import a.Alpha

                val x = 1
            """.trimIndent()

            val formatted = """
                import a.Alpha
                import b.Zed
                import java.util.Date
                import javax.inject.Inject
                import kotlin.collections.List

                val x = 1
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    1,
                    1,
                    MESSAGE_DEFAULT,
                )
                .isFormattedAs(formatted)
        }

        @Test
        fun `removes blank lines between buckets because the default layout has no separators`() {
            // ----- Arrange -----
            val code = """
                import a.Alpha

                import java.util.Date

                val x = 1
            """.trimIndent()

            val formatted = """
                import a.Alpha
                import java.util.Date

                val x = 1
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    1,
                    1,
                    MESSAGE_DEFAULT,
                )
                .isFormattedAs(formatted)
        }

        @Test
        fun `puts aliased imports in their own trailing bucket`() {
            // ----- Arrange -----
            val code = """
                import kotlin.collections.List
                import a.Alpha as AlphaAlias
                import z.Zed
                import java.util.Date as JavaDate

                val x = 1
            """.trimIndent()

            val formatted = """
                import z.Zed
                import kotlin.collections.List
                import a.Alpha as AlphaAlias
                import java.util.Date as JavaDate

                val x = 1
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .hasLintViolation(
                    1,
                    1,
                    MESSAGE_DEFAULT,
                )
                .isFormattedAs(formatted)
        }

        @Test
        fun `leaves a block that holds a comment alone`() {
            // ----- Arrange -----
            val code = """
                import z.Zed
                // keep the order
                import a.Alpha

                val x = 1
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `reports nothing when the file has no imports`() {
            // ----- Arrange -----
            val code = "val x = 1"

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }
    }

    @Nested
    inner class CustomLayout {
        @Test
        fun `follows the layout from the editorconfig with blank-line separators`() {
            // ----- Arrange -----
            val code = """
                import kotlin.collections.List
                import java.util.Date
                import a.Alpha
                import b.Beta

                val x = 1
            """.trimIndent()

            val formatted = """
                import java.util.Date

                import kotlin.collections.List

                import a.Alpha
                import b.Beta

                val x = 1
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .withEditorConfigOverride(ImportLayoutRule.IMPORT_LAYOUT_PROPERTY to "java.**,|,kotlin.**,|,*")
                .hasLintViolation(
                    1,
                    1,
                    "Imports must follow the layout 'java.**,|,kotlin.**,|,*' (§Import Ordering)",
                )
                .isFormattedAs(formatted)
        }

        @Test
        fun `treats a single-star pattern as exactly that package`() {
            // ----- Arrange -----
            val code = """
                import a.sub.Deep
                import a.Top
                import b.Other

                val x = 1
            """.trimIndent()

            val formatted = """
                import a.Top
                import a.sub.Deep
                import b.Other

                val x = 1
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code)
                .withEditorConfigOverride(ImportLayoutRule.IMPORT_LAYOUT_PROPERTY to "a.*,*")
                .hasLintViolation(
                    1,
                    1,
                    "Imports must follow the layout 'a.*,*' (§Import Ordering)",
                )
                .isFormattedAs(formatted)
        }
    }

    @Nested
    inner class Idempotence {
        @Test
        fun `formatting an already laid out block is a no-op`() {
            // ----- Arrange -----
            val code = """
                import a.Alpha
                import java.util.Date
                import kotlin.collections.List
                import a.Alpha as AlphaAlias

                val x = 1
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }
    }

    private companion object {
        const val MESSAGE_DEFAULT =
            "Imports must follow the layout '*,java.**,javax.**,kotlin.**,^' (§Import Ordering)"
    }
}
