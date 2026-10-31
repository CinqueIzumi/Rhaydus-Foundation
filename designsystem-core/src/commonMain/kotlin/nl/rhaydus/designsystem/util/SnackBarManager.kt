package nl.rhaydus.designsystem.util

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
object SnackBarManager {
    private val _snackBarState = MutableStateFlow(SnackbarHostState())
    val snackBarState = _snackBarState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val isShowing = AtomicBoolean(false)

    fun showSnackbar(
        title: String,
        duration: SnackbarDuration = SnackbarDuration.Short,
    ) {
        val acquired = isShowing.compareAndSet(
            false,
            true,
        )

        if (acquired.not()) return

        scope.launch {
            try {
                snackBarState.value.showSnackbar(
                    message = title,
                    duration = duration,
                )
            } finally {
                isShowing.store(false)
            }
        }
    }

    fun showSnackBar(
        title: String,
        actionLabel: String,
        duration: SnackbarDuration = SnackbarDuration.Long,
        onActionClick: () -> Unit,
        onDismiss: () -> Unit = {},
    ) {
        val acquired = isShowing.compareAndSet(
            false,
            true,
        )

        if (acquired.not()) return

        scope.launch {
            try {
                val result = snackBarState.value.showSnackbar(
                    message = title,
                    actionLabel = actionLabel,
                    duration = duration,
                    withDismissAction = true,
                )

                when (result) {
                    SnackbarResult.Dismissed -> onDismiss()
                    SnackbarResult.ActionPerformed -> onActionClick()
                }
            } finally {
                isShowing.store(false)
            }
        }
    }
}
