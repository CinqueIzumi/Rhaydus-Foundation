package nl.rhaydus.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable

/**
 * Brand-agnostic Material 3 Expressive theme scaffold. The app supplies its own [colorScheme] and
 * [typography] (and decides whether to derive [colorScheme] from dynamic color); this scaffold only
 * wires the expressive theme + motion and provides nothing brand-specific.
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
    content: @Composable () -> Unit,
) {
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = typography,
        motionScheme = motionScheme,
        content = content,
    )
}
