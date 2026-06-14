package nl.rhaydus.ui.common

import kotlinx.coroutines.CoroutineDispatcher

/**
 * The app's coroutine dispatchers, injected (e.g. via Koin) so call sites and tests do not reach for
 * `Dispatchers.Main`/`IO`/`Default` directly. This is the dispatcher source a TOAD `ActionDependencies`
 * draws its `mainDispatcher` from.
 */
data class AppDispatchers(
    val main: CoroutineDispatcher,
    val io: CoroutineDispatcher,
    val default: CoroutineDispatcher,
)
