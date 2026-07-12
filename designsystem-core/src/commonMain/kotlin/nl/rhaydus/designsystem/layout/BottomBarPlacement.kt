package nl.rhaydus.designsystem.layout

/**
 * How a [BottomBarScaffold] places its bar relative to the content, and therefore how much trailing
 * padding scrolling content has to reserve for it.
 */
enum class BottomBarPlacement {
    /**
     * The bar is attached to the foot of the layout and takes its own space, so the content is already
     * laid out above it. Scrolling content reserves only the breathing gap.
     */
    DOCKED,

    /**
     * The bar floats *over* the content, so layout reserves nothing for it. Scrolling content reserves
     * the bar's measured footprint plus the breathing gap.
     */
    OVERLAY,
}
