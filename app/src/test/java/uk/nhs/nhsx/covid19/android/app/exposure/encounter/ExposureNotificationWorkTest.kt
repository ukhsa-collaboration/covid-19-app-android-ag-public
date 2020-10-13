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
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandleInitialExposureNotification.InitialCircuitBreakerResult
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandlePollingExposureNotification.PollingCircuitBreakerResult
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import java.time.Instant
import javax.inject.Provider
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExposureNotificationWorkTest {

    private val exposureNotificationTokensProvider =
        mockk<ExposureNotificationTokensProvider>(relaxed = true)
    private val handleInitialExposureNotification = mockk<HandleInitialExposureNotification>()
    private val handlePollingExposureNotification = mockk<HandlePollingExposureNotification>()
    private val stateMachine = mockk<IsolationStateMachine>(relaxed = true)
    private val potentialExposureExplanationHandlerProvider = mockk<Provider<PotentialExposureExplanationHandler>>()
    private val potentialExposureExplanationHandler = mockk<PotentialExposureExplanationHandler>(relaxUnitFun = true)

    private val testSubject = ExposureNotificationWork(
        exposureNotificationTokensProvider,
        handleInitialExposureNotification,
        handlePollingExposureNotification,
        stateMachine,
        potentialExposureExplanationHandlerProvider
    )

    @Before
    fun setUp() {
        every { potentialExposureExplanationHandlerProvider.get() } returns potentialExposureExplanationHandler
    }

    @Test
    fun `no tokens return success`() = runBlocking {
        every { exposureNotificationTokensProvider.tokens } returns emptyList()

        val result = testSubject()

        coVerify(exactly = 0) { handleInitialExposureNotification.invoke(any()) }
        coVerify(exactly = 0) { handlePollingExposureNotification.invoke(any()) }
        verify(exactly = 0) { stateMachine.processEvent(any()) }
        verify(exactly = 0) { potentialExposureExplanationHandler.addResult(any()) }
        verify(exactly = 1) { potentialExposureExplanationHandler.showNotificationIfNeeded() }

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

        val result = testSubject()

        coVerify(exactly = 2) { handleInitialExposureNotification.invoke(any()) }
        verify(exactly = 2) { stateMachine.processEvent(any()) }
        coVerify(exactly = 0) { handlePollingExposureNotification.invoke(any()) }
        verify(exactly = 2) { exposureNotificationTokensProvider.remove(any()) }
        verify(exactly = 0) { exposureNotificationTokensProvider.updateToPolling(any(), any()) }
        verify(exactly = 2) { potentialExposureExplanationHandler.addResult(any()) }
        verify(exactly = 1) { potentialExposureExplanationHandler.showNotificationIfNeeded() }

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

            val result = testSubject()

            coVerify(exactly = 1) { handleInitialExposureNotification.invoke(any()) }
            verify(exactly = 1) {
                exposureNotificationTokensProvider.updateToPolling(
                    "token1",
                    123
                )
            }
            coVerify(exactly = 0) { handlePollingExposureNotification.invoke(any()) }
            verify(exactly = 0) { stateMachine.processEvent(any()) }
            verify(exactly = 0) { exposureNotificationTokensProvider.remove("token1") }
            val slot = slot<Result<InitialCircuitBreakerResult>>()
            verify(exactly = 1) { potentialExposureExplanationHandler.addResult(capture(slot)) }
            assertEquals(pendingResult, slot.captured)
            verify(exactly = 1) { potentialExposureExplanationHandler.showNotificationIfNeeded() }

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

            val result = testSubject()

            coVerify(exactly = 1) { handleInitialExposureNotification.invoke(any()) }
            verify(exactly = 1) { exposureNotificationTokensProvider.remove("token1") }
            verify(exactly = 0) { exposureNotificationTokensProvider.updateToPolling(any(), any()) }
            coVerify(exactly = 0) { handlePollingExposureNotification.invoke(any()) }
            verify(exactly = 0) { stateMachine.processEvent(any()) }
            val slot = slot<Result<InitialCircuitBreakerResult>>()
            verify(exactly = 1) { potentialExposureExplanationHandler.addResult(capture(slot)) }
            assertEquals(noResult, slot.captured)
            verify(exactly = 1) { potentialExposureExplanationHandler.showNotificationIfNeeded() }

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

        val result = testSubject()

        coVerify(exactly = 0) { handleInitialExposureNotification.invoke(any()) }
        verify(exactly = 1) { stateMachine.processEvent(any()) }
        coVerify(exactly = 1) { handlePollingExposureNotification.invoke("token1") }
        verify(exactly = 1) { exposureNotificationTokensProvider.remove(any()) }
        verify(exactly = 0) { exposureNotificationTokensProvider.updateToPolling(any(), any()) }
        verify(exactly = 0) { potentialExposureExplanationHandler.addResult(any()) }
        verify(exactly = 1) { potentialExposureExplanationHandler.showNotificationIfNeeded() }

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

            val result = testSubject()

            coVerify(exactly = 1) { handlePollingExposureNotification.invoke("token1") }
            coVerify(exactly = 0) { handleInitialExposureNotification.invoke(any()) }
            verify(exactly = 0) { stateMachine.processEvent(any()) }
            verify(exactly = 0) { exposureNotificationTokensProvider.remove(any()) }
            verify(exactly = 0) { exposureNotificationTokensProvider.updateToPolling(any(), any()) }
            verify(exactly = 0) { potentialExposureExplanationHandler.addResult(any()) }
            verify(exactly = 1) { potentialExposureExplanationHandler.showNotificationIfNeeded() }

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

        val result = testSubject()

        coVerify(exactly = 1) { handlePollingExposureNotification.invoke("token1") }
        coVerify(exactly = 0) { handleInitialExposureNotification.invoke(any()) }
        verify(exactly = 0) { stateMachine.processEvent(any()) }
        verify(exactly = 1) { exposureNotificationTokensProvider.remove(any()) }
        verify(exactly = 0) { exposureNotificationTokensProvider.updateToPolling(any(), any()) }
        verify(exactly = 0) { potentialExposureExplanationHandler.addResult(any()) }
        verify(exactly = 1) { potentialExposureExplanationHandler.showNotificationIfNeeded() }

        assertEquals(Result.Success(Unit), result)
    }

    @Test
    fun `failure on polling circuit breaker return failure`() = runBlocking {
        val testException = Exception()
        every { exposureNotificationTokensProvider.tokens } returns listOf(
            TokenInfo("token1", 123, startedAt = Instant.now().toEpochMilli())
        )

        coEvery { handlePollingExposureNotification.invoke(any()) } throws testException

        val result = testSubject()

        coVerify(exactly = 1) { handlePollingExposureNotification.invoke("token1") }
        coVerify(exactly = 0) { handleInitialExposureNotification.invoke(any()) }
        verify(exactly = 0) { stateMachine.processEvent(any()) }
        verify(exactly = 0) { exposureNotificationTokensProvider.remove(any()) }
        verify(exactly = 0) { exposureNotificationTokensProvider.updateToPolling(any(), any()) }

        verify(exactly = 0) { potentialExposureExplanationHandler.addResult(any()) }
        verify(exactly = 1) { potentialExposureExplanationHandler.showNotificationIfNeeded() }

        assertTrue { result is Result.Failure }
    }

    @Test
    fun `failure on initial circuit breaker return failure`() = runBlocking {
        val testException = Exception()
        every { exposureNotificationTokensProvider.tokens } returns listOf(
            TokenInfo("token1", startedAt = Instant.now().toEpochMilli())
        )

        coEvery { handleInitialExposureNotification.invoke(any()) } throws testException

        val result = testSubject()

        coVerify(exactly = 1) { handleInitialExposureNotification.invoke("token1") }

        coVerify(exactly = 0) { handlePollingExposureNotification.invoke(any()) }
        verify(exactly = 0) { stateMachine.processEvent(any()) }
        verify(exactly = 0) { exposureNotificationTokensProvider.remove(any()) }
        verify(exactly = 0) { exposureNotificationTokensProvider.updateToPolling(any(), any()) }
        verify(exactly = 1) { potentialExposureExplanationHandler.addResult(any()) }
        verify(exactly = 1) { potentialExposureExplanationHandler.showNotificationIfNeeded() }

        assertTrue { result is Result.Failure }
    }
}
