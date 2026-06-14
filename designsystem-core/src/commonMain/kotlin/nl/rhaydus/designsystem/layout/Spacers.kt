package nl.rhaydus.designsystem.layout

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@Composable
fun BottomNavigationSpacer() {
    Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
}

@Composable
fun rememberBottomBarPadding(): Dp {
    val currentBottomBarHeight = LocalBottomBarPadding.current

    return remember(currentBottomBarHeight) { currentBottomBarHeight }
}
