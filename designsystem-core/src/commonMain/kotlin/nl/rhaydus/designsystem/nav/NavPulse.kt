package nl.rhaydus.designsystem.nav

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf

/**
 * A process-wide, non-persistent one-shot signal for cross-tab nav pulses: fire [pulse] for a key when
 * an event lands on a *different* tab (e.g. a book added from the Reading tab lands on the Library
 * shelf), and the observing nav chrome briefly pulses that tab's icon.
 *
 * Keyed and instance-owned, so the concrete signal name stays app-side - the app holds one [NavPulse]
 * (a DI singleton), keeps its own [NavPulseKey] constant (a tab object, an enum), and
 * emits `navPulse.pulse(LibraryKey)` from wherever the cross-tab event originates. The foundation ships
 * the mechanism; the app owns the instance and the key vocabulary.
 *
 * Backed by snapshot state, so [pulse] is safe to call from outside composition (controllers, use
 * cases) and [countFor] recomposes its observers on each pulse. Observe it with [rememberPulseScale].
 */
@Stable
class NavPulse {
    private val counts = mutableStateMapOf<NavPulseKey, Int>()

    /**
     * Fire a one-shot pulse for [key]. Safe to call from outside composition. The increment is not atomic
     * across threads, but a lost increment is harmless: observers react to the count *changing*, not to its
     * magnitude.
     */
    fun pulse(key: NavPulseKey) {
        counts[key] = (counts[key] ?: 0) + 1
    }

    /** The pulse count for [key], read as snapshot state so observers recompose on each pulse. */
    fun countFor(key: NavPulseKey): Int = counts[key] ?: 0
}
