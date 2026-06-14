package nl.rhaydus.ui.common

data class HoursMinutesSeconds(
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
) {
    val totalSeconds: Int
        get() = hours * 3600 + minutes * 60 + seconds
}
