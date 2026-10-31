package nl.rhaydus.designsystem.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp

/** Lines shown before any expansion, when a caller doesn't specify one. */
private const val DEFAULT_COLLAPSED_LINES = 2

private const val MORE_SLOT = "expandableFlowRow.more"

private const val LESS_SLOT = "expandableFlowRow.less"

/**
 * A wrapping row that collapses to [collapsedLines] behind a trailing "show more" affordance, so a
 * long set (e.g. chips or tags) never buries the content beneath it. How much one tap reveals is
 * the caller's call ([expansion]): [FlowRowExpansion.Progressive] meters the tail out a few lines at
 * a time, [FlowRowExpansion.Full] opens the whole set at once. Either way the affordance disappears
 * once everything is visible.
 *
 * Composes the items it places, the first item that overflows, and the affordance slot:
 * [itemContent] runs for indices in order and stops at the first item that doesn't fit, so a
 * collapsed row costs its visible lines rather than [itemCount]. The last visible line keeps room
 * for the affordance, so an item that would crowd it out moves behind the fold. Each [itemContent]
 * call must emit one item. As a `SubcomposeLayout` it reports no intrinsic measurements.
 *
 * [key] gives each item a stable identity across recompositions, so an item that moves keeps its
 * remembered state; it defaults to the index.
 *
 * [collapsible] gives the expanded state a matching "show less" back to [collapsedLines]. It is
 * off by default (a gradual reveal rarely needs a way back) but should be **on** for a
 * [FlowRowExpansion.Full] reveal over an unbounded set, where one tap can otherwise displace
 * everything below the row for as long as the surface lives.
 *
 * Reach for this wherever a wrapping set of chips/tags can grow unbounded. Both affordances default
 * to a pill labelled [showMoreLabel] / [showLessLabel] and can be replaced wholesale via
 * [showMoreIndicator] / [showLessIndicator].
 */
@Composable
fun ExpandableFlowRow(
    itemCount: Int,
    modifier: Modifier = Modifier,
    key: ((index: Int) -> Any)? = null,
    collapsedLines: Int = DEFAULT_COLLAPSED_LINES,
    expansion: FlowRowExpansion = FlowRowExpansion.Progressive(),
    collapsible: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
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
    itemContent: @Composable (index: Int) -> Unit,
) {
    val maxLines = remember(collapsedLines) { mutableIntStateOf(collapsedLines) }

    val expand: () -> Unit = remember(expansion, maxLines) {
        {
            maxLines.intValue = when (expansion) {
                is FlowRowExpansion.Progressive -> maxLines.intValue + expansion.linesPerExpand
                FlowRowExpansion.Full -> Int.MAX_VALUE
            }
        }
    }

    val collapse: () -> Unit = remember(collapsedLines, maxLines) { { maxLines.intValue = collapsedLines } }

    SubcomposeLayout(modifier = modifier) { constraints ->
        measureFlowRow(
            spec = FlowRowSpec(
                itemCount = itemCount,
                key = key,
                collapsedLines = collapsedLines,
                collapsible = collapsible,
                horizontalArrangement = horizontalArrangement,
                verticalArrangement = verticalArrangement,
                itemContent = itemContent,
                moreIndicator = { showMoreIndicator(expand) },
                lessIndicator = { showLessIndicator(collapse) },
            ),
            maxLines = maxLines.intValue,
            constraints = constraints,
        )
    }
}

private class FlowRowSpec(
    val itemCount: Int,
    val key: ((index: Int) -> Any)?,
    val collapsedLines: Int,
    val collapsible: Boolean,
    val horizontalArrangement: Arrangement.Horizontal,
    val verticalArrangement: Arrangement.Vertical,
    val itemContent: @Composable (index: Int) -> Unit,
    val moreIndicator: @Composable () -> Unit,
    val lessIndicator: @Composable () -> Unit,
)

