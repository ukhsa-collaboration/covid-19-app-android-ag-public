package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedRiskyVenueM1Warning
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedRiskyVenueM2Warning
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.NO
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.PENDING
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.YES
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.RiskyVenueAlert
import uk.nhs.nhsx.covid19.android.app.notifications.RiskyVenueAlertProvider
import uk.nhs.nhsx.covid19.android.app.remote.RiskyVenuesCircuitBreakerApi
import uk.nhs.nhsx.covid19.android.app.remote.data.MessageType.BOOK_TEST
import uk.nhs.nhsx.covid19.android.app.remote.data.MessageType.INFORM
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueConfigurationDurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenuesCircuitBreakerPollingResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenuesCircuitBreakerResponse
import uk.nhs.nhsx.covid19.android.app.util.toLocalDate
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class RiskyVenuesCircuitBreakerPollingTest {

    private val riskyVenuesCircuitBreakerApi = mockk<RiskyVenuesCircuitBreakerApi>()
    private val notificationProvider = mockk<NotificationProvider>(relaxUnitFun = true)
    private val riskyVenueAlertProvider = mockk<RiskyVenueAlertProvider>(relaxUnitFun = true)
    private val riskyVenuePollingConfigurationProvider =
        mockk<RiskyVenueCircuitBreakerConfigurationProvider>(relaxUnitFun = true)
    private val removeOutdatedRiskyVenuePollingConfigurations =
        mockk<RemoveOutdatedRiskyVenuePollingConfigurations>(relaxUnitFun = true)
    private val lastVisitedBookTestTypeVenueDateProvider =
        mockk<LastVisitedBookTestTypeVenueDateProvider>(relaxUnitFun = true)
    private val shouldShowRiskyVenueNotification = mockk<ShouldShowRiskyVenueNotification>()
    private val riskyVenueConfigurationProvider = mockk<RiskyVenueConfigurationProvider>(relaxUnitFun = true)
    private val analyticsEventProcessor = mockk<AnalyticsEventProcessor>(relaxUnitFun = true)

    private val fixedClock = Clock.fixed(Instant.parse("2020-05-21T10:00:00Z"), ZoneOffset.UTC)

    private val testSubject =
        RiskyVenuesCircuitBreakerPolling(
            riskyVenuesCircuitBreakerApi,
            notificationProvider,
            riskyVenueAlertProvider,
            riskyVenuePollingConfigurationProvider,
            removeOutdatedRiskyVenuePollingConfigurations,
            lastVisitedBookTestTypeVenueDateProvider,
            shouldShowRiskyVenueNotification,
            riskyVenueConfigurationProvider,
            analyticsEventProcessor,
            fixedClock
        )

    private val firstVenueId = "1"
    private val secondVenueId = "2"
    private val approvalToken = "approval_token1"
    private val firstVenueStartedAt = Instant.parse("2020-10-10T10:00:00Z")
    private val secondVenueStartedAt = Instant.parse("2020-10-11T10:00:00Z")
    private val configuration = RiskyVenueCircuitBreakerConfiguration(Instant.now(), firstVenueId, approvalToken)
    private val riskyVenueConfigurationDurationDays = RiskyVenueConfigurationDurationDays(optionToBookATest = 10)

    private val pollingConfigurations = listOf(
        RiskyVenueCircuitBreakerConfiguration(firstVenueStartedAt, firstVenueId, approvalToken, isPolling = true),
        RiskyVenueCircuitBreakerConfiguration(
            secondVenueStartedAt,
            secondVenueId,
            "approval_token2",
            isPolling = true,
            messageType = BOOK_TEST
        )
    )
    private val initialConfigurationsDoubleInform = listOf(
        RiskyVenueCircuitBreakerConfiguration(
            firstVenueStartedAt,
            firstVenueId,
            null,
            isPolling = false,
            messageType = INFORM
        ),
        RiskyVenueCircuitBreakerConfiguration(
            secondVenueStartedAt,
            secondVenueId,
            null,
            isPolling = false,
            messageType = INFORM
        )
    )

    private val initialConfigurationsBookTestThenInform = listOf(
        RiskyVenueCircuitBreakerConfiguration(
            firstVenueStartedAt,
            firstVenueId,
            null,
            isPolling = false,
            messageType = BOOK_TEST
        ),
        RiskyVenueCircuitBreakerConfiguration(
            secondVenueStartedAt,
            secondVenueId,
            null,
            isPolling = false,
            messageType = INFORM
        )
    )

    private val initialConfigurationsInformThenBookTest = listOf(
        RiskyVenueCircuitBreakerConfiguration(
            firstVenueStartedAt,
            firstVenueId,
            null,
            isPolling = false,
            messageType = INFORM
        ),
        RiskyVenueCircuitBreakerConfiguration(
            secondVenueStartedAt,
            secondVenueId,
            null,
            isPolling = false,
            messageType = BOOK_TEST
        )
    )

    private val initialConfigurationsDoubleBookTest = listOf(
        RiskyVenueCircuitBreakerConfiguration(
            firstVenueStartedAt,
            firstVenueId,
            null,
            isPolling = false,
            messageType = BOOK_TEST
        ),
        RiskyVenueCircuitBreakerConfiguration(
            secondVenueStartedAt,
            secondVenueId,
            null,
            isPolling = false,
            messageType = BOOK_TEST
        )
    )

    private val initialConfigurationsDoubleBookTestReverseOrder = listOf(
        RiskyVenueCircuitBreakerConfiguration(
            secondVenueStartedAt,
            secondVenueId,
            null,
            isPolling = false,
            messageType = BOOK_TEST
        ),
        RiskyVenueCircuitBreakerConfiguration(
            firstVenueStartedAt,
            firstVenueId,
            null,
            isPolling = false,
            messageType = BOOK_TEST
        )
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
        every { shouldShowRiskyVenueNotification.invoke(INFORM) } returns true

        testSubject()

        verify(exactly = 1) { notificationProvider.showRiskyVenueVisitNotification() }
        verify(exactly = 1) {
            riskyVenueAlertProvider setProperty "riskyVenueAlert" value eq(RiskyVenueAlert(firstVenueId, INFORM))
        }
        verify(exactly = 1) { riskyVenuePollingConfigurationProvider.remove(configuration) }
        verify(exactly = 0) { lastVisitedBookTestTypeVenueDateProvider.lastVisitedVenue = any() }
        coVerify(exactly = 1) { analyticsEventProcessor.track(ReceivedRiskyVenueM1Warning) }
    }

    @Test
    fun `polling responds no doesn't show notification`() = runBlocking {
        every { riskyVenuePollingConfigurationProvider.configs } returns listOf(configuration)
        coEvery { riskyVenuesCircuitBreakerApi.getRiskyVenuesBreakerResolution(approvalToken) } returns RiskyVenuesCircuitBreakerPollingResponse(
            approval = NO
        )

        testSubject()

        verify(exactly = 0) { notificationProvider.showRiskyVenueVisitNotification() }
        verify(exactly = 0) {
            riskyVenueAlertProvider setProperty "riskyVenueAlert" value eq(RiskyVenueAlert(firstVenueId, INFORM))
        }
        verify(exactly = 1) { riskyVenuePollingConfigurationProvider.remove(configuration) }
        verify(exactly = 0) { lastVisitedBookTestTypeVenueDateProvider.lastVisitedVenue = any() }
        coVerify(exactly = 0) { analyticsEventProcessor.track(any()) }
    }

    @Test
    fun `polling responds pending doesn't show notification`() = runBlocking {
        every { riskyVenuePollingConfigurationProvider.configs } returns listOf(configuration)
        coEvery { riskyVenuesCircuitBreakerApi.getRiskyVenuesBreakerResolution(approvalToken) } returns RiskyVenuesCircuitBreakerPollingResponse(
            approval = PENDING
        )

        testSubject()

        verify(exactly = 0) { notificationProvider.showRiskyVenueVisitNotification() }
        verify(exactly = 0) {
            riskyVenueAlertProvider setProperty "riskyVenueAlert" value eq(RiskyVenueAlert(firstVenueId, INFORM))
        }
        verify(exactly = 0) { riskyVenuePollingConfigurationProvider.remove(configuration) }
        verify(exactly = 0) { lastVisitedBookTestTypeVenueDateProvider.lastVisitedVenue = any() }
        coVerify(exactly = 0) { analyticsEventProcessor.track(any()) }
    }

    @Test
    fun `exception fetching resolution for first config and approval yes for second config`() =
        runBlocking {
            every { riskyVenuePollingConfigurationProvider.configs } returns pollingConfigurations
            every { shouldShowRiskyVenueNotification.invoke(BOOK_TEST) } returns true
            coEvery { riskyVenuesCircuitBreakerApi.getRiskyVenuesBreakerResolution(pollingConfigurations[0].approvalToken!!) }.throws(
                Exception()
            )
            coEvery { riskyVenuesCircuitBreakerApi.getRiskyVenuesBreakerResolution(pollingConfigurations[1].approvalToken!!) } returns RiskyVenuesCircuitBreakerPollingResponse(
                approval = YES
            )
            every { riskyVenueConfigurationProvider.durationDays } returns riskyVenueConfigurationDurationDays

            testSubject()

            val firstVenueAlert = RiskyVenueAlert(pollingConfigurations[0].venueId, INFORM)
            val secondVenueAlert = RiskyVenueAlert(pollingConfigurations[1].venueId, BOOK_TEST)

            verify(exactly = 0) {
                riskyVenueAlertProvider setProperty "riskyVenueAlert" value eq(firstVenueAlert)
            }
            verify(exactly = 0) { riskyVenuePollingConfigurationProvider.remove(pollingConfigurations[0]) }
            verify(exactly = 1) { shouldShowRiskyVenueNotification(BOOK_TEST) }
            verify(exactly = 1) { notificationProvider.showRiskyVenueVisitNotification() }
            verify(exactly = 1) {
                riskyVenueAlertProvider setProperty "riskyVenueAlert" value eq(secondVenueAlert)
            }
            verify(exactly = 1) { riskyVenuePollingConfigurationProvider.remove(pollingConfigurations[1]) }
            verify(exactly = 1) {
                lastVisitedBookTestTypeVenueDateProvider.lastVisitedVenue = LastVisitedBookTestTypeVenueDate(
                    secondVenueStartedAt.toLocalDate(fixedClock.zone),
                    riskyVenueConfigurationDurationDays
                )
            }
            coVerify(exactly = 1) { analyticsEventProcessor.track(ReceivedRiskyVenueM2Warning) }
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

            val firstVenueAlert = RiskyVenueAlert(pollingConfigurations[0].venueId, INFORM)
            val secondVenueAlert = RiskyVenueAlert(pollingConfigurations[1].venueId, BOOK_TEST)

            verify(exactly = 0) {
                riskyVenueAlertProvider setProperty "riskyVenueAlert" value eq(firstVenueAlert)
            }
            verify(exactly = 0) { riskyVenuePollingConfigurationProvider.remove(pollingConfigurations[0]) }
            verify(exactly = 0) { notificationProvider.showRiskyVenueVisitNotification() }
            verify(exactly = 0) {
                riskyVenueAlertProvider setProperty "riskyVenueAlert" value eq(secondVenueAlert)
            }
            verify(exactly = 1) { riskyVenuePollingConfigurationProvider.remove(pollingConfigurations[1]) }
            verify(exactly = 0) { lastVisitedBookTestTypeVenueDateProvider.lastVisitedVenue = any() }
            coVerify(exactly = 0) { analyticsEventProcessor.track(any()) }
        }

    @Test
    fun `triggers notification if circuit breaker approves for double inform message types`() = runBlocking {
        every { riskyVenuePollingConfigurationProvider.configs } returns initialConfigurationsDoubleInform
        every { shouldShowRiskyVenueNotification.invoke(INFORM) } returns true
        coEvery { riskyVenuesCircuitBreakerApi.getApproval(any()) } returns
            RiskyVenuesCircuitBreakerResponse(
                approvalToken = approvalToken,
                approval = YES
            )

        testSubject()

        verify(exactly = 1) { notificationProvider.showRiskyVenueVisitNotification() }
        verify(exactly = 1) { shouldShowRiskyVenueNotification.invoke(INFORM) }
        verify(exactly = 1) {
            riskyVenueAlertProvider setProperty "riskyVenueAlert" value eq(RiskyVenueAlert(secondVenueId, INFORM))
        }
        verify(exactly = 0) {
            riskyVenuePollingConfigurationProvider.add(any())
        }
        verify(exactly = 2) { riskyVenuePollingConfigurationProvider.remove(any()) }
        verify(exactly = 0) { lastVisitedBookTestTypeVenueDateProvider.lastVisitedVenue = any() }
        coVerify(exactly = 1) { analyticsEventProcessor.track(ReceivedRiskyVenueM1Warning) }
    }

    @Test
    fun `triggers notification if circuit breaker approves for double book test message types`() = runBlocking {
        every { riskyVenuePollingConfigurationProvider.configs } returns initialConfigurationsDoubleBookTest
        every { shouldShowRiskyVenueNotification.invoke(BOOK_TEST) } returns true
        coEvery { riskyVenuesCircuitBreakerApi.getApproval(any()) } returns
            RiskyVenuesCircuitBreakerResponse(
                approvalToken = approvalToken,
                approval = YES
            )
        every { riskyVenueConfigurationProvider.durationDays } returns riskyVenueConfigurationDurationDays

        testSubject()

        verify(exactly = 1) { shouldShowRiskyVenueNotification.invoke(BOOK_TEST) }
        verify(exactly = 1) { notificationProvider.showRiskyVenueVisitNotification() }
        verify(exactly = 1) {
            riskyVenueAlertProvider setProperty "riskyVenueAlert" value eq(RiskyVenueAlert(secondVenueId, BOOK_TEST))
        }
        verify(exactly = 0) {
            riskyVenuePollingConfigurationProvider.add(any())
        }
        verify(exactly = 2) { riskyVenuePollingConfigurationProvider.remove(any()) }
        verify(exactly = 1) {
            lastVisitedBookTestTypeVenueDateProvider.lastVisitedVenue = LastVisitedBookTestTypeVenueDate(
                secondVenueStartedAt.toLocalDate(fixedClock.zone),
                riskyVenueConfigurationDurationDays
            )
        }
        coVerify(exactly = 1) { analyticsEventProcessor.track(ReceivedRiskyVenueM2Warning) }
    }

    @Test
    fun `triggers notification if circuit breaker approves for double book test message types in reverse order`() =
        runBlocking {
            every { riskyVenuePollingConfigurationProvider.configs } returns initialConfigurationsDoubleBookTestReverseOrder
            every { shouldShowRiskyVenueNotification.invoke(BOOK_TEST) } returns true
            coEvery { riskyVenuesCircuitBreakerApi.getApproval(any()) } returns
                RiskyVenuesCircuitBreakerResponse(
                    approvalToken = approvalToken,
                    approval = YES
                )
            every { riskyVenueConfigurationProvider.durationDays } returns riskyVenueConfigurationDurationDays

            testSubject()

            verify(exactly = 1) { shouldShowRiskyVenueNotification.invoke(BOOK_TEST) }
            verify(exactly = 1) { notificationProvider.showRiskyVenueVisitNotification() }
            verify(exactly = 1) {
                riskyVenueAlertProvider setProperty "riskyVenueAlert" value eq(RiskyVenueAlert(secondVenueId, BOOK_TEST))
            }
            verify(exactly = 0) {
                riskyVenuePollingConfigurationProvider.add(any())
            }
            verify(exactly = 2) { riskyVenuePollingConfigurationProvider.remove(any()) }
            verify(exactly = 1) {
                lastVisitedBookTestTypeVenueDateProvider.lastVisitedVenue = LastVisitedBookTestTypeVenueDate(
                    secondVenueStartedAt.toLocalDate(fixedClock.zone),
                    riskyVenueConfigurationDurationDays
                )
            }
            coVerify(exactly = 1) { analyticsEventProcessor.track(ReceivedRiskyVenueM2Warning) }
        }

    @Test
    fun `triggers notification if circuit breaker approves for book test then inform message types`() = runBlocking {
        every { riskyVenuePollingConfigurationProvider.configs } returns initialConfigurationsBookTestThenInform
        every { shouldShowRiskyVenueNotification.invoke(BOOK_TEST) } returns true
        coEvery { riskyVenuesCircuitBreakerApi.getApproval(any()) } returns
            RiskyVenuesCircuitBreakerResponse(
                approvalToken = approvalToken,
                approval = YES
            )
        every { riskyVenueConfigurationProvider.durationDays } returns riskyVenueConfigurationDurationDays

        testSubject()

        verify(exactly = 1) { shouldShowRiskyVenueNotification.invoke(BOOK_TEST) }
        verify(exactly = 1) { notificationProvider.showRiskyVenueVisitNotification() }
        verify(exactly = 1) {
            riskyVenueAlertProvider setProperty "riskyVenueAlert" value eq(RiskyVenueAlert(firstVenueId, BOOK_TEST))
        }
        verify(exactly = 0) {
            riskyVenuePollingConfigurationProvider.add(any())
        }
        verify(exactly = 2) { riskyVenuePollingConfigurationProvider.remove(any()) }
        verify(exactly = 1) {
            lastVisitedBookTestTypeVenueDateProvider.lastVisitedVenue = LastVisitedBookTestTypeVenueDate(
                firstVenueStartedAt.toLocalDate(fixedClock.zone),
                riskyVenueConfigurationDurationDays
            )
        }
        coVerify(exactly = 1) { analyticsEventProcessor.track(ReceivedRiskyVenueM2Warning) }
    }

    @Test
    fun `triggers notification if circuit breaker approves for inform then book test message types`() = runBlocking {
        every { riskyVenuePollingConfigurationProvider.configs } returns initialConfigurationsInformThenBookTest
        every { shouldShowRiskyVenueNotification.invoke(BOOK_TEST) } returns true
        coEvery { riskyVenuesCircuitBreakerApi.getApproval(any()) } returns
            RiskyVenuesCircuitBreakerResponse(
                approvalToken = approvalToken,
                approval = YES
            )
        every { riskyVenueConfigurationProvider.durationDays } returns riskyVenueConfigurationDurationDays

        testSubject()

        verify(exactly = 1) { shouldShowRiskyVenueNotification.invoke(BOOK_TEST) }
        verify(exactly = 1) { notificationProvider.showRiskyVenueVisitNotification() }
        verify(exactly = 1) {
            riskyVenueAlertProvider setProperty "riskyVenueAlert" value eq(RiskyVenueAlert(secondVenueId, BOOK_TEST))
        }
        verify(exactly = 0) {
            riskyVenuePollingConfigurationProvider.add(any())
        }
        verify(exactly = 2) { riskyVenuePollingConfigurationProvider.remove(any()) }
        verify(exactly = 1) {
            lastVisitedBookTestTypeVenueDateProvider.lastVisitedVenue = LastVisitedBookTestTypeVenueDate(
                secondVenueStartedAt.toLocalDate(fixedClock.zone),
                riskyVenueConfigurationDurationDays
            )
        }
        coVerify(exactly = 1) { analyticsEventProcessor.track(ReceivedRiskyVenueM2Warning) }
    }

    @Test
    fun `does not trigger notification if circuit breaker doesn't approve`() = runBlocking {
        coEvery { riskyVenuesCircuitBreakerApi.getApproval(any()) } returns RiskyVenuesCircuitBreakerResponse(
            approvalToken = approvalToken,
            approval = NO
        )
        every { riskyVenuePollingConfigurationProvider.configs } returns initialConfigurationsDoubleInform

        testSubject()

        verify(exactly = 0) { notificationProvider.showRiskyVenueVisitNotification() }
        verify(exactly = 0) {
            riskyVenueAlertProvider setProperty "riskyVenueAlert" value eq(RiskyVenueAlert(firstVenueId, INFORM))
        }
        verify(exactly = 2) { riskyVenuePollingConfigurationProvider.remove(any()) }
        verify(exactly = 0) {
            riskyVenuePollingConfigurationProvider.add(any())
        }
        coVerify(exactly = 0) { analyticsEventProcessor.track(any()) }
    }

    @Test
    fun `will start polling if circuit breaker response pending`() = runBlocking {
        every { riskyVenuePollingConfigurationProvider.configs } returns initialConfigurationsDoubleInform
        coEvery { riskyVenuesCircuitBreakerApi.getApproval(any()) } returns
            RiskyVenuesCircuitBreakerResponse(
                approvalToken = approvalToken,
                approval = PENDING
            )

        testSubject()

        verify(exactly = 0) { notificationProvider.showRiskyVenueVisitNotification() }
        verify(exactly = 0) {
            riskyVenueAlertProvider setProperty "riskyVenueAlert" value eq(RiskyVenueAlert(firstVenueId, INFORM))
        }
        verify(exactly = 2) { riskyVenuePollingConfigurationProvider.remove(any()) }
        verify(exactly = 2) {
            riskyVenuePollingConfigurationProvider.add(any())
        }
        coVerify(exactly = 0) { analyticsEventProcessor.track(any()) }
    }
}
