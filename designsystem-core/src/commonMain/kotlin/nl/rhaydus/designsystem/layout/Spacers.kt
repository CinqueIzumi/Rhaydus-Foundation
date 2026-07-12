package nl.rhaydus.designsystem.layout

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@Composable
fun BottomNavigationSpacer() {
    Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
}

/**
 * The trailing padding a bottom bar demands of scrolling content, provided by [BottomBarScaffold]. Apply it
 * as the content padding at the foot of a scrolling surface so the last item is never occluded. The value
 * already accounts for the bar's placement - a screen never branches on docked-vs-overlay itself. Resolves
 * to `0.dp` outside a [BottomBarScaffold], where there is no bar to clear.
 */
@Composable
fun rememberBottomBarPadding(): Dp = LocalBottomBarPadding.current
