package nl.rhaydus.designsystem.util

import androidx.compose.runtime.Composable

/**
 * Reads the current plain-text contents of the system clipboard, returning an empty string when the
 * clipboard is empty or holds something that can't be coerced to text.
 */
fun interface ClipboardReader {
    fun read(): String
}

/**
 * The active [ClipboardReader] for the current platform - Android reads the primary clip from
 * `ClipboardManager`, iOS reads `UIPasteboard.generalPasteboard`, desktop reads the AWT clipboard.
 * Exposed as a seam so common UI can paste without touching the raw platform clipboard.
 */
@Composable
expect fun rememberClipboardReader(): ClipboardReader
