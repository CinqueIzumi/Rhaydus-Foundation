package nl.rhaydus.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nl.rhaydus.designsystem.icon.RhaydusIconResource
import nl.rhaydus.designsystem.modifier.pointerHandCursor

/**
 * The static back strip a pushed desktop screen carries instead of a scroll-collapsing top bar - a
 * single hand-cursored back `IconButton` in the leading gutter. jvm-only: the mobile counterpart is
 * each app's top bar. The app supplies its own [backIcon] (an arrow glyph from its icon catalog,
 * wrapped in [RhaydusIconResource]) so no icon asset is baked into the shared module; [tooltipText] is
 * the hover label shown on desktop.
 */
@Composable
fun DesktopBackStrip(
    onNavigateBack: () -> Unit,
    backIcon: RhaydusIconResource,
    modifier: Modifier = Modifier,
    tooltipText: String = "Back",
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = 8.dp,
                vertical = 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DesktopTooltip(text = tooltipText) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.pointerHandCursor(),
            ) {
                Icon(
                    painter = backIcon.getIconPainter(),
                    contentDescription = backIcon.contentDescription,
                )
            }
        }
    }
}
