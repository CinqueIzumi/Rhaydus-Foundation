package nl.rhaydus.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import nl.rhaydus.designsystem.haptics.LocalHaptics
import nl.rhaydus.designsystem.haptics.rememberHaptics
import nl.rhaydus.designsystem.motion.SnappingMotionScheme
import nl.rhaydus.designsystem.motion.playDecorativeMotion

/**
 * Brand-agnostic Material 3 Expressive theme scaffold. The app supplies its own [colorScheme] and
 * [typography] (and decides whether to derive [colorScheme] from dynamic color); this scaffold only
 * wires the expressive theme + motion and provides nothing brand-specific.
 *
 * [motionScheme] is the full-motion scheme; under reduced motion it is replaced by one whose specs all snap.
 * [shapes] is the Material shape scale; the default is Material's own. Provides [LocalHaptics] with the
 * platform haptics and [Motion] ([Motion.Full], or [Motion.Reduced] under reduced motion), read through
 * [MaterialTheme.motion].
 *
 * App-specific custom typography (a reader/editorial scale) is provided by the app inside [content]
 * via its own CompositionLocal + `MaterialTheme.<x>Typography` extension - see
 * `docs/design-system-foundations.md` for that pattern. The shared editorial role vocabulary that
 * foundation editorial components consume lives in the opt-in `designsystem-editorial` module, not here.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RhaydusTheme(
    colorScheme: ColorScheme,
    typography: Typography,
    motionScheme: MotionScheme = MotionScheme.expressive(),
    shapes: Shapes = MaterialTheme.shapes,
    content: @Composable () -> Unit,
) {
    val playMotion = playDecorativeMotion()

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = typography,
        motionScheme = if (playMotion) motionScheme else SnappingMotionScheme,
        shapes = shapes,
    ) {
        CompositionLocalProvider(
            LocalHaptics provides rememberHaptics(),
            LocalMotion provides if (playMotion) Motion.Full else Motion.Reduced,
            content = content,
        )
    }
}
