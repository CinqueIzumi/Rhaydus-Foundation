// FlowRow's `overflow` parameter and `FlowRowOverflow` are deprecated in Compose ("FlowLayout overflow
// is no longer maintained"), but the Compose team has shipped no stable replacement — the obvious
// successor `ContextualFlowRow` is itself deprecated, and the official guidance is to keep using (or
// vendor) the existing implementation until a real replacement lands. The progressive line-reveal here
// depends on `FlowRowOverflow.expandIndicator`, so the deprecation is suppressed file-wide; revisit once
// Compose ships a maintained overflow API.
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Lines shown before any expansion, when a caller doesn't specify one. */
private const val DEFAULT_COLLAPSED_LINES = 2

/** Lines revealed per "show more" tap, when a caller doesn't specify one. */
private const val DEFAULT_LINES_PER_EXPAND = 2

/** Default gap between wrapped items. */
private val FlowGap = 8.dp

/**
 * A [FlowRow] that wraps its items and, when they exceed [collapsedLines], collapses to that many
 * lines behind a trailing "show more" affordance. Each tap reveals [linesPerExpand] more lines until
 * everything is visible and the affordance disappears — a gradual reveal rather than an all-at-once
 * expansion, so a long set (e.g. a wrapping row of chips or tags) never buries the content beneath it.
 *
 * Reach for this wherever a wrapping set of chips/tags can grow unbounded. Both bounds are caller-set
 * ([collapsedLines], [linesPerExpand]); the trailing affordance defaults to a pill labelled
 * [showMoreLabel] but can be replaced wholesale via [showMoreIndicator]. [content] is a plain slot (no
 * `FlowRowScope` receiver) so a call site needs no experimental opt-in.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpandableFlowRow(
    modifier: Modifier = Modifier,
    collapsedLines: Int = DEFAULT_COLLAPSED_LINES,
    linesPerExpand: Int = DEFAULT_LINES_PER_EXPAND,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(FlowGap),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(FlowGap),
    showMoreLabel: String = "Show more",
    showMoreIndicator: @Composable (onExpand: () -> Unit) -> Unit = { onExpand ->
        ShowMoreChip(
            label = showMoreLabel,
            onClick = onExpand,
        )
    },
    content: @Composable () -> Unit,
) {
    var maxLines by remember(collapsedLines) { mutableStateOf(collapsedLines) }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        maxLines = maxLines,
        overflow = FlowRowOverflow.expandIndicator {
            showMoreIndicator { maxLines += linesPerExpand }
        },
        content = { content() },
    )
}

@Composable
private fun ShowMoreChip(
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
