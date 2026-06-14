package nl.rhaydus.designsystem.haptics

import androidx.compose.runtime.Composable

// Desktop has no haptic hardware, so the whole vocabulary maps to the shared no-op.
@Composable
actual fun rememberHaptics(): Haptics = NoOpHaptics
