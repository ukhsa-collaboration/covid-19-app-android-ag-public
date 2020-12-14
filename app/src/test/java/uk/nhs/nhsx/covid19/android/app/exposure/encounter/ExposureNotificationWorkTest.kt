package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandleInitialExposureNotification.InitialCircuitBreakerResult
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandlePollingExposureNotification.PollingCircuitBreakerResult
import uk.nhs.nhsx.covid19.android.app.remote.EmptyApi
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionSource.CIRCUIT_BREAKER
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionSource.EXPOSURE_WINDOW
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.testordering.SubmitFakeExposureWindows
import java.time.Instant
import javax.inject.Provider
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import uk.nhs.nhsx.covid19.android.app.payment.CheckIsolationPaymentToken

class ExposureNotificationWorkTest {

    private val exposureNotificationTokensProvider = mockk<ExposureNotificationTokensProvider>(relaxed = true)
    private val handleInitialExposureNotification = mockk<HandleInitialExposureNotification>()
    private val handlePollingExposureNotification = mockk<HandlePollingExposureNotification>()
    private val stateMachine = mockk<IsolationStateMachine>(relaxed = true)
    private val potentialExposureExplanationHandlerProvider = mockk<Provider<PotentialExposureExplanationHandler>>()
    private val potentialExposureExplanationHandler = mockk<PotentialExposureExplanationHandler>(relaxUnitFun = true)
    private val exposureNotificationApi = mockk<ExposureNotificationApi>(relaxed = true)
    private val emptyApi = mockk<EmptyApi>(relaxed = true)
    private val submitFakeExposureWindows = mockk<SubmitFakeExposureWindows>(relaxed = true)
    private val checkIsolationPaymentToken = mockk<CheckIsolationPaymentToken>(relaxed = true)

    private val testSubject = ExposureNotificationWork(
        exposureNotificationTokensProvider,
        handleInitialExposureNotification,
        handlePollingExposureNotification,
        stateMachine,
        potentialExposureExplanationHandlerProvider,
        exposureNotificationApi,
        emptyApi,
        submitFakeExposureWindows,
        checkIsolationPaymentToken
    )

    @Before
    fun setUp() {
        every { potentialExposureExplanationHandlerProvider.get() } returns potentialExposureExplanationHandler
        coEvery { exposureNotificationApi.version() } returns null
    }

    @Test
    fun `no tokens return success`() = runBlocking {
        every { exposureNotificationTokensProvider.tokens } returns emptyList()

        val result = testSubject.handleMatchesFound()

        coVerify(exactly = 0) { handleInitialExposureNotification.invoke(any()) }
        coVerify(exactly = 0) { handlePollingExposureNotification.invoke(any()) }
        verify(exactly = 0) { stateMachine.processEvent(any()) }
        coVerify(exactly = 1) { checkIsolationPaymentToken.invoke() }
        verify(exactly = 0) { potentialExposureExplanationHandler.addResult(any()) }
        verify(exactly = 1) { potentialExposureExplanationHandler.showNotificationIfNeeded() }
        verify(exactly = 0) { submitFakeExposureWindows(any(), any()) }

        assertEquals(Result.Success(Unit), result)
    }

    @Test
    fun `no tokens return success for EN version higher than 1,6`() = runBlocking {
        every { exposureNotificationTokensProvider.tokens } returns emptyList()
        coEvery { exposureNotificationApi.version() } returns 170345

        val result = testSubject.handleMatchesFound()

        coVerify(exactly = 0) { handleInitialExposureNotification.invoke(any()) }
        coVerify(exactly = 0) { handlePollingExposureNotification.invoke(any()) }
        verify(exactly = 0) { stateMachine.processEvent(any()) }
        coVerify(exactly = 1) { checkIsolationPaymentToken.invoke() }
        verify(exactly = 0) { potentialExposureExplanationHandler.addResult(any()) }
        verify(exactly = 0) { potentialExposureExplanationHandler.showNotificationIfNeeded() }
        verify(exactly = 0) { submitFakeExposureWindows(any(), any()) }

        assertEquals(Result.Success(Unit), result)
    }

