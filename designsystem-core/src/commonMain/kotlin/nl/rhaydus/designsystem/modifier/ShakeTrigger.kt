package nl.rhaydus.designsystem.modifier

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** A latch for a shake that must run to the end even when whatever asked for it clears first. */
@Stable
class ShakeTrigger internal constructor() {
    var shaking by mutableStateOf(false)
        private set

    fun shake() {
        shaking = true
    }

    internal fun reset() {
        shaking = false
    }
}

@Composable
fun rememberShakeTrigger(): ShakeTrigger = remember { ShakeTrigger() }
