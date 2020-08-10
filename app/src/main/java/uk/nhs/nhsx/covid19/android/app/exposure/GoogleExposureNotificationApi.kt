package uk.nhs.nhsx.covid19.android.app.exposure

import android.content.Context
import android.util.Base64
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import com.google.android.gms.nearby.exposurenotification.ExposureInformation
import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import kotlinx.coroutines.tasks.await
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import java.io.File
import java.lang.Exception

class GoogleExposureNotificationApi(context: Context) : ExposureNotificationApi {

    private val exposureNotificationClient = Nearby.getExposureNotificationClient(context)

    override suspend fun isEnabled(): Boolean {
        return try {
            exposureNotificationClient.isEnabled.await()
        } catch (exception: Exception) {
            false
        }
    }

    override suspend fun start() {
        exposureNotificationClient.start().await()
    }

    override suspend fun stop() {
        try {
            if (exposureNotificationClient.isEnabled.await()) {
                exposureNotificationClient.stop().await()
            }
        } catch (exception: Exception) {
        }
    }

    override suspend fun temporaryExposureKeyHistory(): List<NHSTemporaryExposureKey> =
        exposureNotificationClient.temporaryExposureKeyHistory.await()
            .map { it.toNHSTemporaryExposureKey() }

    override suspend fun provideDiagnosisKeys(
        files: List<File>,
        exposureConfiguration: ExposureConfiguration,
        token: String
    ) {
        exposureNotificationClient.provideDiagnosisKeys(files, exposureConfiguration, token).await()
    }

    override suspend fun getExposureInformation(token: String): List<ExposureInformation> =
        exposureNotificationClient.getExposureInformation(token).await()

    override suspend fun getExposureSummary(token: String): ExposureSummary =
        exposureNotificationClient.getExposureSummary(token).await()

    private fun TemporaryExposureKey.toNHSTemporaryExposureKey(): NHSTemporaryExposureKey =
        NHSTemporaryExposureKey(
            key = Base64.encodeToString(keyData, Base64.NO_WRAP),
            rollingStartNumber = rollingStartIntervalNumber,
            rollingPeriod = rollingPeriod
        )
}
