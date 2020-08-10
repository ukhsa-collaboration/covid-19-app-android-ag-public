package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import androidx.work.ListenableWorker
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.NO
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.PENDING
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.YES
import uk.nhs.nhsx.covid19.android.app.common.PeriodicTasks
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.UserInbox
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowVenueAlert
import uk.nhs.nhsx.covid19.android.app.remote.RiskyVenuesCircuitBreakerApi
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenuesCircuitBreakerResponse
import kotlin.test.assertEquals

class RiskyVenuesCircuitBreakerInitialWorkTest {
    private val riskyVenuesCircuitBreakerApi = mockk<RiskyVenuesCircuitBreakerApi>(relaxed = true)
    private val notificationProvider = mockk<NotificationProvider>(relaxed = true)
    private val periodicTasks = mockk<PeriodicTasks>(relaxed = true)
    private val userInbox = mockk<UserInbox>(relaxed = true)

    private val testSubject = RiskyVenuesCircuitBreakerInitialWork(
        riskyVenuesCircuitBreakerApi,
        notificationProvider,
        periodicTasks,
        userInbox
    )

    private val venueId = "1"

    @Test
    fun `triggers notification and returns success if circuit breaker approves`() =
        runBlocking {
            coEvery { riskyVenuesCircuitBreakerApi.submitInitialVenueIdForApproval(any()) } returns
                RiskyVenuesCircuitBreakerResponse(
                    approvalToken = "",
                    approval = YES
                )

            val result = testSubject.doWork(venueId)

            assertEquals(ListenableWorker.Result.success(), result)

            verify(exactly = 1) { notificationProvider.showRiskyVenueVisitNotification() }
            verify(exactly = 1) { userInbox.addUserInboxItem(ShowVenueAlert(venueId)) }
            verify(exactly = 0) {
                periodicTasks.scheduleRiskyVenuesCircuitBreakerPolling(
                    any(),
                    any()
                )
            }
        }

    @Test
    fun `does not trigger notification and return success if circuit breaker doesn't approve`() =
        runBlocking {
            coEvery { riskyVenuesCircuitBreakerApi.submitInitialVenueIdForApproval(any()) } returns RiskyVenuesCircuitBreakerResponse(
                approvalToken = "",
                approval = NO
            )

            val result = testSubject.doWork(venueId)

            assertEquals(ListenableWorker.Result.success(), result)

            verify(exactly = 0) { notificationProvider.showRiskyVenueVisitNotification() }
            verify(exactly = 0) { userInbox.addUserInboxItem(ShowVenueAlert(venueId)) }
            verify(exactly = 0) {
                periodicTasks.scheduleRiskyVenuesCircuitBreakerPolling(
                    any(),
                    any()
                )
            }
        }

    @Test
    fun `will return success and start polling if circuit breaker response pending`() =
        runBlocking {
            coEvery { riskyVenuesCircuitBreakerApi.submitInitialVenueIdForApproval(any()) } returns
                RiskyVenuesCircuitBreakerResponse(
                    approvalToken = "",
                    approval = PENDING
                )

            val result = testSubject.doWork(venueId)

            assertEquals(ListenableWorker.Result.success(), result)

            verify(exactly = 0) { notificationProvider.showRiskyVenueVisitNotification() }
            verify(exactly = 0) { userInbox.addUserInboxItem(ShowVenueAlert(venueId)) }
            verify(exactly = 1) {
                periodicTasks.scheduleRiskyVenuesCircuitBreakerPolling(
                    any(),
                    any()
                )
            }
        }

    @Test
    fun `will retry if exception is thrown`() = runBlocking {
        coEvery { riskyVenuesCircuitBreakerApi.submitInitialVenueIdForApproval(any()) }.throws(
            Exception()
        )

        val result = testSubject.doWork(venueId)

        assertEquals(ListenableWorker.Result.retry(), result)

        verify(exactly = 0) { notificationProvider.showRiskyVenueVisitNotification() }
        verify(exactly = 0) { userInbox.addUserInboxItem(ShowVenueAlert(venueId)) }
        verify(exactly = 0) {
            periodicTasks.scheduleRiskyVenuesCircuitBreakerPolling(
                any(),
                any()
            )
        }
    }
}
