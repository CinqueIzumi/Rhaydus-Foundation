package nl.rhaydus.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private val ruleAssertThat = assertThatRule { ProjectImportOrderRule() }

class ProjectImportOrderRuleTest {
    @Nested
    inner class Fires {
        private val message = "Project import block out of alphabetical order - sort nl.rhaydus.* imports (§Import Ordering)"

        @Test
        fun `fires when nl rhaydus imports are out of alphabetical order`() {
            // ----- Arrange -----
            val code = """
                import nl.rhaydus.zeta.Zeta
                import nl.rhaydus.alpha.Alpha
            """.trimIndent()

            // ----- Act & Assert -----
            // The out-of-order import is on line 2, col 1
            ruleAssertThat(code).hasLintViolationWithoutAutoCorrect(
                2,
                1,
                message,
            )
        }
    }

    @Nested
    inner class Clean {
        @Test
        fun `clean when nl rhaydus imports are in alphabetical order`() {
            // ----- Arrange -----
            val code = """
                import nl.rhaydus.alpha.Alpha
                import nl.rhaydus.zeta.Zeta
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `clean for a single nl rhaydus import`() {
            // ----- Arrange -----
            val code = """
                import nl.rhaydus.alpha.Alpha
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `clean for non-project imports in any order`() {
            // ----- Arrange -----
            val code = """
                import androidx.compose.ui.Modifier
                import android.app.Activity
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }
    }
}
