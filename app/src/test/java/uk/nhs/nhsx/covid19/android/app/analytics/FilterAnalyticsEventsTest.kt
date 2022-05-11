package uk.nhs.nhsx.covid19.android.app.analytics

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FilterAnalyticsEventsTest {
    private val analyticsFilterProvider = mockk<AnalyticsFilterProvider>()
    private val testSubject = FilterAnalyticsEvents(analyticsFilterProvider)

    @Test
    fun `given risky contact filter enabled should null metrics`() = runBlocking {
        val expectedResult = metrics.copy(
            acknowledgedStartOfIsolationDueToRiskyContact = null,
            isIsolatingForHadRiskyContactBackgroundTick = null
        )
        coEvery { analyticsFilterProvider.invoke() } returns analyticsFilter.copy(shouldFilterRiskyContactInfo = true)

        val result = testSubject.invoke(metrics = metrics)

        assertEquals(result, expectedResult)
    }

    @Test
    fun `given risky contact filter disabled should not null any metrics`() = runBlocking {
        val expectedResult = metrics.copy()
        coEvery { analyticsFilterProvider.invoke() } returns analyticsFilter

        val result = testSubject.invoke(metrics = metrics)

        assertNotNull(result.receivedActiveIpcToken)
        assertEquals(result, expectedResult)
    }

    @Test
    fun `given isolation hub filter enabled should null metrics`() = runBlocking {
        val expectedResult = metrics.copy(
            didAccessSelfIsolationNoteLink = null,
            receivedActiveIpcToken = null,
            haveActiveIpcTokenBackgroundTick = null,
            selectedIsolationPaymentsButton = null,
            launchedIsolationPaymentsApplication = null
        )

        coEvery { analyticsFilterProvider.invoke() } returns analyticsFilter.copy(shouldFilterSelfIsolation = true)

        val result = testSubject.invoke(metrics = metrics)

        assertEquals(result, expectedResult)
    }

    @Test
    fun `given isolation hub filter disabled should not null metrics`() = runBlocking {
        val expectedResult = metrics.copy()
        coEvery { analyticsFilterProvider.invoke() } returns analyticsFilter.copy()

        val result = testSubject.invoke(metrics = metrics)

        assertNotNull(result.acknowledgedStartOfIsolationDueToRiskyContact)
        assertEquals(result, expectedResult)
    }

    @Test
    fun `given venue check-in filter enabled should null metrics`() = runBlocking {
        val expectedResult = metrics.copy(
            receivedRiskyVenueM2Warning = null,
            hasReceivedRiskyVenueM2WarningBackgroundTick = null,
            didAccessRiskyVenueM2Notification = null,
            selectedTakeTestM2Journey = null,
            selectedTakeTestLaterM2Journey = null,
            selectedHasSymptomsM2Journey = null,
            selectedHasNoSymptomsM2Journey = null,
            selectedLFDTestOrderingM2Journey = null,
            selectedHasLFDTestM2Journey = null,
            receivedRiskyVenueM1Warning = null
        )
        coEvery { analyticsFilterProvider.invoke() } returns analyticsFilter.copy(shouldFilterVenueCheckIn = true)

        val result = testSubject.invoke(metrics = metrics)

        assertEquals(result, expectedResult)
    }

    @Test
    fun `given venue check-in filter disabled should not null metrics`() = runBlocking {
        val expectedResult = metrics.copy()
        coEvery { analyticsFilterProvider.invoke() } returns analyticsFilter.copy()

        val result = testSubject.invoke(metrics = metrics)

        assertNotNull(result.receivedRiskyVenueM2Warning)
        assertEquals(result, expectedResult)
    }

    @Test
    fun `given custom filter has values should null metrics`() = runBlocking {
        val expectedResult = metrics.copy()

        coEvery { analyticsFilterProvider.invoke() } returns analyticsFilter.copy(
            enabledCustomAnalyticsFilters = listOf()
        )

        val result = testSubject.invoke(metrics = metrics)

        assertEquals(result, expectedResult)
    }

    @Test
    fun `given custom filter is empty should not null metrics`() = runBlocking {
        val expectedResult = metrics.copy()

        coEvery { analyticsFilterProvider.invoke() } returns analyticsFilter

        val result = testSubject.invoke(metrics = metrics)

        assertEquals(result, expectedResult)
    }

    private val analyticsFilter = AnalyticsFilter(
        shouldFilterRiskyContactInfo = false,
        shouldFilterSelfIsolation = false,
        shouldFilterVenueCheckIn = false,
        enabledCustomAnalyticsFilters = listOf()
    )

    private val metrics = Metrics()
}
