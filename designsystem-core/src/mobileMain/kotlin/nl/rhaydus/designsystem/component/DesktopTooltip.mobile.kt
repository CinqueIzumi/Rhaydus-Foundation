package nl.rhaydus.designsystem.component

import androidx.compose.runtime.Composable

@Composable
actual fun DesktopTooltip(
    text: String,
    content: @Composable () -> Unit,
) {
    content()
}
