package uk.nhs.nhsx.covid19.android.app.flow.functionalities

import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.QuestionnaireRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ReviewSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SymptomsAdviceIsolateRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.waitFor
import kotlin.test.assertTrue

class SelfDiagnosis(
    private val espressoTest: EspressoTest
) {
    private val statusRobot = StatusRobot()
    private val questionnaireRobot = QuestionnaireRobot()
    private val reviewSymptomsRobot = ReviewSymptomsRobot()
    private val symptomsAdviceIsolateRobot = SymptomsAdviceIsolateRobot()
    private val orderTest = OrderTest(espressoTest)

    private fun selfDiagnosePositive() {
        statusRobot.checkActivityIsDisplayed()

        val isContactCase = (espressoTest.testAppContext.getCurrentState() as? Isolation)?.isContactCase() ?: false

        statusRobot.clickReportSymptoms()

        waitFor { questionnaireRobot.checkActivityIsDisplayed() }

        questionnaireRobot.selectSymptomsAtPositions(0, 1, 2)

        questionnaireRobot.reviewSymptoms()

        reviewSymptomsRobot.confirmReviewSymptomsScreenIsDisplayed()

        reviewSymptomsRobot.selectCannotRememberDate()

        reviewSymptomsRobot.confirmSelection()

        val newState = espressoTest.testAppContext.getCurrentState()
        assertTrue(newState is Isolation)
        if (isContactCase) {
            assertTrue(newState.isBothCases())
        } else {
            assertTrue(newState.isIndexCaseOnly())
        }

        symptomsAdviceIsolateRobot.checkActivityIsDisplayed()
    }

    fun selfDiagnosePositiveAndPressBack() {
        selfDiagnosePositive()
        espressoTest.testAppContext.device.pressBack()
    }

    fun selfDiagnosePositiveAndOrderTest(receiveResultImmediately: Boolean) {
        espressoTest.testAppContext.virologyTestingApi.pollingTestResultHttpStatusCode =
            if (receiveResultImmediately) 200 else 204

        selfDiagnosePositive()

        symptomsAdviceIsolateRobot.clickBottomActionButton()

        orderTest()
    }
}
