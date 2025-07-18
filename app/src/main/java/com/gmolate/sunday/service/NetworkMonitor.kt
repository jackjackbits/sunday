package com.gmolate.sunday.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _connectionType = MutableStateFlow<ConnectionType>(ConnectionType.NONE)
    val connectionType: StateFlow<ConnectionType> = _connectionType.asStateFlow()

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    init {
        startMonitoring()
        // Verificar estado inicial
        checkInitialConnectivity()
    }

    private fun startMonitoring() {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                updateConnectionStatus(true)
                updateConnectionType(network)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                updateConnectionStatus(false)
                _connectionType.value = ConnectionType.NONE
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                updateConnectionType(network)
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        try {
            connectivityManager.registerNetworkCallback(networkRequest, callback)
        } catch (e: Exception) {
            // Fallback si no se puede registrar el callback
            _isConnected.value = isConnectedFallback()
            _isOnline.value = _isConnected.value
        }
    }

    private fun checkInitialConnectivity() {
        val isConnected = isConnectedFallback()
        _isConnected.value = isConnected
        _isOnline.value = isConnected

        if (isConnected) {
            connectivityManager.activeNetwork?.let { network ->
                updateConnectionType(network)
            }
        }
    }

    private fun updateConnectionStatus(connected: Boolean) {
        _isConnected.value = connected
        _isOnline.value = connected
    }

    private fun updateConnectionType(network: Network) {
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        val connectionType = when {
            capabilities == null -> ConnectionType.NONE
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> ConnectionType.VPN
            else -> ConnectionType.OTHER
        }
        _connectionType.value = connectionType
    }

    private fun isConnectedFallback(): Boolean {
        return try {
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } catch (e: Exception) {
            false
        }
    }

    fun refresh() {
        checkInitialConnectivity()
    }
}
