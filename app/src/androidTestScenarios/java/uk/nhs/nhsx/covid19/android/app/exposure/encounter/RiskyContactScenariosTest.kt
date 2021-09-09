package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ExposureNotificationAgeLimitRobot
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
        waitFor { exposureNotificationAgeLimitRobot.checkActivityIsDisplayed() }
        exposureNotificationAgeLimitRobot.clickYesButton()
        exposureNotificationAgeLimitRobot.clickContinueButton()

        waitFor { exposureNotificationVaccinationStatusRobot.checkActivityIsDisplayed() }
        exposureNotificationVaccinationStatusRobot.clickDosesNoButton()
        exposureNotificationVaccinationStatusRobot.clickMedicallyExemptNoButton()
        exposureNotificationVaccinationStatusRobot.clickClinicalTrialNoButton()
        exposureNotificationVaccinationStatusRobot.clickContinueButton()

        waitFor { exposureNotificationRiskyContactIsolationAdviceRobot.checkActivityIsDisplayed() }
        exposureNotificationRiskyContactIsolationAdviceRobot.checkIsInAlreadyIsolatingViewState(remainingDaysInIsolation = 11)
        exposureNotificationRiskyContactIsolationAdviceRobot.clickPrimaryBackToHome()

        waitFor { statusRobot.checkActivityIsDisplayed() }
        statusRobot.checkIsolationViewIsDisplayed()
    }

    @Test
    fun whenUserClicksContinue_navigateToAgeLimitActivity_userCanComeBack() {
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
    fun givenContactIsolation_whenSelectingNoToAgeLimitQuestionAndClickingConfirm_thenNavigatesToIsolatingScreenOptingOut() {
        givenContactIsolation()

        startTestActivity<ExposureNotificationActivity>()

        exposureNotificationRobot.checkActivityIsDisplayed()

        exposureNotificationRobot.clickContinueButton()

        waitFor { exposureNotificationAgeLimitRobot.checkActivityIsDisplayed() }

        exposureNotificationAgeLimitRobot.clickNoButton()

        exposureNotificationAgeLimitRobot.clickContinueButton()

        waitFor { exposureNotificationRiskyContactIsolationAdviceRobot.checkActivityIsDisplayed() }

        exposureNotificationRiskyContactIsolationAdviceRobot.checkIsInNotIsolatingAsMinorViewState()
    }

    @Test
    fun whenErrorIsShownOnAgeLimitScreen_thenValidAnswerSelected_thenClickContinue_thenNavigateBack_errorIsNotShown_andSelectedValueStored() {
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

        waitFor { exposureNotificationAgeLimitRobot.checkActivityIsDisplayed() }

        exposureNotificationAgeLimitRobot.checkYesSelected()

        exposureNotificationAgeLimitRobot.checkErrorVisible(false)
    }

    @Test
    fun givenContactIsolation_whenSelectingYesToAgeLimitAndVaccinationQuestionsAndClickingConfirm_thenNavigatesToNotIsolatingAsFullyVaccinatedScreen() {
        givenContactIsolation()

        startTestActivity<ExposureNotificationActivity>()

        exposureNotificationRobot.checkActivityIsDisplayed()

        exposureNotificationRobot.clickContinueButton()

        waitFor { exposureNotificationAgeLimitRobot.checkActivityIsDisplayed() }

        exposureNotificationAgeLimitRobot.clickYesButton()

        exposureNotificationAgeLimitRobot.clickContinueButton()

        waitFor { exposureNotificationVaccinationStatusRobot.checkActivityIsDisplayed() }

        exposureNotificationVaccinationStatusRobot.clickDosesYesButton()

        waitFor { exposureNotificationVaccinationStatusRobot.checkDosesDateQuestionContainerDisplayed(true) }

        exposureNotificationVaccinationStatusRobot.clickDateYesButton()

        exposureNotificationVaccinationStatusRobot.clickContinueButton()

        waitFor { exposureNotificationRiskyContactIsolationAdviceRobot.checkActivityIsDisplayed() }

        exposureNotificationRiskyContactIsolationAdviceRobot.checkIsInNotIsolatingAsFullyVaccinatedViewState()
    }

    @Test
    fun givenContactIsolation_whenSelectingYesToAgeLimitQuestionAndYesToVaccinatedQuestionAndNoToDateQuestionAndClickingConfirm_thenErrorIsShown() {
        givenContactIsolation()

        startTestActivity<ExposureNotificationActivity>()

        exposureNotificationRobot.checkActivityIsDisplayed()

        exposureNotificationRobot.clickContinueButton()

        waitFor { exposureNotificationAgeLimitRobot.checkActivityIsDisplayed() }

        exposureNotificationAgeLimitRobot.clickYesButton()

        exposureNotificationAgeLimitRobot.clickContinueButton()

        waitFor { exposureNotificationVaccinationStatusRobot.checkActivityIsDisplayed() }

        exposureNotificationVaccinationStatusRobot.clickDosesYesButton()

        waitFor { exposureNotificationVaccinationStatusRobot.checkDosesDateQuestionContainerDisplayed(true) }

        exposureNotificationVaccinationStatusRobot.clickDateNoButton()

        exposureNotificationVaccinationStatusRobot.clickContinueButton()

        exposureNotificationVaccinationStatusRobot.checkErrorVisible(true)
    }

    @Test
    fun givenContactIsolation_whenSelectingYesToAgeLimit_thenYesToAllDoses_thenNoToDate_thenYesToClinicalTrial_navigatesToNotIsolatingAsFullyVaccinatedScreen() {
        givenContactIsolation()

        startTestActivity<ExposureNotificationActivity>()

        exposureNotificationRobot.checkActivityIsDisplayed()

        exposureNotificationRobot.clickContinueButton()

        waitFor { exposureNotificationAgeLimitRobot.checkActivityIsDisplayed() }

        exposureNotificationAgeLimitRobot.clickYesButton()

        exposureNotificationAgeLimitRobot.clickContinueButton()

        waitFor { exposureNotificationVaccinationStatusRobot.checkActivityIsDisplayed() }

        exposureNotificationVaccinationStatusRobot.clickDosesYesButton()

        waitFor { exposureNotificationVaccinationStatusRobot.checkDosesDateQuestionContainerDisplayed(true) }

        exposureNotificationVaccinationStatusRobot.clickDateNoButton()

        exposureNotificationVaccinationStatusRobot.clickClinicalTrialYesButton()

        exposureNotificationVaccinationStatusRobot.clickContinueButton()

        waitFor { exposureNotificationRiskyContactIsolationAdviceRobot.checkActivityIsDisplayed() }

        exposureNotificationRiskyContactIsolationAdviceRobot.checkIsInNotIsolatingAsFullyVaccinatedViewState()
    }

    @Test
    fun givenIsInWales_inContactIsolation_whenYesToFullyVaccinated_thenNoToDate_thenNoToClinicalTrial_navigatesToIsolationScreen() {
        givenLocalAuthorityIsInWales()
        givenContactIsolation()

        startTestActivity<ExposureNotificationVaccinationStatusActivity>()

        exposureNotificationVaccinationStatusRobot.checkActivityIsDisplayed()

        exposureNotificationVaccinationStatusRobot.clickDosesYesButton()

        waitFor { exposureNotificationVaccinationStatusRobot.checkDosesDateQuestionContainerDisplayed(true) }

        exposureNotificationVaccinationStatusRobot.clickDateNoButton()

        exposureNotificationVaccinationStatusRobot.clickClinicalTrialNoButton()

        exposureNotificationVaccinationStatusRobot.clickContinueButton()

        waitFor { exposureNotificationRiskyContactIsolationAdviceRobot.checkActivityIsDisplayed() }

        exposureNotificationRiskyContactIsolationAdviceRobot.checkIsInNewlyIsolatingViewState(remainingDaysInIsolation = 9)
    }

    @Test
    fun givenIsInWales_inContactIsolation_whenYesToFullyVaccinated_thenNoToDate_thenYesToClinicalTrial_navigatesToNotIsolatingAsFullyVaccinatedScreen() {
        givenLocalAuthorityIsInWales()
        givenContactIsolation()

        startTestActivity<ExposureNotificationVaccinationStatusActivity>()

        exposureNotificationVaccinationStatusRobot.checkActivityIsDisplayed()

        exposureNotificationVaccinationStatusRobot.clickDosesYesButton()

        waitFor { exposureNotificationVaccinationStatusRobot.checkDosesDateQuestionContainerDisplayed(true) }

        exposureNotificationVaccinationStatusRobot.clickDateNoButton()

        exposureNotificationVaccinationStatusRobot.clickClinicalTrialYesButton()

        exposureNotificationVaccinationStatusRobot.clickContinueButton()

        waitFor { exposureNotificationRiskyContactIsolationAdviceRobot.checkActivityIsDisplayed() }

        exposureNotificationRiskyContactIsolationAdviceRobot.checkIsInNotIsolatingAsFullyVaccinatedViewState()
    }

    @Test
    fun givenIsInWales_inContactIsolation_whenNoToFullyVaccinated_thenNoToClinicalTrial_navigatesToIsolationScreen() {
        givenLocalAuthorityIsInWales()
        givenContactIsolation()

        startTestActivity<ExposureNotificationVaccinationStatusActivity>()

        exposureNotificationVaccinationStatusRobot.checkActivityIsDisplayed()

        exposureNotificationVaccinationStatusRobot.clickDosesNoButton()

        waitFor { exposureNotificationVaccinationStatusRobot.checkClinicalTrialQuestionContainerDisplayed(true) }

        exposureNotificationVaccinationStatusRobot.clickClinicalTrialNoButton()

        exposureNotificationVaccinationStatusRobot.clickContinueButton()

        waitFor { exposureNotificationRiskyContactIsolationAdviceRobot.checkActivityIsDisplayed() }

        exposureNotificationRiskyContactIsolationAdviceRobot.checkIsInNewlyIsolatingViewState(remainingDaysInIsolation = 9)
    }

    @Test
    fun givenIsInWales_inContactIsolation_whenNoToFullyVaccinated_thenYesToClinicalTrial_navigatesToNotIsolatingAsFullyVaccinatedScreen() {
        givenLocalAuthorityIsInWales()
        givenContactIsolation()

        startTestActivity<ExposureNotificationVaccinationStatusActivity>()

        exposureNotificationVaccinationStatusRobot.checkActivityIsDisplayed()

        exposureNotificationVaccinationStatusRobot.clickDosesNoButton()

        waitFor { exposureNotificationVaccinationStatusRobot.checkClinicalTrialQuestionContainerDisplayed(true) }

        exposureNotificationVaccinationStatusRobot.clickClinicalTrialYesButton()

        exposureNotificationVaccinationStatusRobot.clickContinueButton()

        waitFor { exposureNotificationRiskyContactIsolationAdviceRobot.checkActivityIsDisplayed() }

        exposureNotificationRiskyContactIsolationAdviceRobot.checkIsInNotIsolatingAsFullyVaccinatedViewState()
    }

    @Test
    fun givenIsInEngland_inContactIsolation_whenNoToFullyVaccinated_thenYesToMedicallyExempt_navigatesToNotIsolatingAsMedicallyExemptScreen() {
        givenLocalAuthorityIsInEngland()
        givenContactIsolation()

        startTestActivity<ExposureNotificationVaccinationStatusActivity>()

        exposureNotificationVaccinationStatusRobot.checkActivityIsDisplayed()

        exposureNotificationVaccinationStatusRobot.clickDosesNoButton()

        exposureNotificationVaccinationStatusRobot.clickMedicallyExemptYesButton()

        exposureNotificationVaccinationStatusRobot.clickContinueButton()

        waitFor { exposureNotificationRiskyContactIsolationAdviceRobot.checkActivityIsDisplayed() }

        exposureNotificationRiskyContactIsolationAdviceRobot.checkIsInNotIsolatingAsMedicallyExemptViewState()
    }

    @Test
    fun givenIsInEngland_inContactIsolation_whenNoToFullyVaccinated_thenNoToMedicallyExempt_thenNoToClinicalTrial_navigatesToIsolating() {
        givenLocalAuthorityIsInEngland()
        givenContactIsolation()

        startTestActivity<ExposureNotificationVaccinationStatusActivity>()

        exposureNotificationVaccinationStatusRobot.checkActivityIsDisplayed()

        exposureNotificationVaccinationStatusRobot.clickDosesNoButton()

        exposureNotificationVaccinationStatusRobot.clickMedicallyExemptNoButton()

        exposureNotificationVaccinationStatusRobot.clickClinicalTrialNoButton()

        exposureNotificationVaccinationStatusRobot.clickContinueButton()

        waitFor { exposureNotificationRiskyContactIsolationAdviceRobot.checkActivityIsDisplayed() }

        exposureNotificationRiskyContactIsolationAdviceRobot.checkIsInNewlyIsolatingViewState(remainingDaysInIsolation = 9)
    }

    @Test
    fun givenIsInEngland_inContactIsolation_whenNoToFullyVaccinated_thenNoToMedicallyExempt_thenYesToClinicalTrial_navigatesToNotIsolatingAsFullyVaccinatedScreen() {
        givenLocalAuthorityIsInEngland()
        givenContactIsolation()

        startTestActivity<ExposureNotificationVaccinationStatusActivity>()

        exposureNotificationVaccinationStatusRobot.checkActivityIsDisplayed()

        exposureNotificationVaccinationStatusRobot.clickDosesNoButton()

        exposureNotificationVaccinationStatusRobot.clickMedicallyExemptNoButton()

        exposureNotificationVaccinationStatusRobot.clickClinicalTrialYesButton()

        exposureNotificationVaccinationStatusRobot.clickContinueButton()

        waitFor { exposureNotificationRiskyContactIsolationAdviceRobot.checkActivityIsDisplayed() }

        exposureNotificationRiskyContactIsolationAdviceRobot.checkIsInNotIsolatingAsFullyVaccinatedViewState()
    }

    @Test
    fun givenIsInEngland_inContactIsolation_whenYesToFullyVaccinated_thenNoToDate_thenNoToClinicalTrial_thenNoToMedicallyExempt_navigatesToIsolationScreen() {
        givenLocalAuthorityIsInEngland()
        givenContactIsolation()

        startTestActivity<ExposureNotificationVaccinationStatusActivity>()

        exposureNotificationVaccinationStatusRobot.checkActivityIsDisplayed()

        waitFor { exposureNotificationVaccinationStatusRobot.clickDosesYesButton() }

        waitFor { exposureNotificationVaccinationStatusRobot.checkDosesDateQuestionContainerDisplayed(true) }

        exposureNotificationVaccinationStatusRobot.clickDateNoButton()

        exposureNotificationVaccinationStatusRobot.clickClinicalTrialNoButton()

        exposureNotificationVaccinationStatusRobot.clickMedicallyExemptNoButton()

        exposureNotificationVaccinationStatusRobot.clickContinueButton()

        waitFor { exposureNotificationRiskyContactIsolationAdviceRobot.checkActivityIsDisplayed() }

        exposureNotificationRiskyContactIsolationAdviceRobot.checkIsInNewlyIsolatingViewState(remainingDaysInIsolation = 9)
    }

    @Test
    fun givenIsInEngland_inContactIsolation_whenYesToFullyVaccinated_thenNoToDate_thenNoToClinicalTrial_thenYesToExempt_navigatesToNotIsolatingAsMedicallyExempt() {
        givenLocalAuthorityIsInEngland()
        givenContactIsolation()

        startTestActivity<ExposureNotificationVaccinationStatusActivity>()

        exposureNotificationVaccinationStatusRobot.checkActivityIsDisplayed()

        exposureNotificationVaccinationStatusRobot.clickDosesYesButton()

        waitFor { exposureNotificationVaccinationStatusRobot.checkDosesDateQuestionContainerDisplayed(true) }

        exposureNotificationVaccinationStatusRobot.clickDateNoButton()

        exposureNotificationVaccinationStatusRobot.clickClinicalTrialNoButton()

        exposureNotificationVaccinationStatusRobot.clickMedicallyExemptYesButton()

        exposureNotificationVaccinationStatusRobot.clickContinueButton()

        waitFor { exposureNotificationRiskyContactIsolationAdviceRobot.checkActivityIsDisplayed() }

        exposureNotificationRiskyContactIsolationAdviceRobot.checkIsInNotIsolatingAsMedicallyExemptViewState()
    }
}
