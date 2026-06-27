package nl.rhaydus.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import nl.rhaydus.designsystem.model.ButtonSize
import nl.rhaydus.designsystem.model.ButtonStyle

/**
 * Inline error state: renders [message] in the error colour role above a retry affordance that calls
 * [onRetry]. The standard in-content surface for a failed load or submit — the render side of the TOAD
 * `String?` error-slot convention (see `toad-architecture.md`); drop it where a failure should surface
 * instead of the normal content.
 *
 * The caller owns sizing and outer padding via [modifier]. Content is always centred horizontally;
 * vertical centring is opt-in — it only takes effect when [modifier] gives the column a height (e.g.
 * `Modifier.fillMaxSize()` for a full results pane). With no height the column wraps its content, so it
 * simply stacks inline below the preceding element (e.g. under a form's submit button).
 *
 * [retryLabel] and [textStyle] are params so the app supplies its own copy and (editorial) type role;
 * the error tint is the Material `error` role and the retry affordance is the shared [RhaydusButton].
 */
@Composable
fun InlineErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryLabel: String = "Retry",
    textStyle: TextStyle = MaterialTheme.typography.bodySmall,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = textStyle,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(12.dp))

        RhaydusButton(
            label = retryLabel,
            style = ButtonStyle.OUTLINED,
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
            size = ButtonSize.S,
        )
    }
}
