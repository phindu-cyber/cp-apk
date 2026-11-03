package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

interface ConnectivityObserver {
  val isOnline: Flow<Boolean>
}

class AndroidConnectivityObserver(
  context: Context,
) : ConnectivityObserver {
  private val connectivityManager =
    context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

  override val isOnline: Flow<Boolean> = callbackFlow {
    trySend(checkCurrentConnectivity())

    val callback = object : ConnectivityManager.NetworkCallback() {
      override fun onAvailable(network: Network) {
        trySend(checkCurrentConnectivity())
      }

      override fun onLost(network: Network) {
        trySend(checkCurrentConnectivity())
      }

      override fun onCapabilitiesChanged(
        network: Network,
        networkCapabilities: NetworkCapabilities,
      ) {
        val hasInternet = networkCapabilities.hasCapability(
          NetworkCapabilities.NET_CAPABILITY_INTERNET
        )
        trySend(hasInternet)
      }
    }

    val request = NetworkRequest.Builder()
      .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
      .build()

    runCatching {
      connectivityManager.registerNetworkCallback(request, callback)
    }.onFailure {
      trySend(true)
    }

    awaitClose {
      runCatching { connectivityManager.unregisterNetworkCallback(callback) }
    }
  }.distinctUntilChanged()

  private fun checkCurrentConnectivity(): Boolean {
    val activeNetwork = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }
}

class StaticConnectivityObserver(online: Boolean = true) : ConnectivityObserver {
  override val isOnline: Flow<Boolean> = MutableStateFlow(online)
}
