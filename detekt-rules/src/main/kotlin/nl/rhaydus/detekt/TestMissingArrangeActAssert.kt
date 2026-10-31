package nl.rhaydus.detekt

import com.intellij.psi.PsiComment
import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

/**
 * Reports a test function whose body does not carry the Arrange, Act and Assert marker comments in order.
 * The Arrange marker comes first and is followed by one or more Act and Assert phases, so a test with two
 * acts may repeat the pair, and a run of the same marker counts once. One `collapsedMarker` may stand in
 * for an Act and Assert pair, for a test whose act and assertion are the same expression. The finding is
 * reported at the test's name.
 *
 * The rule has no source-set knowledge: the consumer scopes it to test sources with `includes`.
 */
class TestMissingArrangeActAssert(config: Config) :
    Rule(
        config,
        "A test body must carry the Arrange, Act and Assert markers in order.",
    ) {
    @Configuration("The annotations that mark a function as a test.")
    private val testAnnotations: List<String> by config(listOf("Test", "ParameterizedTest", "TestFactory", "RepeatedTest"))

    @Configuration("The marker comments, in the order they must appear.")
    private val markers: List<String> by config(
        listOf("// ----- Arrange -----", "// ----- Act -----", "// ----- Assert -----"),
    )

    @Configuration("The single marker that may replace the last two markers when act and assert coincide.")
    private val collapsedMarker: String by config("// ----- Act & Assert -----")

    override fun preVisit(root: KtFile) {
        require(markers.size == MARKER_COUNT) {
            "TestMissingArrangeActAssert: 'markers' must list exactly $MARKER_COUNT comments " +
                "(Arrange, Act, Assert), but has ${markers.size}."
        }
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)

        if (function.annotationEntries.none { it.shortName?.asString() in testAnnotations }) return

        val body = function.bodyExpression ?: return

        val sequence = body
            .collectDescendantsOfType<PsiComment>()
            .map { it.text.trim() }
            .filter { it in markers || it == collapsedMarker }
            .withoutRepeats()

        if (sequence.isCarryingAllPhases()) return

        report(
            Finding(
                entity = Entity.atName(function),
                message = "Test body must carry ${markers.joinToString()} in that order, or the Act & Assert marker for the last two.",
            ),
        )
    }

    private fun List<String>.withoutRepeats(): List<String> {
        return filterIndexed { index, marker -> index == 0 || marker != this[index - 1] }
    }

    private fun List<String>.isCarryingAllPhases(): Boolean {
        val (arrange, act, assert) = markers

        if (firstOrNull() != arrange || size < 2) return false

        var index = 1

        while (index < size) {
            index += when {
                this[index] == collapsedMarker -> 1
                this[index] == act && getOrNull(index + 1) == assert -> 2
                else -> return false
            }
        }

        return true
    }

    private companion object {
        const val MARKER_COUNT = 3
    }
}
