package uk.nhs.nhsx.covid19.android.app.questionnaire

import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.QuestionnaireActivity
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.PositiveSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.NoSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.QuestionnaireRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ReviewSymptomsRobot

class QuestionnaireScenarioTest : EspressoTest() {

    private val questionnaireRobot = QuestionnaireRobot()
    private val noSymptomsRobot = NoSymptomsRobot()
    private val reviewSymptomsRobot = ReviewSymptomsRobot()
    private val positiveSymptomsRobot = PositiveSymptomsRobot()

    @Before
    fun setUp() {
        startTestActivity<QuestionnaireActivity>()

        questionnaireRobot.checkActivityIsDisplayed()
    }

    @Test
    fun successfullySelectSymptomsAndCannotRememberDate_GoesToIsolationState() = notReported {
        questionnaireRobot.selectSymptomsAtPositions(0, 1, 2)

        questionnaireRobot.reviewSymptoms()

        reviewSymptomsRobot.confirmReviewSymptomsScreenIsDisplayed()

        reviewSymptomsRobot.selectCannotRememberDate()

        reviewSymptomsRobot.confirmSelection()

        positiveSymptomsRobot.checkActivityIsDisplayed()

        positiveSymptomsRobot.checkTestOrderingButtonIsDisplayed()
    }

    @Test
    fun selectNotCoronavirusSymptomsAndCannotRememberDate_StaysInDefaultState() = notReported {
        questionnaireRobot.selectSymptomsAtPositions(3)

        questionnaireRobot.reviewSymptoms()

        reviewSymptomsRobot.confirmReviewSymptomsScreenIsDisplayed()

        reviewSymptomsRobot.selectCannotRememberDate()

        reviewSymptomsRobot.confirmSelection()

        noSymptomsRobot.confirmNoSymptomsScreenIsDisplayed()
    }

    @Test
    fun successfullySelectSymptomsAndChangeSymptoms_GoesBackToQuestionnaire() = notReported {
        questionnaireRobot.selectSymptomsAtPositions(0, 1, 2)

        questionnaireRobot.reviewSymptoms()

        reviewSymptomsRobot.confirmReviewSymptomsScreenIsDisplayed()

        reviewSymptomsRobot.changeFirstNegativeSymptom()

        questionnaireRobot.checkActivityIsDisplayed()
    }

    @Test
    fun clickOnReviewSymptomsWithoutSelectingSymptoms_DisplaysErrorPanel() = notReported {
        questionnaireRobot.reviewSymptoms()

        questionnaireRobot.confirmErrorScreenIsDisplayed()
    }

    @Test
    fun selectNoSymptoms_NavigatesToConfirmationScreen() = notReported {
        questionnaireRobot.selectNoSymptoms()

        noSymptomsRobot.confirmNoSymptomsScreenIsDisplayed()
    }
}
