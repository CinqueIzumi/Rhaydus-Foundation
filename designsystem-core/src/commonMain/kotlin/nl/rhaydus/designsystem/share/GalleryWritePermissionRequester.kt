package nl.rhaydus.designsystem.share

import androidx.compose.runtime.Composable

/**
 * Gate for whatever runtime permission a platform needs before [ShareCardCapture.saveToGallery] can write
 * to the gallery. Call [request] before saving; [onResult] (passed to [rememberGalleryWritePermissionRequester])
 * reports whether it was granted. On platforms/versions that need no permission it reports granted
 * synchronously.
 */
interface GalleryWritePermissionRequester {
    fun request()
}

/**
 * Remembers the platform [GalleryWritePermissionRequester]. On Android it gates the legacy
 * `WRITE_EXTERNAL_STORAGE` permission (needed only on API ≤ 28; API 29+ reports granted immediately via
 * scoped storage). Desktop and iOS report granted immediately.
 */
@Composable
expect fun rememberGalleryWritePermissionRequester(
    onResult: (Boolean) -> Unit,
): GalleryWritePermissionRequester
