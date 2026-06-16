package nl.rhaydus.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import nl.rhaydus.designsystem.layout.WindowWidthClass
import nl.rhaydus.designsystem.layout.rememberWindowSizeClass
import nl.rhaydus.designsystem.model.ModalSheetForm

private val DesktopPanelWidth = 420.dp
private val DesktopPanelCorner = 28.dp
private const val DESKTOP_PANEL_HEIGHT_FRACTION = 0.9f

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
 * A modal surface that adapts to the window width (design-system-foundations.md section 5.9): a
 * bottom sheet on compact and medium widths, and a centered, bordered panel on expanded (desktop)
 * windows - where a sheet welded to the bottom edge under a tall scrim reads as the phone-derived
 * layout a desktop redesign retires.
 *
 * The [content] slot is identical to [ModalBottomSheet]'s - a [ColumnScope] body that owns its own
 * padding and scroll - so a call site swaps `ModalBottomSheet` for `AdaptiveModalSheet` without
 * touching the body. Both forms open on `surfaceContainerLowest` and lead with the canonical section
 * header (section 5.6); neither carries a close button. An in-content dismiss button reaches the
 * right close behaviour through [LocalModalSheetDismiss] rather than its own sheet state.
 */
@Composable
fun AdaptiveModalSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    dismissOnTapOutside: Boolean = true,
    dismissOnBackPress: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val windowSize = rememberWindowSizeClass()

    if (windowSize.widthClass == WindowWidthClass.EXPANDED) {
        DesktopSheetPanel(
            onDismissRequest = onDismissRequest,
            maxHeight = windowSize.heightDp * DESKTOP_PANEL_HEIGHT_FRACTION,
            dismissOnTapOutside = dismissOnTapOutside,
            dismissOnBackPress = dismissOnBackPress,
            modifier = modifier,
            content = content,
        )
    } else {
        BottomSheetForm(
            onDismissRequest = onDismissRequest,
            dismissOnTapOutside = dismissOnTapOutside,
            dismissOnBackPress = dismissOnBackPress,
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
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    val dismiss: () -> Unit = remember(
        sheetState,
        coroutineScope,
        onDismissRequest,
    ) {
        {
            coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = dismissOnBackPress,
            shouldDismissOnClickOutside = dismissOnTapOutside,
        ),
    ) {
        val columnScope = this

        CompositionLocalProvider(
            LocalModalSheetForm provides ModalSheetForm.SHEET,
            LocalModalSheetDismiss provides dismiss,
        ) {
            with(columnScope) { content() }
        }
    }
}

@Composable
private fun DesktopSheetPanel(
    onDismissRequest: () -> Unit,
    maxHeight: Dp,
    dismissOnTapOutside: Boolean,
    dismissOnBackPress: Boolean,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnTapOutside,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            modifier = modifier
                .widthIn(max = DesktopPanelWidth)
                .fillMaxWidth()
                .heightIn(max = maxHeight),
            shape = RoundedCornerShape(size = DesktopPanelCorner),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                val columnScope = this

                CompositionLocalProvider(
                    LocalModalSheetForm provides ModalSheetForm.PANEL,
                    LocalModalSheetDismiss provides onDismissRequest,
                ) {
                    with(columnScope) { content() }
                }
            }
        }
    }
}
