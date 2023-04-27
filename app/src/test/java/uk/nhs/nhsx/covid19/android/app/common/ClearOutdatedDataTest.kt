package uk.nhs.nhsx.covid19.android.app.common

import com.jeroenmols.featureflag.framework.FeatureFlag.DECOMMISSIONING_CLOSURE_SCREEN
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogStorage
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationTokensProvider
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.EpidemiologyEventProvider
import uk.nhs.nhsx.covid19.android.app.onboarding.OnboardingCompletedProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDateProvider
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.LastCompletedV2SymptomsQuestionnaireDateProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.CountrySpecificConfiguration
import uk.nhs.nhsx.covid19.android.app.settings.DeleteAllUserData
import uk.nhs.nhsx.covid19.android.app.state.GetLatestConfiguration
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeature
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
    private val clearOutdatedTestOrderPollingConfigs = mockk<ClearOutdatedTestOrderPollingConfigs>(relaxUnitFun = true)
    private val getLatestConfiguration = mockk<GetLatestConfiguration>()
    private val epidemiologyEventProvider = mockk<EpidemiologyEventProvider>(relaxUnitFun = true)
    private val exposureNotificationTokensProvider = mockk<ExposureNotificationTokensProvider>(relaxUnitFun = true)
    private val analyticsLogStorage = mockk<AnalyticsLogStorage>(relaxUnitFun = true)
    private val fixedClock = Clock.fixed(Instant.parse("2021-01-01T00:00:00.00Z"), ZoneOffset.UTC)
    private val lastCompletedV2SymptomsQuestionnaireDateProvider = mockk<LastCompletedV2SymptomsQuestionnaireDateProvider>(relaxUnitFun = true)
    private val deleteAllUserData = mockk<DeleteAllUserData>(relaxUnitFun = true)
    private val onboardingCompletedProvider = mockk<OnboardingCompletedProvider>()

    private val clearOutdatedData = ClearOutdatedData(
        resetIsolationStateIfNeeded,
        lastVisitedBookTestTypeVenueDateProvider,
        clearOutdatedKeySharingInfo,
        clearOutdatedTestOrderPollingConfigs,
        epidemiologyEventProvider,
        exposureNotificationTokensProvider,
        analyticsLogStorage,
        getLatestConfiguration,
        lastCompletedV2SymptomsQuestionnaireDateProvider,
        fixedClock,
        deleteAllUserData,
        onboardingCompletedProvider
    )

    private val configuration = mockk<CountrySpecificConfiguration>()
    private val retentionPeriod = 14
    private val expectedLocalDateForEpidemiologyEventCleanUp =
        LocalDate.now(fixedClock)
            .minusDays(retentionPeriod.toLong())
    private val expectedInstantForAnalyticsLogsCleanUp =
        Instant.now(fixedClock)
            .truncatedTo(ChronoUnit.DAYS)
            .minus(retentionPeriod.toLong(), ChronoUnit.DAYS)

    @Before
    fun setUp() {
        every { getLatestConfiguration() } returns configuration
        every { configuration.pendingTasksRetentionPeriod } returns retentionPeriod
    }

    @Test
    fun `app is in decommissioning state and onboarding completed flag is still set to true, all data should be deleted or reset`() {
        runWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, enabled = true) {
            every { onboardingCompletedProvider.value } returns true

            clearOutdatedData()

            verify { deleteAllUserData(shouldKeepLanguage = true) }
            verify(exactly = 0) { resetIsolationStateIfNeeded() }
        }
    }

    @Test
    fun `app is in decommissioning state and onboarding completed flag is not set to true should return without calling anything else`() {
        runWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, enabled = true) {
            every { onboardingCompletedProvider.value } returns false

            clearOutdatedData()

            verify(exactly = 0) { deleteAllUserData(shouldKeepLanguage = true) }
            verify(exactly = 0) { resetIsolationStateIfNeeded() }
        }
    }

    @Test
    fun `verify call order when clearing outdated data and a date is stored for a book a test type venue visit and v2 symptoms questionnaire`() {
        runWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, enabled = false) {
            every { lastVisitedBookTestTypeVenueDateProvider.containsBookTestTypeVenueAtRisk() } returns true
            every { lastCompletedV2SymptomsQuestionnaireDateProvider.containsCompletedV2SymptomsQuestionnaire() } returns true
            every { lastCompletedV2SymptomsQuestionnaireDateProvider.containsCompletedV2SymptomsQuestionnaireAndTryToStayAtHomeResult() } returns true

            clearOutdatedData()

            verifyOrder {
                resetIsolationStateIfNeeded()
                clearOutdatedKeySharingInfo()
                clearOutdatedTestOrderPollingConfigs()
                epidemiologyEventProvider.clearOnAndBefore(expectedLocalDateForEpidemiologyEventCleanUp)
                analyticsLogStorage.removeBeforeOrEqual(expectedInstantForAnalyticsLogsCleanUp)
                exposureNotificationTokensProvider.clear()
            }
        }
    }

    @Test
    fun `verify call order when clearing outdated data and no date is stored for a book a test type venue visit`() {
        runWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, enabled = false) {
            every { lastVisitedBookTestTypeVenueDateProvider.containsBookTestTypeVenueAtRisk() } returns false
            every { lastCompletedV2SymptomsQuestionnaireDateProvider.containsCompletedV2SymptomsQuestionnaire() } returns true
            every { lastCompletedV2SymptomsQuestionnaireDateProvider.containsCompletedV2SymptomsQuestionnaireAndTryToStayAtHomeResult() } returns true

            clearOutdatedData()

            verifyOrder {
                resetIsolationStateIfNeeded()
                lastVisitedBookTestTypeVenueDateProvider setProperty "lastVisitedVenue" value null
                clearOutdatedKeySharingInfo()
                clearOutdatedTestOrderPollingConfigs()
                epidemiologyEventProvider.clearOnAndBefore(expectedLocalDateForEpidemiologyEventCleanUp)
                analyticsLogStorage.removeBeforeOrEqual(expectedInstantForAnalyticsLogsCleanUp)
                exposureNotificationTokensProvider.clear()
            }
        }
    }

    @Test
    fun `verify call order when clearing outdated data and no dates are stored for v2 symptoms questionnaire with result`() {
        runWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, enabled = false) {
            every { lastVisitedBookTestTypeVenueDateProvider.containsBookTestTypeVenueAtRisk() } returns true
            every { lastCompletedV2SymptomsQuestionnaireDateProvider.containsCompletedV2SymptomsQuestionnaire() } returns false
            every { lastCompletedV2SymptomsQuestionnaireDateProvider.containsCompletedV2SymptomsQuestionnaireAndTryToStayAtHomeResult() } returns false

            clearOutdatedData()

            verifyOrder {
                resetIsolationStateIfNeeded()
                lastCompletedV2SymptomsQuestionnaireDateProvider setProperty "lastCompletedV2SymptomsQuestionnaire" value null
                lastCompletedV2SymptomsQuestionnaireDateProvider setProperty "lastCompletedV2SymptomsQuestionnaireAndStayAtHome" value null
                clearOutdatedKeySharingInfo()
                clearOutdatedTestOrderPollingConfigs()
                epidemiologyEventProvider.clearOnAndBefore(expectedLocalDateForEpidemiologyEventCleanUp)
                analyticsLogStorage.removeBeforeOrEqual(expectedInstantForAnalyticsLogsCleanUp)
                exposureNotificationTokensProvider.clear()
            }
        }
    }
}
