package nl.rhaydus.designsystem.component

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.runtime.Composable

@Composable
actual fun DesktopContextMenu(
    items: List<DesktopContextMenuItem>,
    content: @Composable () -> Unit,
) {
    if (items.isEmpty()) {
        content()
        return
    }

    ContextMenuArea(
        items = {
            items.map { item ->
                ContextMenuItem(
                    item.label,
                    item.onClick,
                )
            }
        },
        content = content,
    )
}
