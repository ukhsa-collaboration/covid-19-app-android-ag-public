package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EvaluateTestingAdviceToShow.TestingAdviceToShow.Default
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.ExposureNotificationAgeLimitActivity
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.OptOutResponseEntry
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType.ClinicalTrial
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType.DoseDate
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType.FullyVaccinated
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType.MedicallyExempt
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry.ENGLAND
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ExposureNotificationAgeLimitRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ExposureNotificationReviewRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ExposureNotificationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ExposureNotificationVaccinationStatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.RiskyContactIsolationAdviceRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.IsolationSetupHelper
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.LocalAuthoritySetupHelper

class RiskyContactScenariosTest : EspressoTest(), LocalAuthoritySetupHelper, IsolationSetupHelper {

    private val statusRobot = StatusRobot()
    private val exposureNotificationRobot = ExposureNotificationRobot()
    private val exposureNotificationAgeLimitRobot = ExposureNotificationAgeLimitRobot()
    private val exposureNotificationVaccinationStatusRobot = ExposureNotificationVaccinationStatusRobot()
    private val exposureNotificationReviewRobot = ExposureNotificationReviewRobot(testAppContext)
    private val exposureNotificationRiskyContactIsolationAdviceRobot = RiskyContactIsolationAdviceRobot()

    override val isolationHelper = IsolationHelper(testAppContext.clock)

    @Test
    fun givenInIndexCaseIsolation_whenReceivesExposureNotification_seesAlreadyIsolatingScreen_clickBackToHome_navigatesToStatusActivity() {
        givenLocalAuthorityIsInEngland()
        givenSelfAssessmentIsolation()

        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()

        testAppContext.sendExposureStateUpdatedBroadcast()

        waitFor { exposureNotificationRobot.checkActivityIsDisplayed() }
        exposureNotificationRobot.clickContinueButton()
        selectAdult()

        selectEnglandMedicallyExemptAnswers()

        waitFor { exposureNotificationReviewRobot.checkActivityIsDisplayed() }
        exposureNotificationReviewRobot.clickSubmitButton()

        exposureNotificationRiskyContactIsolationAdviceRobot.checkActivityIsDisplayed()
        exposureNotificationRiskyContactIsolationAdviceRobot.checkIsInAlreadyIsolatingViewState(
            remainingDaysInIsolation = 7,
            testingAdviceToShow = Default
        )
        exposureNotificationRiskyContactIsolationAdviceRobot.clickPrimaryBackToHome()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        statusRobot.checkIsolationViewIsDisplayed()
    }

    @Test
    fun whenUserClicksContinue_navigateToAgeLimitActivity_userCanComeBack() {
        givenLocalAuthorityIsInEngland()
        givenContactIsolation()

        startTestActivity<ExposureNotificationActivity>()

        exposureNotificationRobot.checkActivityIsDisplayed()

        exposureNotificationRobot.clickContinueButton()

        waitFor { exposureNotificationAgeLimitRobot.checkActivityIsDisplayed() }

        testAppContext.device.pressBack()

        waitFor { exposureNotificationRobot.checkActivityIsDisplayed() }

        // selection not maintained

        exposureNotificationRobot.clickContinueButton()

        waitFor { exposureNotificationAgeLimitRobot.checkActivityIsDisplayed() }

        exposureNotificationAgeLimitRobot.clickYesButton()

        testAppContext.device.pressBack()

        waitFor { exposureNotificationRobot.checkActivityIsDisplayed() }

        exposureNotificationRobot.clickContinueButton()

        waitFor { exposureNotificationAgeLimitRobot.checkActivityIsDisplayed() }

        exposureNotificationAgeLimitRobot.checkNothingSelected()
    }

    @Test
    fun whenErrorIsShownOnAgeLimitScreen_thenValidAnswerSelected_thenClickContinue_thenNavigateBack_errorIsNotShown_andSelectedValueStored() {
        givenLocalAuthorityIsInEngland()
        givenContactIsolation()

        startTestActivity<ExposureNotificationAgeLimitActivity>()

        waitFor { exposureNotificationAgeLimitRobot.checkActivityIsDisplayed() }

        exposureNotificationAgeLimitRobot.clickContinueButton()

        waitFor { exposureNotificationAgeLimitRobot.checkErrorVisible(true) }

        exposureNotificationAgeLimitRobot.clickYesButton()

        exposureNotificationAgeLimitRobot.checkYesSelected()

        exposureNotificationAgeLimitRobot.checkErrorVisible(true)

        exposureNotificationAgeLimitRobot.clickContinueButton()

        testAppContext.device.pressBack()

        verifyAdult()

        exposureNotificationAgeLimitRobot.checkErrorVisible(false)
    }

