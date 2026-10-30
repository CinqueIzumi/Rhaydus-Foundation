package nl.rhaydus.designsystem.component

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalDate
import nl.rhaydus.designsystem.model.ButtonStyle

/**
 * The calendar dialog for picking a single day. It reports a [LocalDate] and owns the Material 3
 * UTC-millis contract internally (see `PickerDates.kt`), so a caller never converts an epoch
 * timestamp itself and cannot reintroduce the timezone off-by-one that shifted a picked day.
 *
 * The calendar opens on [initialDate], which the caller supplies so the dialog never reads the clock
 * during composition. Confirming with no selection calls [onDismiss], the same as cancelling.
 *
 * Any "pick a date" surface reaches for this dialog; hand-rolling a `DatePickerDialog` and converting
 * the millis at the call site is the bug it exists to prevent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RhaydusDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    confirmLabel: String,
    dismissLabel: String,
    modifier: Modifier = Modifier,
    selectableDates: SelectableDates = DatePickerDefaults.AllDates,
) {
    val initialMillis = remember(initialDate) { initialDate.toPickerMillis() }

    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        initialDisplayMode = DisplayMode.Picker,
        selectableDates = selectableDates,
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        confirmButton = {
            RhaydusButton(
                label = confirmLabel,
                style = ButtonStyle.TEXT,
                onClick = {
                    val millis = pickerState.selectedDateMillis

                    if (millis != null) {
                        onConfirm(millis.toPickerLocalDate())
                    } else {
                        onDismiss()
                    }
                },
            )
        },
        dismissButton = {
            RhaydusButton(
                label = dismissLabel,
                style = ButtonStyle.TEXT,
                onClick = onDismiss,
            )
        },
    ) {
        DatePicker(state = pickerState)
    }
}
