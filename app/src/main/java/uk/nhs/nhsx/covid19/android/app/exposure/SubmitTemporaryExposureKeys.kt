package uk.nhs.nhsx.covid19.android.app.exposure

import com.google.android.gms.common.ConnectionResult.RESOLUTION_REQUIRED
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.covid19.android.app.common.PeriodicTasks
import uk.nhs.nhsx.covid19.android.app.exposure.SubmitTemporaryExposureKeys.SubmitResult.Failure
import uk.nhs.nhsx.covid19.android.app.exposure.SubmitTemporaryExposureKeys.SubmitResult.ResolutionRequired
import uk.nhs.nhsx.covid19.android.app.exposure.SubmitTemporaryExposureKeys.SubmitResult.Success
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.remote.data.TemporaryExposureKeysPayload
import uk.nhs.nhsx.covid19.android.app.testordering.LatestTestResultProvider
import java.time.LocalDate
import javax.inject.Inject

class SubmitTemporaryExposureKeys @Inject constructor(
    private val exposureNotificationApi: ExposureNotificationApi,
    private val periodicTasks: PeriodicTasks,
    private val latestTestResultProvider: LatestTestResultProvider
) {

    suspend operator fun invoke(dateWindow: DateWindow? = null): SubmitResult =
        withContext(Dispatchers.IO) {

            runCatching {
                val diagnosisKeySubmissionToken =
                    latestTestResultProvider.latestTestResult?.diagnosisKeySubmissionToken

                requireNotNull(diagnosisKeySubmissionToken) {
                    "Can not submit keys without diagnosisKeySubmissionToken from virology test result"
                }

                val keys: List<NHSTemporaryExposureKey> =
                    exposureNotificationApi.temporaryExposureKeyHistory()

                val keysInWindow = filterKeysInWindow(dateWindow, keys)

                val payload = TemporaryExposureKeysPayload(
                    diagnosisKeySubmissionToken = diagnosisKeySubmissionToken,
                    temporaryExposureKeys = keysInWindow
                )

                periodicTasks.scheduleKeysSubmission(payload)
            }.fold(
                onFailure = { t ->
                    when (t) {
                        is ApiException -> handleApiException(t)
                        else -> Failure(t)
                    }
                },
                onSuccess = { Success }
            )
        }

    private fun handleApiException(apiException: ApiException): SubmitResult =
        if (apiException.statusCode == RESOLUTION_REQUIRED) ResolutionRequired(apiException.status)
        else Failure(apiException)

    data class DateWindow(val fromInclusive: LocalDate, val toInclusive: LocalDate)

    sealed class SubmitResult {
        object Success : SubmitResult()
        data class Failure(val throwable: Throwable) : SubmitResult()
        data class ResolutionRequired(val status: Status) : SubmitResult()
    }
}
