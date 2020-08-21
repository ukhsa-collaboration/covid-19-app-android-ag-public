package uk.nhs.nhsx.covid19.android.app.analytics

import android.content.SharedPreferences
import android.net.TrafficStats
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject
import kotlin.math.absoluteValue

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

        val totalDownloadedBytes = networkDownloadDataProvider().absoluteValue

        return if (lastStoredValue != null) {

            val dailyDownloadedBytes =
                if (totalDownloadedBytes > lastStoredValue)
                    totalDownloadedBytes - lastStoredValue
                else
                    totalDownloadedBytes

            networkStatsStorage.lastDownloadedBytes = dailyDownloadedBytes
            dailyDownloadedBytes
        } else {
            networkStatsStorage.lastDownloadedBytes = totalDownloadedBytes
            0
        }
    }.getOrNull()

    fun getTotalBytesUploaded(): Int? = runCatching {
        val lastStored = networkStatsStorage.lastUploadedBytes
        val totalUploadedBytes = networkUploadDataProvider().absoluteValue
        return if (lastStored != null) {
            val dailyUploadedBytes =
                if (totalUploadedBytes > lastStored)
                    totalUploadedBytes - lastStored
                else
                    totalUploadedBytes
            networkStatsStorage.lastUploadedBytes = dailyUploadedBytes
            dailyUploadedBytes
        } else {
            networkStatsStorage.lastUploadedBytes = totalUploadedBytes
            0
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
