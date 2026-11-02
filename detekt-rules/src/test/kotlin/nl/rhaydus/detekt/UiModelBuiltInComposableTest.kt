package nl.rhaydus.detekt

import dev.detekt.api.Config
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.KotlinEnvironmentContainer
import dev.detekt.test.utils.createEnvironment
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class UiModelBuiltInComposableTest {
    private val rule = UiModelBuiltInComposable(Config.empty)

    private fun lintWithTypes(body: String) = rule.lintWithContext(
        env,
        HEADER + body,
    )

    @Nested
    inner class ReportedInRender {
        @Test
        fun `reports a constructor inside a composable`() {
            // ----- Arrange -----
            val code = """
            @Composable
            fun Screen() {
                FooUiModel(1)
            }
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `reports a sealed variant constructor whose supertype is a UI model`() {
            // ----- Arrange -----
            val code = """
            @Composable
            fun Screen() {
                BarUiModel.Large(1)
            }
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `reports a copy of a UI model`() {
            // ----- Arrange -----
            val code = """
            @Composable
            fun Screen(model: FooUiModel) {
                model.copy(n = 2)
            }
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `reports a project mapper function returning a UI model`() {
            // ----- Arrange -----
            val code = """
            @Composable
            fun Screen() {
                1.toFooUiModel()
            }
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `reports a top-level property holding a model in a file that declares a composable`() {
            // ----- Arrange -----
            val code = """
            val model = FooUiModel(1)

            @Composable
            fun Screen() {
            }
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `reports a constructor in a composable-expected lambda inside a non-composable function`() {
            // ----- Arrange -----
            val code = """
            fun host(content: @Composable () -> Unit) {
            }

            fun build() {
                host { FooUiModel(1) }
            }
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `reports a constructor in a composable property getter`() {
            // ----- Arrange -----
            val code = """
            val model: FooUiModel
                @Composable get() = FooUiModel(1)
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `reports a composable member function of a class with a Preview-suffixed annotation`() {
            // ----- Arrange -----
            val code = """
            @StandardPreview
            class Host {
                @Composable
                fun Screen() {
                    FooUiModel(1)
                }
            }
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `reports a top-level function taking a UiState and returning a UI model`() {
            // ----- Arrange -----
            val code = """
            class FooUiState(val n: Int)

            fun mastheadFor(state: FooUiState) = FooUiModel(state.n)

            @Composable
            fun Screen(state: FooUiState) {
                mastheadFor(state)
            }
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }

        @Test
        fun `reports a constructor in a remember lambda in a composable`() {
            // ----- Arrange -----
            val code = """
            inline fun <T> remember(calculation: () -> T): T = calculation()

            @Composable
            fun Screen() {
                remember { FooUiModel(1) }
            }
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            findings shouldHaveSize 1
        }
    }

    @Nested
    inner class NotReported {
        @Test
        fun `does not report a UiState member function returning a stored UI model`() {
            // ----- Arrange -----
            val code = """
            class FooUiState(
                val models: Map<Int, FooUiModel>,
                val fallback: FooUiModel,
            ) {
                fun mastheadFor(tabId: Int): FooUiModel = models[tabId] ?: fallback
            }

            @Composable
            fun Screen(state: FooUiState, tabId: Int) {
                state.mastheadFor(tabId)
            }
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }

        @Test
        fun `does not report a library call returning a UI model from a map`() {
            // ----- Arrange -----
            val code = """
            @Composable
            fun Screen(map: Map<String, FooUiModel>) {
                map.getValue("a")
            }
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }

        @Test
        fun `does not report a constructor in a non-composable function`() {
            // ----- Arrange -----
            val code = """
            fun map(): FooUiModel {
                return FooUiModel(1)
            }
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }

        @Test
        fun `does not report a top-level property in a file without a composable`() {
            // ----- Arrange -----
            val code = """
            val model = FooUiModel(1)
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }

        @Test
        fun `does not report inside a function with a Preview-suffixed annotation`() {
            // ----- Arrange -----
            val code = """
            @StandardPreview
            @Composable
            fun Screen() {
                FooUiModel(1)
            }
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }

        @Test
        fun `does not report inside a function annotated ModelBuiltInComposition`() {
            // ----- Arrange -----
            val code = """
            @ModelBuiltInComposition(reason = "state is local")
            @Composable
            fun Screen() {
                FooUiModel(1)
            }
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }

        @Test
        fun `does not report a top-level property annotated ModelBuiltInComposition`() {
            // ----- Arrange -----
            val code = """
            @ModelBuiltInComposition(reason = "fixture")
            val model = FooUiModel(1)

            @Composable
            fun Screen() {
            }
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }

        @Test
        fun `does not report a constructor in a plain lambda inside a non-composable function`() {
            // ----- Arrange -----
            val code = """
            fun host(content: () -> Unit) {
            }

            fun build() {
                host { FooUiModel(1) }
            }
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }

        @Test
        fun `does not report a constructor built in an event lambda of a composable`() {
            // ----- Arrange -----
            val code = """
            fun button(onClick: () -> Unit) {
            }

            @Composable
            fun Screen(accept: (FooUiModel) -> Unit) {
                button(onClick = { accept(FooUiModel(1)) })
            }
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            findings.shouldBeEmpty()
        }
    }

    companion object {
        private lateinit var env: KotlinEnvironmentContainer

        private fun header(packageName: String) = """
            package $packageName

            annotation class Composable
            annotation class Preview
            annotation class StandardPreview
            annotation class ModelBuiltInComposition(val reason: String)

            data class FooUiModel(val n: Int)

            sealed interface BarUiModel {
                data class Large(val n: Int) : BarUiModel
            }

            fun Int.toFooUiModel() = FooUiModel(this)

            """.trimIndent() + "\n"

        private val HEADER = header("nl.rhaydus.sample")

        @BeforeAll
        @JvmStatic
        fun setUp() {
            env = createEnvironment()
        }
    }
}
