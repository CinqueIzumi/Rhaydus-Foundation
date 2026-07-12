package nl.rhaydus.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.dispatch_get_global_queue

/**
 * iOS connectivity provider: tracks reachability via `nw_path_monitor`, reporting online whenever the
 * current path status is `satisfied`.
 */
@OptIn(ExperimentalForeignApi::class)
class IosNetworkAvailabilityProvider : BaseNetworkAvailabilityProvider() {
    private val monitor = nw_path_monitor_create()

    private val _isOnline = MutableStateFlow(false)

    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        nw_path_monitor_set_update_handler(monitor) { path ->
            _isOnline.value = nw_path_get_status(path) == nw_path_status_satisfied
        }
        nw_path_monitor_set_queue(
            monitor,
            dispatch_get_global_queue(
                DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(),
                0u,
            ),
        )
        nw_path_monitor_start(monitor)
    }
}
