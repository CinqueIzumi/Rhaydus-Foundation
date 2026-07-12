package nl.rhaydus.platform

import kotlinx.coroutines.flow.StateFlow

/**
 * Reactive connectivity seam. [isOnline] tracks the current network state and [awaitOnline] suspends
 * until the network is available. Platform providers back this with the OS connectivity API
 * (Android `ConnectivityManager`, iOS `nw_path_monitor`, desktop reachability polling); the instant,
 * non-suspending check is exposed separately via [NetworkAvailability].
 */
interface NetworkAvailabilityProvider {
    val isOnline: StateFlow<Boolean>

    suspend fun awaitOnline()
}
