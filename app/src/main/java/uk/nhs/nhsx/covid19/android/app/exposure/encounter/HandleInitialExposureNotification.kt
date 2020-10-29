package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.annotation.WorkerThread
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.NO
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.PENDING
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.YES
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandleInitialExposureNotification.InitialCircuitBreakerResult.Pending
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.ExposureRiskManagerProvider
import uk.nhs.nhsx.covid19.android.app.remote.ExposureCircuitBreakerApi
import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureCircuitBreakerRequest
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class HandleInitialExposureNotification @Inject constructor(
    private val exposureCircuitBreakerApi: ExposureCircuitBreakerApi,
    private val exposureRiskManagerProvider: ExposureRiskManagerProvider
) {
    @WorkerThread
    suspend operator fun invoke(token: String): Result<InitialCircuitBreakerResult> =
        runSafely {
            val riskManager = exposureRiskManagerProvider.riskManager()

            riskManager.getRisk(token)?.let { riskyExposureInfo ->
                val response = exposureCircuitBreakerApi
                    .submitExposureInfo(
                        ExposureCircuitBreakerRequest(
                            maximumRiskScore = riskyExposureInfo.calculatedRisk,
                            daysSinceLastExposure = Instant.ofEpochMilli(riskyExposureInfo.startOfDayMillis)
                                .until(Instant.now(), ChronoUnit.DAYS).toInt(),
                            matchedKeyCount = 1,
                            riskCalculationVersion = riskManager.getRiskCalculationVersion()
                        )
                    )

                return@runSafely when (CircuitBreakerResult.valueOf(response.approval.toUpperCase())) {
                    YES -> InitialCircuitBreakerResult.Yes(riskyExposureInfo.startOfDayMillis)
                    NO -> InitialCircuitBreakerResult.No
                    PENDING -> Pending(riskyExposureInfo.startOfDayMillis)
                }
            } ?: run {
                Timber.d("Not risky encounter with token: $token")
                InitialCircuitBreakerResult.No
            }
        }

    sealed class InitialCircuitBreakerResult {
        data class Yes(val exposureDate: Long) : InitialCircuitBreakerResult()
        object No : InitialCircuitBreakerResult()
        data class Pending(val exposureDate: Long) : InitialCircuitBreakerResult()
    }
}
