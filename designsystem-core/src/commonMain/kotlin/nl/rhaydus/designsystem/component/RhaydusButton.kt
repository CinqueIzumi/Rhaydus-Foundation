package nl.rhaydus.designsystem.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import nl.rhaydus.designsystem.icon.RhaydusIconResource
import nl.rhaydus.designsystem.model.ButtonSize
import nl.rhaydus.designsystem.model.ButtonStyle
import nl.rhaydus.designsystem.modifier.pressScale

/**
 * The text button across all Material 3 Expressive [ButtonStyle]s and [ButtonSize]s, with an optional
 * leading [icon] (suppressed for [ButtonStyle.TEXT], which leads with its label). The filled style adds
 * the design system's press-scale feedback; the other styles keep their stock ripple and shape morph.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RhaydusButton(
    label: String,
    style: ButtonStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: RhaydusIconResource? = null,
    size: ButtonSize = ButtonSize.S,
) {
    val shapes = ButtonDefaults.shapesFor(buttonHeight = size.height)

    val content: @Composable RowScope.() -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null && style != ButtonStyle.TEXT) {
                Icon(
                    painter = icon.getIconPainter(),
                    contentDescription = icon.contentDescription,
                    modifier = Modifier.size(size.iconSize),
                )

                Spacer(modifier = Modifier.width(size.iconSpacing))
            }

            Text(
                text = label,
                style = size.textStyle,
            )
        }
    }

    when (style) {
        ButtonStyle.FILLED -> {
            val filledInteractionSource = remember { MutableInteractionSource() }

            Button(
                onClick = onClick,
                shapes = shapes,
                enabled = enabled,
                contentPadding = size.contentPadding,
                modifier = modifier
                    .height(height = size.height)
                    .pressScale(filledInteractionSource),
                content = content,
                interactionSource = filledInteractionSource,
            )
        }

        ButtonStyle.TONAL -> FilledTonalButton(
            onClick = onClick,
            shapes = shapes,
            enabled = enabled,
            contentPadding = size.contentPadding,
            modifier = modifier.height(height = size.height),
            content = content,
        )

        ButtonStyle.ELEVATED -> ElevatedButton(
            onClick = onClick,
            shapes = shapes,
            enabled = enabled,
            contentPadding = size.contentPadding,
            modifier = modifier.height(height = size.height),
            content = content,
        )

        ButtonStyle.OUTLINED -> OutlinedButton(
            onClick = onClick,
            shapes = shapes,
            enabled = enabled,
            contentPadding = size.contentPadding,
            modifier = modifier.height(height = size.height),
            content = content,
        )

        ButtonStyle.TEXT -> TextButton(
            onClick = onClick,
            shapes = shapes,
            enabled = enabled,
            modifier = modifier.height(height = size.height),
            content = content,
        )
    }
}
