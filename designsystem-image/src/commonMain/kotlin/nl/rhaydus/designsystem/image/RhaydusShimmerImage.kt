package nl.rhaydus.designsystem.image

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import nl.rhaydus.designsystem.modifier.shimmer
import nl.rhaydus.designsystem.motion.playDecorativeMotion

/**
 * An async image whose loading placeholder is a shimmer. Shimmers while [isLoading] is true with no
 * resolved [model] yet (a host-driven wait) and while Coil fetches the [model]. The shimmer is suppressed
 * under reduced motion (it falls back to a flat box).
 */
@Composable
fun RhaydusShimmerImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    isLoading: Boolean = false,
) {
    RhaydusPlaceholderImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        isLoading = isLoading,
    ) {
        Box(
            modifier = if (playDecorativeMotion()) {
                Modifier.fillMaxSize().shimmer()
            } else {
                Modifier.fillMaxSize()
            },
        )
    }
}
