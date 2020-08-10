package uk.nhs.nhsx.covid19.android.app.exposure.keysdownload

import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.exposure.keysdownload.DownloadKeysParams.Intervals.Daily
import uk.nhs.nhsx.covid19.android.app.exposure.keysdownload.DownloadKeysParams.Intervals.Hourly
import uk.nhs.nhsx.covid19.android.app.exposure.toExposureConfiguration
import uk.nhs.nhsx.covid19.android.app.remote.ExposureConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.KeysDistributionApi
import uk.nhs.nhsx.covid19.android.app.util.FileHelper
import java.util.UUID
import javax.inject.Inject

class DownloadAndProcessKeys @Inject constructor(
    private val keysDistributionApi: KeysDistributionApi,
    private val exposureConfigurationApi: ExposureConfigurationApi,
    private val exposureApi: ExposureNotificationApi,
    private val fileHelper: FileHelper,
    private val downloadKeysParams: DownloadKeysParams,
    private val lastDownloadedKeyTimeProvider: LastDownloadedKeyTimeProvider
) {

    suspend operator fun invoke(): Result<Unit> = withContext(Dispatchers.IO) {
        runSafely {
            if (exposureApi.isEnabled()) {
                val exposureConfiguration =
                    exposureConfigurationApi.getExposureConfiguration().toExposureConfiguration()
                downloadKeysParams.getNextQueries().forEach { interval ->
                    when (interval) {
                        is Daily -> downloadDaily(
                            interval.timestamp.zipExt(),
                            exposureConfiguration
                        )
                        is Hourly -> downloadHourly(
                            interval.timestamp.zipExt(),
                            exposureConfiguration
                        )
                    }
                    lastDownloadedKeyTimeProvider.saveLastStoredTime(interval.timestamp)
                }
            }
        }
    }

    private suspend fun downloadHourly(
        timestamp: String,
        exposureConfiguration: ExposureConfiguration
    ) {
        val keyFile = keysDistributionApi.fetchHourlyKeys(timestamp)
        processKeyFiles(keyFile, exposureConfiguration, timestamp)
    }

    private suspend fun downloadDaily(
        timestamp: String,
        exposureConfiguration: ExposureConfiguration
    ) {
        val keyFile = keysDistributionApi.fetchDailyKeys(timestamp)
        processKeyFiles(keyFile, exposureConfiguration, timestamp)
    }

    private suspend fun processKeyFiles(
        keyFile: ResponseBody,
        exposureConfiguration: ExposureConfiguration,
        timestamp: String
    ) {
        val file = fileHelper.provideFile(keyFile.byteStream())

        exposureApi.provideDiagnosisKeys(
            listOf(file),
            exposureConfiguration,
            createToken(timestamp)
        )
    }

    private fun String.zipExt() = "$this.zip"

    private fun createToken(timestamp: String): String {
        return "$timestamp-${UUID.randomUUID()}"
    }
}
