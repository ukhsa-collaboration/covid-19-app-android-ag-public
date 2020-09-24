package uk.nhs.nhsx.covid19.android.app.questionnaire

import com.schibsted.spain.barista.interaction.BaristaSleepInteractions.sleep
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.Translatable
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.ReviewSymptomsActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem.Question
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.QuestionnaireActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.Symptom
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.report.Reporter.Kind.FLOW
import uk.nhs.nhsx.covid19.android.app.report.Reporter.Kind.SCREEN
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.report.reporter
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.NoSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.QuestionnaireRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ReviewSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SymptomsAdviceIsolateRobot
import java.time.Instant
import java.time.LocalDate

class QuestionnaireScenarioTest : EspressoTest() {

    private val statusRobot = StatusRobot()
    private val questionnaireRobot = QuestionnaireRobot()
    private val noSymptomsRobot = NoSymptomsRobot()
    private val reviewSymptomsRobot = ReviewSymptomsRobot()
    private val symptomsAdviceIsolateRobot = SymptomsAdviceIsolateRobot()

    @Test
    fun successfullySelectSymptomsAndCannotRememberDate_GoesToIsolationState() = reporter(
        scenario = "Self-diagnosis",
        title = "Positive symptoms path",
        description = "User selects symptoms and is notified of coronavirus symptoms",
        kind = FLOW
    ) {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        step(
            stepName = "Home screen - Default state",
            stepDescription = "When the user is on the Home screen they can tap 'Report symptoms'"
        )

        statusRobot.clickReportSymptoms()

        questionnaireRobot.checkActivityIsDisplayed()

        step(
            stepName = "Symptom list",
            stepDescription = "The user is presented a list of symptoms"
        )

        questionnaireRobot.selectSymptomsAtPositions(0, 1, 2)

        step(
            stepName = "Symptom selected",
            stepDescription = "The user selects a symptom and confirms the screen"
        )

        questionnaireRobot.reviewSymptoms()

        reviewSymptomsRobot.confirmReviewSymptomsScreenIsDisplayed()

        step(
            stepName = "Review symptoms",
            stepDescription = "The user is presented a list of the selected symptoms for review"
        )

        reviewSymptomsRobot.selectCannotRememberDate()

        step(
            stepName = "No Date",
            stepDescription = "The user can specify an onset date or tick that they don't remember the onset date, before confirming"
        )

        reviewSymptomsRobot.confirmSelection()

        symptomsAdviceIsolateRobot.checkActivityIsDisplayed()

        symptomsAdviceIsolateRobot.checkStateInfoViewForPositiveSymptoms()

        step(
            stepName = "Positive Symptoms screen",
            stepDescription = "The user is asked to isolate, and given the option to book a test. They choose to close the screen."
        )

        symptomsAdviceIsolateRobot.checkBottomActionButtonIsDisplayed()

        testAppContext.device.pressBack()

        step(
            stepName = "Home screen - Isolation state",
            stepDescription = "The user is presented with the home screen in isolation state"
        )
    }

    @Test
    fun selectNotCoronavirusSymptomsAndCannotRememberDate_StaysInDefaultState() = notReported {
        startTestActivity<QuestionnaireActivity>()

        questionnaireRobot.checkActivityIsDisplayed()

        questionnaireRobot.selectSymptomsAtPositions(3)

        questionnaireRobot.reviewSymptoms()

        reviewSymptomsRobot.confirmReviewSymptomsScreenIsDisplayed()

        reviewSymptomsRobot.selectCannotRememberDate()

        reviewSymptomsRobot.confirmSelection()

        noSymptomsRobot.confirmNoSymptomsScreenIsDisplayed()
    }

    @Test
    fun successfullySelectSymptomsAndChangeSymptoms_GoesBackToQuestionnaire() = notReported {
        startTestActivity<QuestionnaireActivity>()

        questionnaireRobot.checkActivityIsDisplayed()

        questionnaireRobot.selectSymptomsAtPositions(0, 1, 2)

        questionnaireRobot.reviewSymptoms()

        reviewSymptomsRobot.confirmReviewSymptomsScreenIsDisplayed()

        reviewSymptomsRobot.changeFirstNegativeSymptom()

        questionnaireRobot.checkActivityIsDisplayed()
    }

