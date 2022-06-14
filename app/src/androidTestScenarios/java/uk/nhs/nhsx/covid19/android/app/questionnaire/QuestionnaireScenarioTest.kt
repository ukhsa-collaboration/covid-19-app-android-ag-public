package uk.nhs.nhsx.covid19.android.app.questionnaire

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import uk.nhs.nhsx.covid19.android.app.MockApiResponseType.ALWAYS_FAIL
import uk.nhs.nhsx.covid19.android.app.MockApiResponseType.ALWAYS_SUCCEED
import uk.nhs.nhsx.covid19.android.app.common.TranslatableString
import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.IndexCaseThenHasSymptomsDidUpdateIsolation
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.IndexCaseThenHasSymptomsNoEffectOnIsolation
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.IndexCaseThenNoSymptoms
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.NoIndexCaseThenIsolationDueToSelfAssessment
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.NoIndexCaseThenIsolationDueToSelfAssessmentNoTimerWales
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.NoIndexCaseThenSelfAssessmentNoImpactOnIsolation
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.ReviewSymptomsActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem.Question
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.QuestionnaireActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.Symptom
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.report.Reported
import uk.nhs.nhsx.covid19.android.app.report.Reporter
import uk.nhs.nhsx.covid19.android.app.report.Reporter.Kind.FLOW
import uk.nhs.nhsx.covid19.android.app.report.Reporter.Kind.SCREEN
import uk.nhs.nhsx.covid19.android.app.report.config.TestConfiguration
import uk.nhs.nhsx.covid19.android.app.report.reporter
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfiguration
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.Contact
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.retry.RetryFlakyTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.NoSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.QuestionnaireRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ReviewSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SymptomsAdviceIsolateRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithIntents
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.IsolationSetupHelper
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.LocalAuthoritySetupHelper
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import java.time.LocalDate

