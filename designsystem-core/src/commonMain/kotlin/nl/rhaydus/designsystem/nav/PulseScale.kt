package nl.rhaydus.designsystem.nav

import androidx.compose.animation.core.Animatable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import nl.rhaydus.designsystem.motion.playDecorativeMotion

private const val DEFAULT_PULSE_PEAK_SCALE = 1.22f

/**
 * A scale that plays one pulse to [peakScale] and back each time [trigger] changes after the first
 * composition. Apply it as a `graphicsLayer` scale. Peak and settle come from the motion scheme's spatial
 * specs; under reduced motion it stays a flat `1f`.
 */
@Composable
fun rememberPulseScale(
    trigger: Int,
    peakScale: Float = DEFAULT_PULSE_PEAK_SCALE,
): Float = rememberPulseScaleProvider(
    trigger = trigger,
    peakScale = peakScale,
)()

/** Same pulse as the [trigger] overload of [rememberPulseScale], read through a lambda so a `graphicsLayer {}` block tracks it without recomposing. */
@Composable
fun rememberPulseScaleProvider(
    trigger: Int,
    peakScale: Float = DEFAULT_PULSE_PEAK_SCALE,
): () -> Float {
    val playMotion = playDecorativeMotion()
    val handledTrigger = remember { mutableIntStateOf(trigger) }
    val scale = remember { Animatable(initialValue = 1f) }
    val peakSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val settleSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()

    LaunchedEffect(trigger, playMotion) {
        if (trigger == handledTrigger.intValue) return@LaunchedEffect

        handledTrigger.intValue = trigger

        if (playMotion.not()) return@LaunchedEffect

        scale.snapTo(targetValue = 1f)
        scale.animateTo(
            targetValue = peakScale,
            animationSpec = peakSpec,
        )
        scale.animateTo(
            targetValue = 1f,
            animationSpec = settleSpec,
        )
    }

    return { scale.value }
}

/**
 * Pulses each time [pulse] fires for [key]. Rapid repeated pulses while one is mid-flight coalesce into a
 * single follow-up animation.
 */
@Composable
fun rememberPulseScale(
    pulse: NavPulse,
    key: NavPulseKey,
    peakScale: Float = DEFAULT_PULSE_PEAK_SCALE,
): Float = key(key) {
    rememberPulseScale(
        trigger = pulse.countFor(key),
        peakScale = peakScale,
    )
}
