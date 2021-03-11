package uk.nhs.nhsx.covid19.android.app.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities.TRANSPORT_BLUETOOTH
import android.net.NetworkCapabilities.TRANSPORT_CELLULAR
import android.net.NetworkCapabilities.TRANSPORT_ETHERNET
import android.net.NetworkCapabilities.TRANSPORT_WIFI
import javax.inject.Inject

class HasInternetConnectivity @Inject constructor(private val context: Context) {

    operator fun invoke(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val activeNetwork = connectivityManager?.activeNetwork ?: return false
        val activeNetworkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return activeNetworkCapabilities.hasTransport(TRANSPORT_WIFI) ||
            activeNetworkCapabilities.hasTransport(TRANSPORT_CELLULAR) ||
            activeNetworkCapabilities.hasTransport(TRANSPORT_ETHERNET) ||
            activeNetworkCapabilities.hasTransport(TRANSPORT_BLUETOOTH)
    }
}
