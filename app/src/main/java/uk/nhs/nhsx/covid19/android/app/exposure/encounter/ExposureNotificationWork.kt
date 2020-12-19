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
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandleInitialExposureNotification.InitialCircuitBreakerResult.Skipped
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandleInitialExposureNotification.InitialCircuitBreakerResult.Yes
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandlePollingExposureNotification.PollingCircuitBreakerResult
import uk.nhs.nhsx.covid19.android.app.remote.EmptyApi
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionSource.CIRCUIT_BREAKER
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionSource.EXPOSURE_WINDOW
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnExposedNotification
import uk.nhs.nhsx.covid19.android.app.testordering.SubmitFakeExposureWindows
import java.time.Instant
import javax.inject.Inject
import uk.nhs.nhsx.covid19.android.app.payment.CheckIsolationPaymentToken

class ExposureNotificationWork @Inject constructor(
    private val exposureNotificationTokensProvider: ExposureNotificationTokensProvider,
    private val handleInitialExposureNotification: HandleInitialExposureNotification,
    private val handlePollingExposureNotification: HandlePollingExposureNotification,
    private val stateMachine: IsolationStateMachine,
    private val emptyApi: EmptyApi,
    private val submitFakeExposureWindows: SubmitFakeExposureWindows,
    private val checkIsolationPaymentToken: CheckIsolationPaymentToken
) {

    suspend fun handleNoMatchesFound(): Result<Unit> {
        submitEmptyCircuitBreaker()
        submitEmptyExposureWindows()
        return Success(Unit)
    }

    suspend fun handleMatchesFound(tokenToCheck: String? = null): Result<Unit> =
        withContext(Dispatchers.IO) {
            runSafely {
                val allTokens = exposureNotificationTokensProvider.tokens
                val exposureNotificationTokens = if (tokenToCheck != null) {
                    val tokenInfo = allTokens.firstOrNull { it.token == tokenToCheck }
                    listOfNotNull(tokenInfo)
                } else {
                    allTokens
                }

                Timber.d("Number of exposure tokens to handle: ${exposureNotificationTokens.size}")

                exposureNotificationTokens.forEach {
                    Timber.d("Exposure notification token info: $it")
                    when (it.exposureDate) {
                        null -> {
                            handleInitial(it.token)
                        }
                        else -> {
                            handlePolling(it.token, it.exposureDate)
                        }
                    }
                }
            }
                .apply {
                    checkIsolationPaymentToken()
                }
        }

    private suspend fun handleInitial(
        token: String
    ) {
        val result = handleInitialExposureNotification(token)
        Timber.d("Handle initial circuit breaker result: $result for token $token")
        handleInitialResult(result, token)
    }

    private suspend fun handleInitialResult(
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
                Skipped -> {
                    clearToken(token)
                    submitEmptyCircuitBreaker()
                }
            }
            is Failure -> Timber.e(result.throwable)
        }
    }

    private suspend fun submitEmptyCircuitBreaker() {
        withContext(Dispatchers.IO) {
            runSafely {
                emptyApi.submit(EmptySubmissionRequest(CIRCUIT_BREAKER))
            }
        }
    }

    private suspend fun submitEmptyExposureWindows() {
        withContext(Dispatchers.IO) {
            runSafely {
                submitFakeExposureWindows(EXPOSURE_WINDOW, 0)
            }
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
}