    @Test
    fun `two tokens for initial circuit breaker with yes response returns success`() = runBlocking {
        every { exposureNotificationTokensProvider.tokens } returns listOf(
            TokenInfo("token1", startedAt = Instant.now().toEpochMilli()),
            TokenInfo("token2", startedAt = Instant.now().toEpochMilli())
        )

        coEvery { handleInitialExposureNotification.invoke(any()) } returns Result.Success(
            InitialCircuitBreakerResult.Yes(123)
        )

        val result = testSubject.handleMatchesFound()

        coVerify(exactly = 2) { handleInitialExposureNotification.invoke(any()) }
        verify(exactly = 2) { stateMachine.processEvent(any()) }
        coVerify(exactly = 1) { checkIsolationPaymentToken.invoke() }
        coVerify(exactly = 0) { handlePollingExposureNotification.invoke(any()) }
        verify(exactly = 2) { exposureNotificationTokensProvider.remove(any()) }
        verify(exactly = 0) { exposureNotificationTokensProvider.updateToPolling(any(), any()) }
        verify(exactly = 2) { potentialExposureExplanationHandler.addResult(any()) }
        verify(exactly = 1) { potentialExposureExplanationHandler.showNotificationIfNeeded() }
        verify(exactly = 0) { submitFakeExposureWindows(any(), any()) }

        assertEquals(Result.Success(Unit), result)
    }

    @Test
    fun `one token for initial circuit breaker with pending response returns success`() =
        runBlocking {
            every { exposureNotificationTokensProvider.tokens } returns listOf(
                TokenInfo("token1", startedAt = Instant.now().toEpochMilli())
            )

            val pendingResult = Result.Success(
                InitialCircuitBreakerResult.Pending(123)
            )
            coEvery { handleInitialExposureNotification.invoke(any()) } returns pendingResult

            val result = testSubject.handleMatchesFound()

            coVerify(exactly = 1) { handleInitialExposureNotification.invoke(any()) }
            verify(exactly = 1) {
                exposureNotificationTokensProvider.updateToPolling(
                    "token1",
                    123
                )
            }
            coVerify(exactly = 0) { handlePollingExposureNotification.invoke(any()) }
            verify(exactly = 0) { stateMachine.processEvent(any()) }
            coVerify(exactly = 1) { checkIsolationPaymentToken.invoke() }
            verify(exactly = 0) { exposureNotificationTokensProvider.remove("token1") }
            val slot = slot<Result<InitialCircuitBreakerResult>>()
            verify(exactly = 1) { potentialExposureExplanationHandler.addResult(capture(slot)) }
            assertEquals(pendingResult, slot.captured)
            verify(exactly = 1) { potentialExposureExplanationHandler.showNotificationIfNeeded() }
            verify(exactly = 0) { submitFakeExposureWindows(any(), any()) }

            assertEquals(Result.Success(Unit), result)
        }

    @Test
    fun `one token for initial circuit breaker with no response returns success`() =
        runBlocking {
            every { exposureNotificationTokensProvider.tokens } returns listOf(
                TokenInfo("token1", startedAt = Instant.now().toEpochMilli())
            )

            val noResult = Result.Success(
                InitialCircuitBreakerResult.No
            )
            coEvery { handleInitialExposureNotification.invoke(any()) } returns noResult

            val result = testSubject.handleMatchesFound()

            coVerify(exactly = 1) { handleInitialExposureNotification.invoke(any()) }
            verify(exactly = 1) { exposureNotificationTokensProvider.remove("token1") }
            verify(exactly = 0) { exposureNotificationTokensProvider.updateToPolling(any(), any()) }
            coVerify(exactly = 0) { handlePollingExposureNotification.invoke(any()) }
            verify(exactly = 0) { stateMachine.processEvent(any()) }
            coVerify(exactly = 1) { checkIsolationPaymentToken.invoke() }
            val slot = slot<Result<InitialCircuitBreakerResult>>()
            verify(exactly = 1) { potentialExposureExplanationHandler.addResult(capture(slot)) }
            assertEquals(noResult, slot.captured)
            verify(exactly = 1) { potentialExposureExplanationHandler.showNotificationIfNeeded() }
            verify(exactly = 0) { submitFakeExposureWindows(any(), any()) }

            assertEquals(Result.Success(Unit), result)
        }

