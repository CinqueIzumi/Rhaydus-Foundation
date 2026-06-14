package nl.rhaydus.designsystem.motion

import androidx.compose.runtime.Composable
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

@Composable
actual fun playDecorativeMotion(): Boolean = UIAccessibilityIsReduceMotionEnabled().not()
