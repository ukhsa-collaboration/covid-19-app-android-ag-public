package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedRiskyContactNotification
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.common.Result.Failure
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandleInitialExposureNotification.InitialCircuitBreakerResult.No
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandleInitialExposureNotification.InitialCircuitBreakerResult.Pending
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandleInitialExposureNotification.InitialCircuitBreakerResult.Yes
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandlePollingExposureNotification.PollingCircuitBreakerResult
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnExposedNotification
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExposureCircuitBreaker @Inject constructor(
    private val handleInitialExposureNotification: HandleInitialExposureNotification,
    private val handlePollingExposureNotification: HandlePollingExposureNotification,
    private val stateMachine: IsolationStateMachine,
    private val exposureCircuitBreakerInfoProvider: ExposureCircuitBreakerInfoProvider,
    private val analyticsEventProcessor: AnalyticsEventProcessor
) {

    suspend fun handleInitial(info: ExposureCircuitBreakerInfo) {
        when (val result = handleInitialExposureNotification(info)) {
            is Success -> when (result.value) {
                Yes -> handleYes(info)
                No -> handleNo(info)
                is Pending -> exposureCircuitBreakerInfoProvider.setApprovalToken(info, result.value.approvalToken)
            }
            is Failure -> Timber.e(result.throwable)
        }
    }

    suspend fun handlePolling(info: ExposureCircuitBreakerInfo) {
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
}
