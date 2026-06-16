package nl.rhaydus.designsystem.image

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

/**
 * A plain async image - a thin wrapper over Coil's `AsyncImage` with no loading affordance. Reach for
 * [RhaydusPlaceholderImage] or [RhaydusShimmerImage] when the load should show a placeholder. [model] is
 * anything Coil resolves (a URL string, file, resource, ...).
 */
@Composable
fun RhaydusImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
    )
}
