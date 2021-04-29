package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedRiskyContactNotification
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.common.Result.Failure
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandleInitialExposureNotification.InitialCircuitBreakerResult
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandleInitialExposureNotification.InitialCircuitBreakerResult.Yes
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandlePollingExposureNotification.PollingCircuitBreakerResult
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnExposedNotification
import java.time.Instant

class ExposureCircuitBreakerTest {

    private val handleInitialExposureNotification = mockk<HandleInitialExposureNotification>()
    private val handlePollingExposureNotification = mockk<HandlePollingExposureNotification>()
    private val stateMachine = mockk<IsolationStateMachine>(relaxed = true)
    private val exposureCircuitBreakerInfoProvider = mockk<ExposureCircuitBreakerInfoProvider>(relaxUnitFun = true)
    private val analyticsEventProcessor = mockk<AnalyticsEventProcessor>(relaxUnitFun = true)

    val testSubject = ExposureCircuitBreaker(
        handleInitialExposureNotification,
        handlePollingExposureNotification,
        stateMachine,
        exposureCircuitBreakerInfoProvider,
        analyticsEventProcessor
    )

    @Test
    fun `calling handleInitial and circuit breaker responds yes then trigger exposure`() =
        runBlocking {
            coEvery { handleInitialExposureNotification.invoke(infoWithoutToken) } returns Success(
                Yes
            )

            testSubject.handleInitial(infoWithoutToken)

            coVerifyOrder {
                handleInitialExposureNotification.invoke(infoWithoutToken)
                stateMachine.processEvent(OnExposedNotification(Instant.ofEpochMilli(infoWithoutToken.startOfDayMillis)))
                exposureCircuitBreakerInfoProvider.remove(infoWithoutToken)
                analyticsEventProcessor.track(ReceivedRiskyContactNotification)
            }
            verify(exactly = 0) { exposureCircuitBreakerInfoProvider.setApprovalToken(any(), any()) }
        }

    @Test
    fun `calling handleInitial and circuit breaker responds no then remove info item`() =
        runBlocking {
            coEvery { handleInitialExposureNotification.invoke(infoWithoutToken) } returns Success(
                InitialCircuitBreakerResult.No
            )

            testSubject.handleInitial(infoWithoutToken)

            coVerifyOrder {
                handleInitialExposureNotification.invoke(infoWithoutToken)
                exposureCircuitBreakerInfoProvider.remove(infoWithoutToken)
            }
            verify(exactly = 0) { exposureCircuitBreakerInfoProvider.setApprovalToken(any(), any()) }
            verify(exactly = 0) { stateMachine.processEvent(any()) }
            coVerify(exactly = 0) { analyticsEventProcessor.track(any()) }
        }

    @Test
    fun `calling handleInitial and circuit breaker responds pending then update info item`() =
        runBlocking {
            coEvery { handleInitialExposureNotification.invoke(infoWithoutToken) } returns Success(
                InitialCircuitBreakerResult.Pending("token")
            )

            testSubject.handleInitial(infoWithoutToken)

            coVerifyOrder {
                handleInitialExposureNotification.invoke(infoWithoutToken)
                exposureCircuitBreakerInfoProvider.setApprovalToken(infoWithoutToken, "token")
            }
            verify(exactly = 0) { exposureCircuitBreakerInfoProvider.remove(any()) }
            verify(exactly = 0) { stateMachine.processEvent(any()) }
            coVerify(exactly = 0) { analyticsEventProcessor.track(any()) }
        }

    @Test
    fun `calling handleInitial and circuit breaker responds Failure then do nothing`() =
        runBlocking {
            coEvery { handleInitialExposureNotification.invoke(infoWithoutToken) } returns Failure(Exception())

            testSubject.handleInitial(infoWithoutToken)

            coVerify { handleInitialExposureNotification.invoke(infoWithoutToken) }
            verify(exactly = 0) { exposureCircuitBreakerInfoProvider.remove(any()) }
            verify(exactly = 0) { exposureCircuitBreakerInfoProvider.setApprovalToken(any(), any()) }
            verify(exactly = 0) { stateMachine.processEvent(any()) }
            coVerify(exactly = 0) { analyticsEventProcessor.track(any()) }
        }

    @Test
    fun `calling handlePolling and circuit breaker responds yes then trigger exposure`() =
        runBlocking {
            coEvery { handlePollingExposureNotification.invoke("token") } returns Success(
                PollingCircuitBreakerResult.Yes
            )

            testSubject.handlePolling(infoWithToken)

            coVerifyOrder {
                handlePollingExposureNotification.invoke("token")
                stateMachine.processEvent(OnExposedNotification(Instant.ofEpochMilli(infoWithToken.startOfDayMillis)))
                exposureCircuitBreakerInfoProvider.remove(infoWithToken)
                analyticsEventProcessor.track(ReceivedRiskyContactNotification)
            }
            verify(exactly = 0) { exposureCircuitBreakerInfoProvider.setApprovalToken(any(), any()) }
        }

    @Test
    fun `calling handlePolling and circuit breaker responds no then remove info item`() =
        runBlocking {
            coEvery { handlePollingExposureNotification.invoke("token") } returns Success(
                PollingCircuitBreakerResult.No
            )

            testSubject.handlePolling(infoWithToken)

            coVerifyOrder {
                handlePollingExposureNotification.invoke("token")
                exposureCircuitBreakerInfoProvider.remove(infoWithToken)
            }
            verify(exactly = 0) { exposureCircuitBreakerInfoProvider.setApprovalToken(any(), any()) }
            verify(exactly = 0) { stateMachine.processEvent(any()) }
            coVerify(exactly = 0) { analyticsEventProcessor.track(any()) }
        }

    @Test
    fun `calling handlePolling and circuit breaker responds pending then do nothing`() =
        runBlocking {
            coEvery { handlePollingExposureNotification.invoke("token") } returns Success(
                PollingCircuitBreakerResult.Pending
            )

            testSubject.handlePolling(infoWithToken)

            coVerify { handlePollingExposureNotification.invoke("token") }
            verify(exactly = 0) { exposureCircuitBreakerInfoProvider.remove(any()) }
            verify(exactly = 0) { exposureCircuitBreakerInfoProvider.setApprovalToken(any(), any()) }
            verify(exactly = 0) { stateMachine.processEvent(any()) }
            coVerify(exactly = 0) { analyticsEventProcessor.track(any()) }
        }

    @Test
    fun `calling handlePolling and circuit breaker responds Failure then do nothing`() =
        runBlocking {
            coEvery { handlePollingExposureNotification.invoke("token") } returns Failure(Exception())

            testSubject.handlePolling(infoWithToken)

            coVerify { handlePollingExposureNotification.invoke("token") }
            verify(exactly = 0) { exposureCircuitBreakerInfoProvider.remove(any()) }
            verify(exactly = 0) { exposureCircuitBreakerInfoProvider.setApprovalToken(any(), any()) }
            verify(exactly = 0) { stateMachine.processEvent(any()) }
            coVerify(exactly = 0) { analyticsEventProcessor.track(any()) }
        }

    companion object {
        private val now = Instant.now().toEpochMilli()
        private val infoWithoutToken = ExposureCircuitBreakerInfo(1.0, now, 1, 2, now)
        private val infoWithToken = ExposureCircuitBreakerInfo(1.0, Instant.now().toEpochMilli(), 1, 2, now, "token")
    }
}
