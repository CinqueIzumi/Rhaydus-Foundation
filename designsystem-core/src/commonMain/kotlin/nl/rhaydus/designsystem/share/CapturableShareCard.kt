package nl.rhaydus.designsystem.share

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent

/**
 * Hosts a capturable share card: renders [content] and records it into [capture]'s `GraphicsLayer` on
 * every draw, so [ShareCardCapture.saveToGallery] / [ShareCardCapture.share] can read it back to a bitmap.
 * The app supplies its own card design as the [content] slot; only the capture plumbing lives here. Size
 * and position the card via [modifier] (e.g. a fixed export width, or a `layout` that scales an on-screen
 * preview) — the recorded bitmap matches what is drawn. The host is a plain `Box`, so [content] sizes its
 * own bounds (e.g. its own `Modifier.fillMaxWidth()` / fixed height); the `Box` does not stretch it.
 */
@Composable
fun CapturableShareCard(
    capture: ShareCardCapture,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.drawWithContent { recordAndDraw(capture.graphicsLayer) },
    ) {
        content()
    }
}
