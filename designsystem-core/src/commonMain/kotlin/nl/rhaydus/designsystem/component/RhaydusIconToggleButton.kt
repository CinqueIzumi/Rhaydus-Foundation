package nl.rhaydus.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import nl.rhaydus.designsystem.icon.RhaydusIconResource
import nl.rhaydus.designsystem.model.ButtonSize
import nl.rhaydus.designsystem.model.IconToggleButtonShape
import nl.rhaydus.designsystem.model.IconToggleButtonStyle

/**
 * An icon-only toggle button across the Material 3 Expressive [IconToggleButtonStyle]s and [ButtonSize]s.
 * [shape] picks whether the idle state is round or square; the toggle morphs to the opposite shape when
 * checked, and to the size's pressed shape while pressed.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RhaydusIconToggleButton(
    checked: Boolean,
    icon: RhaydusIconResource,
    style: IconToggleButtonStyle,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: IconToggleButtonShape = IconToggleButtonShape.ROUND,
    size: ButtonSize = ButtonSize.S,
) {
    val content: @Composable () -> Unit = {
        Icon(
            painter = icon.getIconPainter(),
            contentDescription = icon.contentDescription,
            modifier = Modifier.size(size.iconSize),
        )
    }

    val roundShapeDefault = shape == IconToggleButtonShape.ROUND

    val shapes = when (size) {
        ButtonSize.XS -> {
            val roundShape = IconButtonDefaults.extraSmallRoundShape
            val squareShape = IconButtonDefaults.extraSmallSquareShape

            IconButtonDefaults.toggleableShapes(
                shape = if (roundShapeDefault) roundShape else squareShape,
                checkedShape = if (roundShapeDefault) squareShape else roundShape,
                pressedShape = IconButtonDefaults.extraSmallPressedShape,
            )
        }

        ButtonSize.S -> {
            val roundShape = IconButtonDefaults.smallRoundShape
            val squareShape = IconButtonDefaults.smallSquareShape

            IconButtonDefaults.toggleableShapes(
                shape = if (roundShapeDefault) roundShape else squareShape,
                checkedShape = if (roundShapeDefault) squareShape else roundShape,
                pressedShape = IconButtonDefaults.smallPressedShape,
            )
        }

        ButtonSize.M -> {
            val roundShape = IconButtonDefaults.mediumRoundShape
            val squareShape = IconButtonDefaults.mediumSquareShape

            IconButtonDefaults.toggleableShapes(
                shape = if (roundShapeDefault) roundShape else squareShape,
                checkedShape = if (roundShapeDefault) squareShape else roundShape,
                pressedShape = IconButtonDefaults.mediumPressedShape,
            )
        }

        ButtonSize.L -> {
            val roundShape = IconButtonDefaults.largeRoundShape
            val squareShape = IconButtonDefaults.largeSquareShape

            IconButtonDefaults.toggleableShapes(
                shape = if (roundShapeDefault) roundShape else squareShape,
                checkedShape = if (roundShapeDefault) squareShape else roundShape,
                pressedShape = IconButtonDefaults.largePressedShape,
            )
        }

        ButtonSize.XL -> {
            val roundShape = IconButtonDefaults.extraLargeRoundShape
            val squareShape = IconButtonDefaults.extraLargeSquareShape

            IconButtonDefaults.toggleableShapes(
                shape = if (roundShapeDefault) roundShape else squareShape,
                checkedShape = if (roundShapeDefault) squareShape else roundShape,
                pressedShape = IconButtonDefaults.extraLargePressedShape,
            )
        }
    }

    when (style) {
        IconToggleButtonStyle.FILLED -> FilledIconToggleButton(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier.size(size.height),
            content = content,
            shapes = shapes,
            enabled = enabled,
        )

        IconToggleButtonStyle.TONAL -> FilledTonalIconToggleButton(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier.size(size.height),
            content = content,
            shapes = shapes,
            enabled = enabled,
        )

        IconToggleButtonStyle.OUTLINED -> OutlinedIconToggleButton(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier.size(size.height),
            content = content,
            shapes = shapes,
            enabled = enabled,
        )
    }
}
