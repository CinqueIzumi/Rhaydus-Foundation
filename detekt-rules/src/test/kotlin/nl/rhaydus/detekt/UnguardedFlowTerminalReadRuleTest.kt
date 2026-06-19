package nl.rhaydus.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
class UnguardedFlowTerminalReadRuleTest(private val env: KotlinCoreEnvironment) {
    private val rule = UnguardedFlowTerminalReadRule(Config.empty)

    @Nested
    inner class Fires {
        @Test
        fun `flags Flow first() with no argument`() {
            // ----- Arrange -----
            val code =
                """
                import kotlinx.coroutines.flow.Flow
                import kotlinx.coroutines.flow.first

                suspend fun read(flow: Flow<Int>): Int = flow.first()
                """.trimIndent()

            // ----- Act -----
            val findings = rule.compileAndLintWithContext(
                env,
                code,
            )

            // ----- Assert -----
            assertEquals(
                1,
                findings.size,
            )
        }

        @Test
        fun `flags Flow single()`() {
            // ----- Arrange -----
            val code =
                """
                import kotlinx.coroutines.flow.Flow
                import kotlinx.coroutines.flow.single

                suspend fun read(flow: Flow<Int>): Int = flow.single()
                """.trimIndent()

            // ----- Act -----
            val findings = rule.compileAndLintWithContext(
                env,
                code,
            )

            // ----- Assert -----
            assertEquals(
                1,
                findings.size,
            )
        }

        @Test
        fun `flags Flow first() with predicate — still a crashing terminal`() {
            // ----- Arrange -----
            val code =
                """
                import kotlinx.coroutines.flow.Flow
                import kotlinx.coroutines.flow.first

                suspend fun read(flow: Flow<Int>): Int = flow.first { it > 0 }
                """.trimIndent()

            // ----- Act -----
            val findings = rule.compileAndLintWithContext(
                env,
                code,
            )

            // ----- Assert -----
            assertEquals(
                1,
                findings.size,
            )
        }
    }

    @Nested
    inner class Clean {
        @Test
        fun `ignores Collection first()`() {
            // ----- Arrange -----
            val code = "fun read(): Int = listOf(1, 2).first()"

            // ----- Act -----
            val findings = rule.compileAndLintWithContext(
                env,
                code,
            )

            // ----- Assert -----
            assertEquals(
                0,
                findings.size,
            )
        }

        @Test
        fun `ignores Collection single()`() {
            // ----- Arrange -----
            val code = "fun read(): Int = listOf(1, 2).single()"

            // ----- Act -----
            val findings = rule.compileAndLintWithContext(
                env,
                code,
            )

            // ----- Assert -----
            assertEquals(
                0,
                findings.size,
            )
        }

        @Test
        fun `ignores Flow firstOrNull() — guarded form`() {
            // ----- Arrange -----
            val code =
                """
                import kotlinx.coroutines.flow.Flow
                import kotlinx.coroutines.flow.firstOrNull

                suspend fun read(flow: Flow<Int>): Int? = flow.firstOrNull()
                """.trimIndent()

            // ----- Act -----
            val findings = rule.compileAndLintWithContext(
                env,
                code,
            )

            // ----- Assert -----
            assertEquals(
                0,
                findings.size,
            )
        }

        @Test
        fun `ignores Flow singleOrNull() — guarded form`() {
            // ----- Arrange -----
            val code =
                """
                import kotlinx.coroutines.flow.Flow
                import kotlinx.coroutines.flow.singleOrNull

                suspend fun read(flow: Flow<Int>): Int? = flow.singleOrNull()
                """.trimIndent()

            // ----- Act -----
            val findings = rule.compileAndLintWithContext(
                env,
                code,
            )

            // ----- Assert -----
            assertEquals(
                0,
                findings.size,
            )
        }

        @Test
        fun `ignores Flow consumed reactively via collect`() {
            // ----- Arrange -----
            val code =
                """
                import kotlinx.coroutines.flow.Flow

                suspend fun consume(flow: Flow<Int>) {
                    flow.collect { value -> println(value) }
                }
                """.trimIndent()

            // ----- Act -----
            val findings = rule.compileAndLintWithContext(
                env,
                code,
            )

            // ----- Assert -----
            assertEquals(
                0,
                findings.size,
            )
        }
    }
}