    @Test
    fun givenContactIsolation_whenSelectingYesToAgeLimitQuestionAndYesToVaccinatedQuestionAndNoToDateQuestionAndClickingConfirm_thenErrorIsShown() {
        givenLocalAuthorityIsInEngland()
        givenContactIsolation()

        startTestActivity<ExposureNotificationActivity>()

        exposureNotificationRobot.checkActivityIsDisplayed()

        exposureNotificationRobot.clickContinueButton()

        selectAdult()

        waitFor { exposureNotificationVaccinationStatusRobot.checkActivityIsDisplayed() }

        exposureNotificationVaccinationStatusRobot.clickDosesYesButton()

        waitFor { exposureNotificationVaccinationStatusRobot.checkDosesDateQuestionContainerDisplayed(true) }

        exposureNotificationVaccinationStatusRobot.clickDateNoButton()

        exposureNotificationVaccinationStatusRobot.clickContinueButton()

        exposureNotificationVaccinationStatusRobot.checkErrorVisible(true)
    }

    @Test
    fun whenChangingAgeOnReviewScreen_navigatesDirectlyToAgeLimitScreen_andClearsVaccinationAnswers() {
        givenLocalAuthorityIsInEngland()
        givenContactIsolation()

        startTestActivity<ExposureNotificationAgeLimitActivity>()

        selectAdult()

        selectEnglandMedicallyExemptAnswers()

        verifyEnglandMedicallyExemptAnswersOnReviewScreen()

        exposureNotificationReviewRobot.clickChangeAge()

        verifyAdult()
        exposureNotificationAgeLimitRobot.clickContinueButton()

        waitFor { exposureNotificationVaccinationStatusRobot.checkActivityIsDisplayed() }
        exposureNotificationVaccinationStatusRobot.checkDosesNothingSelected()
    }

    @Test
    fun canChangeAgeLimitAnswer() {
        givenLocalAuthorityIsInEngland()
        givenContactIsolation()

        startTestActivity<ExposureNotificationAgeLimitActivity>()

        selectAdult()

        selectEnglandMedicallyExemptAnswers()

        verifyEnglandMedicallyExemptAnswersOnReviewScreen()

        exposureNotificationReviewRobot.clickChangeAge()

        verifyAdult()

        selectMinor()

        verifyMinorOnReviewScreen()

        exposureNotificationReviewRobot.clickSubmitButton()

        exposureNotificationRiskyContactIsolationAdviceRobot.checkActivityIsDisplayed()
        exposureNotificationRiskyContactIsolationAdviceRobot.checkIsNotIsolatingAsMinorViewState(country = ENGLAND, Default)
    }

    @Test
    fun canChangeVaccinationStatusAnswers() {
        givenLocalAuthorityIsInEngland()
        givenContactIsolation()

        startTestActivity<ExposureNotificationAgeLimitActivity>()

        selectAdult()

        selectEnglandMedicallyExemptAnswers()

        verifyEnglandMedicallyExemptAnswersOnReviewScreen()

        exposureNotificationReviewRobot.clickChangeVaccinationStatus()

        selectEnglandFullyVaccinatedAnswers()

        verifyEnglandFullyVaccinatedAnswersOnReviewScreen()

        exposureNotificationReviewRobot.clickSubmitButton()

        exposureNotificationRiskyContactIsolationAdviceRobot.checkActivityIsDisplayed()
        exposureNotificationRiskyContactIsolationAdviceRobot.checkIsInNotIsolatingAsFullyVaccinatedViewState(ENGLAND, Default)
    }

