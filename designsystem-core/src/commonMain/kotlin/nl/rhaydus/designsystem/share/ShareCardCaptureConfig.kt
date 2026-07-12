package nl.rhaydus.designsystem.share

/**
 * Brand/app configuration for [rememberShareCardCapture]. Keeps the capture seam itself brand-agnostic —
 * the app supplies these once and the mechanism reads them.
 *
 * - [fileNamePrefix] prefixes every exported PNG's file name (e.g. `"myapp"` → `myapp-<name>-<ts>.png`).
 * - [galleryAlbum] is the album/folder saved images land in (on Android, `Pictures/<galleryAlbum>`).
 * - [androidFileProviderAuthority] is the authority of the app's `FileProvider`, used **only on Android**
 *   to grant read access on the share `Intent`. The app must declare a matching `<provider>` in its
 *   manifest with this authority. Ignored on desktop and iOS (they share/save without a content URI).
 */
data class ShareCardCaptureConfig(
    val fileNamePrefix: String,
    val galleryAlbum: String,
    val androidFileProviderAuthority: String,
)
