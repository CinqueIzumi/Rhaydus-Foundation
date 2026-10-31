package nl.rhaydus.detekt

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import dev.detekt.test.utils.compileContentForTest
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

class RawCornerDpTest {
    private val rule = RawCornerDp(Config.empty)

    @Nested
    inner class FlaggedCorners {
        @Test
        fun `flags a positional dp in RoundedCornerShape`() {
            // ----- Arrange -----
            val code = "val s = RoundedCornerShape(8.dp)"

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `flags every raw corner in a multi-argument call`() {
            // ----- Arrange -----
            val code = "val s = RoundedCornerShape(topStart = 8.dp, bottomEnd = 12.dp)"

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings shouldHaveSize 2
        }

        @Test
        fun `flags only the raw corner next to a zero one`() {
            // ----- Arrange -----
            val code = "val s = RoundedCornerShape(topStart = 0.dp, bottomEnd = 12.dp)"

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `flags a negative dp`() {
            // ----- Arrange -----
            val code = "val s = RoundedCornerShape(-4.dp)"

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `flags a dp literal written with underscores`() {
            // ----- Arrange -----
            val code = "val s = RoundedCornerShape(1_2.dp)"

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `flags a float dp literal`() {
            // ----- Arrange -----
            val code = "val s = RoundedCornerShape(2.5f.dp)"

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `flags a named percent argument`() {
            // ----- Arrange -----
            val code = "val s = RoundedCornerShape(percent = 50)"

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `flags a positional int percent`() {
            // ----- Arrange -----
            val code = "val s = RoundedCornerShape(50)"

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `flags a CornerSize dp inside a copy of a token`() {
            // ----- Arrange -----
            val code = "val s = Corners.md.copy(topStart = CornerSize(8.dp))"

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `flags a non-literal named percent`() {
            // ----- Arrange -----
            val code = "val s = RoundedCornerShape(percent = radiusPercent)"

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `reports the offending argument with the token message`() {
            // ----- Arrange -----
            val code = "val s = RoundedCornerShape(8.dp)"

            // ----- Act -----
            val finding = rule.lint(code).firstOrNull()

            // ----- Assert -----
            finding.shouldNotBeNull()
            finding.entity.signature.contains("8.dp") shouldBe true
        }
    }

    @Nested
    inner class AllowedCorners {
        @Test
        fun `does not flag zero`() {
            // ----- Arrange -----
            val code = "val s = RoundedCornerShape(0.dp)"

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }

        @Test
        fun `does not flag a zero written as a float`() {
            // ----- Arrange -----
            val code = "val s = RoundedCornerShape(0.0f.dp)"

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }

        @Test
        fun `does not flag a Corners token`() {
            // ----- Arrange -----
            val code = "val s = RoundedCornerShape(Corners.md)"

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }

        @Test
        fun `does not flag a zero named percent`() {
            // ----- Arrange -----
            val code = "val s = RoundedCornerShape(percent = 0)"

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }

        @Test
        fun `does not flag a dp in an unrelated call`() {
            // ----- Arrange -----
            val code = "val m = Modifier.padding(8.dp)"

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }

        @Test
        fun `does not flag a non-dp unit`() {
            // ----- Arrange -----
            val code = "val s = RoundedCornerShape(8.sp)"

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }
    }

    @Nested
    inner class TokenFile {
        @Test
        fun `does not flag raw corners in Corners kt`() {
            // ----- Arrange -----
            val file = compileContentForTest(
                content = "val md = RoundedCornerShape(8.dp)",
                filename = "Corners.kt",
            )

            // ----- Act -----
            val findings = rule.lint(file)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }

        @Test
        fun `does not flag raw corners in a Corners kt reached through a full path`() {
            // ----- Arrange -----
            val file = compileContentForTest(
                content = "val md = RoundedCornerShape(8.dp)",
                path = Path("/project/core/component/src/commonMain/Corners.kt"),
            )

            // ----- Act -----
            val findings = rule.lint(file)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }
    }

    @Nested
    inner class Configured {
        @Test
        fun `does not flag a value listed in allowedValues`() {
            // ----- Arrange -----
            val configured = RawCornerDp(TestConfig("allowedValues" to listOf("8")))
            val code = "val s = RoundedCornerShape(8.dp)"

            // ----- Act -----
            val findings = configured.lint(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }

        @Test
        fun `does not flag raw values in a file listed in tokenFiles`() {
            // ----- Arrange -----
            val configured = RawCornerDp(TestConfig("tokenFiles" to listOf("Tokens.kt")))

            val file = compileContentForTest(
                content = "val md = RoundedCornerShape(8.dp)",
                filename = "Tokens.kt",
            )

            // ----- Act -----
            val findings = configured.lint(file)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }
    }
}
