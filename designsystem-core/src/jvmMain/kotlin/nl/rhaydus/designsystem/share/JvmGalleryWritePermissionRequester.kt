package nl.rhaydus.designsystem.share

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// Desktop has no runtime gallery permission; report granted so the caller's pending-save flow proceeds.
internal class JvmGalleryWritePermissionRequester internal constructor(
    private val onResult: (Boolean) -> Unit,
) : GalleryWritePermissionRequester {
    override fun request() {
        onResult(true)
    }
}

@Composable
actual fun rememberGalleryWritePermissionRequester(
    onResult: (Boolean) -> Unit,
): GalleryWritePermissionRequester =
    remember(onResult) {
        JvmGalleryWritePermissionRequester(onResult = onResult)
    }
