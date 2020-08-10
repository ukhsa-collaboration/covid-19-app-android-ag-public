package uk.nhs.nhsx.covid19.android.app.analytics

import android.content.SharedPreferences
import android.net.TrafficStats
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject

class NetworkTrafficStats(
    private val networkStatsStorage: NetworkStatsStorage,
    private val networkDownloadDataProvider: () -> Int,
    private val networkUploadDataProvider: () -> Int
) {

    @Inject
    constructor(networkStatsStorage: NetworkStatsStorage) : this(
        networkStatsStorage,
        { TrafficStats.getTotalRxBytes().toInt() },
        { TrafficStats.getTotalTxBytes().toInt() }
    )

    fun getTotalBytesDownloaded(): Int? = runCatching {
        val lastStoredValue = networkStatsStorage.lastDownloadedBytes
        return if (lastStoredValue != null) {

            val totalDownloadedBytes =
                if (networkDownloadDataProvider() > lastStoredValue)
                    networkDownloadDataProvider() - lastStoredValue
                else
                    networkDownloadDataProvider()

            networkStatsStorage.lastDownloadedBytes = totalDownloadedBytes
            totalDownloadedBytes
        } else {
            networkStatsStorage.lastDownloadedBytes = networkDownloadDataProvider()
            null
        }
    }.getOrNull()

    fun getTotalBytesUploaded(): Int? = runCatching {
        val lastStored = networkStatsStorage.lastUploadedBytes
        return if (lastStored != null) {
            val totalUploadedBytes =
                if (networkUploadDataProvider() > lastStored)
                    networkUploadDataProvider() - lastStored
                else
                    networkUploadDataProvider()
            networkStatsStorage.lastUploadedBytes = totalUploadedBytes
            totalUploadedBytes
        } else {
            networkStatsStorage.lastUploadedBytes = networkUploadDataProvider()
            null
        }
    }.getOrNull()
}

class NetworkStatsStorage @Inject constructor(sharedPreferences: SharedPreferences) {
    private val downloadNetworkStatsPref = sharedPreferences.with<Int>(DOWNLOAD_NETWORK_STATS_KEY)
    private val uploadNetworkStatsPref = sharedPreferences.with<Int>(UPLOAD_NETWORK_STATS_KEY)

    var lastDownloadedBytes by downloadNetworkStatsPref

    var lastUploadedBytes by uploadNetworkStatsPref

    companion object {
        const val DOWNLOAD_NETWORK_STATS_KEY = "DOWNLOAD_NETWORK_STATS_KEY"
        const val UPLOAD_NETWORK_STATS_KEY = "UPLOAD_NETWORK_STATS_KEY"
    }
}
