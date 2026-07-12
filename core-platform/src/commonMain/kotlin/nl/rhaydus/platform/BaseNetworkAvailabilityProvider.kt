package nl.rhaydus.platform

import kotlinx.coroutines.flow.first

/**
 * Base for platform [NetworkAvailabilityProvider]s: implements the shared [awaitOnline] over the
 * platform-supplied [isOnline] flow, so each target only provides the connectivity state.
 */
abstract class BaseNetworkAvailabilityProvider : NetworkAvailabilityProvider {
    override suspend fun awaitOnline() {
        isOnline.first { it }
    }
}
