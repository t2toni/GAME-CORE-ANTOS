package com.antos.gaming.monitoring

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetAddress

class NetworkMonitor(private val context: Context) {
    private val _latency = MutableStateFlow(-1L)
    val latency: StateFlow<Long> = _latency.asStateFlow()

    private val _networkType = MutableStateFlow("None")
    val networkType: StateFlow<String> = _networkType.asStateFlow()

    private var job: Job? = null

    fun start(coroutineScope: CoroutineScope) {
        job = coroutineScope.launch(Dispatchers.IO) {
            while (isActive) {
                updateNetworkType()
                pingServer()
                delay(3000) // Poll every 3 seconds to avoid battery drain
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private fun updateNetworkType() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        _networkType.value = when {
            capabilities == null -> "None"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Unknown"
        }
    }

    private fun pingServer() {
        if (_networkType.value == "None") {
            _latency.value = -1L
            return
        }
        try {
            val startTime = System.currentTimeMillis()
            // Ping Google DNS as a general connectivity estimate
            val address = InetAddress.getByName("8.8.8.8")
            val isReachable = address.isReachable(2000)
            if (isReachable) {
                _latency.value = System.currentTimeMillis() - startTime
            } else {
                _latency.value = -1L
            }
        } catch (e: Exception) {
            _latency.value = -1L
        }
    }
}
