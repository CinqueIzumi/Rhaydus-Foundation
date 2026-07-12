package nl.rhaydus.designsystem.nav

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.drop
import nl.rhaydus.designsystem.motion.playDecorativeMotion

private const val DEFAULT_PULSE_PEAK_SCALE = 1.22f
private const val DEFAULT_PULSE_PEAK_MS = 180
private const val DEFAULT_PULSE_SETTLE_MS = 240

/**
 * A process-wide, non-persistent one-shot signal for cross-tab nav pulses: fire [pulse] for a key when
 * an event lands on a *different* tab (e.g. a book added from the Reading tab lands on the Library
 * shelf), and the observing nav chrome briefly pulses that tab's icon.
 *
 * Keyed and instance-owned, so the concrete signal name stays app-side - the app holds one [NavPulse]
 * (a DI singleton), keeps its own key constant (a tab object, an enum, a `"library"` string), and
 * emits `navPulse.pulse(LibraryTab)` from wherever the cross-tab event originates. The foundation ships
 * the mechanism; the app owns the instance and the key vocabulary.
 *
 * Backed by snapshot state, so [pulse] is safe to call from outside composition (controllers, use
 * cases) and [countFor] recomposes its observers on each pulse. Observe it with [rememberPulseScale].
 */
@Stable
class NavPulse {
    private val counts = mutableStateMapOf<Any, Int>()

    /**
     * Fire a one-shot pulse for [key]. Safe to call from outside composition. The increment is not atomic
     * across threads, but a lost increment is harmless: observers react to the count *changing*, not to its
     * magnitude.
     */
    fun pulse(key: Any) {
        counts[key] = (counts[key] ?: 0) + 1
    }

    /** The pulse count for [key], read as snapshot state so observers recompose on each pulse. */
    fun countFor(key: Any): Int = counts[key] ?: 0
}

/**
 * An animated icon scale that plays a single scale pulse each time [pulse] fires for [key]: a quick
 * scale up to [peakScale] over [peakMillis] then a settle back to `1f` over [settleMillis]. Apply the
 * returned value as a `graphicsLayer` scale on the tab's icon.
 *
 * Only [key] is caller-supplied, so the concrete tab identity (`LibraryTab`, etc.) stays app-side.
 * Gated by [playDecorativeMotion]: when the user has asked to reduce motion this stays a flat `1f` and
 * never collects. The pulse is purely visual - pair a haptic (`rememberHaptics()`) at the emit site if
 * an app wants one, rather than baking it into a decorative scale.
 *
 * Rapid repeated pulses for the same key while one is mid-flight coalesce into a single follow-up animation
 * (the `snapshotFlow` conflates), which is the intended behaviour for a decorative indicator.
 */
@Composable
fun rememberPulseScale(
    pulse: NavPulse,
    key: Any,
    peakScale: Float = DEFAULT_PULSE_PEAK_SCALE,
    peakMillis: Int = DEFAULT_PULSE_PEAK_MS,
    settleMillis: Int = DEFAULT_PULSE_SETTLE_MS,
): Float {
    val playMotion = playDecorativeMotion()

    val scale = remember { Animatable(1f) }

    LaunchedEffect(pulse, key, playMotion) {
        if (playMotion.not()) return@LaunchedEffect

        snapshotFlow { pulse.countFor(key) }
            .drop(1)
            .collect {
                scale.snapTo(1f)
                scale.animateTo(
                    targetValue = peakScale,
                    animationSpec = tween(durationMillis = peakMillis),
                )
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = settleMillis),
                )
            }
    }

    return scale.value
}