private fun SubcomposeMeasureScope.measureFlowRow(
    spec: FlowRowSpec,
    maxLines: Int,
    constraints: Constraints,
): MeasureResult {
    val childConstraints = Constraints(maxWidth = constraints.maxWidth)
    val gap = spec.horizontalArrangement.spacing.roundToPx()
    val limit = maxLines.coerceAtLeast(1)

    val moreIndicator = if (limit < Int.MAX_VALUE) {
        measureSlot(
            MORE_SLOT,
            childConstraints,
            spec.moreIndicator,
        )
    } else {
        null
    }

    val breaker = FlowLineBreaker(
        maxWidth = constraints.maxWidth,
        gap = gap,
        maxLines = limit,
        indicatorWidth = moreIndicator?.width ?: 0,
    )

    val lines = mutableListOf<MutableList<Placeable>>()

    val overflowed = placeItems(
        spec = spec,
        breaker = breaker,
        childConstraints = childConstraints,
        lines = lines,
    )

    if (overflowed) {
        moreIndicator?.let {
            if (lines.isEmpty()) lines.add(mutableListOf())

            val line = lines.last()
            val kept = breaker.fittingCount(line.map { item -> item.width })

            while (line.size > kept) line.removeAt(line.lastIndex)

            line.add(it)
        }
    } else if (spec.collapsible && lines.size > spec.collapsedLines) {
        measureSlot(
            LESS_SLOT,
            childConstraints,
            spec.lessIndicator,
        )?.let { less ->
            lines.addAt(
                breaker.placeTrailing(less.width),
                less,
            )
        }
    }

    return layoutLines(
        lines = lines,
        spec = spec,
        gap = gap,
        constraints = constraints,
    )
}

/** Measures the items into [lines] until one no longer fits; returns whether that happened. */
private fun SubcomposeMeasureScope.placeItems(
    spec: FlowRowSpec,
    breaker: FlowLineBreaker,
    childConstraints: Constraints,
    lines: MutableList<MutableList<Placeable>>,
): Boolean {
    for (index in 0 until spec.itemCount) {
        val item = measureSlot(ItemSlot(spec.key?.invoke(index) ?: index), childConstraints) { spec.itemContent(index) }

        if (item != null) {
            val line = breaker.place(
                width = item.width,
                hasMore = index < spec.itemCount - 1,
            )

            if (line == NOT_PLACED) return true

            lines.addAt(
                line,
                item,
            )
        }
    }

    return false
}

private fun SubcomposeMeasureScope.layoutLines(
    lines: List<List<Placeable>>,
    spec: FlowRowSpec,
    gap: Int,
    constraints: Constraints,
): MeasureResult {
    val width = constraints.constrainWidth(
        lines.maxOfOrNull { line -> line.sumOf { it.width } + gap * (line.size - 1) } ?: 0,
    )

    val lineHeights = IntArray(lines.size) { index -> lines[index].maxOf { it.height } }
    val rowGap = spec.verticalArrangement.spacing.roundToPx()
    val height = constraints.constrainHeight(lineHeights.sum() + rowGap * (lines.size - 1).coerceAtLeast(0))

    val ys = IntArray(lines.size)

    with(spec.verticalArrangement) {
        arrange(
            height,
            lineHeights,
            ys,
        )
    }

    val xs = lines.map { line ->
        IntArray(line.size).also { positions ->
            with(spec.horizontalArrangement) {
                arrange(
                    width,
                    IntArray(line.size) { line[it].width },
                    layoutDirection,
                    positions,
                )
            }
        }
    }

    return layout(width, height) {
        lines.forEachIndexed { row, line ->
            line.forEachIndexed { column, placeable ->
                placeable.place(
                    xs[row][column],
                    ys[row],
                )
            }
        }
    }
}

private fun MutableList<MutableList<Placeable>>.addAt(
    line: Int,
    placeable: Placeable,
) {
    if (line == size) add(mutableListOf())

    this[line].add(placeable)
}

private fun SubcomposeMeasureScope.measureSlot(
    slotId: Any,
    constraints: Constraints,
    content: @Composable () -> Unit,
): Placeable? = subcompose(slotId) { Box { content() } }.firstOrNull()?.measure(constraints)

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
