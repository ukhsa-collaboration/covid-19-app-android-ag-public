package uk.nhs.nhsx.covid19.android.app.flow.analytics

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.NoIndexCaseThenIsolationDueToSelfAssessment
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.PositiveSymptomsNoIsolationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.QuestionnaireRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ReviewSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SymptomsAdviceIsolateRobot

class QuestionnaireAnalyticsTest : AnalyticsTest() {
    private val reviewSymptomsRobot = ReviewSymptomsRobot()
    private val statusRobot = StatusRobot()
    private val questionnaireRobot = QuestionnaireRobot()
    private val symptomsAdviceIsolateRobot = SymptomsAdviceIsolateRobot()
    private val positiveSymptomsNoIsolationRobot = PositiveSymptomsNoIsolationRobot()

    @Test
    fun reportSymptomsSelfIsolationEnabledWales() {
        // Current date: 1st Jan
        // Starting state - app running normally
        runBackgroundTasks()

        // Current date: 2nd Jan - Analytics packet for: 1st Jan
        assertAnalyticsPacketIsNormal()
        reportSymptomsWales(true)

        symptomsAdviceIsolateRobot.checkActivityIsDisplayed()

        symptomsAdviceIsolateRobot.checkViewState(
            NoIndexCaseThenIsolationDueToSelfAssessment(testAppContext.getRemainingDaysInIsolation())
        )

        testAppContext.device.pressBack()

        statusRobot.checkActivityIsDisplayed()
        statusRobot.checkIsolationViewIsDisplayed()

        assertOnFields {
            assertEquals(0, Metrics::completedV3SymptomsQuestionnaireAndHasSymptoms)
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertEquals(1, Metrics::startedIsolation)
        }

        // Still in isolation
        assertOnFieldsForDateRange(dateRange = 4..11) {
            assertPresent(Metrics::isIsolatingBackgroundTick)
        }

        assertAnalyticsPacketIsNormal()
    }

    @Test
    fun reportSymptomsSelfIsolationDisabledWales() {
        // Current date: 1st Jan
        // Starting state - app running normally
        runBackgroundTasks()

        // Current date: 2nd Jan - Analytics packet for: 1st Jan
        assertAnalyticsPacketIsNormal()
        reportSymptomsWales(false)

        waitFor { positiveSymptomsNoIsolationRobot.checkIsPositiveSymptomsNoIsolationTitleDisplayed() }

        testAppContext.device.pressBack()

        statusRobot.checkActivityIsDisplayed()

        assertOnFields {
            assertEquals(1, Metrics::completedV3SymptomsQuestionnaireAndHasSymptoms)
        }

        assertAnalyticsPacketIsNormal()
    }

    private fun reportSymptomsWales(isSelfIsolationEnabled: Boolean) {
        testAppContext.questionnaireApi.isSymptomaticSelfIsolationForWalesEnabled = isSelfIsolationEnabled
        givenLocalAuthorityIsInWales()
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.clickReportSymptoms()

        questionnaireRobot.checkActivityIsDisplayed()

        questionnaireRobot.selectSymptomsAtPositions(0, 1, 2)

        questionnaireRobot.reviewSymptoms()

        reviewSymptomsRobot.checkActivityIsDisplayed()

        if (isSelfIsolationEnabled) {
            reviewSymptomsRobot.selectCannotRememberDate()
        }

        reviewSymptomsRobot.confirmSelection()
    }
}
