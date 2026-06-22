package nl.rhaydus.platform

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android connectivity provider: tracks reachability via [ConnectivityManager]'s default-network
 * callback, treating a network as online only once it has both `INTERNET` and `VALIDATED`
 * capabilities. The callback is intentionally never unregistered - this is a process-lifetime
 * singleton the app installs once at startup, so the OS reclaims it on teardown.
 */
class AndroidNetworkAvailabilityProvider(context: Context) : BaseNetworkAvailabilityProvider() {
    private val connectivityManager: ConnectivityManager? =
        context.getSystemService(ConnectivityManager::class.java)

    private val _isOnline = MutableStateFlow(initialOnlineState())

    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        connectivityManager?.registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val caps = connectivityManager.getNetworkCapabilities(network)
                    _isOnline.value = caps.isUsableInternet()
                }

                override fun onLost(network: Network) {
                    _isOnline.value = false
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    capabilities: NetworkCapabilities,
                ) {
                    _isOnline.value = capabilities.isUsableInternet()
                }
            },
        )
    }

    private fun initialOnlineState(): Boolean {
        val active = connectivityManager?.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(active)

        return caps.isUsableInternet()
    }

    private fun NetworkCapabilities?.isUsableInternet(): Boolean {
        if (this == null) return false

        return hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