    @Test
    fun whenBackOnAgeLimitScreenFromReviewScreen_backButtonLeadsToExposureNotificationScreen() {
        givenLocalAuthorityIsInEngland()
        givenContactIsolation()

        startTestActivity<ExposureNotificationActivity>()

        exposureNotificationRobot.checkActivityIsDisplayed()

        exposureNotificationRobot.clickContinueButton()

        selectMinor()

        exposureNotificationReviewRobot.clickChangeAge()

        verifyMinor()

        testAppContext.device.pressBack()

        waitFor { exposureNotificationRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun whenBackOnVaccinationStatusScreenFromReviewScreen_backButtonLeadsToAgeLimitScreen() {
        givenLocalAuthorityIsInEngland()
        givenContactIsolation()

        startTestActivity<ExposureNotificationActivity>()

        exposureNotificationRobot.checkActivityIsDisplayed()

        exposureNotificationRobot.clickContinueButton()

        selectAdult()

        selectEnglandMedicallyExemptAnswers()

        exposureNotificationReviewRobot.clickChangeVaccinationStatus()

        testAppContext.device.pressBack()

        waitFor { exposureNotificationAgeLimitRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun whenChangingVaccinationStatusOnReviewScreen_navigatesDirectlyToVaccinationStatusScreen_andKeepsVaccinationAnswers() {
        givenLocalAuthorityIsInEngland()
        givenContactIsolation()

        startTestActivity<ExposureNotificationActivity>()

        exposureNotificationRobot.checkActivityIsDisplayed()

        exposureNotificationRobot.clickContinueButton()

        selectAdult()

        selectEnglandMedicallyExemptAnswers()

        verifyEnglandMedicallyExemptAnswersOnReviewScreen()

        exposureNotificationReviewRobot.clickChangeVaccinationStatus()

        verifyEnglandMedicallyExemptAnswersOnVaccinationStatusScreen()
    }

    private fun verifyEnglandMedicallyExemptAnswersOnVaccinationStatusScreen() {
        waitFor { exposureNotificationVaccinationStatusRobot.checkActivityIsDisplayed() }
        exposureNotificationVaccinationStatusRobot.checkDosesYesSelected()
        exposureNotificationVaccinationStatusRobot.checkDateNoSelected()
        exposureNotificationVaccinationStatusRobot.checkClinicalTrialNoSelected()
        exposureNotificationVaccinationStatusRobot.checkMedicallyExemptYesSelected()
    }

    private fun selectAdult() {
        waitFor { exposureNotificationAgeLimitRobot.checkActivityIsDisplayed() }
        exposureNotificationAgeLimitRobot.clickYesButton()
        exposureNotificationAgeLimitRobot.clickContinueButton()
    }

    private fun selectEnglandMedicallyExemptAnswers() {
        exposureNotificationVaccinationStatusRobot.checkActivityIsDisplayed()
        waitFor { exposureNotificationVaccinationStatusRobot.clickDosesYesButton() }
        waitFor { exposureNotificationVaccinationStatusRobot.checkDosesDateQuestionContainerDisplayed(true) }
        exposureNotificationVaccinationStatusRobot.clickDateNoButton()
        exposureNotificationVaccinationStatusRobot.clickClinicalTrialNoButton()
        exposureNotificationVaccinationStatusRobot.clickMedicallyExemptYesButton()
        exposureNotificationVaccinationStatusRobot.clickContinueButton()
    }

    private fun selectEnglandFullyVaccinatedAnswers() {
        exposureNotificationVaccinationStatusRobot.checkActivityIsDisplayed()
        waitFor { exposureNotificationVaccinationStatusRobot.clickDosesYesButton() }
        waitFor { exposureNotificationVaccinationStatusRobot.checkDosesDateQuestionContainerDisplayed(true) }
        exposureNotificationVaccinationStatusRobot.clickDateYesButton()
        exposureNotificationVaccinationStatusRobot.clickContinueButton()
    }

    private fun verifyEnglandMedicallyExemptAnswersOnReviewScreen() {
        waitFor { exposureNotificationReviewRobot.checkActivityIsDisplayed() }
        exposureNotificationReviewRobot.verifyReviewViewState(
            vaccinationStatusResponses = listOf(
                OptOutResponseEntry(questionType = FullyVaccinated, response = true),
                OptOutResponseEntry(questionType = DoseDate, response = false),
                OptOutResponseEntry(questionType = ClinicalTrial, response = false),
                OptOutResponseEntry(questionType = MedicallyExempt, response = true)
            )
        )
    }

    private fun verifyEnglandFullyVaccinatedAnswersOnReviewScreen() {
        waitFor { exposureNotificationReviewRobot.checkActivityIsDisplayed() }
        exposureNotificationReviewRobot.verifyReviewViewState(
            vaccinationStatusResponses = listOf(
                OptOutResponseEntry(questionType = FullyVaccinated, response = true),
                OptOutResponseEntry(questionType = DoseDate, response = true)
            )
        )
    }

    private fun verifyMinorOnReviewScreen() {
        waitFor { exposureNotificationReviewRobot.checkActivityIsDisplayed() }
        exposureNotificationReviewRobot.verifyReviewViewState(
            ageResponse = false,
            vaccinationStatusResponses = emptyList()
        )
    }

    private fun selectMinor() {
        exposureNotificationAgeLimitRobot.clickNoButton()
        exposureNotificationAgeLimitRobot.clickContinueButton()
    }

    private fun verifyAdult() {
        waitFor { exposureNotificationAgeLimitRobot.checkActivityIsDisplayed() }
        exposureNotificationAgeLimitRobot.checkYesSelected()
    }

    private fun verifyMinor() {
        waitFor { exposureNotificationAgeLimitRobot.checkActivityIsDisplayed() }
        exposureNotificationAgeLimitRobot.checkNoSelected()
    }
}