    @Test
    fun `one token for initial circuit breaker with no response returns success for EN version higher that 1,6`() =
        runBlocking {
            every { exposureNotificationTokensProvider.tokens } returns listOf(
                TokenInfo("token1", startedAt = Instant.now().toEpochMilli())
            )
            coEvery { exposureNotificationApi.version() } returns 17837

            val noResult = Result.Success(
                InitialCircuitBreakerResult.No
            )
            coEvery { handleInitialExposureNotification.invoke(any()) } returns noResult

            val result = testSubject.handleMatchesFound()

            coVerify(exactly = 1) { handleInitialExposureNotification.invoke(any()) }
            verify(exactly = 1) { exposureNotificationTokensProvider.remove("token1") }
            verify(exactly = 0) { exposureNotificationTokensProvider.updateToPolling(any(), any()) }
            coVerify(exactly = 0) { handlePollingExposureNotification.invoke(any()) }
            verify(exactly = 0) { stateMachine.processEvent(any()) }
            coVerify(exactly = 1) { checkIsolationPaymentToken.invoke() }
            verify(exactly = 0) { potentialExposureExplanationHandler.addResult(any()) }
            verify(exactly = 0) { potentialExposureExplanationHandler.showNotificationIfNeeded() }
            verify(exactly = 0) { submitFakeExposureWindows(any(), any()) }

            assertEquals(Result.Success(Unit), result)
        }

    @Test
    fun `one token for initial circuit breaker with skipped response returns success`() =
        runBlocking {
            every { exposureNotificationTokensProvider.tokens } returns listOf(
                TokenInfo("token1", startedAt = Instant.now().toEpochMilli())
            )

            val noResult = Result.Success(
                InitialCircuitBreakerResult.Skipped
            )
            coEvery { handleInitialExposureNotification.invoke(any()) } returns noResult

            val result = testSubject.handleMatchesFound()

            coVerify(exactly = 1) { handleInitialExposureNotification.invoke(any()) }
            verify(exactly = 1) { exposureNotificationTokensProvider.remove("token1") }
            coVerify(exactly = 1) { emptyApi.submit(EmptySubmissionRequest(CIRCUIT_BREAKER)) }
            verify(exactly = 0) { exposureNotificationTokensProvider.updateToPolling(any(), any()) }
            coVerify(exactly = 0) { handlePollingExposureNotification.invoke(any()) }
            verify(exactly = 0) { stateMachine.processEvent(any()) }
            coVerify(exactly = 1) { checkIsolationPaymentToken.invoke() }
            val slot = slot<Result<InitialCircuitBreakerResult>>()
            verify(exactly = 1) { potentialExposureExplanationHandler.addResult(capture(slot)) }
            assertEquals(noResult, slot.captured)
            verify(exactly = 1) { potentialExposureExplanationHandler.showNotificationIfNeeded() }
            verify(exactly = 0) { submitFakeExposureWindows(any(), any()) }

            assertEquals(Result.Success(Unit), result)
        }

    @Test
    fun `one token for initial circuit breaker with skipped response returns success for EN version higher that 1,6`() =
        runBlocking {
            every { exposureNotificationTokensProvider.tokens } returns listOf(
                TokenInfo("token1", startedAt = Instant.now().toEpochMilli())
            )
            coEvery { exposureNotificationApi.version() } returns 17837

            val noResult = Result.Success(
                InitialCircuitBreakerResult.Skipped
            )
            coEvery { handleInitialExposureNotification.invoke(any()) } returns noResult

            val result = testSubject.handleMatchesFound()

            coVerify(exactly = 1) { handleInitialExposureNotification.invoke(any()) }
            verify(exactly = 1) { exposureNotificationTokensProvider.remove("token1") }
            coVerify(exactly = 1) { emptyApi.submit(EmptySubmissionRequest(CIRCUIT_BREAKER)) }
            verify(exactly = 0) { exposureNotificationTokensProvider.updateToPolling(any(), any()) }
            coVerify(exactly = 0) { handlePollingExposureNotification.invoke(any()) }
            verify(exactly = 0) { stateMachine.processEvent(any()) }
            coVerify(exactly = 1) { checkIsolationPaymentToken.invoke() }
            verify(exactly = 0) { potentialExposureExplanationHandler.addResult(any()) }
            verify(exactly = 0) { potentialExposureExplanationHandler.showNotificationIfNeeded() }
            verify(exactly = 0) { submitFakeExposureWindows(any(), any()) }

            assertEquals(Result.Success(Unit), result)
        }