@RunWith(Parameterized::class)
class QuestionnaireScenarioTest(override val configuration: TestConfiguration) : EspressoTest(), IsolationSetupHelper,
    LocalAuthoritySetupHelper {

    private val statusRobot = StatusRobot()
    private val questionnaireRobot = QuestionnaireRobot()
    private val noSymptomsRobot = NoSymptomsRobot()
    private val reviewSymptomsRobot = ReviewSymptomsRobot()
    private val symptomsAdviceIsolateRobot = SymptomsAdviceIsolateRobot()

    override val isolationHelper = IsolationHelper(testAppContext.clock)

    @Test
    @Reported
    fun whenSymptomaticSelfIsolationIsEnabledAndNotIsolating_userSelectsPositiveSymptoms_transitionsIntoIsolation_forWales() = reporter(
        scenario = "Self Diagnosis Wales",
        title = "Currently not in isolation - Positive symptoms - Symptomatic self isolation enabled",
        description = "User is currently not in isolation, selects symptoms and is notified of coronavirus symptoms",
        kind = FLOW
    ) {
        testAppContext.questionnaireApi.isSymptomaticSelfIsolationForWalesEnabled = true
        givenLocalAuthorityIsInWales()
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

        reviewSymptomsRobot.checkActivityIsDisplayed()

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

        symptomsAdviceIsolateRobot.checkViewState(
            NoIndexCaseThenIsolationDueToSelfAssessment(testAppContext.getRemainingDaysInIsolation())
        )

        step(
            stepName = "Positive Symptoms screen",
            stepDescription = "The user is asked to isolate, and given the option to book a test. They choose to close the screen."
        )

        testAppContext.device.pressBack()

        step(
            stepName = "Home screen - Isolation state",
            stepDescription = "The user is presented with the home screen in isolation state"
        )

        statusRobot.checkActivityIsDisplayed()
        statusRobot.checkIsolationViewIsDisplayed()
    }

    @Test
    @Reported
    fun whenSymptomaticSelfIsolationIsDisabledAndNotIsolating_userSelectsPositiveSymptoms_doNotTransitionsIntoIsolation_forWales() = reporter(
        scenario = "Self Diagnosis Wales",
        title = "Currently not in isolation - Positive symptoms - Symptomatic self isolation disabled",
        description = "User is currently not in isolation, selects symptoms and is notified of coronavirus symptoms",
        kind = FLOW
    ) {
        testAppContext.questionnaireApi.isSymptomaticSelfIsolationForWalesEnabled = false
        givenLocalAuthorityIsInWales()
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

        reviewSymptomsRobot.checkActivityIsDisplayed()

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

        symptomsAdviceIsolateRobot.checkViewState(
            NoIndexCaseThenIsolationDueToSelfAssessmentNoTimerWales(DurationDays().wales.indexCaseSinceSelfDiagnosisOnset)
        )

        step(
            stepName = "Positive Symptoms screen",
            stepDescription = "The user is asked to isolate, and given the option to book a test. They choose to close the screen."
        )

        testAppContext.device.pressBack()

        step(
            stepName = "Home screen - Default state",
            stepDescription = "The user is presented with the home screen in default state"
        )

        statusRobot.checkActivityIsDisplayed()
        statusRobot.checkIsolationViewIsNotDisplayed()
    }

    @Test
    @Reported
    fun whenNotIsolating_userSelectsNoSymptoms_NavigatesToNoSymptomsScreen() = reporter(
        scenario = "Self Diagnosis",
        title = "Currently not in isolation - No symptoms",
        description = "User is currently not in isolation, has no symptoms selected and taps 'I don't have any of these symptoms'",
        kind = FLOW
    ) {
        startTestActivity<QuestionnaireActivity>()

        questionnaireRobot.checkActivityIsDisplayed()

        step(
            stepName = "Symptom list",
            stepDescription = "The user is presented a list of symptoms"
        )

        questionnaireRobot.clickNoSymptoms()

        waitFor { questionnaireRobot.discardSymptomsDialogIsDisplayed() }

        step(
            stepName = "Confirmation dialog",
            stepDescription = "The user does not select any symptoms and taps 'I don't have any of these symptoms'. A dialog is shown. The user taps 'remove'."
        )

        waitFor { questionnaireRobot.discardSymptomsDialogIsDisplayed() }

        waitFor { questionnaireRobot.continueOnDiscardSymptomsDialog() }

        noSymptomsRobot.confirmNoSymptomsScreenIsDisplayed()

        step(
            stepName = "No symptoms",
            stepDescription = "The user is informed they are unlikely to have coronavirus"
        )
    }

    @Test
    @Reported
    fun contactCase_userSelectsPositiveSymptoms_StaysInContactIsolation() = reporter(
        scenario = "Self Diagnosis",
        title = "Isolating due to contact case - Positive symptoms",
        description = "User is in contact case isolation, selects symptoms that do not result in isolation due to self-assessment and is asked to continue isolating",
        kind = FLOW
    ) {
        givenLocalAuthorityIsInWales()
        testAppContext.setState(isolationHelper.contact().asIsolation())

        completeQuestionnaire(selectMainSymptom = false)

        symptomsAdviceIsolateRobot.checkViewState(
            NoIndexCaseThenSelfAssessmentNoImpactOnIsolation(testAppContext.getRemainingDaysInIsolation())
        )

        step(
            stepName = "Continue isolation screen",
            stepDescription = "The user is informed that they do not have symptoms but should continue to self-isolate."
        )
    }

    @Test
    @Reported
    fun indexCaseWithPositiveTestResult_userSelectsPositiveSymptoms_withOnsetDateAfterTestEndDate_showIsolationUpdateScreen() =
        reporter(
            scenario = "Self Diagnosis",
            title = "Isolating due to positive test result - Positive symptoms with onset date more recent than test end date",
            description = "User is in index case isolation due to a positive test result, has " +
                    "symptoms selected and chooses a onset date after the stored test end date. " +
                    "This extends the current isolation and shows the appropriate screen.",
            kind = FLOW
        ) {
            givenLocalAuthorityIsInWales()
            isolatingDueToPositiveTestResult(testEndDate = LocalDate.now(testAppContext.clock).minusDays(3))

            completeQuestionnaire(selectMainSymptom = true)

            symptomsAdviceIsolateRobot.checkViewState(
                IndexCaseThenHasSymptomsDidUpdateIsolation(testAppContext.getRemainingDaysInIsolation())
            )

            step(
                stepName = "Positive symptoms screen",
                stepDescription = "The user is informed that they have symptoms and should continue to self-isolate."
            )
        }

    @Test
    @Reported
    fun indexCaseWithPositiveTestResult_userSelectsPositiveSymptoms_withOnsetDateBeforeTestEndDate_showKeepIsolatingScreen() =
        reporter(
            scenario = "Self Diagnosis",
            title = "Isolating due to positive test result - Positive symptoms with onset date older than test end date",
            description = "User is in index case isolation due to a positive test result, has " +
                    "symptoms selected and chooses an onset date before the stored test end date. " +
                    "This has no effect on the current isolation and asks the user to keep isolating.",
            kind = FLOW
        ) {
            givenLocalAuthorityIsInEngland()
            isolatingDueToPositiveTestResult()

            completeQuestionnaire(selectMainSymptom = true)

            symptomsAdviceIsolateRobot.checkViewState(IndexCaseThenHasSymptomsNoEffectOnIsolation)

            step(
                stepName = "Continue isolation screen",
                stepDescription = "The user is informed that they have symptoms and should continue to self-isolate."
            )
        }

    @Test
    @Reported
    fun indexCaseWithPositiveTestResult_userSelectsLowRiskSymptoms_showKeepIsolatingScreen() =
        reporter(
            scenario = "Self Diagnosis",
            title = "Isolating due to positive test result - Low risk symptoms",
            description = "User is in index case isolation due to a positive test result, has " +
                    "low risk symptoms selected and chooses an onset date before the stored test end date. " +
                    "This has no effect on the current isolation and asks the user to keep isolating.",
            kind = FLOW
        ) {
            givenLocalAuthorityIsInWales()
            isolatingDueToPositiveTestResult()

            completeQuestionnaire(selectMainSymptom = false)

            symptomsAdviceIsolateRobot.checkViewState(IndexCaseThenNoSymptoms)

            step(
                stepName = "Continue isolation screen",
                stepDescription = "The user is informed that they do not have symptoms but should continue to self-isolate as per previous advice."
            )
        }

    @Test
    @Reported
    fun indexCaseWithPositiveTestResult_userSelectsNoSymptoms_showKeepIsolatingScreen() = reporter(
        scenario = "Self Diagnosis",
        title = "Isolating due to positive test result - No symptoms",
        description = "User is in index case isolation due to a positive test result, has no symptoms selected and taps 'I don't have any of these symptoms'",
        kind = FLOW
    ) {
        givenLocalAuthorityIsInWales()
        isolatingDueToPositiveTestResult()

        startTestActivity<QuestionnaireActivity>()

        step(
            stepName = "Symptom list",
            stepDescription = "The user is presented with a list of symptoms"
        )

        questionnaireRobot.clickNoSymptoms()

        waitFor { questionnaireRobot.discardSymptomsDialogIsDisplayed() }

        step(
            stepName = "Confirmation dialog",
            stepDescription = "The user does not select any symptoms and taps 'I don't have any of these symptoms'. A dialog is shown. The user taps 'remove'."
        )

        waitFor { questionnaireRobot.continueOnDiscardSymptomsDialog() }

        symptomsAdviceIsolateRobot.checkViewState(IndexCaseThenNoSymptoms)

        step(
            stepName = "Continue isolation screen",
            stepDescription = "The user is informed that they do not have symptoms but should continue to self-isolate as per previous advice."
        )
    }

    private fun Reporter.completeQuestionnaire(selectMainSymptom: Boolean) {
        startTestActivity<QuestionnaireActivity>()

        questionnaireRobot.checkActivityIsDisplayed()

        step(
            stepName = "Symptom list",
            stepDescription = "The user is presented a list of symptoms"
        )

        if (selectMainSymptom) selectMainSymptom()
        else selectLowThresholdSymptom()

        questionnaireRobot.reviewSymptoms()

        step(
            stepName = "Review symptoms",
            stepDescription = "The user is presented a list of the selected symptoms for review"
        )

        reviewSymptomsRobot.checkActivityIsDisplayed()

        reviewSymptomsRobot.selectCannotRememberDate()

        step(
            stepName = "No Date",
            stepDescription = "The user can specify an onset date or tick that they don't remember the onset date, before confirming"
        )

        reviewSymptomsRobot.confirmSelection()
    }

    private fun Reporter.selectLowThresholdSymptom() {
        // Select "Dummy" symptom that does not result in isolation
        questionnaireRobot.selectSymptomsAtPositions(3)

        step(
            stepName = "Symptom selected",
            stepDescription = "The user selects a symptom that does not exceed the threshold to result in isolation and confirms the screen"
        )
    }

    private fun Reporter.selectMainSymptom() {
        questionnaireRobot.selectSymptomsAtPositions(2)

        step(
            stepName = "Symptom selected",
            stepDescription = "The user selects a main symptom and confirms the screen"
        )
    }

    @Test
    fun selectNotCoronavirusSymptomsAndCannotRememberDate_StaysInDefaultState() {
        startTestActivity<QuestionnaireActivity>()

        questionnaireRobot.checkActivityIsDisplayed()

        questionnaireRobot.selectSymptomsAtPositions(3)

        questionnaireRobot.reviewSymptoms()

        reviewSymptomsRobot.checkActivityIsDisplayed()

        reviewSymptomsRobot.selectCannotRememberDate()

        reviewSymptomsRobot.confirmSelection()

        noSymptomsRobot.confirmNoSymptomsScreenIsDisplayed()
    }

    @Test
    fun successfullySelectSymptomsAndChangeSymptoms_GoesBackToQuestionnaire() {
        startTestActivity<QuestionnaireActivity>()

        questionnaireRobot.checkActivityIsDisplayed()

        questionnaireRobot.selectSymptomsAtPositions(0, 1, 2)

        questionnaireRobot.reviewSymptoms()

        reviewSymptomsRobot.checkActivityIsDisplayed()

        reviewSymptomsRobot.changeFirstNegativeSymptom()

        questionnaireRobot.checkActivityIsDisplayed()
    }

    @Test
    @Reported
    fun clickOnReviewSymptomsWithoutSelectingSymptoms_DisplaysErrorPanel() = reporter(
        scenario = "Self Diagnosis",
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

        waitFor { questionnaireRobot.confirmErrorScreenIsDisplayed() }

        step(
            stepName = "No symptoms selected",
            stepDescription = "An error message is shown to the user"
        )
    }

    @RetryFlakyTest
    @Test
    fun selectNoSymptoms_CancelDialog() {
        startTestActivity<QuestionnaireActivity>()

        questionnaireRobot.checkActivityIsDisplayed()

        questionnaireRobot.clickNoSymptoms()

        questionnaireRobot.discardSymptomsDialogIsDisplayed()

        questionnaireRobot.cancelOnDiscardSymptomsDialog()

        questionnaireRobot.checkActivityIsDisplayed()
    }

    @Test
    fun contactCase_SelectNotCoronavirusSymptoms_StaysInIsolation() {
        givenLocalAuthorityIsInWales()
        testAppContext.setState(
            IsolationState(
                isolationConfiguration = IsolationConfiguration(),
                contact = Contact(
                    exposureDate = LocalDate.now().minusDays(1),
                    notificationDate = LocalDate.now()
                )
            )
        )

        startTestActivity<QuestionnaireActivity>()

        questionnaireRobot.checkActivityIsDisplayed()

        questionnaireRobot.selectSymptomsAtPositions(3)

        questionnaireRobot.reviewSymptoms()

        reviewSymptomsRobot.checkActivityIsDisplayed()

        reviewSymptomsRobot.selectCannotRememberDate()

        reviewSymptomsRobot.confirmSelection()

        symptomsAdviceIsolateRobot.checkViewState(
            NoIndexCaseThenSelfAssessmentNoImpactOnIsolation(testAppContext.getRemainingDaysInIsolation())
        )
    }

    @Test
    fun contactCase_selectNoCoronavirusSymptoms_staysInIsolation() {
        givenLocalAuthorityIsInWales()
        testAppContext.setState(
            IsolationState(
                isolationConfiguration = IsolationConfiguration(),
                contact = Contact(
                    exposureDate = LocalDate.now().minusDays(1),
                    notificationDate = LocalDate.now()
                )
            )
        )

        startTestActivity<QuestionnaireActivity>()

        questionnaireRobot.checkActivityIsDisplayed()

        questionnaireRobot.clickNoSymptoms()

        waitFor { questionnaireRobot.discardSymptomsDialogIsDisplayed() }

        waitFor { questionnaireRobot.continueOnDiscardSymptomsDialog() }

        symptomsAdviceIsolateRobot.checkActivityIsDisplayed()

        symptomsAdviceIsolateRobot.checkViewState(
            NoIndexCaseThenSelfAssessmentNoImpactOnIsolation(testAppContext.getRemainingDaysInIsolation())
        )
    }

    @Test
    @Reported
    fun reviewSymptoms_doNotSelectDateOrTickDoNotRemember() = reporter(
        scenario = "Self Diagnosis",
        title = "No onset date",
        description = "User reviews symptoms and does not choose onset date or tap 'I don'r remember the date'",
        kind = FLOW
    ) {
        val questions = arrayListOf(
            Question(
                Symptom(
                    title = TranslatableString(mapOf("en" to "A high temperature (fever)")),
                    description = TranslatableString(mapOf("en" to "This means that you feel hot to touch on your chest or back (you do not need to measure your temperature).")),
                    riskWeight = 0.0
                ),
                isChecked = true
            )
        )

        startTestActivity<ReviewSymptomsActivity> {
            putParcelableArrayListExtra("EXTRA_QUESTIONS", questions)
        }

        reviewSymptomsRobot.checkActivityIsDisplayed()

        step(
            stepName = "Review symptoms",
            stepDescription = "The user is on the review symptoms screen, does not select an onset date or tap 'I don’t remember the date' and attempts to submit their symptoms"
        )

        reviewSymptomsRobot.confirmSelection()

        waitFor { reviewSymptomsRobot.checkReviewSymptomsErrorIsDisplayed() }

        step(
            stepName = "No onset date selected",
            stepDescription = "An error message is shown to the user"
        )
    }

    @Test
    fun selectSymptoms_SelectTodayAsDate_NoSymptomsScreenIsDisplayed() {
        startTestActivity<QuestionnaireActivity>()

        questionnaireRobot.checkActivityIsDisplayed()

        questionnaireRobot.selectSymptomsAtPositions(3)

        questionnaireRobot.reviewSymptoms()

        reviewSymptomsRobot.checkActivityIsDisplayed()

        reviewSymptomsRobot.clickSelectDate()

        reviewSymptomsRobot.selectDayOfMonth(LocalDate.now().dayOfMonth)

        reviewSymptomsRobot.confirmSelection()

        noSymptomsRobot.confirmNoSymptomsScreenIsDisplayed()
    }

    @Test
    fun selectSymptoms_SelectDoNotRememberDateThenSelectTodayAsDate_DoNotRememberIsNotChecked() {
        startTestActivity<QuestionnaireActivity>()

        questionnaireRobot.checkActivityIsDisplayed()

        questionnaireRobot.selectSymptomsAtPositions(3)

        questionnaireRobot.reviewSymptoms()

        reviewSymptomsRobot.checkActivityIsDisplayed()

        reviewSymptomsRobot.selectCannotRememberDate()

        reviewSymptomsRobot.checkDoNotRememberDateIsChecked()

        reviewSymptomsRobot.clickSelectDate()

        reviewSymptomsRobot.selectDayOfMonth(LocalDate.now().dayOfMonth)

        reviewSymptomsRobot.checkDoNotRememberDateIsNotChecked()
    }

    @Test
    fun selectSymptoms_NavigateToReviewScreen_NavigateBackToSelectSymptoms() {
        startTestActivity<QuestionnaireActivity>()

        questionnaireRobot.checkActivityIsDisplayed()

        questionnaireRobot.selectSymptomsAtPositions(3)

        questionnaireRobot.reviewSymptoms()

        testAppContext.device.pressBack()

        questionnaireRobot.checkActivityIsDisplayed()
    }

    @Test
    fun navigateToQuestionnaire_LoadingQuestionnaireFails_ShowsErrorState() {
        MockApiModule.behaviour.responseType = ALWAYS_FAIL

        startTestActivity<QuestionnaireActivity>()

        questionnaireRobot.checkErrorStateIsDisplayed()
    }

    @Test
    fun navigateToQuestionnaire_LoadingQuestionnaireFailsAndTryAgainSucceeds_NavigateToQuestionnaire() {
        MockApiModule.behaviour.responseType = ALWAYS_FAIL

        startTestActivity<QuestionnaireActivity>()

        questionnaireRobot.checkErrorStateIsDisplayed()

        MockApiModule.behaviour.responseType = ALWAYS_SUCCEED

        questionnaireRobot.clickTryAgainButton()

        waitFor { questionnaireRobot.checkQuestionnaireIsDisplayed() }
    }

    @Test
    fun successfullySelectSymptoms_ReviewAndCancel_NothingHappens() {
        runWithIntents {
            val result = Instrumentation.ActivityResult(Activity.RESULT_CANCELED, Intent())
            Intents.intending(IntentMatchers.hasComponent(ReviewSymptomsActivity::class.qualifiedName))
                .respondWith(result)

            startTestActivity<QuestionnaireActivity>()
            questionnaireRobot.selectSymptomsAtPositions(0)
            questionnaireRobot.reviewSymptoms()
        }
    }

    @Test
    fun successfullySelectSymptoms_ReviewAndDoNotReturnData_NothingHappens() {
        runWithIntents {
            val result = Instrumentation.ActivityResult(Activity.RESULT_OK, null)
            Intents.intending(IntentMatchers.hasComponent(ReviewSymptomsActivity::class.qualifiedName))
                .respondWith(result)

            startTestActivity<QuestionnaireActivity>()
            questionnaireRobot.selectSymptomsAtPositions(0)
            questionnaireRobot.reviewSymptoms()
        }
    }

    @Test
    fun startReviewSymptomsActivityWithoutQuestions_NothingHappens() {
        startTestActivity<ReviewSymptomsActivity>()
    }

    private fun isolatingDueToPositiveTestResult(testEndDate: LocalDate = LocalDate.now(testAppContext.clock)) {
        val testResult = AcknowledgedTestResult(
            testEndDate = testEndDate,
            testResult = POSITIVE,
            testKitType = LAB_RESULT,
            acknowledgedDate = LocalDate.now(testAppContext.clock)
        )
        testAppContext.setState(testResult.asIsolation())
    }
}
