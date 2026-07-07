package nl.rhaydus.designsystem.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
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
 * The write side of the [LocalBottomBarPadding] contract: a host that puts [bottomBar] at the foot of
 * [content] and provides, through [LocalBottomBarPadding], the trailing padding scrolling content must
 * reserve so its last item is never occluded. Content reads the value back through
 * [rememberBottomBarPadding].
 *
 * Pure layout - it knows nothing about navigation, screen models, tab sets, or how the bar is styled;
 * it only hosts the [bottomBar] slot and shares the resulting padding. The brand-styled bar (a Material
 * `NavigationBar`, a floating toolbar, whatever) is supplied by the caller.
 *
 * [placement] decides both how the bar is laid out and what the provided padding means:
 *
 * - [BottomBarPlacement.OVERLAY] - the bar draws *over* the content, so layout reserves nothing for it.
 *   The host measures the bar's laid-out footprint and provides `footprint + barSpacing`.
 * - [BottomBarPlacement.DOCKED] - the bar is hosted in a Material `Scaffold`, which reserves its space
 *   through `innerPadding`. The footprint is therefore already accounted for and the host provides
 *   [barSpacing] alone: the breathing gap between the last item and the bar.
 *
 * Either way the value that reaches content answers exactly one question - *how much trailing padding do
 * I reserve?* - so screens read it unconditionally and never branch on the placement themselves. An app
 * with a runtime docked/floating preference flips [placement] and nothing else moves.
 *
 * Double-inset safety (overlay): the footprint is *measured* from the laid-out bar via [onSizeChanged]
 * placed **outside** [windowInsetsPadding], so the navigation-bar inset the bar reserves is already baked
 * into the measured height and counted exactly once. Do not recompute and re-add
 * `WindowInsets.navigationBars` on top of a measured height - that double-counts the inset.
 *
 * On the first overlay frame the footprint is `0.dp`, so content reserves only [barSpacing]; it settles to
 * the full footprint within a frame once the bar has measured.
 */
@Composable
fun BottomBarScaffold(
    bottomBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    placement: BottomBarPlacement = BottomBarPlacement.OVERLAY,
    barSpacing: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    when (placement) {
        BottomBarPlacement.DOCKED -> DockedBottomBarScaffold(
            bottomBar = bottomBar,
            modifier = modifier,
            barSpacing = barSpacing,
            content = content,
        )

        BottomBarPlacement.OVERLAY -> OverlayBottomBarScaffold(
            bottomBar = bottomBar,
            modifier = modifier,
            barSpacing = barSpacing,
            content = content,
        )
    }
}

@Composable
private fun DockedBottomBarScaffold(
    bottomBar: @Composable () -> Unit,
    modifier: Modifier,
    barSpacing: Dp,
    content: @Composable () -> Unit,
) {
    val contentPadding = bottomBarContentPadding(
        barFootprint = 0.dp,
        barSpacing = barSpacing,
    )

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        bottomBar = bottomBar,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            CompositionLocalProvider(LocalBottomBarPadding provides contentPadding) {
                content()
            }
        }
    }
}

@Composable
private fun OverlayBottomBarScaffold(
    bottomBar: @Composable () -> Unit,
    modifier: Modifier,
    barSpacing: Dp,
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
 * The trailing padding scrolling content reserves for a bottom bar: the [barFootprint] the bar occupies
 * over the content (already including the navigation-bar inset, and `0.dp` when the bar is docked and its
 * space is reserved by layout) plus a [barSpacing] breathing gap. Pure so the measure-once contract can be
 * asserted without a Compose host.
 */
internal fun bottomBarContentPadding(
    barFootprint: Dp,
    barSpacing: Dp,
): Dp = barFootprint + barSpacing
