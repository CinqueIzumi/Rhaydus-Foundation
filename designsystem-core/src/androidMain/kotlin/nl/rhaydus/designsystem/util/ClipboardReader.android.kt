package nl.rhaydus.designsystem.util

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberClipboardReader(): ClipboardReader {
    val context = LocalContext.current

    return remember(context) {
        ClipboardReader {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = clipboard?.primaryClip?.takeIf { it.itemCount > 0 }

            clip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
        }
    }
}
