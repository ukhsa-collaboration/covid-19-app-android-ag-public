package uk.nhs.nhsx.covid19.android.app.exposure

import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import com.google.android.gms.nearby.exposurenotification.ExposureInformation
import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import java.io.File

interface ExposureNotificationApi {
    suspend fun isEnabled(): Boolean
    suspend fun start()
    suspend fun stop()
    suspend fun temporaryExposureKeyHistory(): List<NHSTemporaryExposureKey>
    suspend fun provideDiagnosisKeys(files: List<File>, exposureConfiguration: ExposureConfiguration, token: String)
    suspend fun getExposureInformation(token: String): List<ExposureInformation>
    suspend fun getExposureSummary(token: String): ExposureSummary
    suspend fun isAvailable(): Boolean
}
