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
 * The bottom-bar footprint an overlay bar reserves, provided by [BottomBarScaffold]. Read this as the
 * trailing content padding of scrolling content so the last item is never occluded. Resolves to `0.dp`
 * outside a [BottomBarScaffold] (e.g. under a docked bar, where the Material `Scaffold` innerPadding
 * reserves the bar instead).
 */
@Composable
fun rememberBottomBarPadding(): Dp = LocalBottomBarPadding.current
