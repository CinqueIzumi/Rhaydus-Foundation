package nl.rhaydus.konsist

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DeclarationAssertionsTest {
    @Nested
    inner class OneTypePerFile {
        @Test
        fun `fails on a second top-level type`() {
            // ----- Arrange -----
            val scope = files("types/TwoClasses.kt")

            // ----- Act & Assert -----
            shouldThrow<AssertionError> {
                scope.assertOneTypePerFile(excludedPaths = emptyList())
            }.message.orEmpty() shouldContain "TwoClasses.kt: 2 top-level types"
        }

        @Test
        fun `passes a sealed type with its variants`() {
            // ----- Arrange -----
            val scope = files("types/SealedHierarchy.kt")

            // ----- Act & Assert -----
            shouldNotThrowAny { scope.assertOneTypePerFile(excludedPaths = emptyList()) }
        }

        @Test
        fun `passes an interface with its single implementation`() {
            // ----- Arrange -----
            val scope = files("types/InterfaceWithImpl.kt")

            // ----- Act & Assert -----
            shouldNotThrowAny { scope.assertOneTypePerFile(excludedPaths = emptyList()) }
        }

        @Test
        fun `fails an interface with two implementations`() {
            // ----- Arrange -----
            val scope = files("types/InterfaceWithTwoImpls.kt")

            // ----- Act & Assert -----
            shouldThrow<AssertionError> {
                scope.assertOneTypePerFile(excludedPaths = emptyList())
            }
        }

        @Test
        fun `passes one type next to top-level functions`() {
            // ----- Arrange -----
            val scope = files("types/SingleType.kt")

            // ----- Act & Assert -----
            shouldNotThrowAny { scope.assertOneTypePerFile(excludedPaths = emptyList()) }
        }
    }

    @Nested
    inner class PlumbingVisibility {
        @Test
        fun `fails a public action in the action package`() {
            // ----- Arrange -----
            val scope = files("plumbing/PublicAction.kt")

            // ----- Act & Assert -----
            shouldThrow<AssertionError> {
                scope.assertPlumbingIsInternal(excludedPaths = emptyList())
            }.message.orEmpty() shouldContain "LoadAction is module-internal plumbing"
        }

        @Test
        fun `passes an internal action`() {
            // ----- Arrange -----
            val scope = files("plumbing/InternalAction.kt")

            // ----- Act & Assert -----
            shouldNotThrowAny { scope.assertPlumbingIsInternal(excludedPaths = emptyList()) }
        }

        @Test
        fun `passes a public type whose name has no plumbing suffix`() {
            // ----- Arrange -----
            val scope = files("plumbing/PublicNonPlumbingName.kt")

            // ----- Act & Assert -----
            shouldNotThrowAny { scope.assertPlumbingIsInternal(excludedPaths = emptyList()) }
        }

        @Test
        fun `passes a public action-named type outside the plumbing package`() {
            // ----- Arrange -----
            val scope = files("plumbing/PublicActionElsewhere.kt")

            // ----- Act & Assert -----
            shouldNotThrowAny { scope.assertPlumbingIsInternal(excludedPaths = emptyList()) }
        }
    }

    @Nested
    inner class NestedTests {
        @Test
        fun `fails a test directly in a plain class`() {
            // ----- Arrange -----
            val scope = files("nested/src/test/TopLevelTest.kt")

            // ----- Act & Assert -----
            shouldThrow<AssertionError> {
                scope.assertTestsAreNested(excludedPaths = emptyList())
            }.message.orEmpty() shouldContain "Plain.loose"
        }

        @Test
        fun `fails a file-level test function`() {
            // ----- Arrange -----
            val scope = files("nested/src/test/FileLevelTest.kt")

            // ----- Act & Assert -----
            shouldThrow<AssertionError> {
                scope.assertTestsAreNested(excludedPaths = emptyList())
            }.message.orEmpty() shouldContain "loose"
        }

        @Test
        fun `passes a test in a nested inner class`() {
            // ----- Arrange -----
            val scope = files("nested/src/test/NestedTest.kt")

            // ----- Act & Assert -----
            shouldNotThrowAny { scope.assertTestsAreNested(excludedPaths = emptyList()) }
        }

        @Test
        fun `fails a test in a nested class that is not inner`() {
            // ----- Arrange -----
            val scope = files("nested/src/test/NestedWithoutInner.kt")

            // ----- Act & Assert -----
            shouldThrow<AssertionError> {
                scope.assertTestsAreNested(excludedPaths = emptyList())
            }
        }

        @Test
        fun `ignores files outside a test source set`() {
            // ----- Arrange -----
            val scope = files("nested/src/test/TopLevelTest.kt")

            // ----- Act & Assert -----
            shouldNotThrowAny {
                scope.assertTestsAreNested(
                    testSourceSets = listOf("jvmTest"),
                    excludedPaths = emptyList(),
                )
            }
        }
    }

    @Nested
    inner class ModelFieldNames {
        @Test
        fun `fails an action field on a UiModel`() {
            // ----- Arrange -----
            val scope = files("models/ActionField.kt")

            // ----- Act & Assert -----
            shouldThrow<AssertionError> {
                scope.assertNoModelFieldNamed(excludedPaths = emptyList())
            }.message.orEmpty() shouldContain "ProfileUiModel.action"
        }

        @Test
        fun `passes an action field on a class that is not a UiModel`() {
            // ----- Arrange -----
            val scope = files("models/ActionOnNonModel.kt")

            // ----- Act & Assert -----
            shouldNotThrowAny { scope.assertNoModelFieldNamed(excludedPaths = emptyList()) }
        }

        @Test
        fun `passes a UiModel whose fields are named after what they hold`() {
            // ----- Arrange -----
            val scope = files("models/NamedField.kt")

            // ----- Act & Assert -----
            shouldNotThrowAny { scope.assertNoModelFieldNamed(excludedPaths = emptyList()) }
        }
    }

    @Nested
    inner class StateMembers {
        @Test
        fun `fails a getter that constructs a UiModel`() {
            // ----- Arrange -----
            val scope = files("states/GetterBuilds.kt")

            // ----- Act & Assert -----
            shouldThrow<AssertionError> {
                scope.assertNoUiModelBuiltInStateMember(excludedPaths = emptyList())
            }.message.orEmpty() shouldContain "ProfileUiState.model"
        }

        @Test
        fun `fails a getter that constructs a qualified UiModel`() {
            // ----- Arrange -----
            val scope = files("states/GetterBuildsQualified.kt")

            // ----- Act & Assert -----
            shouldThrow<AssertionError> {
                scope.assertNoUiModelBuiltInStateMember(excludedPaths = emptyList())
            }.message.orEmpty() shouldContain "QualifiedUiState.model"
        }

        @Test
        fun `fails a getter that converts items with a toUiModel call`() {
            // ----- Arrange -----
            val scope = files("states/GetterMapsToModels.kt")

            // ----- Act & Assert -----
            shouldThrow<AssertionError> {
                scope.assertNoUiModelBuiltInStateMember(excludedPaths = emptyList())
            }.message.orEmpty() shouldContain "MappedUiState.models"
        }

        @Test
        fun `fails a function returning a UiModel that constructs one`() {
            // ----- Arrange -----
            val scope = files("states/FunctionBuilds.kt")

            // ----- Act & Assert -----
            shouldThrow<AssertionError> {
                scope.assertNoUiModelBuiltInStateMember(excludedPaths = emptyList())
            }.message.orEmpty() shouldContain "BuildingFunctionUiState.model()"
        }

        @Test
        fun `fails a function returning a UiModel that copies one`() {
            // ----- Arrange -----
            val scope = files("states/FunctionCopies.kt")

            // ----- Act & Assert -----
            shouldThrow<AssertionError> {
                scope.assertNoUiModelBuiltInStateMember(excludedPaths = emptyList())
            }.message.orEmpty() shouldContain "CopyingFunctionUiState.renamed()"
        }

        @Test
        fun `fails a function without a declared return type that constructs a UiModel`() {
            // ----- Arrange -----
            val scope = files("states/FunctionWithoutReturnType.kt")

            // ----- Act & Assert -----
            shouldThrow<AssertionError> {
                scope.assertNoUiModelBuiltInStateMember(excludedPaths = emptyList())
            }.message.orEmpty() shouldContain "InferredFunctionUiState.model()"
        }

        @Test
        fun `passes a stored field and an unrelated getter`() {
            // ----- Arrange -----
            val scope = files("states/StoredField.kt")

            // ----- Act & Assert -----
            shouldNotThrowAny {
                scope.assertNoUiModelBuiltInStateMember(excludedPaths = emptyList())
            }
        }

        @Test
        fun `passes a getter that only copies a stored model`() {
            // ----- Arrange -----
            val scope = files("states/GetterReadsStored.kt")

            // ----- Act & Assert -----
            shouldNotThrowAny {
                scope.assertNoUiModelBuiltInStateMember(excludedPaths = emptyList())
            }
        }

        @Test
        fun `passes a reducer that returns the UiState itself`() {
            // ----- Arrange -----
            val scope = files("states/ReducerReturnsState.kt")

            // ----- Act & Assert -----
            shouldNotThrowAny {
                scope.assertNoUiModelBuiltInStateMember(excludedPaths = emptyList())
            }
        }

        @Test
        fun `passes members carrying an exempt annotation`() {
            // ----- Arrange -----
            val scope = files("states/ExemptMembers.kt")

            // ----- Act & Assert -----
            shouldNotThrowAny {
                scope.assertNoUiModelBuiltInStateMember(exemptAnnotations = listOf("Exempt"),
                    excludedPaths = emptyList(),)
            }
        }

        @Test
        fun `fails the same members when no annotation is exempt`() {
            // ----- Arrange -----
            val scope = files("states/ExemptMembers.kt")

            // ----- Act & Assert -----
            shouldThrow<AssertionError> {
                scope.assertNoUiModelBuiltInStateMember(excludedPaths = emptyList())
            }.message.orEmpty() shouldContain "ExemptUiState.model"
        }
    }
}
