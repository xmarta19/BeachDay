package pt.umaia.beachday.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

fun estadoRede(context: Context): Flow<Boolean> = callbackFlow {
    val gestor = context.getSystemService(ConnectivityManager::class.java)

    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            trySend(true)
        }

        override fun onLost(network: Network) {
            trySend(false)
        }
    }

    val capacidades = gestor.getNetworkCapabilities(gestor.activeNetwork)
    trySend(
        capacidades?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    )

    gestor.registerDefaultNetworkCallback(callback)
    awaitClose { gestor.unregisterNetworkCallback(callback) }
}.distinctUntilChanged()