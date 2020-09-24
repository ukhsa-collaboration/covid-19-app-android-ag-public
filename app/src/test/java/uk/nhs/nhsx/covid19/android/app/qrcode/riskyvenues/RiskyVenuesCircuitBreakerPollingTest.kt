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
import java.time.Instant

class RiskyVenuesCircuitBreakerPollingTest {

    private val riskyVenuesCircuitBreakerApi = mockk<RiskyVenuesCircuitBreakerApi>()
    private val notificationProvider = mockk<NotificationProvider>(relaxed = true)
    private val userInbox = mockk<UserInbox>(relaxed = true)
    private val riskyVenuePollingConfigurationProvider =
        mockk<RiskyVenuePollingConfigurationProvider>(relaxed = true)
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
    private val configuration = RiskyVenuePollingConfiguration(Instant.now(), venueId, approvalToken)
    private val configurations = listOf(
        RiskyVenuePollingConfiguration(Instant.now(), venueId, approvalToken),
        RiskyVenuePollingConfiguration(Instant.now(), "2", "approval_token2")
    )

    @Test
    fun `no risky venue polling configurations`() = runBlocking {
        every { riskyVenuePollingConfigurationProvider.configs } returns listOf()

        testSubject.doWork()

        coVerify(exactly = 0) { riskyVenuesCircuitBreakerApi.getRiskyVenuesBreakerResolution(any()) }
    }

    @Test
    fun `polling responds yes and then show notification`() = runBlocking {
        every { riskyVenuePollingConfigurationProvider.configs } returns listOf(configuration)
        coEvery { riskyVenuesCircuitBreakerApi.getRiskyVenuesBreakerResolution(approvalToken) } returns RiskyVenuesCircuitBreakerPollingResponse(
            approval = YES
        )

        testSubject.doWork()

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

        testSubject.doWork()

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

        testSubject.doWork()

        verify(exactly = 0) { notificationProvider.showRiskyVenueVisitNotification() }
        verify(exactly = 0) { userInbox.addUserInboxItem(ShowVenueAlert(venueId)) }
        verify(exactly = 0) { riskyVenuePollingConfigurationProvider.remove(configuration) }
    }

    @Test
    fun `exception fetching resolution for first config and approval yes for second config`() =
        runBlocking {
            every { riskyVenuePollingConfigurationProvider.configs } returns configurations

            coEvery { riskyVenuesCircuitBreakerApi.getRiskyVenuesBreakerResolution(configurations[0].approvalToken) }.throws(
                Exception()
            )
            coEvery { riskyVenuesCircuitBreakerApi.getRiskyVenuesBreakerResolution(configurations[1].approvalToken) } returns RiskyVenuesCircuitBreakerPollingResponse(
                approval = YES
            )

            testSubject.doWork()

            verify(exactly = 0) { userInbox.addUserInboxItem(ShowVenueAlert(configurations[0].venueId)) }
            verify(exactly = 0) { riskyVenuePollingConfigurationProvider.remove(configurations[0]) }
            verify(exactly = 1) { notificationProvider.showRiskyVenueVisitNotification() }
            verify(exactly = 1) { userInbox.addUserInboxItem(ShowVenueAlert(configurations[1].venueId)) }
            verify(exactly = 1) { riskyVenuePollingConfigurationProvider.remove(configurations[1]) }
        }

    @Test
    fun `exception fetching resolution for first config and approval no for second config`() =
        runBlocking {
            every { riskyVenuePollingConfigurationProvider.configs } returns configurations

            coEvery { riskyVenuesCircuitBreakerApi.getRiskyVenuesBreakerResolution(configurations[0].approvalToken) }.throws(
                Exception()
            )
            coEvery { riskyVenuesCircuitBreakerApi.getRiskyVenuesBreakerResolution(configurations[1].approvalToken) } returns RiskyVenuesCircuitBreakerPollingResponse(
                approval = NO
            )

            testSubject.doWork()

            verify(exactly = 0) { userInbox.addUserInboxItem(ShowVenueAlert(configurations[0].venueId)) }
            verify(exactly = 0) { riskyVenuePollingConfigurationProvider.remove(configurations[0]) }
            verify(exactly = 0) { notificationProvider.showRiskyVenueVisitNotification() }
            verify(exactly = 0) { userInbox.addUserInboxItem(ShowVenueAlert(configurations[1].venueId)) }
            verify(exactly = 1) { riskyVenuePollingConfigurationProvider.remove(configurations[1]) }
        }
}
