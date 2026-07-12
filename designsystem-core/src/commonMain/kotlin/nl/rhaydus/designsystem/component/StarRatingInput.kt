package nl.rhaydus.designsystem.component

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import nl.rhaydus.designsystem.haptics.rememberHaptics
import nl.rhaydus.designsystem.icon.RhaydusIconResource

/**
 * The rating value a horizontal offset [x] maps to, in half-star steps. The row is a run of
 * [starCount] stars each [starSizePx] wide separated by [spacingPx]; the offset falls in one star's
 * slot, and its left half resolves to `.5` while its right half resolves to the whole star. The
 * result is clamped to `[0.5, starCount]` so a rating is never zero and never overshoots the row.
 *
 * Extracted from the gesture handler as a pure function so the half-star math is unit-testable
 * without a Compose harness.
 */
internal fun ratingForOffsetX(
    x: Float,
    starSizePx: Float,
    spacingPx: Float,
    starCount: Int,
): Double {
    val slot = starSizePx + spacingPx
    val index = (x / slot).toInt().coerceIn(
        0,
        starCount - 1,
    )
    val localX = x - index * slot
    val isHalf = localX < starSizePx / 2f

    return (index + if (isHalf) 0.5 else 1.0).coerceIn(
        0.5,
        starCount.toDouble(),
    )
}

/**
 * Interactive personal-rating control: a row of [starCount] stars the user rates against in
 * half-star steps. Tapping a star sets the rating to that point (left half → `.5`, right half →
 * whole); dragging horizontally scrubs through the range, firing `Haptics.tickle()` on every
 * half-step crossing. The committed value is reported through [onRatingChange] on tap or on
 * drag release — never mid-drag — so the caller's write path runs once per gesture.
 *
 * [rating] is the source-of-truth value (e.g. the persisted personal rating); a live drag
 * preview overrides it visually until the committed value lands back in [rating].
 *
 * The component is brand-agnostic: [starIcon] supplies the glyph (the app's own star), [filledColor]
 * and [emptyColor] tint the filled and empty portions (defaulting to Material theme roles), and
 * [contentDescription] overrides the generated accessibility label when a localized string is wanted.
 *
 * Accessibility lives at the row level: the [Row] carries a single merged [contentDescription], and each
 * star [Icon] is marked decorative (`contentDescription = null`) so the control reads as one rating
 * value rather than N unlabeled stars. [starIcon]'s own `contentDescription` is therefore unused here.
 */
@Composable
fun StarRatingInput(
    rating: Double?,
    onRatingChange: (Double) -> Unit,
    starIcon: RhaydusIconResource,
    modifier: Modifier = Modifier,
    filledColor: Color = MaterialTheme.colorScheme.primary,
    emptyColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f),
    starCount: Int = 5,
    starSize: Dp = 32.dp,
    spacing: Dp = 6.dp,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    val haptics = rememberHaptics()

    val density = LocalDensity.current
    val starSizePx = with(density) { starSize.toPx() }
    val spacingPx = with(density) { spacing.toPx() }

    val ratingForX: (Float) -> Double = remember(starSizePx, spacingPx, starCount) {
        { x ->
            ratingForOffsetX(
                x = x,
                starSizePx = starSizePx,
                spacingPx = spacingPx,
                starCount = starCount,
            )
        }
    }

    var previewRating by remember { mutableStateOf<Double?>(null) }

    // Once a committed value lands back in [rating], drop the drag preview so the source value —
    // including any server-side normalisation — becomes authoritative again. A drag never mutates
    // [rating] mid-gesture, so the live preview survives the drag and only yields on commit.
    LaunchedEffect(rating) {
        previewRating = null
    }

    val displayRating = previewRating ?: rating ?: 0.0

    val gestureModifier = if (enabled) {
        Modifier
            .pointerInput(ratingForX) {
                detectTapGestures { offset ->
                    val value = ratingForX(offset.x)

                    haptics.tickle()

                    onRatingChange(value)
                }
            }
            .pointerInput(ratingForX) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val value = ratingForX(offset.x)

                        previewRating = value

                        haptics.tickle()
                    },
                    onDragEnd = { previewRating?.let(onRatingChange) },
                    onDragCancel = { previewRating = null },
                ) { change, _ ->
                    change.consume()

                    val value = ratingForX(change.position.x)

                    if (value != previewRating) {
                        previewRating = value

                        haptics.tickle()
                    }
                }
            }
    } else {
        Modifier
    }

    val label = contentDescription ?: "Your rating: $displayRating out of $starCount stars"

    Row(
        modifier = modifier
            .semantics {
                this.contentDescription = label
            }
            .then(gestureModifier),
        horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
        repeat(starCount) { index ->
            val fill = (displayRating - index).coerceIn(
                0.0,
                1.0,
            ).toFloat()

            Box(modifier = Modifier.size(starSize)) {
                Icon(
                    painter = starIcon.getIconPainter(),
                    contentDescription = null,
                    tint = emptyColor,
                    modifier = Modifier.size(starSize),
                )

                if (fill > 0f) {
                    Icon(
                        painter = starIcon.getIconPainter(),
                        contentDescription = null,
                        tint = filledColor,
                        modifier = Modifier
                            .size(starSize)
                            .drawWithContent {
                                clipRect(right = size.width * fill) {
                                    this@drawWithContent.drawContent()
                                }
                            },
                    )
                }
            }
        }
    }
}
