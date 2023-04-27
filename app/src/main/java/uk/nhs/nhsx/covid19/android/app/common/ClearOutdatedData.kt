@file:Suppress("DEPRECATION")
package uk.nhs.nhsx.covid19.android.app.common

import com.jeroenmols.featureflag.framework.FeatureFlag.DECOMMISSIONING_CLOSURE_SCREEN
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogStorage
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationTokensProvider
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.EpidemiologyEventProvider
import uk.nhs.nhsx.covid19.android.app.onboarding.OnboardingCompletedProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDateProvider
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.LastCompletedV2SymptomsQuestionnaireDateProvider
import uk.nhs.nhsx.covid19.android.app.settings.DeleteAllUserData
import uk.nhs.nhsx.covid19.android.app.state.GetLatestConfiguration
import uk.nhs.nhsx.covid19.android.app.util.defaultFalse
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS
import javax.inject.Inject

class ClearOutdatedData @Inject constructor(
    private val resetIsolationStateIfNeeded: ResetIsolationStateIfNeeded,
    private val lastVisitedBookTestTypeVenueDateProvider: LastVisitedBookTestTypeVenueDateProvider,
    private val clearOutdatedKeySharingInfo: ClearOutdatedKeySharingInfo,
    private val clearOutdatedTestOrderPollingConfigs: ClearOutdatedTestOrderPollingConfigs,
    private val epidemiologyEventProvider: EpidemiologyEventProvider,
    private val exposureNotificationTokensProvider: ExposureNotificationTokensProvider,
    private val analyticsLogStorage: AnalyticsLogStorage,
    private val getLatestConfiguration: GetLatestConfiguration,
    private val lastCompletedV2SymptomsQuestionnaireDateProvider: LastCompletedV2SymptomsQuestionnaireDateProvider,
    private val clock: Clock,
    private val deleteAllUserData: DeleteAllUserData,
    private val onboardingCompletedProvider: OnboardingCompletedProvider
) {

    operator fun invoke() {
        runCatching {
            Timber.d("Clearing outdated data")
            if (RuntimeBehavior.isFeatureEnabled(DECOMMISSIONING_CLOSURE_SCREEN)) {
                val isOnboardingCompleted = onboardingCompletedProvider.value.defaultFalse()
                if (isOnboardingCompleted) {
                    deleteAllUserData(shouldKeepLanguage = true)
                }
                return
            }

            resetIsolationStateIfNeeded()

            if (!lastVisitedBookTestTypeVenueDateProvider.containsBookTestTypeVenueAtRisk()) {
                lastVisitedBookTestTypeVenueDateProvider.lastVisitedVenue = null
            }

            if (!lastCompletedV2SymptomsQuestionnaireDateProvider.containsCompletedV2SymptomsQuestionnaire()) {
                lastCompletedV2SymptomsQuestionnaireDateProvider.lastCompletedV2SymptomsQuestionnaire = null
            }

            if (!lastCompletedV2SymptomsQuestionnaireDateProvider.containsCompletedV2SymptomsQuestionnaireAndTryToStayAtHomeResult()) {
                lastCompletedV2SymptomsQuestionnaireDateProvider.lastCompletedV2SymptomsQuestionnaireAndStayAtHome = null
            }

            clearOutdatedKeySharingInfo()
            clearOutdatedTestOrderPollingConfigs()

            val retentionPeriodDays = getLatestConfiguration().pendingTasksRetentionPeriod
            clearOldEpidemiologyEvents(retentionPeriodDays)
            clearOutdatedAnalyticsLogs(retentionPeriodDays)

            clearLegacyData()
        }
    }

    private fun clearOldEpidemiologyEvents(retentionPeriodDays: Int) {
        epidemiologyEventProvider.clearOnAndBefore(LocalDate.now(clock).minusDays(retentionPeriodDays.toLong()))
    }

    private fun clearOutdatedAnalyticsLogs(retentionPeriodDays: Int) {
        val startOfToday = Instant.now(clock).truncatedTo(DAYS)
        val outdatedThreshold = startOfToday.minus(retentionPeriodDays.toLong(), DAYS)
        analyticsLogStorage.removeBeforeOrEqual(outdatedThreshold)
    }

    private fun clearLegacyData() {
        exposureNotificationTokensProvider.clear()
    }
}
