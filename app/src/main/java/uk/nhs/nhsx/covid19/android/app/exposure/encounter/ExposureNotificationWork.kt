package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import com.jeroenmols.featureflag.framework.FeatureFlag.STORE_EXPOSURE_WINDOWS
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedRiskyContactNotification
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.common.Result.Failure
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.common.SubmitEmptyData
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandleInitialExposureNotification.InitialCircuitBreakerResult.No
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandleInitialExposureNotification.InitialCircuitBreakerResult.Pending
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandleInitialExposureNotification.InitialCircuitBreakerResult.Yes
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandlePollingExposureNotification.PollingCircuitBreakerResult
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.EpidemiologyEvent
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.EpidemiologyEventProvider
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.ExposureWindowRiskManager
import uk.nhs.nhsx.covid19.android.app.payment.CheckIsolationPaymentToken
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionSource.CIRCUIT_BREAKER
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionSource.EXPOSURE_WINDOW
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnExposedNotification
import uk.nhs.nhsx.covid19.android.app.testordering.SubmitFakeExposureWindows
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExposureNotificationWork @Inject constructor(
    private val handleInitialExposureNotification: HandleInitialExposureNotification,
    private val handlePollingExposureNotification: HandlePollingExposureNotification,
    private val stateMachine: IsolationStateMachine,
    private val submitEmptyData: SubmitEmptyData,
    private val submitFakeExposureWindows: SubmitFakeExposureWindows,
    private val submitEpidemiologyData: SubmitEpidemiologyData,
    private val checkIsolationPaymentToken: CheckIsolationPaymentToken,
    private val exposureCircuitBreakerInfoProvider: ExposureCircuitBreakerInfoProvider,
    private val exposureWindowRiskManager: ExposureWindowRiskManager,
    private val epidemiologyEventProvider: EpidemiologyEventProvider,
    private val analyticsEventProcessor: AnalyticsEventProcessor,
    private val hasSuccessfullyProcessedNewExposureProvider: HasSuccessfullyProcessedNewExposureProvider,
    private val clock: Clock
) {

    private val mutex = Mutex()

    suspend fun handleNoMatchesFound(): Result<Unit> = mutex.withLock {
        submitEmptyData(CIRCUIT_BREAKER)
        submitEmptyExposureWindows()
        return Success(Unit)
    }

    suspend fun handleNewExposure(): Result<Unit> = mutex.withLock {
        withContext(Dispatchers.IO) {
            hasSuccessfullyProcessedNewExposureProvider.value = false

            runSafely {
                val riskCalculationResult = exposureWindowRiskManager.getRisk()

                val relevantRisk = riskCalculationResult.relevantRisk
                if (relevantRisk == null) {
                    Timber.d("Not a risky encounter")
                    submitEmptyData(CIRCUIT_BREAKER)
                    submitEmptyExposureWindows()
                    hasSuccessfullyProcessedNewExposureProvider.value = true
                    return@runSafely
                }

                val exposureCircuitBreakerInfo = ExposureCircuitBreakerInfo(
                    maximumRiskScore = relevantRisk.calculatedRisk,
                    startOfDayMillis = relevantRisk.startOfDayMillis,
                    matchedKeyCount = relevantRisk.matchedKeyCount,
                    riskCalculationVersion = relevantRisk.riskCalculationVersion,
                    exposureNotificationDate = Instant.now(clock).toEpochMilli()
                )

                exposureCircuitBreakerInfoProvider.add(exposureCircuitBreakerInfo)

                hasSuccessfullyProcessedNewExposureProvider.value = true

                val epidemiologyEvents = riskCalculationResult.exposureWindowsWithRisk.map { it.toEpidemiologyEvent() }
                storeEpidemiologyEvents(epidemiologyEvents)
                submitEpidemiologyData.submit(epidemiologyEvents)

                handleInitial(exposureCircuitBreakerInfo)
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
                        handlePolling(info)
                    } ?: handleInitial(info)
                }
            }
                .apply {
                    checkIsolationPaymentToken()
                }
        }
    }

    private fun storeEpidemiologyEvents(epidemiologyEvents: List<EpidemiologyEvent>) {
        if (RuntimeBehavior.isFeatureEnabled(STORE_EXPOSURE_WINDOWS) && epidemiologyEvents.isNotEmpty()) {
            epidemiologyEventProvider.add(epidemiologyEvents)
        }
    }

    private suspend fun handleInitial(info: ExposureCircuitBreakerInfo) {
        when (val result = handleInitialExposureNotification(info)) {
            is Success -> when (result.value) {
                Yes -> handleYes(info)
                No -> handleNo(info)
                is Pending -> exposureCircuitBreakerInfoProvider.setApprovalToken(info, result.value.approvalToken)
            }
            is Failure -> Timber.e(result.throwable)
        }
    }

    private suspend fun handlePolling(info: ExposureCircuitBreakerInfo) {
        when (val result = handlePollingExposureNotification(info.approvalToken!!)) {
            is Success -> when (result.value) {
                PollingCircuitBreakerResult.Yes -> handleYes(info)
                PollingCircuitBreakerResult.No -> handleNo(info)
                PollingCircuitBreakerResult.Pending -> Unit
            }
            is Failure -> Timber.e(result.throwable)
        }
    }

    private suspend fun handleYes(info: ExposureCircuitBreakerInfo) {
        Timber.d("Circuit breaker answered YES, exposureDate=${info.startOfDayMillis}")
        stateMachine.processEvent(OnExposedNotification(Instant.ofEpochMilli(info.startOfDayMillis)))
        exposureCircuitBreakerInfoProvider.remove(info)
        analyticsEventProcessor.track(ReceivedRiskyContactNotification)
    }

    private fun handleNo(info: ExposureCircuitBreakerInfo) {
        Timber.d("Circuit breaker answered NO, remove exposure circuit breaker info: ${info.approvalToken}")
        exposureCircuitBreakerInfoProvider.remove(info)
    }

    private suspend fun submitEmptyExposureWindows() {
        withContext(Dispatchers.IO) {
            runSafely {
                submitFakeExposureWindows(EXPOSURE_WINDOW)
            }
        }
    }
}
