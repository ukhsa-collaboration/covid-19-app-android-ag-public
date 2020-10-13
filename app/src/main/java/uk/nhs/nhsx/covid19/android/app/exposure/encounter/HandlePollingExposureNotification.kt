package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.annotation.WorkerThread
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.NO
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.PENDING
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.YES
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.remote.ExposureCircuitBreakerApi
import javax.inject.Inject

class HandlePollingExposureNotification @Inject constructor(
    private val exposureCircuitBreakerApi: ExposureCircuitBreakerApi
) {

    @WorkerThread
    suspend operator fun invoke(approvalToken: String): Result<PollingCircuitBreakerResult> =
        runSafely {

            val response =
                exposureCircuitBreakerApi.getExposureCircuitBreakerResolution(approvalToken = approvalToken)

            return@runSafely when (CircuitBreakerResult.valueOf(response.approval.toUpperCase())) {
                YES -> PollingCircuitBreakerResult.Yes
                NO -> PollingCircuitBreakerResult.No
                PENDING -> PollingCircuitBreakerResult.Pending
            }
        }

    sealed class PollingCircuitBreakerResult {
        object Yes : PollingCircuitBreakerResult()
        object No : PollingCircuitBreakerResult()
        object Pending : PollingCircuitBreakerResult()
    }
}
