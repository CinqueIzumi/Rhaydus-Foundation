package nl.rhaydus.designsystem.model

import nl.rhaydus.designsystem.icon.RhaydusIconResource

/** One entry in a dropdown menu (e.g. a `RhaydusSplitButton`'s). [onClick] runs when the user picks it. */
class RhaydusMenuItem(
    val label: String,
    val onClick: () -> Unit,
    val icon: RhaydusIconResource,
)
