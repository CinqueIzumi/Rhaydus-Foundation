package nl.rhaydus.designsystem.share

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer

/**
 * Captures a composable (rendered through [CapturableShareCard]) to a bitmap and then saves or shares it.
 * The [graphicsLayer] is the record target the host draws into; [saveToGallery] writes a PNG to the
 * device's photo gallery, [saveToCache] writes it to app cache (returning a shareable identifier), and
 * [share] hands the PNG to the platform share surface (share sheet on Android/iOS, clipboard on desktop).
 * Obtain an instance with [rememberShareCardCapture].
 */
interface ShareCardCapture {
    val graphicsLayer: GraphicsLayer

    suspend fun saveToGallery(displayName: String): SaveOutcome

    suspend fun saveToCache(displayName: String): SaveOutcome

    suspend fun share(displayName: String): ShareOutcome
}

/**
 * Remembers a platform [ShareCardCapture] bound to a fresh `GraphicsLayer`, configured by [config] (the
 * app's file-name prefix, gallery album, and Android `FileProvider` authority). Feed the returned
 * instance to [CapturableShareCard] to make a card capturable, then call its save/share methods.
 */
@Composable
expect fun rememberShareCardCapture(config: ShareCardCaptureConfig): ShareCardCapture

internal fun ContentDrawScope.recordAndDraw(graphicsLayer: GraphicsLayer) {
    graphicsLayer.record { this@recordAndDraw.drawContent() }

    drawLayer(graphicsLayer)
}
