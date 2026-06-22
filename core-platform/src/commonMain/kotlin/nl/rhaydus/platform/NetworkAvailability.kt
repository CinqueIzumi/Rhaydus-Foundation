package nl.rhaydus.platform

import kotlin.concurrent.Volatile

/**
 * Process-wide, instant connectivity check for non-suspending guard clauses (e.g. "throw offline if
 * not connected"). Install the app's [NetworkAvailabilityProvider] once at startup; [isOnline] then
 * reflects its current state, defaulting to `true` before installation so guards never spuriously
 * report offline.
 */
object NetworkAvailability {
    @Volatile
    private var providerRef: NetworkAvailabilityProvider? = null

    fun install(provider: NetworkAvailabilityProvider) {
        providerRef = provider
    }

    fun isOnline(): Boolean = providerRef?.isOnline?.value ?: true
}
