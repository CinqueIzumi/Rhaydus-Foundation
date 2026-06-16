package nl.rhaydus.designsystem.editorial

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Provides the editorial design language to [content]: it makes [editorialTypography] reachable through
 * `MaterialTheme.editorialTypography` so editorial components resolve their type roles. Opt in by nesting
 * it inside `RhaydusTheme` (from `designsystem-core`); an app that does not use editorial components never
 * applies it, and the neutral core stays free of any editorial vocabulary.
 *
 * [editorialTypography] defaults to a neutral mapping of the ambient `MaterialTheme.typography`; an app
 * passes its branded instance (built with [buildEditorialTypography] and `.copy(...)`, or from scratch)
 * to give the editorial components its voice. Because that default reads the ambient Material typography,
 * call this inside `RhaydusTheme` (or any `MaterialExpressiveTheme`); to use it outside one, pass an
 * explicit [editorialTypography].
 */
@Composable
fun EditorialTheme(
    editorialTypography: EditorialTypography = buildEditorialTypography(MaterialTheme.typography),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalEditorialTypography provides editorialTypography) {
        content()
    }
}
