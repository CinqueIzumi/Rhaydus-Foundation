package nl.rhaydus.designsystem.component

/** One entry in a [DesktopContextMenu]. [onClick] runs when the user picks the item. */
data class DesktopContextMenuItem(
    val label: String,
    val onClick: () -> Unit,
)
