package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.common.Result.Failure
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandleInitialExposureNotification.InitialCircuitBreakerResult
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandleInitialExposureNotification.InitialCircuitBreakerResult.No
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandleInitialExposureNotification.InitialCircuitBreakerResult.Pending
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandleInitialExposureNotification.InitialCircuitBreakerResult.Yes
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandlePollingExposureNotification.PollingCircuitBreakerResult
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnExposedNotification
import java.time.Instant
import javax.inject.Inject
import javax.inject.Provider

class ExposureNotificationWork @Inject constructor(
    private val exposureNotificationTokensProvider: ExposureNotificationTokensProvider,
    private val handleInitialExposureNotification: HandleInitialExposureNotification,
    private val handlePollingExposureNotification: HandlePollingExposureNotification,
    private val stateMachine: IsolationStateMachine,
    private val potentialExposureExplanationHandler: Provider<PotentialExposureExplanationHandler>,
    private val exposureNotificationApi: ExposureNotificationApi
) {

    suspend operator fun invoke(): Result<Unit> = withContext(Dispatchers.IO) {
        val potentialExposureExplanationHandler = potentialExposureExplanationHandler.get()
        var checkingInitial = false
        runSafely {
            val exposureNotificationTokens = exposureNotificationTokensProvider.tokens

            Timber.d("Number of exposure tokens to handle: ${exposureNotificationTokens.size}")

            exposureNotificationTokens.forEach {
                Timber.d("Exposure notification token info: $it")
                when (it.exposureDate) {
                    null -> {
                        checkingInitial = true
                        handleInitial(it.token, potentialExposureExplanationHandler)
                    }
                    else -> {
                        checkingInitial = false
                        handlePolling(it.token, it.exposureDate)
                    }
                }
            }
        }
            .apply {
                if (is1dot5ENVersion()) {
                    if (this is Failure && checkingInitial) {
                        potentialExposureExplanationHandler.addResult(result = this)
                    }
                    potentialExposureExplanationHandler.showNotificationIfNeeded()
                }
            }
    }

    private suspend fun handleInitial(
        token: String,
        potentialExposureExplanationHandler: PotentialExposureExplanationHandler
    ) {
        val result = handleInitialExposureNotification.invoke(token)
        Timber.d("Handle initial circuit breaker result: $result for token $token")
        handleInitialResult(result, token)
        if (is1dot5ENVersion()) {
            potentialExposureExplanationHandler.addResult(result)
        }
    }

    private fun handleInitialResult(
        result: Result<InitialCircuitBreakerResult>,
        token: String
    ) {
        when (result) {
            is Success -> when (result.value) {
                is Yes -> handleYes(token, result.value.exposureDate)
                No -> clearToken(token)
                is Pending -> updateToPolling(
                    token,
                    result.value.exposureDate
                )
            }
            is Failure -> Timber.e(result.throwable)
        }
    }

    private suspend fun handlePolling(token: String, exposureDate: Long) {
        when (val result = handlePollingExposureNotification.invoke(token)) {
            is Success -> when (result.value) {
                PollingCircuitBreakerResult.Yes -> handleYes(token, exposureDate)
                PollingCircuitBreakerResult.No -> clearToken(token)
                PollingCircuitBreakerResult.Pending -> Unit
            }
            is Failure -> Timber.e(result.throwable)
        }
    }

    private fun updateToPolling(token: String, exposureDate: Long) {
        Timber.d("Circuit breaker answered POLLING, update token ($token, exposureDate=$exposureDate)")
        exposureNotificationTokensProvider.updateToPolling(token, exposureDate)
    }

    private fun handleYes(token: String, exposureDate: Long) {
        Timber.d("Circuit breaker answered YES for token $token, exposureDate=$exposureDate")
        stateMachine.processEvent(OnExposedNotification(Instant.ofEpochMilli(exposureDate)))
        clearToken(token)
    }

    private fun clearToken(token: String) {
        Timber.d("Circuit breaker answered YES or NO, remove exposure notification token: $token")
        exposureNotificationTokensProvider.remove(token)
    }

    private suspend fun is1dot5ENVersion() = exposureNotificationApi.version() == null
}
