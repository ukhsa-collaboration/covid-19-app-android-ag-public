package uk.nhs.nhsx.covid19.android.app.flow.functionalities

import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.CannotRememberDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.ExplicitDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.NotStated
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.QuestionnaireRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ReviewSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SymptomsAdviceIsolateRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.waitFor
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SelfDiagnosis(
    private val espressoTest: EspressoTest
) {
    private val statusRobot = StatusRobot()
    private val questionnaireRobot = QuestionnaireRobot()
    private val reviewSymptomsRobot = ReviewSymptomsRobot()
    private val symptomsAdviceIsolateRobot = SymptomsAdviceIsolateRobot()
    private val orderTest = OrderTest(espressoTest)

    private fun selfDiagnosePositive(selectedDate: SelectedDate) {
        statusRobot.checkActivityIsDisplayed()

        val isContactCase = (espressoTest.testAppContext.getCurrentLogicalState() as? PossiblyIsolating)
            ?.isActiveContactCase(espressoTest.testAppContext.clock)
            ?: false

        statusRobot.clickReportSymptoms()

        waitFor { questionnaireRobot.checkActivityIsDisplayed() }

        questionnaireRobot.selectSymptomsAtPositions(0, 1, 2)

        questionnaireRobot.reviewSymptoms()

        reviewSymptomsRobot.checkActivityIsDisplayed()

        when (selectedDate) {
            CannotRememberDate -> reviewSymptomsRobot.selectCannotRememberDate()
            is ExplicitDate -> {
                reviewSymptomsRobot.clickSelectDate()
                reviewSymptomsRobot.selectDayOfMonth(selectedDate.date.dayOfMonth)
            }
            NotStated -> throw Exception("Self diagnosis onset date not stated")
        }

        reviewSymptomsRobot.confirmSelection()

        val newState = espressoTest.testAppContext.getCurrentLogicalState()
        assertTrue(newState is PossiblyIsolating)
        assertTrue(newState.isActiveIndexCase(espressoTest.testAppContext.clock))
        if (isContactCase) {
            assertTrue(newState.isActiveContactCase(espressoTest.testAppContext.clock))
        } else {
            assertFalse(newState.remembersContactCase())
        }

        symptomsAdviceIsolateRobot.checkActivityIsDisplayed()
    }

    fun selfDiagnosePositiveAndPressBack(selectedDate: SelectedDate = CannotRememberDate) {
        selfDiagnosePositive(selectedDate)
        espressoTest.testAppContext.device.pressBack()
    }

    fun selfDiagnosePositiveAndOrderTest(receiveResultImmediately: Boolean) {
        selfDiagnosePositive(receiveResultImmediately)
        orderTest()
    }

    fun selfDiagnosePositive(receiveResultImmediately: Boolean) {
        espressoTest.testAppContext.virologyTestingApi.pollingTestResultHttpStatusCode =
            if (receiveResultImmediately) 200 else 204

        selfDiagnosePositive(CannotRememberDate)

        symptomsAdviceIsolateRobot.clickBottomActionButton()
    }
}
