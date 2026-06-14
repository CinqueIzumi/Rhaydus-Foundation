package nl.rhaydus.ui.common

import java.text.NumberFormat

actual fun formatGroupedNumber(value: Int): String = NumberFormat.getIntegerInstance().format(value.toLong())

actual fun formatDecimalNumber(
    value: Double,
    fractionDigits: Int,
): String = NumberFormat.getNumberInstance().apply {
    minimumFractionDigits = fractionDigits
    maximumFractionDigits = fractionDigits
}.format(value)
