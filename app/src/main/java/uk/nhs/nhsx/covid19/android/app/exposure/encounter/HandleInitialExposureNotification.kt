package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.annotation.WorkerThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.NO
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.PENDING
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.YES
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.remote.ExposureCircuitBreakerApi
import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureCircuitBreakerRequest
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS
import javax.inject.Inject

class HandleInitialExposureNotification @Inject constructor(
    private val exposureCircuitBreakerApi: ExposureCircuitBreakerApi,
    private val clock: Clock
) {

    @WorkerThread
    suspend operator fun invoke(exposureCircuitBreakerInfo: ExposureCircuitBreakerInfo): Result<InitialCircuitBreakerResult> =
        withContext(Dispatchers.IO) {
            runSafely {
                val response = exposureCircuitBreakerApi.submitExposureInfo(
                    exposureCircuitBreakerInfo.toExposureCircuitBreakerRequest()
                )

                when (response.approval) {
                    YES -> InitialCircuitBreakerResult.Yes
                    NO -> InitialCircuitBreakerResult.No
                    PENDING -> InitialCircuitBreakerResult.Pending(response.approvalToken)
                }
            }
        }

    private fun ExposureCircuitBreakerInfo.toExposureCircuitBreakerRequest() =
        ExposureCircuitBreakerRequest(
            maximumRiskScore = maximumRiskScore,
            daysSinceLastExposure = Instant.ofEpochMilli(startOfDayMillis)
                .until(Instant.now(clock), DAYS).toInt(),
            matchedKeyCount = matchedKeyCount,
            riskCalculationVersion = riskCalculationVersion
        )

    sealed class InitialCircuitBreakerResult {
        object Yes : InitialCircuitBreakerResult()
        object No : InitialCircuitBreakerResult()
        data class Pending(val approvalToken: String) : InitialCircuitBreakerResult()
    }
}
