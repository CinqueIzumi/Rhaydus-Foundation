package nl.rhaydus.detekt

import dev.detekt.api.Config
import dev.detekt.test.lint
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NestedCommentInKDocTest {
    private val rule = NestedCommentInKDoc(Config.empty)

    @Nested
    inner class Flagged {
        @Test
        fun `flags a block-comment opener inside KDoc`() {
            // ----- Arrange -----
            val code = """
            /**
             * Matches src/*.kt files.
             */
            class Foo
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
        fun `allows a plain block comment`() {
            // ----- Arrange -----
            val code = """
            /* just a comment */
            class Foo
            """.trimIndent()

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }

        @Test
        fun `allows a clean KDoc`() {
            // ----- Arrange -----
            val code = """
            /**
             * Describes Foo.
             */
            class Foo
            """.trimIndent()

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }
    }
}
