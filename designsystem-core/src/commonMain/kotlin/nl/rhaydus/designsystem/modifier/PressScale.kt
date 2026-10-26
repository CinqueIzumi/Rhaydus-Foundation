package nl.rhaydus.designsystem.modifier

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import nl.rhaydus.designsystem.haptics.LocalHaptics

private const val PRESS_SCALE_TARGET = 0.97f

/**
 * Scales the receiver down to ~0.97 while the [interactionSource] reports a press, easing back to 1f on
 * release with the motion scheme's fast spatial spec. It carries no `playDecorativeMotion` gate: the
 * reduced-motion scheme already snaps.
 *
 * Only renders the feedback and attaches no click listener. For ripple-less surfaces prefer
 * [pressScaleClickable], [pressScaleSelectable] or [pressScaleToggleable], which bundle it.
 */
@Composable
fun Modifier.pressScale(interactionSource: InteractionSource): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) PRESS_SCALE_TARGET else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "pressScale",
    )

    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Ripple-less clickable that bundles press scale, [hoverHighlight] and the hand cursor. A null [hoverShape]
 * paints no hover wash; a disabled surface gets none of the feedback. Plays no haptic: [onClick] fires its
 * own. Pass a shared [interactionSource] to drive other feedback from the same press.
 */
@Composable
fun Modifier.pressScaleClickable(
    onClick: () -> Unit,
    role: Role? = Role.Button,
    enabled: Boolean = true,
    hoverShape: Shape? = RectangleShape,
    interactionSource: MutableInteractionSource? = null,
): Modifier {
    val source = interactionSource ?: remember { MutableInteractionSource() }

    return this
        .pressScaleFeedback(
            interactionSource = source,
            enabled = enabled,
            hoverShape = hoverShape,
        )
        .clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick,
        )
}

/**
 * [pressScaleClickable] with a long-press handler. [onLongClick] fires its own haptic: this modifier plays
 * none, so callers pick the right semantic.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.pressScaleCombinedClickable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    role: Role? = Role.Button,
    enabled: Boolean = true,
    hoverShape: Shape? = RectangleShape,
    interactionSource: MutableInteractionSource? = null,
): Modifier {
    val source = interactionSource ?: remember { MutableInteractionSource() }

    return this
        .pressScaleFeedback(
            interactionSource = source,
            enabled = enabled,
            hoverShape = hoverShape,
        )
        .combinedClickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick,
            onLongClick = onLongClick,
        )
}

/** Ripple-less selectable with [pressScaleClickable]'s feedback; selecting a new item plays the select haptic. */
@Composable
fun Modifier.pressScaleSelectable(
    selected: Boolean,
    onClick: () -> Unit,
    role: Role? = null,
    enabled: Boolean = true,
    hoverShape: Shape? = RectangleShape,
    interactionSource: MutableInteractionSource? = null,
): Modifier {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val haptics = LocalHaptics.current

    return this
        .pressScaleFeedback(
            interactionSource = source,
            enabled = enabled,
            hoverShape = hoverShape,
        )
        .selectable(
            selected = selected,
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = {
                if (selected.not()) haptics.select()
                onClick()
            },
        )
}

/** Ripple-less toggleable with [pressScaleClickable]'s feedback; every change plays the select haptic. */
@Composable
fun Modifier.pressScaleToggleable(
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    role: Role? = null,
    enabled: Boolean = true,
    hoverShape: Shape? = RectangleShape,
    interactionSource: MutableInteractionSource? = null,
): Modifier {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val haptics = LocalHaptics.current

    return this
        .pressScaleFeedback(
            interactionSource = source,
            enabled = enabled,
            hoverShape = hoverShape,
        )
        .toggleable(
            value = value,
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = role,
            onValueChange = { next ->
                haptics.select()
                onValueChange(next)
            },
        )
}

@Composable
private fun Modifier.pressScaleFeedback(
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
    hoverShape: Shape?,
): Modifier = if (enabled) {
    this
        .pointerHandCursor()
        .pressScale(interactionSource)
        .let { base ->
            if (hoverShape == null) {
                base
            } else {
                base.hoverHighlight(
                    interactionSource = interactionSource,
                    shape = hoverShape,
                )
            }
        }
} else {
    this
}
