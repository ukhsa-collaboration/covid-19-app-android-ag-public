package uk.nhs.nhsx.covid19.android.app.exposure

import android.content.Context
import android.util.Base64
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.exposurenotification.DiagnosisKeysDataMapping
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationStatus
import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import java.io.File

class GoogleExposureNotificationApi(context: Context) : ExposureNotificationApi {

    private val exposureNotificationClient = Nearby.getExposureNotificationClient(context)

    override suspend fun isEnabled(): Boolean {
        return try {
            exposureNotificationClient.isEnabled.await()
        } catch (exception: Exception) {
            Timber.e(exception, "Can't get isEnabled")
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

    override suspend fun version(): Long? {
        return try {
            exposureNotificationClient.version.await()
        } catch (exception: Exception) {
            Timber.e(exception, "Can't get version")
            null
        }
    }

    override suspend fun isRunningNormally(): Boolean {
        return try {
            withTimeout(API_TIMEOUT) {
                val status = exposureNotificationClient.status.await()
                Timber.d("Status: $status")
                status.contains(ExposureNotificationStatus.ACTIVATED)
            }
        } catch (exception: Exception) {
            Timber.e(exception, "Can't get status")
            false
        }
    }

    override suspend fun temporaryExposureKeyHistory(): List<NHSTemporaryExposureKey> =
        exposureNotificationClient.temporaryExposureKeyHistory.await()
            .map { it.toNHSTemporaryExposureKey() }
            .apply {
                Timber.d("Initial keys: $this")
            }

    override suspend fun provideDiagnosisKeys(files: List<File>) {
        exposureNotificationClient.provideDiagnosisKeys(files).await()
    }

    override suspend fun getExposureWindows(): List<ExposureWindow> =
        exposureNotificationClient.exposureWindows.await()

    override suspend fun getDiagnosisKeysDataMapping(): DiagnosisKeysDataMapping =
        exposureNotificationClient.diagnosisKeysDataMapping.await()

    override fun setDiagnosisKeysDataMapping(dataMapping: DiagnosisKeysDataMapping) {
        exposureNotificationClient.setDiagnosisKeysDataMapping(dataMapping)
    }

    override suspend fun isAvailable(): Boolean {
        return try {
            exposureNotificationClient.start()
            true
        } catch (apiException: ApiException) {
            apiException.status.hasResolution()
        } catch (exception: Exception) {
            false
        }
    }

    override fun deviceSupportsLocationlessScanning(): Boolean {
        return exposureNotificationClient.deviceSupportsLocationlessScanning()
    }

    private fun TemporaryExposureKey.toNHSTemporaryExposureKey(): NHSTemporaryExposureKey =
        NHSTemporaryExposureKey(
            key = Base64.encodeToString(keyData, Base64.NO_WRAP),
            rollingStartNumber = rollingStartIntervalNumber,
            rollingPeriod = rollingPeriod
        )

    companion object {
        const val API_TIMEOUT = 10_000L
    }
}
