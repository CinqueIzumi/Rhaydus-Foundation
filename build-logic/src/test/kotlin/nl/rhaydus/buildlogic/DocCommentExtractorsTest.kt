package nl.rhaydus.buildlogic

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DocCommentExtractorsTest {
    @Nested
    inner class KotlinStyle {
        @Test
        fun `returns the text after a trailing line comment marker`() {
            // ----- Arrange -----
            val line = "val x = 1 // note"

            // ----- Act -----
            val comment = DocCommentExtractors.kotlinStyle(line)

            // ----- Assert -----
            comment shouldBe " note"
        }

        @Test
        fun `returns the text after a trailing block comment marker`() {
            // ----- Arrange -----
            val line = "val x = 1 /* note */"

            // ----- Act -----
            val comment = DocCommentExtractors.kotlinStyle(line)

            // ----- Assert -----
            comment shouldBe " note */"
        }

        @Test
        fun `treats a KDoc continuation line as a whole-line comment`() {
            // ----- Arrange -----
            val line = "     * see the plan"

            // ----- Act -----
            val comment = DocCommentExtractors.kotlinStyle(line)

            // ----- Assert -----
            comment shouldBe "* see the plan"
        }

        @Test
        fun `treats a KDoc opener as a whole-line comment`() {
            // ----- Arrange -----
            val line = "    /** Summary */"

            // ----- Act -----
            val comment = DocCommentExtractors.kotlinStyle(line)

            // ----- Assert -----
            comment shouldBe "/** Summary */"
        }

        @Test
        fun `returns null for a comment marker inside a string literal`() {
            // ----- Arrange -----
            val line = "val url = \"https://example.com/D17\""

            // ----- Act -----
            val comment = DocCommentExtractors.kotlinStyle(line)

            // ----- Assert -----
            comment shouldBe null
        }

        @Test
        fun `keeps a string open across an escaped quote`() {
            // ----- Arrange -----
            val line = "val s = \"a \\\" // D17\""

            // ----- Act -----
            val comment = DocCommentExtractors.kotlinStyle(line)

            // ----- Assert -----
            comment shouldBe null
        }

        @Test
        fun `finds the real comment after a string that closes past an escaped quote`() {
            // ----- Arrange -----
            val line = "val s = \"a \\\"\" // D17"

            // ----- Act -----
            val comment = DocCommentExtractors.kotlinStyle(line)

            // ----- Assert -----
            comment shouldBe " D17"
        }

        @Test
        fun `does not let a double quote char literal open a string`() {
            // ----- Arrange -----
            val line = "val c = '\"' // S4-1"

            // ----- Act -----
            val comment = DocCommentExtractors.kotlinStyle(line)

            // ----- Assert -----
            comment shouldBe " S4-1"
        }

        @Test
        fun `does not let an escaped single quote char literal swallow the comment`() {
            // ----- Arrange -----
            val line = "val c = '\\'' // D17"

            // ----- Act -----
            val comment = DocCommentExtractors.kotlinStyle(line)

            // ----- Assert -----
            comment shouldBe " D17"
        }

        @Test
        fun `returns null for a line with no comment`() {
            // ----- Arrange -----
            val line = "val x = 1 / 2"

            // ----- Act -----
            val comment = DocCommentExtractors.kotlinStyle(line)

            // ----- Assert -----
            comment shouldBe null
        }
    }

    @Nested
    inner class YamlStyle {
        @Test
        fun `returns the text after a hash at the line start`() {
            // ----- Arrange -----
            val line = "# per D17"

            // ----- Act -----
            val comment = DocCommentExtractors.yamlStyle(line)

            // ----- Assert -----
            comment shouldBe " per D17"
        }

        @Test
        fun `returns the text after a hash following a space`() {
            // ----- Arrange -----
            val line = "active: true  # note"

            // ----- Act -----
            val comment = DocCommentExtractors.yamlStyle(line)

            // ----- Assert -----
            comment shouldBe " note"
        }

        @Test
        fun `ignores a hash with no whitespace before it`() {
            // ----- Arrange -----
            val line = "x: a#D17"

            // ----- Act -----
            val comment = DocCommentExtractors.yamlStyle(line)

            // ----- Assert -----
            comment shouldBe null
        }

        @Test
        fun `ignores a hash inside a double-quoted scalar`() {
            // ----- Arrange -----
            val line = "key: \"a # D17\""

            // ----- Act -----
            val comment = DocCommentExtractors.yamlStyle(line)

            // ----- Assert -----
            comment shouldBe null
        }

        @Test
        fun `ignores a hash inside a single-quoted scalar with a doubled quote`() {
            // ----- Arrange -----
            val line = "key: 'it''s # D17'"

            // ----- Act -----
            val comment = DocCommentExtractors.yamlStyle(line)

            // ----- Assert -----
            comment shouldBe null
        }

        @Test
        fun `finds a comment after a closed quoted scalar`() {
            // ----- Arrange -----
            val line = "x: \"a\" # D17"

            // ----- Act -----
            val comment = DocCommentExtractors.yamlStyle(line)

            // ----- Assert -----
            comment shouldBe " D17"
        }
    }
}
