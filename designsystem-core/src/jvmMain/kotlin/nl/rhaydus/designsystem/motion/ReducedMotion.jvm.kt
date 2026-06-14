package nl.rhaydus.designsystem.motion

import androidx.compose.runtime.Composable

// Desktop exposes no cross-platform "reduce motion" accessibility signal, so decorative motion
// always plays. Revisit if a per-OS query (e.g. macOS NSWorkspace, Windows SystemParametersInfo) is
// wired later.
@Composable
actual fun playDecorativeMotion(): Boolean = true
