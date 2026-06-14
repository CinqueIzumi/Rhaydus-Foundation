package nl.rhaydus.ui.common

import java.text.NumberFormat
import java.util.Locale

actual fun formatGroupedNumber(value: Int): String =
    NumberFormat.getIntegerInstance(Locale.getDefault()).format(value)

actual fun formatDecimalNumber(
    value: Double,
    fractionDigits: Int,
): String =
    NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = fractionDigits
        maximumFractionDigits = fractionDigits
    }.format(value)
