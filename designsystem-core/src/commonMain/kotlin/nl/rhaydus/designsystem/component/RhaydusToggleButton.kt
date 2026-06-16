package nl.rhaydus.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ElevatedToggleButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TonalToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import nl.rhaydus.designsystem.model.ButtonSize
import nl.rhaydus.designsystem.model.ToggleButtonStyle

/**
 * A labelled toggle button across the Material 3 Expressive [ToggleButtonStyle]s and [ButtonSize]s. The
 * shape morphs between unchecked and checked via the Material toggle shapes for the button height.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RhaydusToggleButton(
    checked: Boolean,
    label: String,
    style: ToggleButtonStyle,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: ButtonSize = ButtonSize.S,
) {
    val shapes = ToggleButtonDefaults.shapesFor(buttonHeight = size.height)

    val content: @Composable RowScope.() -> Unit = {
        Text(
            text = label,
            style = size.textStyle,
        )
    }

    when (style) {
        ToggleButtonStyle.FILLED -> ToggleButton(
            checked = checked,
            onCheckedChange = onCheckedChange,
            shapes = shapes,
            enabled = enabled,
            contentPadding = size.contentPadding,
            modifier = modifier.height(height = size.height),
            content = content,
        )

        ToggleButtonStyle.TONAL -> TonalToggleButton(
            checked = checked,
            onCheckedChange = onCheckedChange,
            shapes = shapes,
            enabled = enabled,
            contentPadding = size.contentPadding,
            modifier = modifier.height(height = size.height),
            content = content,
        )

        ToggleButtonStyle.ELEVATED -> ElevatedToggleButton(
            checked = checked,
            onCheckedChange = onCheckedChange,
            shapes = shapes,
            enabled = enabled,
            contentPadding = size.contentPadding,
            modifier = modifier.height(height = size.height),
            content = content,
        )

        ToggleButtonStyle.OUTLINED -> OutlinedToggleButton(
            checked = checked,
            onCheckedChange = onCheckedChange,
            shapes = shapes,
            enabled = enabled,
            contentPadding = size.contentPadding,
            modifier = modifier.height(height = size.height),
            content = content,
        )
    }
}
