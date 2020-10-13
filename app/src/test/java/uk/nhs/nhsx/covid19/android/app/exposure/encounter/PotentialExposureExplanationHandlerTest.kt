package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HandleInitialExposureNotification.InitialCircuitBreakerResult
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import java.io.IOException

class PotentialExposureExplanationHandlerTest {

    private val notificationProvider = mockk<NotificationProvider>(relaxUnitFun = true)
    private val testSubject = PotentialExposureExplanationHandler(notificationProvider)

    @Test
    fun `calling act without adding results do not interact with notifications`() {
        testSubject.showNotificationIfNeeded()

        verify(exactly = 0) {
            notificationProvider.showPotentialExposureExplanationNotification()
            notificationProvider.hidePotentialExposureExplanationNotification()
        }
    }

    @Test
    fun `show notification on failure result`() {
        testSubject.addResult(Result.Failure(IOException()))
        testSubject.showNotificationIfNeeded()

        verify(exactly = 0) { notificationProvider.hidePotentialExposureExplanationNotification() }
        verify(exactly = 1) { notificationProvider.showPotentialExposureExplanationNotification() }
    }

    @Test
    fun `hide notification on circuit breaker yes response`() {
        testSubject.addResult(Result.Success(InitialCircuitBreakerResult.Yes(0)))
        testSubject.showNotificationIfNeeded()

        verify(exactly = 1) { notificationProvider.hidePotentialExposureExplanationNotification() }
        verify(exactly = 0) { notificationProvider.showPotentialExposureExplanationNotification() }
    }

    @Test
    fun `show notification on circuit breaker no response`() {
        testSubject.addResult(Result.Success(InitialCircuitBreakerResult.No))
        testSubject.showNotificationIfNeeded()

        verify(exactly = 0) { notificationProvider.hidePotentialExposureExplanationNotification() }
        verify(exactly = 1) { notificationProvider.showPotentialExposureExplanationNotification() }
    }

    @Test
    fun `show notification on circuit breaker pending response`() {
        testSubject.addResult(Result.Success(InitialCircuitBreakerResult.Pending(0)))
        testSubject.showNotificationIfNeeded()

        verify(exactly = 0) { notificationProvider.hidePotentialExposureExplanationNotification() }
        verify(exactly = 1) { notificationProvider.showPotentialExposureExplanationNotification() }
    }

    @Test
    fun `hide notification if there was at least one yes`() {
        testSubject.addResult(Result.Failure(IOException()))
        testSubject.addResult(Result.Success(InitialCircuitBreakerResult.Yes(0)))
        testSubject.addResult(Result.Success(InitialCircuitBreakerResult.No))
        testSubject.addResult(Result.Success(InitialCircuitBreakerResult.Pending(0)))
        testSubject.showNotificationIfNeeded()

        verify(exactly = 1) { notificationProvider.hidePotentialExposureExplanationNotification() }
        verify(exactly = 0) { notificationProvider.showPotentialExposureExplanationNotification() }
    }

    @Test
    fun `show notification if there was no yes responses`() {
        testSubject.addResult(Result.Failure(IOException()))
        testSubject.addResult(Result.Success(InitialCircuitBreakerResult.No))
        testSubject.addResult(Result.Success(InitialCircuitBreakerResult.Pending(0)))
        testSubject.showNotificationIfNeeded()

        verify(exactly = 0) { notificationProvider.hidePotentialExposureExplanationNotification() }
        verify(exactly = 1) { notificationProvider.showPotentialExposureExplanationNotification() }
    }
}