    @Test
    fun `one token for polling circuit breaker with yes response returns success`() = runBlocking {
        every { exposureNotificationTokensProvider.tokens } returns listOf(
            TokenInfo("token1", 123, startedAt = Instant.now().toEpochMilli())
        )

        coEvery { handlePollingExposureNotification.invoke(any()) } returns Result.Success(
            PollingCircuitBreakerResult.Yes
        )

        val result = testSubject.handleMatchesFound()

        coVerify(exactly = 0) { handleInitialExposureNotification.invoke(any()) }
        verify(exactly = 1) { stateMachine.processEvent(any()) }
        coVerify(exactly = 1) { handlePollingExposureNotification.invoke("token1") }
        verify(exactly = 1) { exposureNotificationTokensProvider.remove(any()) }
        verify(exactly = 0) { exposureNotificationTokensProvider.updateToPolling(any(), any()) }
        coVerify(exactly = 1) { checkIsolationPaymentToken.invoke() }
        verify(exactly = 0) { potentialExposureExplanationHandler.addResult(any()) }
        verify(exactly = 1) { potentialExposureExplanationHandler.showNotificationIfNeeded() }
        verify(exactly = 0) { submitFakeExposureWindows(any(), any()) }

        assertEquals(Result.Success(Unit), result)
    }

    @Test
    fun `one token for polling circuit breaker with pending response returns success`() =
        runBlocking {
            every { exposureNotificationTokensProvider.tokens } returns listOf(
                TokenInfo("token1", 123, startedAt = Instant.now().toEpochMilli())
            )

            coEvery { handlePollingExposureNotification.invoke(any()) } returns Result.Success(
                PollingCircuitBreakerResult.Pending
            )

            val result = testSubject.handleMatchesFound()

            coVerify(exactly = 1) { handlePollingExposureNotification.invoke("token1") }
            coVerify(exactly = 0) { handleInitialExposureNotification.invoke(any()) }
            verify(exactly = 0) { stateMachine.processEvent(any()) }
            verify(exactly = 0) { exposureNotificationTokensProvider.remove(any()) }
            verify(exactly = 0) { exposureNotificationTokensProvider.updateToPolling(any(), any()) }
            coVerify(exactly = 1) { checkIsolationPaymentToken.invoke() }
            verify(exactly = 0) { potentialExposureExplanationHandler.addResult(any()) }
            verify(exactly = 1) { potentialExposureExplanationHandler.showNotificationIfNeeded() }
            verify(exactly = 0) { submitFakeExposureWindows(any(), any()) }

            assertEquals(Result.Success(Unit), result)
        }

    @Test
    fun `one token for polling circuit breaker with no response returns success`() = runBlocking {
        every { exposureNotificationTokensProvider.tokens } returns listOf(
            TokenInfo("token1", 123, startedAt = Instant.now().toEpochMilli())
        )

        coEvery { handlePollingExposureNotification.invoke(any()) } returns Result.Success(
            PollingCircuitBreakerResult.No
        )

        val result = testSubject.handleMatchesFound()

        coVerify(exactly = 1) { handlePollingExposureNotification.invoke("token1") }
        coVerify(exactly = 0) { handleInitialExposureNotification.invoke(any()) }
        verify(exactly = 0) { stateMachine.processEvent(any()) }
        verify(exactly = 1) { exposureNotificationTokensProvider.remove(any()) }
        verify(exactly = 0) { exposureNotificationTokensProvider.updateToPolling(any(), any()) }
        coVerify(exactly = 1) { checkIsolationPaymentToken.invoke() }
        verify(exactly = 0) { potentialExposureExplanationHandler.addResult(any()) }
        verify(exactly = 1) { potentialExposureExplanationHandler.showNotificationIfNeeded() }
        verify(exactly = 0) { submitFakeExposureWindows(any(), any()) }

        assertEquals(Result.Success(Unit), result)
    }

