package nl.rhaydus.designsystem.layout

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * The current bottom navigation bar footprint, provided by the bottom-bar host screen and read by
 * scrolling content so the last item is never occluded. Defaults to `0.dp` (no bar).
 */
val LocalBottomBarPadding = compositionLocalOf { 0.dp }
