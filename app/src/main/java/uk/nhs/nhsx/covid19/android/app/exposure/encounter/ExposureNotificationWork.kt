package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.common.Result.Failure
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.common.runSafely
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
    private val potentialExposureExplanationHandler: Provider<PotentialExposureExplanationHandler>
) {

    suspend operator fun invoke(): Result<Unit> = withContext(Dispatchers.IO) {
        val potentialExposureExplanationHandler = potentialExposureExplanationHandler.get()
        var checkingInitial = false
        runSafely {
            val exposureNotificationTokens = exposureNotificationTokensProvider.tokens

            Timber.d("Number of exposure tokens to handle: ${exposureNotificationTokens.size}")

            exposureNotificationTokens.forEach {
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
                if (this is Failure && checkingInitial) {
                    potentialExposureExplanationHandler.addResult(this)
                }
                potentialExposureExplanationHandler.showNotificationIfNeeded()
            }
    }

    private suspend fun handleInitial(
        token: String,
        potentialExposureExplanationHandler: PotentialExposureExplanationHandler
    ) {
        val result = handleInitialExposureNotification.invoke(token)
        handleInitialResult(result, token)
        potentialExposureExplanationHandler.addResult(result)
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
        exposureNotificationTokensProvider.updateToPolling(token, exposureDate)
    }

    private fun handleYes(token: String, exposureDate: Long) {
        stateMachine.processEvent(OnExposedNotification(Instant.ofEpochMilli(exposureDate)))
        clearToken(token)
    }

    private fun clearToken(token: String) {
        exposureNotificationTokensProvider.remove(token)
    }
}
