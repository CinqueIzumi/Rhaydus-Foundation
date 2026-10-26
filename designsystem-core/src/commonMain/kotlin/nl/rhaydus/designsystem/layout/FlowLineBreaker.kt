package nl.rhaydus.designsystem.layout

/** Returned by [FlowLineBreaker.place] when the item belongs beyond the last visible line. */
internal const val NOT_PLACED = -1

/**
 * Breaks a stream of item widths into lines of at most [maxWidth], one item at a time, so a caller
 * can stop measuring as soon as [maxLines] are full. When more items follow, the last visible line
 * keeps [indicatorWidth] free so a trailing indicator always fits beside its items.
 */
internal class FlowLineBreaker(
    private val maxWidth: Int,
    private val gap: Int,
    private val maxLines: Int,
    private val indicatorWidth: Int,
) {
    private var lineCount = 0
    private var usedWidth = 0

    /** Returns the zero-based line [width] lands on, or [NOT_PLACED] when no visible line has room. */
    fun place(
        width: Int,
        hasMore: Boolean,
    ): Int {
        val reserve = if (hasMore) gap + indicatorWidth else 0

        if (lineCount > 0) {
            val needed = usedWidth + gap + width + if (lineCount == maxLines) reserve else 0

            if (needed <= maxWidth) {
                usedWidth += gap + width

                return lineCount - 1
            }

            if (lineCount == maxLines) return NOT_PLACED
        }

        val startsLastLine = lineCount + 1 == maxLines

        if (lineCount > 0 && startsLastLine && width + reserve > maxWidth) return NOT_PLACED

        lineCount++
        usedWidth = width

        return lineCount - 1
    }

    /** Places an item that ignores the line limit, such as a collapse indicator after the last item. */
    fun placeTrailing(width: Int): Int {
        if (lineCount > 0 && usedWidth + gap + width <= maxWidth) {
            usedWidth += gap + width

            return lineCount - 1
        }

        lineCount++
        usedWidth = width

        return lineCount - 1
    }

    /**
     * Returns how many leading items of the last placed line, given by [lineWidths], stay beside the
     * indicator once trailing items are dropped until the indicator fits [maxWidth].
     */
    fun fittingCount(lineWidths: List<Int>): Int {
        var count = lineWidths.size

        while (count > 0 && lineWidths.take(count).sumOf { it + gap } + indicatorWidth > maxWidth) {
            count--
        }

        return count
    }
}
