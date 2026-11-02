package nl.rhaydus.detekt

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class InlineFullyQualifiedReferenceTest {
    private val rule = InlineFullyQualifiedReference(Config.empty)

    @Nested
    inner class Flagged {
        @Test
        fun `flags a fully-qualified type`() {
            // ----- Arrange -----
            val code = "val x: kotlin.collections.ArrayList<Int> = TODO()"

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `flags a fully-qualified constructor call`() {
            // ----- Arrange -----
            val code = "val x = java.util.UUID.randomUUID()"

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `flags a project-rooted reference`() {
            // ----- Arrange -----
            val code = "val x: nl.rhaydus.core.Foo = TODO()"

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `flags kotlin String used inline`() {
            // ----- Arrange -----
            val code = "val x: kotlin.String = TODO()"

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `flags kotlin Unit used inline`() {
            // ----- Arrange -----
            val code = "val x: kotlin.Unit = TODO()"

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `flags a reference when the exempt segment is not directly before the type`() {
            // ----- Arrange -----
            val configured = InlineFullyQualifiedReference(TestConfig("exemptPackageSegments" to listOf("generated")))
            val code = "val x: nl.rhaydus.generated.core.Foo = TODO()"

            // ----- Act -----
            val findings = configured.lint(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }
    }

    @Nested
    inner class Allowed {
        @Test
        fun `allows an import`() {
            // ----- Arrange -----
            val code = "import kotlin.collections.ArrayList"

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }

        @Test
        fun `allows the package directive`() {
            // ----- Arrange -----
            val code = "package nl.rhaydus.core.Foo"

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }

        @Test
        fun `allows a lowercase chain`() {
            // ----- Arrange -----
            val code = "val x = kotlinx.coroutines.flow.first"

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }

        @Test
        fun `allows a root outside the configured packages`() {
            // ----- Arrange -----
            val code = "val x: com.example.thing.Foo = TODO()"

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }

        @Test
        fun `allows a reference whose package ends directly under an exempt segment`() {
            // ----- Arrange -----
            val configured = InlineFullyQualifiedReference(TestConfig("exemptPackageSegments" to listOf("generated")))
            val code = "val x: nl.rhaydus.core.generated.Foo = TODO()"

            // ----- Act -----
            val findings = configured.lint(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }
    }
}
