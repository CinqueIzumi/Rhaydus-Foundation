package nl.rhaydus.designsystem.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The write side of the [LocalBottomBarPadding] contract: a host that overlays [bottomBar] on top of
 * [content], measures the bar's laid-out footprint, and provides that footprint (plus [barSpacing])
 * through [LocalBottomBarPadding] so scrolling content can reserve trailing space and never be
 * occluded. Content reads the value back through [rememberBottomBarPadding].
 *
 * Pure layout - it knows nothing about navigation, screen models, tab sets, or how the bar is styled;
 * it only measures the [bottomBar] slot and shares the result. The brand-styled bar (a Material
 * `NavigationBar`, a floating toolbar, whatever) is supplied by the caller.
 *
 * This is for an **overlay / floating** bar that draws *over* the content, which is the only case
 * [LocalBottomBarPadding] exists for: an overlay bar cannot be reserved by layout, so content needs a
 * shared channel to learn its height. A **docked** bar is a different pattern - host it in a Material
 * `Scaffold { bottomBar }` and read the reserved space from `innerPadding`; there [LocalBottomBarPadding]
 * correctly stays `0.dp` and this scaffold is not used.
 *
 * Double-inset safety: the footprint is *measured* from the laid-out bar via [onSizeChanged] placed
 * **outside** [windowInsetsPadding], so the navigation-bar inset the bar reserves is already baked into
 * the measured height and counted exactly once. Do not recompute and re-add `WindowInsets.navigationBars`
 * on top of a measured height - that double-counts the inset.
 *
 * On first frame the footprint is `0.dp`, so content reserves only [barSpacing]; it settles to the full
 * footprint within a frame once the bar has measured.
 */
@Composable
fun BottomBarScaffold(
    bottomBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    barSpacing: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current

    var barFootprint by remember { mutableStateOf(0.dp) }

    val contentPadding = bottomBarContentPadding(
        barFootprint = barFootprint,
        barSpacing = barSpacing,
    )

    Box(modifier = modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalBottomBarPadding provides contentPadding) {
            content()
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { size ->
                    barFootprint = with(density) { size.height.toDp() }
                }
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            bottomBar()
        }
    }
}

/**
 * The trailing padding scrolling content reserves for a bottom bar: the measured [barFootprint] (which
 * already includes the navigation-bar inset) plus a [barSpacing] breathing gap. Pure so the
 * measure-once contract can be asserted without a Compose host.
 */
internal fun bottomBarContentPadding(
    barFootprint: Dp,
    barSpacing: Dp,
): Dp = barFootprint + barSpacing
