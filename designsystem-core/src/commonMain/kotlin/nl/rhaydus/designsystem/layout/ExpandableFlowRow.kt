// FlowRow's `overflow` parameter and `FlowRowOverflow` are deprecated in Compose ("FlowLayout overflow
// is no longer maintained"), but the Compose team has shipped no stable replacement — the obvious
// successor `ContextualFlowRow` is itself deprecated, and the official guidance is to keep using (or
// vendor) the existing implementation until a real replacement lands. The line reveal here depends on
// `FlowRowOverflow.expandIndicator` / `expandOrCollapseIndicator`, so the deprecation is suppressed
// file-wide; revisit once Compose ships a maintained overflow API.
@file:Suppress("DEPRECATION")

package nl.rhaydus.designsystem.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowOverflow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Lines shown before any expansion, when a caller doesn't specify one. */
private const val DEFAULT_COLLAPSED_LINES = 2

/** Default gap between wrapped items. */
private val FlowGap = 8.dp

/**
 * A [FlowRow] that wraps its items and, when they exceed [collapsedLines], collapses to that many
 * lines behind a trailing "show more" affordance — so a long set (e.g. a wrapping row of chips or
 * tags) never buries the content beneath it. How much one tap reveals is the caller's call
 * ([expansion]): [FlowRowExpansion.Progressive] meters the tail out a few lines at a time,
 * [FlowRowExpansion.Full] opens the whole set at once. Either way the affordance disappears once
 * everything is visible.
 *
 * [collapsible] gives the expanded state a matching "show less" back to [collapsedLines]. It is
 * off by default (a gradual reveal rarely needs a way back) but should be **on** for a
 * [FlowRowExpansion.Full] reveal over an unbounded set, where one tap can otherwise displace
 * everything below the row for as long as the surface lives.
 *
 * Reach for this wherever a wrapping set of chips/tags can grow unbounded. Both affordances default
 * to a pill labelled [showMoreLabel] / [showLessLabel] and can be replaced wholesale via
 * [showMoreIndicator] / [showLessIndicator]. [content] is a plain slot (no `FlowRowScope` receiver)
 * so a call site needs no experimental opt-in.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpandableFlowRow(
    modifier: Modifier = Modifier,
    collapsedLines: Int = DEFAULT_COLLAPSED_LINES,
    expansion: FlowRowExpansion = FlowRowExpansion.Progressive(),
    collapsible: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(FlowGap),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(FlowGap),
    showMoreLabel: String = "Show more",
    showLessLabel: String = "Show less",
    showMoreIndicator: @Composable (onExpand: () -> Unit) -> Unit = { onExpand ->
        IndicatorChip(
            label = showMoreLabel,
            onClick = onExpand,
        )
    },
    showLessIndicator: @Composable (onCollapse: () -> Unit) -> Unit = { onCollapse ->
        IndicatorChip(
            label = showLessLabel,
            onClick = onCollapse,
        )
    },
    content: @Composable () -> Unit,
) {
    val maxLines = remember(collapsedLines) { mutableIntStateOf(collapsedLines) }

    // Both callbacks are remembered rather than left to the compiler's lambda memoization:
    // `expandOrCollapseIndicator` keys its own `remember` on the indicator lambdas, so a callback
    // whose identity changed every recomposition would rebuild the overflow state each frame.
    val expand: () -> Unit = remember(expansion, maxLines) {
        {
            maxLines.intValue = when (expansion) {
                is FlowRowExpansion.Progressive -> maxLines.intValue + expansion.linesPerExpand
                FlowRowExpansion.Full -> Int.MAX_VALUE
            }
        }
    }

    val collapse: () -> Unit = remember(collapsedLines, maxLines) { { maxLines.intValue = collapsedLines } }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        maxLines = maxLines.intValue,
        overflow = if (collapsible) {
            FlowRowOverflow.expandOrCollapseIndicator(
                expandIndicator = { showMoreIndicator(expand) },
                collapseIndicator = { showLessIndicator(collapse) },
                // Without this the collapse affordance renders whenever `maxLines >= 1` — i.e. on a
                // set short enough to fit the collapsed height, which was never expanded and has
                // nothing to collapse. Requiring one line beyond `collapsedLines` ties it to the
                // only state that can reach it: a row the user actually expanded.
                minRowsToShowCollapse = collapsedLines + 1,
            )
        } else {
            FlowRowOverflow.expandIndicator { showMoreIndicator(expand) }
        },
        content = { content() },
    )
}

@Composable
private fun IndicatorChip(
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(percent = 50),
        onClick = onClick,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 10.dp,
            ),
        )
    }
}
