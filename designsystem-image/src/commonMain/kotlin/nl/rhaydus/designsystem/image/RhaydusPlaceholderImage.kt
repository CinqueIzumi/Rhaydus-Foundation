package nl.rhaydus.designsystem.image

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.SubcomposeAsyncImage
import nl.rhaydus.designsystem.util.SkeletonCrossfade

/**
 * An async image that shows [placeholder] while it loads - both while [isLoading] is true and no [model]
 * has resolved yet (a host-driven wait, crossfaded to the image), and while Coil fetches the resolved
 * [model]. [model] is anything Coil resolves (a URL string, file, resource, ...). Size [placeholder] to
 * fill the image bounds (e.g. `Modifier.fillMaxSize()`) so it occupies the same area the image will.
 */
@Composable
fun RhaydusPlaceholderImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    isLoading: Boolean = false,
    placeholder: @Composable () -> Unit,
) {
    SkeletonCrossfade(
        isLoading = isLoading && model == null,
        modifier = modifier,
        label = "RhaydusPlaceholderImage",
    ) { loading ->
        if (loading) {
            placeholder()
        } else {
            SubcomposeAsyncImage(
                model = model,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                loading = { placeholder() },
                contentScale = contentScale,
            )
        }
    }
}
