package nl.rhaydus.designsystem.share

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal class AndroidShareCardCapture internal constructor(
    override val graphicsLayer: GraphicsLayer,
    private val context: Context,
    private val config: ShareCardCaptureConfig,
) : ShareCardCapture {
    override suspend fun saveToGallery(displayName: String): SaveOutcome {
        val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
        val filename = buildFilename(displayName = displayName)

        return withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveViaScopedStorage(
                        bitmap,
                        filename,
                    )
                } else {
                    saveViaLegacyStorage(
                        bitmap,
                        filename,
                    )
                }
            }.getOrElse { SaveOutcome.Failure(reason = "$it") }
        }
    }

    override suspend fun saveToCache(displayName: String): SaveOutcome {
        val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
        val filename = buildFilename(displayName = displayName)

        return withContext(Dispatchers.IO) {
            runCatching {
                val shareDir = File(
                    context.cacheDir,
                    SHARE_CACHE_FOLDER,
                ).apply { mkdirs() }

                shareDir.listFiles()?.forEach { it.delete() }

                val file = File(
                    shareDir,
                    filename,
                )
                file.outputStream().use { stream ->
                    bitmap.compress(
                        Bitmap.CompressFormat.PNG, /* quality = */
                        100,
                        stream,
                    )
                }

                val uri = FileProvider.getUriForFile(
                    context,
                    config.androidFileProviderAuthority,
                    file,
                )

                SaveOutcome.Cached(identifier = uri.toString())
            }.getOrElse { SaveOutcome.Failure(reason = "$it") }
        }
    }

    override suspend fun share(displayName: String): ShareOutcome {
        val cached = saveToCache(displayName = displayName)

        if (cached !is SaveOutcome.Cached) {
            return ShareOutcome.Failure(
                reason = (cached as? SaveOutcome.Failure)?.reason ?: "Failed to cache share card",
            )
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(
                Intent.EXTRA_STREAM,
                Uri.parse(cached.identifier),
            )
            putExtra(
                Intent.EXTRA_SUBJECT,
                displayName,
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(
            sendIntent,
            "Share $displayName",
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return runCatching {
            context.startActivity(chooser)

            ShareOutcome.Shared
        }.getOrElse { ShareOutcome.Failure(reason = "$it") }
    }

    private fun buildFilename(displayName: String): String {
        val sanitized = displayName.replace(
            Regex("[^A-Za-z0-9-_]"),
            "-",
        )

        return "${config.fileNamePrefix}-$sanitized-${System.currentTimeMillis()}.png"
    }

    private fun saveViaScopedStorage(
        bitmap: Bitmap,
        filename: String,
    ): SaveOutcome {
        val resolver = context.contentResolver

        val values = ContentValues().apply {
            put(
                MediaStore.MediaColumns.DISPLAY_NAME,
                filename,
            )
            put(
                MediaStore.MediaColumns.MIME_TYPE,
                "image/png",
            )
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "$RELATIVE_PICTURES_PATH/${config.galleryAlbum}",
            )
            put(
                MediaStore.MediaColumns.IS_PENDING,
                1,
            )
        }

        val uri = resolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values,
        )
            ?: error("MediaStore insert returned null")

        resolver.openOutputStream(uri).use { stream ->
            requireNotNull(stream) { "openOutputStream returned null for $uri" }

            bitmap.compress(
                Bitmap.CompressFormat.PNG, /* quality = */
                100,
                stream,
            )
        }

        values.clear()
        values.put(
            MediaStore.MediaColumns.IS_PENDING,
            0,
        )
        resolver.update(
            uri,
            values,
            null,
            null,
        )

        return SaveOutcome.Saved(
            identifier = uri.toString(),
            displayPath = "$RELATIVE_PICTURES_PATH/${config.galleryAlbum}/$filename",
        )
    }

    @Suppress("DEPRECATION")
    private fun saveViaLegacyStorage(
        bitmap: Bitmap,
        filename: String,
    ): SaveOutcome {
        val picturesDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            config.galleryAlbum,
        )

        picturesDir.mkdirs()

        val file = File(
            picturesDir,
            filename,
        )
        file.outputStream().use { stream ->
            bitmap.compress(
                Bitmap.CompressFormat.PNG, /* quality = */
                100,
                stream,
            )
        }

        val values = ContentValues().apply {
            put(
                MediaStore.Images.Media.DATA,
                file.absolutePath,
            )
            put(
                MediaStore.Images.Media.MIME_TYPE,
                "image/png",
            )
            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                filename,
            )
        }

        val uri = context.contentResolver
            .insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values,
            )
            ?: Uri.fromFile(file)

        return SaveOutcome.Saved(
            identifier = uri.toString(),
            displayPath = file.absolutePath,
        )
    }

    private companion object {
        const val SHARE_CACHE_FOLDER = "share"

        val RELATIVE_PICTURES_PATH: String = Environment.DIRECTORY_PICTURES
    }
}

@Composable
actual fun rememberShareCardCapture(config: ShareCardCaptureConfig): ShareCardCapture {
    val graphicsLayer = rememberGraphicsLayer()
    val context = LocalContext.current

    return remember(graphicsLayer, context, config) {
        AndroidShareCardCapture(
            graphicsLayer,
            context,
            config,
        )
    }
}
