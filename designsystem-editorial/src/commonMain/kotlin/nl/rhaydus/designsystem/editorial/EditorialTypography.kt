package nl.rhaydus.designsystem.editorial

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle

internal const val TABULAR_FIGURES = "tnum"

/**
 * The shared editorial type *roles* that editorial components consume. This role set is the contract;
 * the role *values* (face, size, weight, italics, tracking) are a brand decision each app supplies by
 * building its own instance and handing it to [EditorialTheme]. An app keeps its richer, app-only scale
 * separate - this holds only the subset editorial components read, and grows as they need more roles.
 *
 * - [eyebrow] / [eyebrowSmall]: the orienting label/kicker that leads a section.
 * - [pageTitle]: an in-page screen title on a root tab.
 * - [headline]: the human-readable headline that follows an eyebrow.
 * - [title]: card and row titles.
 * - [body] / [bodySmall]: primary and secondary copy a screen composes itself.
 * - [statLarge] / [statHero]: large numeric stats (tabular figures so digits do not jitter).
 */
@Immutable
data class EditorialTypography(
    val eyebrow: TextStyle,
    val eyebrowSmall: TextStyle,
    val pageTitle: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val bodySmall: TextStyle,
    val statLarge: TextStyle,
    val statHero: TextStyle,
)

/**
 * Maps a base Material [typography] onto the editorial role vocabulary with only structural tweaks
 * (tabular figures on the stat roles). It bakes in no brand voice - call it and `.copy(...)` the roles an
 * app wants to restyle (an italic body, a wider eyebrow tracking, a display face), or build the instance
 * from scratch. Also backs the neutral fallback used outside [EditorialTheme].
 */
fun buildEditorialTypography(typography: Typography): EditorialTypography = EditorialTypography(
    eyebrow = typography.labelMedium,
    eyebrowSmall = typography.labelSmall,
    pageTitle = typography.displaySmall,
    headline = typography.headlineMedium,
    title = typography.titleLarge,
    body = typography.bodyMedium,
    bodySmall = typography.bodySmall,
    statLarge = typography.displayMedium.copy(fontFeatureSettings = TABULAR_FIGURES),
    statHero = typography.displayLarge.copy(fontFeatureSettings = TABULAR_FIGURES),
)

internal val DefaultEditorialTypography: EditorialTypography = buildEditorialTypography(Typography())

internal val LocalEditorialTypography = staticCompositionLocalOf { DefaultEditorialTypography }

/**
 * The [EditorialTypography] provided by the nearest [EditorialTheme]. Outside one it falls back to a
 * neutral scale derived from Material defaults, so previews and tests still resolve. This extension is the
 * only intended read path; the backing local and its default are `internal`.
 */
val MaterialTheme.editorialTypography: EditorialTypography
    @Composable
    @ReadOnlyComposable
    get() = LocalEditorialTypography.current
