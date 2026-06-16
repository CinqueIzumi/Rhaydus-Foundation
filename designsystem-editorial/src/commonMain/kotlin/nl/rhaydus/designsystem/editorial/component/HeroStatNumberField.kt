package nl.rhaydus.designsystem.editorial.component

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import nl.rhaydus.designsystem.editorial.editorialTypography

/**
 * A borderless single-number input rendered in the editorial `statLarge` face, cursor tinted `primary`,
 * its width sized to [charCount] so it reads as a hero number rather than a boxed form control. Reach for
 * it wherever the user edits a single number framed as a hero stat, rather than a bordered text field.
 */
@Composable
fun HeroStatNumberField(
    value: TextFieldValue,
    charCount: Int,
    onValueChange: (TextFieldValue) -> Unit,
    onFocusReset: () -> Unit,
    onFocusGained: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = MaterialTheme.editorialTypography.statLarge

    val width = computeHeroStatFieldWidth(
        textStyle = style,
        charCount = charCount,
    )

    val focusManager = LocalFocusManager.current

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = style.copy(
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        ),
        cursorBrush = SolidColor(value = MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        modifier = modifier
            .width(width = width)
            .onFocusChanged { focusState ->
                if (focusState.hasFocus.not()) {
                    onFocusReset()

                    return@onFocusChanged
                }

                onFocusGained()
            },
    )
}

@Composable
private fun computeHeroStatFieldWidth(
    textStyle: TextStyle,
    charCount: Int,
): Dp {
    val density = LocalDensity.current

    return remember(density, textStyle, charCount) {
        with(density) {
            val fontSizeInPx = textStyle.fontSize.toPx()
            val padding = 16.dp.toPx()

            ((charCount * fontSizeInPx * 0.62f) + padding).toDp()
        }
    }
}
