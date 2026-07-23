package nl.rhaydus.designsystem.layout

/** Lines an [FlowRowExpansion.Progressive] tap reveals when a caller doesn't specify one. */
private const val DEFAULT_LINES_PER_EXPAND = 2

/**
 * How much of a collapsed [ExpandableFlowRow] one "show more" tap reveals.
 *
 * The choice is a product one, not a layout one: [Progressive] suits a set whose tail is worth
 * metering out a little at a time, [Full] a set the user asked to *browse* — where drip-feeding
 * lines makes the full contents effectively unreachable.
 */
sealed interface FlowRowExpansion {
    /**
     * Each tap reveals [linesPerExpand] more lines, so a long set unfolds gradually and never
     * displaces the content beneath it in one jump. The default for a set of unknown length.
     */
    data class Progressive(val linesPerExpand: Int = DEFAULT_LINES_PER_EXPAND) : FlowRowExpansion

    /**
     * A single tap reveals every remaining line at once. Pair it with `collapsible = true` when the
     * set is unbounded — without a way back, one tap can push everything below the row off-screen
     * for as long as the surface lives.
     */
    data object Full : FlowRowExpansion
}
