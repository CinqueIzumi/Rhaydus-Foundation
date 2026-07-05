package nl.rhaydus.ktlint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private val ruleAssertThat = assertThatRule { OneTypePerFileRule() }

class OneTypePerFileRuleTest {
    @Nested
    inner class Fires {
        @Test
        fun `fires when file contains two unrelated top-level classes`() {
            // ----- Arrange -----
            val code = """
                class Alpha

                class Beta
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasLintViolationWithoutAutoCorrect(
                3,
                1,
                "More than one top-level type in this file (2) - one type per file, named after it; only a single sealed type's own variants, or an interface plus its implementing class, may co-locate (§Files)",
            )
        }

        @Test
        fun `fires for an interface plus an unrelated class`() {
            // ----- Arrange -----
            val code = """
                interface FooDataSource {
                    fun load(): Int
                }

                class Unrelated
            """.trimIndent()

            // ----- Act & Assert -----
            // The impl-exemption requires the class to implement the interface; an unrelated class still fires.
            ruleAssertThat(code).hasLintViolationWithoutAutoCorrect(
                5,
                1,
                "More than one top-level type in this file (2) - one type per file, named after it; only a single sealed type's own variants, or an interface plus its implementing class, may co-locate (§Files)",
            )
        }

        @Test
        fun `fires for two interfaces in one file`() {
            // ----- Arrange -----
            val code = """
                interface Foo

                interface Bar
            """.trimIndent()

            // ----- Act & Assert -----
            // The impl-exemption requires exactly one interface plus a (non-interface) implementing type.
            ruleAssertThat(code).hasLintViolationWithoutAutoCorrect(
                3,
                1,
                "More than one top-level type in this file (2) - one type per file, named after it; only a single sealed type's own variants, or an interface plus its implementing class, may co-locate (§Files)",
            )
        }
    }

    @Nested
    inner class Clean {
        @Test
        fun `clean when file contains a single class`() {
            // ----- Arrange -----
            val code = """
                class Alpha
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `clean for sealed hierarchy with subtypes in the same file`() {
            // ----- Arrange -----
            val code = """
                sealed interface Shape

                data class Circle(val radius: Int) : Shape

                data object Square : Shape
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `clean for an interface plus its implementing class`() {
            // ----- Arrange -----
            val code = """
                interface FooDataSource {
                    fun load(): Int
                }

                internal class FooDataSourceImpl : FooDataSource {
                    override fun load(): Int = 0
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `clean for an interface plus an implementing object`() {
            // ----- Arrange -----
            val code = """
                interface Foo {
                    fun load(): Int
                }

                internal object FooImpl : Foo {
                    override fun load(): Int = 0
                }
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `clean for one classifier plus a top-level function`() {
            // ----- Arrange -----
            val code = """
                class Alpha

                fun helper(): Unit = Unit
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }

        @Test
        fun `clean for one classifier plus a top-level property`() {
            // ----- Arrange -----
            val code = """
                class Alpha

                val CONSTANT = 42
            """.trimIndent()

            // ----- Act & Assert -----
            ruleAssertThat(code).hasNoLintViolations()
        }
    }
}
