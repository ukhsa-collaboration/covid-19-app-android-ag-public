package uk.nhs.nhsx.covid19.android.app.exposure

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.covid19.android.app.exposure.FetchTemporaryExposureKeys.TemporaryExposureKeysFetchResult.Failure
import uk.nhs.nhsx.covid19.android.app.exposure.FetchTemporaryExposureKeys.TemporaryExposureKeysFetchResult.ResolutionRequired
import uk.nhs.nhsx.covid19.android.app.exposure.FetchTemporaryExposureKeys.TemporaryExposureKeysFetchResult.Success
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.KeySharingInfo
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import javax.inject.Inject

class FetchTemporaryExposureKeys @Inject constructor(
    private val exposureNotificationApi: ExposureNotificationApi,
    private val transmissionRiskLevelApplier: TransmissionRiskLevelApplier
) {

    suspend operator fun invoke(keySharingInfo: KeySharingInfo?): TemporaryExposureKeysFetchResult =
        withContext(Dispatchers.IO) {
            if (keySharingInfo == null) {
                runCatching {
                    exposureNotificationApi.temporaryExposureKeyHistory()
                }.fold(
                    onFailure = { t ->
                        when (t) {
                            is ApiException -> handleApiException(t)
                            else -> Failure(t)
                        }
                    },
                    onSuccess = { Success(it) }
                )
            } else {
                runCatching {
                    val keys: List<NHSTemporaryExposureKey> =
                        exposureNotificationApi.temporaryExposureKeyHistory()

                    transmissionRiskLevelApplier.applyTransmissionRiskLevels(keys, keySharingInfo)
                        .filter {
                            it.transmissionRiskLevel != null &&
                                    it.transmissionRiskLevel > MIN_TRANSMISSION_RISK_LEVEL &&
                                    it.rollingPeriod == 144
                        }
                }.fold(
                    onFailure = { t ->
                        when (t) {
                            is ApiException -> handleApiException(t)
                            else -> Failure(t)
                        }
                    },
                    onSuccess = { Success(it) }
                )
            }
        }

    private fun handleApiException(apiException: ApiException): TemporaryExposureKeysFetchResult =
        if (apiException.statusCode == ConnectionResult.RESOLUTION_REQUIRED) {
            ResolutionRequired(apiException.status)
        } else {
            Failure(apiException)
        }

    sealed class TemporaryExposureKeysFetchResult {
        data class Success(val temporaryExposureKeys: List<NHSTemporaryExposureKey>) :
            TemporaryExposureKeysFetchResult()

        data class Failure(val throwable: Throwable) : TemporaryExposureKeysFetchResult()
        data class ResolutionRequired(val status: Status) : TemporaryExposureKeysFetchResult()
    }
}
