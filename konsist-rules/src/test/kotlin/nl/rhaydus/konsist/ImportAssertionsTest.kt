package nl.rhaydus.konsist

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ImportAssertionsTest {
    @Nested
    inner class ForbiddenImports {
        @Test
        fun `fails on an exact import`() {
            // ----- Arrange -----
            val scope = files("imports/ExactImport.kt")

            // ----- Act & Assert -----
            shouldThrow<AssertionError> {
                scope.assertNoForbiddenImports(
                    bans = listOf(TEXT_BAN),
                    excludedPaths = emptyList(),
                )
            }.message.orEmpty() shouldContain "ExactImport.kt"
        }

        @Test
        fun `fails on a wildcard import when the simple name is used`() {
            // ----- Arrange -----
            val scope = files("imports/WildcardImport.kt")

            // ----- Act & Assert -----
            shouldThrow<AssertionError> {
                scope.assertNoForbiddenImports(
                    bans = listOf(TEXT_BAN),
                    excludedPaths = emptyList(),
                )
            }.message.orEmpty() shouldContain "WildcardImport.kt"
        }

        @Test
        fun `passes on a wildcard import when the simple name is never used`() {
            // ----- Arrange -----
            val scope = files("imports/WildcardNoMention.kt")

            // ----- Act & Assert -----
            shouldNotThrowAny {
                scope.assertNoForbiddenImports(
                    bans = listOf(TEXT_BAN),
                    excludedPaths = emptyList(),
                )
            }
        }

        @Test
        fun `fails on a fully qualified reference in a body`() {
            // ----- Arrange -----
            val scope = files("imports/QualifiedReference.kt")

            // ----- Act & Assert -----
            shouldThrow<AssertionError> {
                scope.assertNoForbiddenImports(
                    bans = listOf(TEXT_BAN),
                    excludedPaths = emptyList(),
                )
            }.message.orEmpty() shouldContain "QualifiedReference.kt"
        }

        @Test
        fun `ignores a mention in a comment`() {
            // ----- Arrange -----
            val scope = files("imports/CommentOnly.kt")

            // ----- Act & Assert -----
            shouldNotThrowAny {
                scope.assertNoForbiddenImports(
                    bans = listOf(TEXT_BAN),
                    excludedPaths = emptyList(),
                )
            }
        }

        @Test
        fun `passes a file matching the allowlist glob`() {
            // ----- Arrange -----
            val scope = files("imports/allowed/AppText.kt")

            // ----- Act & Assert -----
            shouldNotThrowAny {
                scope.assertNoForbiddenImports(
                    bans = listOf(TEXT_BAN),
                    excludedPaths = emptyList(),
                )
            }
        }

        @Test
        fun `fails the same file when the allowlist is empty`() {
            // ----- Arrange -----
            val scope = files("imports/allowed/AppText.kt")

            // ----- Act & Assert -----
            shouldThrow<AssertionError> {
                scope.assertNoForbiddenImports(
                    bans = listOf(TEXT_BAN.copy(allowedFiles = emptyList())),
                    excludedPaths = emptyList(),
                )
            }
        }

        @Test
        fun `shows the reason in the failure`() {
            // ----- Arrange -----
            val scope = files("imports/ExactImport.kt")

            // ----- Act & Assert -----
            shouldThrow<AssertionError> {
                scope.assertNoForbiddenImports(
                    bans = listOf(TEXT_BAN),
                    excludedPaths = emptyList(),
                )
            }.message.orEmpty() shouldContain "use AppText"
        }

        @Test
        fun `skips files matching an excluded glob`() {
            // ----- Arrange -----
            val scope = files("imports/ExactImport.kt")

            // ----- Act & Assert -----
            shouldNotThrowAny {
                scope.assertNoForbiddenImports(
                    bans = listOf(TEXT_BAN),
                    excludedPaths = listOf("**/resources/imports/**"),
                )
            }
        }

        @Test
        fun `still fails a file when the excluded glob matches another path`() {
            // ----- Arrange -----
            val scope = files("imports/ExactImport.kt")

            // ----- Act & Assert -----
            shouldThrow<AssertionError> {
                scope.assertNoForbiddenImports(
                    bans = listOf(TEXT_BAN),
                    excludedPaths = listOf("**/resources/layering/**"),
                )
            }.message.orEmpty() shouldContain "ExactImport.kt"
        }
    }

    @Nested
    inner class Layering {
        @Test
        fun `fails when a data file imports presentation`() {
            // ----- Arrange -----
            val scope = files("layering/DataImportsPresentation.kt")

            // ----- Act & Assert -----
            shouldThrow<AssertionError> {
                scope.assertLowerLayersDoNotImportUpperLayer(excludedPaths = emptyList())
            }.message.orEmpty() shouldContain "DataImportsPresentation.kt"
        }

        @Test
        fun `passes when presentation imports data`() {
            // ----- Arrange -----
            val scope = files("layering/PresentationImportsData.kt")

            // ----- Act & Assert -----
            shouldNotThrowAny { scope.assertLowerLayersDoNotImportUpperLayer(excludedPaths = emptyList()) }
        }

        @Test
        fun `does not treat a package that merely contains the layer text as that layer`() {
            // ----- Arrange -----
            val scope = files("layering/DatabaseImportsPresentationLookalike.kt")

            // ----- Act & Assert -----
            shouldNotThrowAny { scope.assertLowerLayersDoNotImportUpperLayer(excludedPaths = emptyList()) }
        }
    }

    private companion object {
        val TEXT_BAN = ForbiddenImport(
            import = "androidx.compose.material3.Text",
            allowedFiles = listOf("**/allowed/AppText.kt"),
            reason = "use AppText",
        )
    }
}
