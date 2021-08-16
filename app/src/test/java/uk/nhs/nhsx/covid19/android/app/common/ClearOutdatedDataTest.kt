package uk.nhs.nhsx.covid19.android.app.common

import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogStorage
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationTokensProvider
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.EpidemiologyEventProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDateProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class ClearOutdatedDataTest {

    private val resetIsolationStateIfNeeded = mockk<ResetIsolationStateIfNeeded>(relaxUnitFun = true)
    private val lastVisitedBookTestTypeVenueDateProvider =
        mockk<LastVisitedBookTestTypeVenueDateProvider>(relaxUnitFun = true)
    private val clearOutdatedKeySharingInfo = mockk<ClearOutdatedKeySharingInfo>(relaxUnitFun = true)
    private val isolationConfigurationProvider = mockk<IsolationConfigurationProvider>()
    private val epidemiologyEventProvider = mockk<EpidemiologyEventProvider>(relaxUnitFun = true)
    private val exposureNotificationTokensProvider = mockk<ExposureNotificationTokensProvider>(relaxUnitFun = true)
    private val analyticsLogStorage = mockk<AnalyticsLogStorage>(relaxUnitFun = true)
    private val fixedClock = Clock.fixed(Instant.parse("2021-01-01T00:00:00.00Z"), ZoneOffset.UTC)

    private val clearOutdatedData = ClearOutdatedData(
        resetIsolationStateIfNeeded,
        lastVisitedBookTestTypeVenueDateProvider,
        clearOutdatedKeySharingInfo,
        isolationConfigurationProvider,
        epidemiologyEventProvider,
        exposureNotificationTokensProvider,
        analyticsLogStorage,
        fixedClock
    )

    private val expectedIsolationDurationDays = mockk<DurationDays>()
    private val expectedRetentionPeriod = 14
    private val expectedLocalDateForEpidemiologyEventCleanUp =
        LocalDate.now(fixedClock)
            .minusDays(expectedRetentionPeriod.toLong())
    private val expectedInstantForAnalyticsLogsCleanUp =
        Instant.now(fixedClock)
            .truncatedTo(ChronoUnit.DAYS)
            .minus(expectedRetentionPeriod.toLong(), ChronoUnit.DAYS)

    @Before
    fun setUp() {
        every { isolationConfigurationProvider.durationDays } returns expectedIsolationDurationDays
        every { expectedIsolationDurationDays.pendingTasksRetentionPeriod } returns expectedRetentionPeriod
    }

    @Test
    fun `verify call order when clearing outdated data and a date is stored for a book a test type venue visit`() {
        every { lastVisitedBookTestTypeVenueDateProvider.containsBookTestTypeVenueAtRisk() } returns true

        clearOutdatedData()

        verifyOrder {
            resetIsolationStateIfNeeded()
            clearOutdatedKeySharingInfo()
            epidemiologyEventProvider.clearOnAndBefore(expectedLocalDateForEpidemiologyEventCleanUp)
            analyticsLogStorage.removeBeforeOrEqual(expectedInstantForAnalyticsLogsCleanUp)
            exposureNotificationTokensProvider.clear()
        }
    }

    @Test
    fun `verify call order when clearing outdated data and no date is stored for a book a test type venue visit`() {
        every { lastVisitedBookTestTypeVenueDateProvider.containsBookTestTypeVenueAtRisk() } returns false

        clearOutdatedData()

        verifyOrder {
            resetIsolationStateIfNeeded()
            lastVisitedBookTestTypeVenueDateProvider setProperty "lastVisitedVenue" value null
            clearOutdatedKeySharingInfo()
            epidemiologyEventProvider.clearOnAndBefore(expectedLocalDateForEpidemiologyEventCleanUp)
            analyticsLogStorage.removeBeforeOrEqual(expectedInstantForAnalyticsLogsCleanUp)
            exposureNotificationTokensProvider.clear()
        }
    }
}
