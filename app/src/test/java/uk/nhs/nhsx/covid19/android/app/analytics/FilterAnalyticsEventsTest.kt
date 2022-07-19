package uk.nhs.nhsx.covid19.android.app.analytics

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.CustomAnalyticsFilter.COMPLETED_QUESTIONNAIRE_AND_STARTED_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.CustomAnalyticsFilter.DID_ASK_FOR_SYMPTOMS_ON_POSITIVE_TEST_ENTRY
import uk.nhs.nhsx.covid19.android.app.analytics.CustomAnalyticsFilter.HAS_COMPLETED_V2_SYMPTOMS_QUESTIONNAIRE_AND_STAY_AT_HOME_BACKGROUND_TICK
import uk.nhs.nhsx.covid19.android.app.analytics.CustomAnalyticsFilter.HAS_COMPLETED_V2_SYMPTOMS_QUESTIONNAIRE_BACKGROUND_TICK
import uk.nhs.nhsx.covid19.android.app.analytics.CustomAnalyticsFilter.HAS_SELF_DIAGNOSED_BACKGROUND_TICK
import uk.nhs.nhsx.covid19.android.app.analytics.CustomAnalyticsFilter.HAS_TESTED_LFD_POSITIVE_BACKGROUND_TICK
import uk.nhs.nhsx.covid19.android.app.analytics.CustomAnalyticsFilter.HAS_TESTED_POSITIVE_BACKGROUND_TICK
import uk.nhs.nhsx.covid19.android.app.analytics.CustomAnalyticsFilter.HAS_TESTED_SELF_RAPID_POSITIVE_BACKGROUND_TICK
import uk.nhs.nhsx.covid19.android.app.analytics.CustomAnalyticsFilter.IS_ISOLATING_FOR_SELF_DIAGNOSED_BACKGROUND_TICK
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
        val expectedResult = metrics.copy(
            didAskForSymptomsOnPositiveTestEntry = null,
            isIsolatingForSelfDiagnosedBackgroundTick = null,
            completedQuestionnaireAndStartedIsolation = null,
            hasTestedPositiveBackgroundTick = null,
            hasTestedLFDPositiveBackgroundTick = null,
            hasTestedSelfRapidPositiveBackgroundTick = null,
            hasCompletedV2SymptomsQuestionnaireAndStayAtHomeBackgroundTick = null,
            hasCompletedV2SymptomsQuestionnaireBackgroundTick = null,
            hasSelfDiagnosedBackgroundTick = null,
        )

        coEvery { analyticsFilterProvider.invoke() } returns analyticsFilter.copy(
            enabledCustomAnalyticsFilters = listOf(
                DID_ASK_FOR_SYMPTOMS_ON_POSITIVE_TEST_ENTRY,
                IS_ISOLATING_FOR_SELF_DIAGNOSED_BACKGROUND_TICK,
                COMPLETED_QUESTIONNAIRE_AND_STARTED_ISOLATION,
                HAS_SELF_DIAGNOSED_BACKGROUND_TICK,
                HAS_TESTED_LFD_POSITIVE_BACKGROUND_TICK,
                HAS_TESTED_SELF_RAPID_POSITIVE_BACKGROUND_TICK,
                HAS_COMPLETED_V2_SYMPTOMS_QUESTIONNAIRE_AND_STAY_AT_HOME_BACKGROUND_TICK,
                HAS_COMPLETED_V2_SYMPTOMS_QUESTIONNAIRE_BACKGROUND_TICK,
                HAS_TESTED_POSITIVE_BACKGROUND_TICK
            )
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
