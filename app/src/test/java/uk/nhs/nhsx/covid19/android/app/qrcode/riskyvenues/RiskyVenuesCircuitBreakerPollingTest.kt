package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.NO
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.PENDING
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.YES
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowVenueAlert
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.UserInbox
import uk.nhs.nhsx.covid19.android.app.remote.RiskyVenuesCircuitBreakerApi
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenuesCircuitBreakerPollingResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenuesCircuitBreakerResponse
import java.time.Instant

class RiskyVenuesCircuitBreakerPollingTest {

    private val riskyVenuesCircuitBreakerApi = mockk<RiskyVenuesCircuitBreakerApi>()
    private val notificationProvider = mockk<NotificationProvider>(relaxed = true)
    private val userInbox = mockk<UserInbox>(relaxed = true)
    private val riskyVenuePollingConfigurationProvider =
        mockk<RiskyVenueCircuitBreakerConfigurationProvider>(relaxed = true)
    private val removeOutdatedRiskyVenuePollingConfigurations =
        mockk<RemoveOutdatedRiskyVenuePollingConfigurations>(relaxed = true)

    private val testSubject =
        RiskyVenuesCircuitBreakerPolling(
            riskyVenuesCircuitBreakerApi,
            notificationProvider,
            userInbox,
            riskyVenuePollingConfigurationProvider,
            removeOutdatedRiskyVenuePollingConfigurations
        )

    private val venueId = "1"
    private val approvalToken = "approval_token1"
    private val configuration = RiskyVenueCircuitBreakerConfiguration(Instant.now(), venueId, approvalToken)
    private val pollingConfigurations = listOf(
        RiskyVenueCircuitBreakerConfiguration(Instant.now(), venueId, approvalToken, isPolling = true),
        RiskyVenueCircuitBreakerConfiguration(Instant.now(), "2", "approval_token2", isPolling = true)
    )
    private val initialConfigurations = listOf(
        RiskyVenueCircuitBreakerConfiguration(Instant.now(), venueId, null, isPolling = false)
    )

    @Test
    fun `no risky venue polling configurations`() = runBlocking {
        every { riskyVenuePollingConfigurationProvider.configs } returns listOf()

        testSubject()

        coVerify(exactly = 0) { riskyVenuesCircuitBreakerApi.getRiskyVenuesBreakerResolution(any()) }
    }

    @Test
    fun `polling responds yes and then show notification`() = runBlocking {
        every { riskyVenuePollingConfigurationProvider.configs } returns listOf(configuration)
        coEvery { riskyVenuesCircuitBreakerApi.getRiskyVenuesBreakerResolution(approvalToken) } returns RiskyVenuesCircuitBreakerPollingResponse(
            approval = YES
        )

        testSubject()

        verify(exactly = 1) { notificationProvider.showRiskyVenueVisitNotification() }
        verify(exactly = 1) { userInbox.addUserInboxItem(ShowVenueAlert(venueId)) }
        verify(exactly = 1) { riskyVenuePollingConfigurationProvider.remove(configuration) }
    }

    @Test
    fun `polling responds no doesn't show notification`() = runBlocking {
        every { riskyVenuePollingConfigurationProvider.configs } returns listOf(configuration)

        coEvery { riskyVenuesCircuitBreakerApi.getRiskyVenuesBreakerResolution(approvalToken) } returns RiskyVenuesCircuitBreakerPollingResponse(
            approval = NO
        )

        testSubject()

        verify(exactly = 0) { notificationProvider.showRiskyVenueVisitNotification() }
        verify(exactly = 0) { userInbox.addUserInboxItem(ShowVenueAlert(venueId)) }
        verify(exactly = 1) { riskyVenuePollingConfigurationProvider.remove(configuration) }
    }

    @Test
    fun `polling responds pending doesn't show notification`() = runBlocking {
        every { riskyVenuePollingConfigurationProvider.configs } returns listOf(configuration)

        coEvery { riskyVenuesCircuitBreakerApi.getRiskyVenuesBreakerResolution(approvalToken) } returns RiskyVenuesCircuitBreakerPollingResponse(
            approval = PENDING
        )

        testSubject()

        verify(exactly = 0) { notificationProvider.showRiskyVenueVisitNotification() }
        verify(exactly = 0) { userInbox.addUserInboxItem(ShowVenueAlert(venueId)) }
        verify(exactly = 0) { riskyVenuePollingConfigurationProvider.remove(configuration) }
    }

