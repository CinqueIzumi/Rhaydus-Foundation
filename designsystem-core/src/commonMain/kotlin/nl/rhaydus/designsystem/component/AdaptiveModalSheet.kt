package nl.rhaydus.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import nl.rhaydus.designsystem.layout.WindowWidthClass
import nl.rhaydus.designsystem.layout.rememberWindowSizeClass
import nl.rhaydus.designsystem.model.ModalSheetForm

private const val DESKTOP_PANEL_HEIGHT_FRACTION = 0.9f
private const val PANEL_ENTER_SCALE = 0.92f
private const val PANEL_SCRIM_ALPHA = 0.32f

/**
 * The [ModalSheetForm] the nearest [AdaptiveModalSheet] resolved to, so a body can adjust its
 * density to the surface it landed in. Defaults to [ModalSheetForm.SHEET] outside an adaptive sheet.
 */
val LocalModalSheetForm = staticCompositionLocalOf { ModalSheetForm.SHEET }

/**
 * Dismisses the nearest [AdaptiveModalSheet] the way that form should close: the bottom sheet
 * animates down first, the desktop panel closes immediately (it has no slide to play). A body calls
 * this from an in-content dismiss action (e.g. an "are you sure?" choice) so it never has to drive
 * the sheet state itself. Defaults to a no-op outside an adaptive sheet.
 */
val LocalModalSheetDismiss = staticCompositionLocalOf<() -> Unit> { {} }

/**
 * True from the first close path (outside tap, back, swipe, [LocalModalSheetDismiss]) until the nearest
 * [AdaptiveModalSheet] is gone, so a body can stop accepting input while the exit plays. False outside an
 * adaptive sheet.
 */
val LocalModalSheetClosing = staticCompositionLocalOf { false }

/**
 * A modal surface that adapts to the window width (design-system-foundations.md section 5.9): a
 * bottom sheet on compact and medium widths, and a centered, elevated panel on expanded (desktop)
 * windows - where a sheet welded to the bottom edge under a tall scrim reads as the phone-derived
 * layout a desktop redesign retires.
 *
 * The [content] slot is identical to [ModalBottomSheet]'s - a [ColumnScope] body that owns its own
 * padding and scroll - so a call site swaps `ModalBottomSheet` for `AdaptiveModalSheet` without
 * touching the body. Both forms open on `surfaceContainerLowest` and lead with the canonical section
 * header (section 5.6); neither carries a close button. An in-content dismiss button reaches the
 * right close behaviour through [LocalModalSheetDismiss] rather than its own sheet state.
 *
 * The expanded panel scales and fades in over a fading scrim and plays the same exit before
 * [onDismissRequest], whichever way it closes. [panelShape] shapes it. [overlay] is composed above the
 * content inside the sheet window in both forms, so a host can place a snackbar there.
 *
 * [onDismissRequest] is sent at most once. Swiping the bottom sheet away follows [dismissOnTapOutside];
 * a [LocalModalSheetDismiss] hide still plays when both outside tap and swipe are refused. A body reads
 * [LocalModalSheetClosing] to learn the close has started.
 */
@Composable
fun AdaptiveModalSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    dismissOnTapOutside: Boolean = true,
    dismissOnBackPress: Boolean = true,
    panelShape: Shape = RoundedCornerShape(size = 28.dp),
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val windowSize = rememberWindowSizeClass()

    if (windowSize.widthClass == WindowWidthClass.EXPANDED) {
        DesktopSheetPanel(
            onDismissRequest = onDismissRequest,
            maxHeight = windowSize.heightDp * DESKTOP_PANEL_HEIGHT_FRACTION,
            dismissOnTapOutside = dismissOnTapOutside,
            dismissOnBackPress = dismissOnBackPress,
            panelShape = panelShape,
            overlay = overlay,
            modifier = modifier,
            content = content,
        )
    } else {
        BottomSheetForm(
            onDismissRequest = onDismissRequest,
            dismissOnTapOutside = dismissOnTapOutside,
            dismissOnBackPress = dismissOnBackPress,
            overlay = overlay,
            modifier = modifier,
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheetForm(
    onDismissRequest: () -> Unit,
    dismissOnTapOutside: Boolean,
    dismissOnBackPress: Boolean,
    overlay: @Composable BoxScope.() -> Unit,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    var closeRequested by remember { mutableStateOf(false) }
    var dismissSent by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    val closing by remember(sheetState) {
        derivedStateOf {
            val leavingVisible = sheetState.currentValue != SheetValue.Hidden &&
                sheetState.targetValue == SheetValue.Hidden

            closeRequested || dismissSent || leavingVisible
        }
    }

    val notifyDismissed: () -> Unit = remember {
        {
            if (dismissSent.not()) {
                dismissSent = true
                currentOnDismissRequest()
            }
        }
    }

    val dismiss: () -> Unit = remember(sheetState, coroutineScope) {
        {
            if (closeRequested.not()) {
                closeRequested = true

                coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { notifyDismissed() }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = notifyDismissed,
        modifier = modifier,
        sheetState = sheetState,
        sheetGesturesEnabled = dismissOnTapOutside,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = dismissOnBackPress,
            shouldDismissOnClickOutside = dismissOnTapOutside,
        ),
    ) {
        CompositionLocalProvider(
            LocalModalSheetForm provides ModalSheetForm.SHEET,
            LocalModalSheetDismiss provides dismiss,
            LocalModalSheetClosing provides closing,
        ) {
            Box {
                Column(content = content)

                overlay()
            }
        }
    }
}

@Composable
private fun DesktopSheetPanel(
    onDismissRequest: () -> Unit,
    maxHeight: Dp,
    dismissOnTapOutside: Boolean,
    dismissOnBackPress: Boolean,
    panelShape: Shape,
    overlay: @Composable BoxScope.() -> Unit,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    var closing by remember { mutableStateOf(false) }
    val scale = remember { Animatable(initialValue = PANEL_ENTER_SCALE) }
    val alpha = remember { Animatable(initialValue = 0f) }
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = PANEL_SCRIM_ALPHA)
    val close: () -> Unit = remember { { closing = true } }

    LaunchedEffect(closing) {
        coroutineScope {
            launch {
                scale.animateTo(
                    targetValue = if (closing) PANEL_ENTER_SCALE else 1f,
                    animationSpec = spatialSpec,
                )
            }

            launch {
                alpha.animateTo(
                    targetValue = if (closing) 0f else 1f,
                    animationSpec = effectsSpec,
                )
            }
        }

        if (closing) currentOnDismissRequest()
    }

    ScrimlessDialog(
        onDismissRequest = close,
        dismissOnBackPress = dismissOnBackPress,
    ) {
        CompositionLocalProvider(
            LocalModalSheetForm provides ModalSheetForm.PANEL,
            LocalModalSheetDismiss provides close,
            LocalModalSheetClosing provides closing,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { this.alpha = alpha.value }
                        .background(scrimColor)
                        .pointerInput(dismissOnTapOutside) {
                            if (dismissOnTapOutside) detectTapGestures { close() }
                        },
                )

                Box(
                    modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier = modifier
                            .graphicsLayer {
                                scaleX = scale.value
                                scaleY = scale.value
                                this.alpha = alpha.value
                            }
                            .widthIn(max = 420.dp)
                            .fillMaxWidth()
                            .heightIn(max = maxHeight),
                        shape = panelShape,
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        shadowElevation = 6.dp,
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 16.dp),
                            content = content,
                        )
                    }
                }

                overlay()
            }
        }
    }
}
