package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import androidx.work.ListenableWorker.Result
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.NO
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.PENDING
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.YES
import uk.nhs.nhsx.covid19.android.app.notifications.AndroidUserInbox
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowVenueAlert
import uk.nhs.nhsx.covid19.android.app.remote.RiskyVenuesCircuitBreakerApi
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenuesCircuitBreakerPollingResponse
import kotlin.test.assertEquals

class RiskyVenuesCircuitBreakerPollingTest {

    private val riskyVenuesCircuitBreakerApi = mockk<RiskyVenuesCircuitBreakerApi>()
    private val notificationProvider = mockk<NotificationProvider>(relaxed = true)
    private val userInbox = mockk<AndroidUserInbox>(relaxed = true)

    private val testSubject =
        RiskyVenuesCircuitBreakerPolling(riskyVenuesCircuitBreakerApi, notificationProvider, userInbox)

    @Test
    fun `polling responds yes and then show notification`() = runBlocking {
        coEvery { riskyVenuesCircuitBreakerApi.getRiskyVenuesBreakerResolution(any()) } returns RiskyVenuesCircuitBreakerPollingResponse(
            approval = YES
        )

        val venueId = ""
        val result = testSubject.doWork("", venueId)

        verify(exactly = 1) { notificationProvider.showRiskyVenueVisitNotification() }
        verify(exactly = 1) { userInbox.addUserInboxItem(ShowVenueAlert(venueId)) }
        assertEquals(Result.success(), result)
    }

    @Test
    fun `polling responds no doesn't show notification`() = runBlocking {
        coEvery { riskyVenuesCircuitBreakerApi.getRiskyVenuesBreakerResolution(any()) } returns RiskyVenuesCircuitBreakerPollingResponse(
            approval = NO
        )

        val result = testSubject.doWork("", "")

        verify(exactly = 0) { notificationProvider.showRiskyVenueVisitNotification() }
        assertEquals(Result.success(), result)
    }

    @Test
    fun `polling responds pending and will retry`() = runBlocking {
        coEvery { riskyVenuesCircuitBreakerApi.getRiskyVenuesBreakerResolution(any()) } returns RiskyVenuesCircuitBreakerPollingResponse(
            approval = PENDING
        )

        val result = testSubject.doWork("", "")

        assertEquals(Result.retry(), result)
    }

    @Test
    fun `polling throws exception and will retry`() = runBlocking {
        coEvery { riskyVenuesCircuitBreakerApi.getRiskyVenuesBreakerResolution(any()) }.throws(
            Exception()
        )

        val result = testSubject.doWork("", "")

        assertEquals(Result.retry(), result)
    }
}
