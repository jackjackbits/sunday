package com.gmolate.sunday.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

enum class ConnectionType {
    WIFI,
    CELLULAR,
    ETHERNET,
    VPN,
    OTHER,
    NONE;

    val description: String
        get() = when (this) {
            WIFI -> "WiFi"
            CELLULAR -> "Cellular"
            ETHERNET -> "Ethernet"
            VPN -> "VPN"
            OTHER -> "Other"
            NONE -> "None"
        }
}

class NetworkMonitor(private val context: Context) {
    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectionType = MutableStateFlow<ConnectionType>(ConnectionType.NONE)
    val connectionType: StateFlow<ConnectionType> = _connectionType.asStateFlow()

    init {
        startMonitoring()
    }

    private fun startMonitoring() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                updateStatus(connectivityManager, network)
            }

            override fun onLost(network: Network) {
                _isConnected.value = false
                _connectionType.value = ConnectionType.NONE
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                updateStatus(connectivityManager, network)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Check initial state
        connectivityManager.activeNetwork?.let { network ->
            updateStatus(connectivityManager, network)
        } ?: run {
            _isConnected.value = false
            _connectionType.value = ConnectionType.NONE
        }
    }

    private fun updateStatus(connectivityManager: ConnectivityManager, network: Network) {
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        capabilities?.let {
            _isConnected.value = true
            _connectionType.value = when {
                it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
                it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.CELLULAR
                it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
                it.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> ConnectionType.VPN
                else -> ConnectionType.OTHER
            }
        }
    }
}
