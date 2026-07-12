package nl.rhaydus.designsystem.layout

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * The trailing padding scrolling content must reserve for the current bottom navigation bar, provided by
 * [BottomBarScaffold] and read through [rememberBottomBarPadding] so the last item is never occluded.
 * Under an overlay bar that is the bar's measured footprint plus the breathing gap; under a docked bar,
 * whose space layout already reserves, it is the breathing gap alone. Defaults to `0.dp` - no bottom bar
 * at all.
 */
val LocalBottomBarPadding = compositionLocalOf { 0.dp }
