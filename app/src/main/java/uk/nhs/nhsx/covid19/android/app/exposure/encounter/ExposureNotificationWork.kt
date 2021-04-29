package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.common.SubmitEmptyData
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.ExposureWindowRiskManager
import uk.nhs.nhsx.covid19.android.app.payment.CheckIsolationPaymentToken
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExposureNotificationWork @Inject constructor(
    private val submitEmptyData: SubmitEmptyData,
    private val checkIsolationPaymentToken: CheckIsolationPaymentToken,
    private val exposureCircuitBreaker: ExposureCircuitBreaker,
    private val exposureCircuitBreakerInfoProvider: ExposureCircuitBreakerInfoProvider,
    private val exposureWindowRiskManager: ExposureWindowRiskManager,
    private val epidemiologyDataManager: EpidemiologyDataManager,
    private val hasSuccessfullyProcessedNewExposureProvider: HasSuccessfullyProcessedNewExposureProvider,
    private val clock: Clock
) {

    private val mutex = Mutex()

    suspend fun handleNoMatchesFound(): Result<Unit> = mutex.withLock {
        submitEmptyData()
        epidemiologyDataManager.submitEmptyExposureWindows()
        return Success(Unit)
    }

    suspend fun handleNewExposure(): Result<Unit> = mutex.withLock {
        withContext(Dispatchers.IO) {
            hasSuccessfullyProcessedNewExposureProvider.value = false

            runSafely {
                val riskCalculationResult = exposureWindowRiskManager.getRisk()

                val relevantRisk = riskCalculationResult.relevantRisk

                val exposureCircuitBreakerInfo = if (relevantRisk == null) {
                    Timber.d("Not a risky encounter")
                    submitEmptyData()
                    null
                } else {
                    ExposureCircuitBreakerInfo(
                        maximumRiskScore = relevantRisk.calculatedRisk,
                        startOfDayMillis = relevantRisk.startOfDayMillis,
                        matchedKeyCount = relevantRisk.matchedKeyCount,
                        riskCalculationVersion = relevantRisk.riskCalculationVersion,
                        exposureNotificationDate = Instant.now(clock).toEpochMilli()
                    ).also {
                        exposureCircuitBreakerInfoProvider.add(it)
                    }
                }

                hasSuccessfullyProcessedNewExposureProvider.value = true

                epidemiologyDataManager.storeAndSubmit(riskCalculationResult.partitionedExposureWindows)

                if (exposureCircuitBreakerInfo != null) {
                    exposureCircuitBreaker.handleInitial(exposureCircuitBreakerInfo)
                }
            }
                .apply {
                    checkIsolationPaymentToken()
                }
        }
    }

    suspend fun handleUnprocessedRequests(): Result<Unit> = mutex.withLock {
        withContext(Dispatchers.IO) {
            runSafely {
                exposureCircuitBreakerInfoProvider.info.forEach { info ->
                    Timber.d("Exposure circuit breaker info: $info")
                    info.approvalToken?.let {
                        exposureCircuitBreaker.handlePolling(info)
                    } ?: exposureCircuitBreaker.handleInitial(info)
                }
            }
                .apply {
                    checkIsolationPaymentToken()
                }
        }
    }
}
