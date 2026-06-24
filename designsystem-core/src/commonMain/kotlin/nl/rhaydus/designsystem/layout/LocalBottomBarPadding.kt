package nl.rhaydus.designsystem.layout

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * The current bottom navigation bar footprint, provided by [BottomBarScaffold] (the overlay host) and read
 * by scrolling content through [rememberBottomBarPadding] so the last item is never occluded. Defaults to
 * `0.dp` - no overlay bar (e.g. under a docked bar, where the Material `Scaffold` innerPadding reserves it).
 */
val LocalBottomBarPadding = compositionLocalOf { 0.dp }
