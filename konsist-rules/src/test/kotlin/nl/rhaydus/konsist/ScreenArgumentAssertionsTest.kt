package nl.rhaydus.konsist

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ScreenArgumentAssertionsTest {
    @Nested
    inner class ScreenArguments {
        @Test
        fun `fails on a non-serializable constructor property`() {
            // ----- Arrange -----
            val scope = directory("screens/bad")

            // ----- Act & Assert -----
            shouldThrow<AssertionError> {
                scope.assertScreenArgumentsSurviveSerialization(
                    screenSupertypes = listOf("Screen"),
                    excludedPaths = emptyList(),
                )
            }.message.orEmpty() shouldContain "BadScreen.items"
        }

        @Test
        fun `passes primitives, enums, serializable types and transient properties`() {
            // ----- Arrange -----
            val scope = directory("screens/good")

            // ----- Act & Assert -----
            shouldNotThrowAny {
                scope.assertScreenArgumentsSurviveSerialization(
                    screenSupertypes = listOf("Screen"),
                    excludedPaths = emptyList(),
                )
            }
        }
    }
}
