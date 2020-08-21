package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.NO
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.PENDING
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.YES
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.UserInbox
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowVenueAlert
import uk.nhs.nhsx.covid19.android.app.remote.RiskyVenuesCircuitBreakerApi
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenuesCircuitBreakerResponse

class RiskyVenuesCircuitBreakerInitialWorkTest {
    private val riskyVenuesCircuitBreakerApi = mockk<RiskyVenuesCircuitBreakerApi>(relaxed = true)
    private val notificationProvider = mockk<NotificationProvider>(relaxed = true)
    private val userInbox = mockk<UserInbox>(relaxed = true)
    private val riskyVenuePollingConfigurationProvider =
        mockk<RiskyVenuePollingConfigurationProvider>(relaxed = true)
    private val visitedVenuesStorage = mockk<VisitedVenuesStorage>(relaxed = true)

    private val testSubject = RiskyVenuesCircuitBreakerInitialWork(
        riskyVenuesCircuitBreakerApi,
        notificationProvider,
        userInbox,
        riskyVenuePollingConfigurationProvider,
        visitedVenuesStorage
    )

    private val venueId = "1"
    private val approvalToken = "approval_token"

    private val venuesIds = listOf(venueId)

    @Test
    fun `triggers notification if circuit breaker approves`() =
        runBlocking {
            coEvery { riskyVenuesCircuitBreakerApi.submitInitialVenueIdForApproval(any()) } returns
                RiskyVenuesCircuitBreakerResponse(
                    approvalToken = approvalToken,
                    approval = YES
                )

            testSubject.doWork(venuesIds)

            verify(exactly = 1) { notificationProvider.showRiskyVenueVisitNotification() }
            verify(exactly = 1) { userInbox.addUserInboxItem(ShowVenueAlert(venueId)) }
            verify(exactly = 0) {
                riskyVenuePollingConfigurationProvider.add(any())
            }
        }

    @Test
    fun `does not trigger notification if circuit breaker doesn't approve`() =
        runBlocking {
            coEvery { riskyVenuesCircuitBreakerApi.submitInitialVenueIdForApproval(any()) } returns RiskyVenuesCircuitBreakerResponse(
                approvalToken = approvalToken,
                approval = NO
            )

            testSubject.doWork(venuesIds)

            verify(exactly = 0) { notificationProvider.showRiskyVenueVisitNotification() }
            verify(exactly = 0) { userInbox.addUserInboxItem(ShowVenueAlert(venueId)) }
            verify(exactly = 0) {
                riskyVenuePollingConfigurationProvider.add(any())
            }
        }

    @Test
    fun `will start polling if circuit breaker response pending`() =
        runBlocking {
            coEvery { riskyVenuesCircuitBreakerApi.submitInitialVenueIdForApproval(any()) } returns
                RiskyVenuesCircuitBreakerResponse(
                    approvalToken = approvalToken,
                    approval = PENDING
                )

            testSubject.doWork(venuesIds)

            verify(exactly = 0) { notificationProvider.showRiskyVenueVisitNotification() }
            verify(exactly = 0) { userInbox.addUserInboxItem(ShowVenueAlert(venueId)) }
            verify(exactly = 1) {
                riskyVenuePollingConfigurationProvider.add(any())
            }
        }

    @Test
    fun `will undo mark was in risky list for risky venue visits if exception is thrown`() = runBlocking {
        coEvery { riskyVenuesCircuitBreakerApi.submitInitialVenueIdForApproval(any()) }.throws(
            Exception()
        )

        testSubject.doWork(venuesIds)

        verify(exactly = 0) { notificationProvider.showRiskyVenueVisitNotification() }
        verify(exactly = 0) { userInbox.addUserInboxItem(ShowVenueAlert(venueId)) }
        verify(exactly = 0) {
            riskyVenuePollingConfigurationProvider.add(any())
        }
        coVerify(exactly = 1) { visitedVenuesStorage.undoMarkWasInRiskyList(venueId) }
    }
}
