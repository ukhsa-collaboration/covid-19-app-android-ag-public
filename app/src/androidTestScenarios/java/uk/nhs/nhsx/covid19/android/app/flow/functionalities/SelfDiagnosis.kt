package uk.nhs.nhsx.covid19.android.app.flow.functionalities

import kotlin.test.assertTrue
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.BrowserRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.QuestionnaireRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ReviewSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SymptomsAdviceIsolateRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestOrderingRobot

class SelfDiagnosis(
    private val espressoTest: EspressoTest
) {
    private val statusRobot = StatusRobot()
    private val questionnaireRobot = QuestionnaireRobot()
    private val reviewSymptomsRobot = ReviewSymptomsRobot()
    private val symptomsAdviceIsolateRobot = SymptomsAdviceIsolateRobot()
    private val testOrderingRobot = TestOrderingRobot()
    private val browserRobot = BrowserRobot()

    private fun selfDiagnosePositive() {
        statusRobot.checkActivityIsDisplayed()

        statusRobot.clickReportSymptoms()

        questionnaireRobot.checkActivityIsDisplayed()

        questionnaireRobot.selectSymptomsAtPositions(0, 1, 2)

        questionnaireRobot.reviewSymptoms()

        reviewSymptomsRobot.confirmReviewSymptomsScreenIsDisplayed()

        reviewSymptomsRobot.selectCannotRememberDate()

        reviewSymptomsRobot.confirmSelection()

        assertTrue { (espressoTest.testAppContext.getCurrentState() as Isolation).isIndexCaseOnly() }

        symptomsAdviceIsolateRobot.checkActivityIsDisplayed()
    }

    fun selfDiagnosePositiveAndPressBack() {
        selfDiagnosePositive()
        espressoTest.testAppContext.device.pressBack()
    }

    fun selfDiagnosePositiveAndOrderTest() {
        espressoTest.testAppContext.virologyTestingApi.pollingTestResultHttpStatusCode = 204

        selfDiagnosePositive()

        symptomsAdviceIsolateRobot.clickBottomActionButton()

        testOrderingRobot.checkActivityIsDisplayed()

        testOrderingRobot.clickOrderTestButton()

        espressoTest.waitFor { browserRobot.checkActivityIsDisplayed() }

        browserRobot.clickCloseButton()
    }
}