    @Test
    fun clickOnReviewSymptomsWithoutSelectingSymptoms_DisplaysErrorPanel() = reporter(
        scenario = "Self-diagnosis",
        title = "Continue no symptoms",
        description = "User attempts to continue without selecting any symptoms",
        kind = SCREEN
    ) {
        startTestActivity<QuestionnaireActivity>()

        questionnaireRobot.checkActivityIsDisplayed()

        step(
            stepName = "Symptom list",
            stepDescription = "The user is presented a list of symptoms"
        )

        questionnaireRobot.reviewSymptoms()

        sleep(100)

        questionnaireRobot.confirmErrorScreenIsDisplayed()

        step(
            stepName = "No symptoms selected",
            stepDescription = "An error message is shown to the user"
        )
    }

    @Test
    fun selectNoSymptoms_NavigatesToConfirmationScreen() = reporter(
        scenario = "Self-diagnosis",
        title = "No symptoms",
        description = "User has no symptoms selected and taps 'I don't have any of these symptoms'",
        kind = FLOW
    ) {
        startTestActivity<QuestionnaireActivity>()

        questionnaireRobot.checkActivityIsDisplayed()

        step(
            stepName = "Symptom list",
            stepDescription = "The user is presented a list of symptoms"
        )

        questionnaireRobot.selectNoSymptoms()

        sleep(100)

        questionnaireRobot.discardSymptomsDialogIsDisplayed()

        step(
            stepName = "Confirmation dialog",
            stepDescription = "The user does not select any symptoms and taps 'I don't have any of these symptoms'. A dialog is shown. The user taps 'remove'."
        )

        questionnaireRobot.continueOnDiscardSymptomsDialog()

        noSymptomsRobot.confirmNoSymptomsScreenIsDisplayed()

        step(
            stepName = "No symptoms",
            stepDescription = "The user is informed they are unlikely to have coronavirus"
        )
    }

    @Test
    fun selectNoSymptoms_CancelDialog() = notReported {
        startTestActivity<QuestionnaireActivity>()

        questionnaireRobot.checkActivityIsDisplayed()

        questionnaireRobot.selectNoSymptoms()

        questionnaireRobot.discardSymptomsDialogIsDisplayed()

        questionnaireRobot.cancelOnDiscardSymptomsDialog()

        questionnaireRobot.checkActivityIsDisplayed()
    }

    @Test
    fun contactCase_SelectNotCoronavirusSymptoms_StaysInIsolation() = notReported {
        testAppContext.setState(
            Isolation(
                Instant.now(),
                DurationDays(),
                contactCase = ContactCase(
                    Instant.parse("2020-05-19T12:00:00Z"),
                    LocalDate.now().plusDays(5)
                )
            )
        )

        startTestActivity<QuestionnaireActivity>()

        questionnaireRobot.checkActivityIsDisplayed()

        questionnaireRobot.selectSymptomsAtPositions(3)

        questionnaireRobot.reviewSymptoms()

        reviewSymptomsRobot.confirmReviewSymptomsScreenIsDisplayed()

        reviewSymptomsRobot.selectCannotRememberDate()

        reviewSymptomsRobot.confirmSelection()

        symptomsAdviceIsolateRobot.checkStateInfoViewForNegativeSymptoms()
    }

    @Test
    fun reviewSymptoms_doNotSelectDateOrTickDoNotRemember() = reporter(
        scenario = "Self-diagnosis",
        title = "No onset date",
        description = "User reviews symptoms and does not choose onset date or tap 'I don'r remember the date'",
        kind = FLOW
    ) {
        val questions = arrayListOf(
            Question(
                Symptom(
                    title = Translatable(mapOf("en" to "A high temperature (fever)")),
                    description = Translatable(mapOf("en" to "This means that you feel hot to touch on your chest or back (you do not need to measure your temperature).")),
                    riskWeight = 0.0
                ),
                isChecked = true
            )
        )

        startTestActivity<ReviewSymptomsActivity> {
            putParcelableArrayListExtra("EXTRA_QUESTIONS", questions)
        }

        reviewSymptomsRobot.confirmReviewSymptomsScreenIsDisplayed()

        step(
            stepName = "Review symptoms",
            stepDescription = "The user is on the review symptoms screen, does not select an onset date or tap 'I don’t remember the date' and attempts to submit their symptoms"
        )

        reviewSymptomsRobot.confirmSelection()

        reviewSymptomsRobot.checkReviewSymptomsErrorIsDisplayed()

        step(
            stepName = "No onset date selected",
            stepDescription = "An error message is shown to the user"
        )
    }
}
