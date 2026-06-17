package nl.rhaydus.ui.common

/**
 * The standard use-case body wrapper. Delegates to [runCatchingCancellable] for the cancellation-safe
 * `Result` conversion, then logs any failure once via [AppLog] — here at the use-case boundary, so a
 * failure is never silently dropped even when the presentation layer chooses not to surface it.
 *
 * Binding the logger here, rather than re-passing it at every call site, is the point: a use case
 * writes `runCatchingLogged { … }` instead of `runCatching { … }` and logging is guaranteed with zero
 * per-site ceremony. `AppLog` is a no-op until the app calls `AppLog.install(...)`, so this stays
 * brand-agnostic — the wrapper logs through whatever tag the consuming app installed.
 *
 * Whether to *surface* the failure to the user stays a separate, presentation-layer decision — this
 * never authors user-facing copy. A use case composed from others (unwrapping them via
 * [Result.getOrThrow]) logs at each boundary it crosses; that is a deliberate call-chain trace, not
 * redundant noise. Pass [context] to label the log line; omit it to fall back to the throwable's own
 * message.
 */
inline fun <T> runCatchingLogged(
    context: String? = null,
    block: () -> T,
): Result<T> = runCatchingCancellable(block).onFailure { error ->
    AppLog.e(
        throwable = error,
        message = context ?: error.message ?: error.toString(),
    )
}
