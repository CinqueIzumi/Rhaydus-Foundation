package nl.rhaydus.detekt

import dev.detekt.api.Config
import dev.detekt.test.lint
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class UseCaseRunCatchingTest {
    private val rule = UseCaseRunCatching(Config.empty)

    @Nested
    inner class Flagged {
        @Test
        fun `flags a bare runCatching`() {
            // ----- Arrange -----
            val code = "fun f() = runCatching { 1 }"

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }
    }

    @Nested
    inner class Allowed {
        @Test
        fun `allows runCatchingLogged`() {
            // ----- Arrange -----
            val code = "fun f() = runCatchingLogged { 1 }"

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }
    }
}
