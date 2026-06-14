package nl.rhaydus.designsystem.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIPasteboard

@Composable
actual fun rememberClipboardReader(): ClipboardReader =
    remember {
        ClipboardReader { UIPasteboard.generalPasteboard.string ?: "" }
    }
