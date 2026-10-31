package nl.rhaydus.detekt

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TestMissingArrangeActAssertTest {
    private val rule = TestMissingArrangeActAssert(Config.empty)

    @Nested
    inner class Flagged {
        @Test
        fun `flags a test with no markers`() {
            // ----- Arrange -----
            val code = """
            class FooTest {
                @Test
                fun works() {
                    val a = 1
                }
            }
            """.trimIndent()

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `flags a test missing the Assert marker`() {
            // ----- Arrange -----
            val code = """
            class FooTest {
                @Test
                fun works() {
                    // ----- Arrange -----
                    // ----- Act -----
                }
            }
            """.trimIndent()

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `flags a test missing the Arrange marker`() {
            // ----- Arrange -----
            val code = """
            class FooTest {
                @Test
                fun works() {
                    // ----- Act -----
                    // ----- Assert -----
                }
            }
            """.trimIndent()

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `flags Assert before Act`() {
            // ----- Arrange -----
            val code = """
            class FooTest {
                @Test
                fun works() {
                    // ----- Arrange -----
                    // ----- Assert -----
                    // ----- Act -----
                }
            }
            """.trimIndent()

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `flags a dangling Act after a complete pair`() {
            // ----- Arrange -----
            val code = """
            class FooTest {
                @Test
                fun works() {
                    // ----- Arrange -----
                    // ----- Act -----
                    // ----- Assert -----
                    // ----- Act -----
                }
            }
            """.trimIndent()

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `flags a test with only the Arrange marker`() {
            // ----- Arrange -----
            val code = """
            class FooTest {
                @Test
                fun works() {
                    // ----- Arrange -----
                }
            }
            """.trimIndent()

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `flags a ParameterizedTest function with no markers by default`() {
            // ----- Arrange -----
            val code = """
            class FooTest {
                @ParameterizedTest
                fun works() {
                    val a = 1
                }
            }
            """.trimIndent()

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `fails with a clear error when markers does not have exactly three entries`() {
            // ----- Arrange -----
            val configured = TestMissingArrangeActAssert(TestConfig("markers" to listOf("// A", "// B")))

            val code = """
            class FooTest {
                @Test
                fun works() {
                    // A
                    // B
                }
            }
            """.trimIndent()

            // ----- Act & Assert -----
            val error = shouldThrow<IllegalArgumentException> { configured.lint(code) }
            error.message.orEmpty() shouldContain "exactly 3"
        }
    }

    @Nested
    inner class Allowed {
        @Test
        fun `allows Arrange Act Assert`() {
            // ----- Arrange -----
            val code = """
            class FooTest {
                @Test
                fun works() {
                    // ----- Arrange -----
                    // ----- Act -----
                    // ----- Assert -----
                }
            }
            """.trimIndent()

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }

        @Test
        fun `allows Arrange then Act and Assert`() {
            // ----- Arrange -----
            val code = """
            class FooTest {
                @Test
                fun works() {
                    // ----- Arrange -----
                    // ----- Act & Assert -----
                }
            }
            """.trimIndent()

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }

        @Test
        fun `allows repeated Act and Assert pairs`() {
            // ----- Arrange -----
            val code = """
            class FooTest {
                @Test
                fun works() {
                    // ----- Arrange -----
                    // ----- Act -----
                    // ----- Assert -----
                    // ----- Act -----
                    // ----- Assert -----
                }
            }
            """.trimIndent()

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }

        @Test
        fun `allows a repeated marker run counted once`() {
            // ----- Arrange -----
            val code = """
            class FooTest {
                @Test
                fun works() {
                    // ----- Arrange -----
                    // ----- Arrange -----
                    // ----- Act -----
                    // ----- Act -----
                    // ----- Assert -----
                    // ----- Assert -----
                }
            }
            """.trimIndent()

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }

        @Test
        fun `allows a collapsed marker after a complete pair`() {
            // ----- Arrange -----
            val code = """
            class FooTest {
                @Test
                fun works() {
                    // ----- Arrange -----
                    // ----- Act -----
                    // ----- Assert -----
                    // ----- Act & Assert -----
                }
            }
            """.trimIndent()

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }

        @Test
        fun `ignores a function that is not a test`() {
            // ----- Arrange -----
            val code = """
            class FooTest {
                fun helper() {
                    val a = 1
                }
            }
            """.trimIndent()

            // ----- Act -----
            val findings = rule.lint(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }
    }
}