    @Test
    fun `exception fetching resolution for first config and approval yes for second config`() =
        runBlocking {
            every { riskyVenuePollingConfigurationProvider.configs } returns pollingConfigurations

            coEvery { riskyVenuesCircuitBreakerApi.getRiskyVenuesBreakerResolution(pollingConfigurations[0].approvalToken!!) }.throws(
                Exception()
            )
            coEvery { riskyVenuesCircuitBreakerApi.getRiskyVenuesBreakerResolution(pollingConfigurations[1].approvalToken!!) } returns RiskyVenuesCircuitBreakerPollingResponse(
                approval = YES
            )

            testSubject()

            verify(exactly = 0) { userInbox.addUserInboxItem(ShowVenueAlert(pollingConfigurations[0].venueId)) }
            verify(exactly = 0) { riskyVenuePollingConfigurationProvider.remove(pollingConfigurations[0]) }
            verify(exactly = 1) { notificationProvider.showRiskyVenueVisitNotification() }
            verify(exactly = 1) { userInbox.addUserInboxItem(ShowVenueAlert(pollingConfigurations[1].venueId)) }
            verify(exactly = 1) { riskyVenuePollingConfigurationProvider.remove(pollingConfigurations[1]) }
        }

    @Test
    fun `exception fetching resolution for first config and approval no for second config`() =
        runBlocking {
            every { riskyVenuePollingConfigurationProvider.configs } returns pollingConfigurations

            coEvery { riskyVenuesCircuitBreakerApi.getRiskyVenuesBreakerResolution(pollingConfigurations[0].approvalToken!!) }.throws(
                Exception()
            )
            coEvery { riskyVenuesCircuitBreakerApi.getRiskyVenuesBreakerResolution(pollingConfigurations[1].approvalToken!!) } returns RiskyVenuesCircuitBreakerPollingResponse(
                approval = NO
            )

            testSubject()

            verify(exactly = 0) { userInbox.addUserInboxItem(ShowVenueAlert(pollingConfigurations[0].venueId)) }
            verify(exactly = 0) { riskyVenuePollingConfigurationProvider.remove(pollingConfigurations[0]) }
            verify(exactly = 0) { notificationProvider.showRiskyVenueVisitNotification() }
            verify(exactly = 0) { userInbox.addUserInboxItem(ShowVenueAlert(pollingConfigurations[1].venueId)) }
            verify(exactly = 1) { riskyVenuePollingConfigurationProvider.remove(pollingConfigurations[1]) }
        }

    @Test
    fun `triggers notification if circuit breaker approves`() = runBlocking {
        coEvery { riskyVenuesCircuitBreakerApi.getApproval(any()) } returns
            RiskyVenuesCircuitBreakerResponse(
                approvalToken = approvalToken,
                approval = YES
            )
        every { riskyVenuePollingConfigurationProvider.configs } returns initialConfigurations

        testSubject()

        verify(exactly = 1) { notificationProvider.showRiskyVenueVisitNotification() }
        verify(exactly = 1) { userInbox.addUserInboxItem(ShowVenueAlert(venueId)) }
        verify(exactly = 0) {
            riskyVenuePollingConfigurationProvider.add(any())
        }
    }

    @Test
    fun `does not trigger notification if circuit breaker doesn't approve`() = runBlocking {
        coEvery { riskyVenuesCircuitBreakerApi.getApproval(any()) } returns RiskyVenuesCircuitBreakerResponse(
            approvalToken = approvalToken,
            approval = NO
        )
        every { riskyVenuePollingConfigurationProvider.configs } returns initialConfigurations

        testSubject()

        verify(exactly = 0) { notificationProvider.showRiskyVenueVisitNotification() }
        verify(exactly = 0) { userInbox.addUserInboxItem(ShowVenueAlert(venueId)) }
        verify(exactly = 0) {
            riskyVenuePollingConfigurationProvider.add(any())
        }
    }

    @Test
    fun `will start polling if circuit breaker response pending`() = runBlocking {
        coEvery { riskyVenuesCircuitBreakerApi.getApproval(any()) } returns
            RiskyVenuesCircuitBreakerResponse(
                approvalToken = approvalToken,
                approval = PENDING
            )
        every { riskyVenuePollingConfigurationProvider.configs } returns initialConfigurations

        testSubject()

        verify(exactly = 0) { notificationProvider.showRiskyVenueVisitNotification() }
        verify(exactly = 0) { userInbox.addUserInboxItem(ShowVenueAlert(venueId)) }
        verify(exactly = 1) {
            riskyVenuePollingConfigurationProvider.add(any())
        }
    }
}
