package nl.rhaydus.designsystem.component

import androidx.compose.runtime.Composable

@Composable
actual fun DesktopContextMenu(
    items: List<DesktopContextMenuItem>,
    content: @Composable () -> Unit,
) {
    content()
}