    @Test
    fun `failure on polling circuit breaker return failure`() = runBlocking {
        val testException = Exception()
        every { exposureNotificationTokensProvider.tokens } returns listOf(
            TokenInfo("token1", 123, startedAt = Instant.now().toEpochMilli())
        )

        coEvery { handlePollingExposureNotification.invoke(any()) } throws testException

        val result = testSubject.handleMatchesFound()

        coVerify(exactly = 1) { handlePollingExposureNotification.invoke("token1") }
        coVerify(exactly = 0) { handleInitialExposureNotification.invoke(any()) }
        verify(exactly = 0) { stateMachine.processEvent(any()) }
        verify(exactly = 0) { exposureNotificationTokensProvider.remove(any()) }
        verify(exactly = 0) { exposureNotificationTokensProvider.updateToPolling(any(), any()) }
        coVerify(exactly = 1) { checkIsolationPaymentToken.invoke() }
        verify(exactly = 0) { potentialExposureExplanationHandler.addResult(any()) }
        verify(exactly = 1) { potentialExposureExplanationHandler.showNotificationIfNeeded() }
        verify(exactly = 0) { submitFakeExposureWindows(any(), any()) }

        assertTrue { result is Result.Failure }
    }

    @Test
    fun `failure on initial circuit breaker return failure`() = runBlocking {
        val testException = Exception()
        every { exposureNotificationTokensProvider.tokens } returns listOf(
            TokenInfo("token1", startedAt = Instant.now().toEpochMilli())
        )

        coEvery { handleInitialExposureNotification.invoke(any()) } throws testException

        val result = testSubject.handleMatchesFound()

        coVerify(exactly = 1) { handleInitialExposureNotification.invoke("token1") }
        coVerify(exactly = 0) { handlePollingExposureNotification.invoke(any()) }
        verify(exactly = 0) { stateMachine.processEvent(any()) }
        verify(exactly = 0) { exposureNotificationTokensProvider.remove(any()) }
        verify(exactly = 0) { exposureNotificationTokensProvider.updateToPolling(any(), any()) }
        coVerify(exactly = 1) { checkIsolationPaymentToken.invoke() }
        verify(exactly = 1) { potentialExposureExplanationHandler.addResult(any()) }
        verify(exactly = 1) { potentialExposureExplanationHandler.showNotificationIfNeeded() }
        verify(exactly = 0) { submitFakeExposureWindows(any(), any()) }

        assertTrue { result is Result.Failure }
    }

    @Test
    fun `failure on initial circuit breaker return failure for EN version higher than 1,6`() = runBlocking {
        val testException = Exception()
        every { exposureNotificationTokensProvider.tokens } returns listOf(
            TokenInfo("token1", startedAt = Instant.now().toEpochMilli())
        )
        coEvery { exposureNotificationApi.version() } returns 17823

        coEvery { handleInitialExposureNotification.invoke(any()) } throws testException

        val result = testSubject.handleMatchesFound()

        coVerify(exactly = 1) { handleInitialExposureNotification.invoke("token1") }
        coVerify(exactly = 0) { handlePollingExposureNotification.invoke(any()) }
        verify(exactly = 0) { stateMachine.processEvent(any()) }
        verify(exactly = 0) { exposureNotificationTokensProvider.remove(any()) }
        verify(exactly = 0) { exposureNotificationTokensProvider.updateToPolling(any(), any()) }
        coVerify(exactly = 1) { checkIsolationPaymentToken.invoke() }
        verify(exactly = 0) { potentialExposureExplanationHandler.addResult(any()) }
        verify(exactly = 0) { potentialExposureExplanationHandler.showNotificationIfNeeded() }
        verify(exactly = 0) { submitFakeExposureWindows(any(), any()) }

        assertTrue { result is Result.Failure }
    }

    @Test
    fun `handle no matches calls fake circuit breaker and returns success`() = runBlocking {
        val result = testSubject.handleNoMatchesFound()

        coVerify { emptyApi.submit(EmptySubmissionRequest(CIRCUIT_BREAKER)) }
        verify(exactly = 1) { submitFakeExposureWindows(EXPOSURE_WINDOW, 0) }

        assertTrue { result is Success }
    }

    @Test
    fun `handle no matches calls fake circuit breaker exceptionally and submits fake exposure windows and returns success`() =
        runBlocking {
            val testException = Exception()
            coEvery { emptyApi.submit(EmptySubmissionRequest(CIRCUIT_BREAKER)) } throws testException

            val result = testSubject.handleNoMatchesFound()

            coVerify { emptyApi.submit(EmptySubmissionRequest(CIRCUIT_BREAKER)) }
            verify(exactly = 1) { submitFakeExposureWindows(EXPOSURE_WINDOW, 0) }

            assertTrue { result is Success }
        }
}
