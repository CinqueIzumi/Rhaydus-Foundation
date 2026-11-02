package nl.rhaydus.detekt

import dev.detekt.api.Config
import dev.detekt.test.lint
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class InlineMockkStubTest {
    private val rule = InlineMockkStub(Config.empty)

    @Nested
    inner class Flagged {
        @Test
        fun `flags a one-line every stub`() {
            // ----- Arrange -----
            val code = """
            fun f() {
                every { repo.get() } returns 1
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
        fun `allows the multi-line form`() {
            // ----- Arrange -----
            val code = """
            fun f() {
                every {
                    repo.get()
                } returns 1
            }
            """.trimIndent()

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }

        @Test
        fun `allows an empty block`() {
            // ----- Arrange -----
            val code = """
            fun f() {
                every { } returns 1
            }
            """.trimIndent()

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }

        @Test
        fun `allows an unrelated call with a one-line lambda`() {
            // ----- Arrange -----
            val code = """
            fun f() {
                run { repo.get() }
            }
            """.trimIndent()

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }
    }
}
