package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import com.jeroenmols.featureflag.framework.FeatureFlag.OLD_ENGLAND_CONTACT_CASE_FLOW
import com.jeroenmols.featureflag.framework.FeatureFlag.OLD_WALES_CONTACT_CASE_FLOW
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R.string
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EvaluateTestingAdviceToShow.TestingAdviceToShow.Default
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EvaluateTestingAdviceToShow.TestingAdviceToShow.WalesWithinAdviceWindow
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.ExposureNotificationAgeLimitActivity
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.OptOutResponseEntry
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType.ClinicalTrial
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType.DoseDate
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType.FullyVaccinated
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType.MedicallyExempt
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry.ENGLAND
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.assertBrowserIsOpened
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ExposureNotificationAgeLimitRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ExposureNotificationReviewRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ExposureNotificationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ExposureNotificationVaccinationStatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.RiskyContactIsolationAdviceRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeature
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeatureEnabled
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.IsolationSetupHelper
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.LocalAuthoritySetupHelper
import java.time.LocalDate

class RiskyContactScenariosTest : EspressoTest(), LocalAuthoritySetupHelper, IsolationSetupHelper {

    private val statusRobot = StatusRobot()
    private val exposureNotificationRobot = ExposureNotificationRobot()
    private val riskyContactIsolationOptOutRobot = RiskyContactIsolationOptOutRobot()
    private val exposureNotificationAgeLimitRobot = ExposureNotificationAgeLimitRobot()
    private val exposureNotificationVaccinationStatusRobot = ExposureNotificationVaccinationStatusRobot()
    private val exposureNotificationReviewRobot = ExposureNotificationReviewRobot(testAppContext)
    private val exposureNotificationRiskyContactIsolationAdviceRobot = RiskyContactIsolationAdviceRobot()

    override val isolationHelper = IsolationHelper(testAppContext.clock)

    @Test
    fun givenInIndexCaseIsolationEnglandNewContactJourneyIsDisabled_whenReceivesExposureNotification_seesAlreadyIsolatingScreen_clickBackToHome_navigatesToHome() {
        runWithFeatureEnabled(OLD_ENGLAND_CONTACT_CASE_FLOW) {
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
    }

    @Test
    fun givenInIndexCaseIsolationWalesNewContactJourneyIsDisabled_whenReceivesExposureNotification_seesAlreadyIsolatingScreen_clickBackToHome_navigatesToHome() {
        runWithFeatureEnabled(OLD_WALES_CONTACT_CASE_FLOW) {
            givenLocalAuthorityIsInWales()
            givenSelfAssessmentIsolation()

            startTestActivity<StatusActivity>()
            statusRobot.checkActivityIsDisplayed()

            testAppContext.sendExposureStateUpdatedBroadcast()

            waitFor { exposureNotificationRobot.checkActivityIsDisplayed() }
            exposureNotificationRobot.clickContinueButton()
            selectAdult()

            selectFullyVaccinatedAnswers()

            waitFor { exposureNotificationReviewRobot.checkActivityIsDisplayed() }
            exposureNotificationReviewRobot.clickSubmitButton()

            exposureNotificationRiskyContactIsolationAdviceRobot.checkActivityIsDisplayed()
            exposureNotificationRiskyContactIsolationAdviceRobot.checkIsInAlreadyIsolatingViewState(
                remainingDaysInIsolation = 7,
                testingAdviceToShow = WalesWithinAdviceWindow(LocalDate.now())
            )
            exposureNotificationRiskyContactIsolationAdviceRobot.clickPrimaryBackToHome()

            waitFor { statusRobot.checkActivityIsDisplayed() }
            statusRobot.checkIsolationViewIsDisplayed()
        }
    }

    @Test
    fun givenInIndexCaseIsolationEnglandNewContactJourneyIsEnabled_whenReceivesExposureNotification_seesAlreadyIsolatingScreen_clickBackToHome_navigatesToHome() {
        runWithFeature(OLD_ENGLAND_CONTACT_CASE_FLOW, false) {
            givenLocalAuthorityIsInEngland()
            givenSelfAssessmentIsolation()

            startTestActivity<StatusActivity>()
            statusRobot.checkActivityIsDisplayed()

            testAppContext.sendExposureStateUpdatedBroadcast()

            exposureNotificationRobot.checkActivityIsDisplayed()
            exposureNotificationRobot.clickContinueButton()

            exposureNotificationRiskyContactIsolationAdviceRobot.checkActivityIsDisplayed()
            exposureNotificationRiskyContactIsolationAdviceRobot.checkIsInAlreadyIsolatingViewState(
                remainingDaysInIsolation = 7,
                testingAdviceToShow = Default
            )
            exposureNotificationRiskyContactIsolationAdviceRobot.clickPrimaryBackToHome()

            statusRobot.checkActivityIsDisplayed()
            statusRobot.checkIsolationViewIsDisplayed()
        }
    }

    @Test
    fun givenInContactCaseIsolationWalesNewContactJourneyIsEnabled_whenReceivesExposureNotification_clickBackToHome_navigatesToHome() {
        runWithFeature(OLD_WALES_CONTACT_CASE_FLOW, false) {
            givenLocalAuthorityIsInWales()

            startTestActivity<StatusActivity>()
            statusRobot.checkActivityIsDisplayed()

            testAppContext.sendExposureStateUpdatedBroadcast()

            exposureNotificationRobot.checkActivityIsDisplayed()
            exposureNotificationRobot.clickContinueButton()

            riskyContactIsolationOptOutRobot.checkActivityIsDisplayed()
            riskyContactIsolationOptOutRobot.clickSecondaryButton()

            statusRobot.checkActivityIsDisplayed()
            statusRobot.checkIsolationViewIsNotDisplayed()
        }
    }

    @Test
    fun givenInIndexCaseIsolationWalesNewContactJourneyIsEnabled_whenReceivesExposureNotification_seesAlreadyIsolatingScreen_clickBackToHome_navigatesToHome() {
        runWithFeature(OLD_WALES_CONTACT_CASE_FLOW, false) {
            givenLocalAuthorityIsInWales()
            givenSelfAssessmentIsolation()

            startTestActivity<StatusActivity>()
            statusRobot.checkActivityIsDisplayed()

            testAppContext.sendExposureStateUpdatedBroadcast()

            exposureNotificationRobot.checkActivityIsDisplayed()
            exposureNotificationRobot.clickContinueButton()

            exposureNotificationRiskyContactIsolationAdviceRobot.checkActivityIsDisplayed()
            exposureNotificationRiskyContactIsolationAdviceRobot.checkIsInAlreadyIsolatingViewState(
                remainingDaysInIsolation = 7,
                testingAdviceToShow = WalesWithinAdviceWindow(LocalDate.now())
            )
            exposureNotificationRiskyContactIsolationAdviceRobot.clickPrimaryBackToHome()

            statusRobot.checkActivityIsDisplayed()
            statusRobot.checkIsolationViewIsDisplayed()
        }
    }

    @Test
    fun whenUserClicksContinue_navigateToAgeLimitActivity_userCanComeBack() {
        runWithFeatureEnabled(OLD_ENGLAND_CONTACT_CASE_FLOW) {
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
    }

    @Test
    fun whenErrorIsShownOnAgeLimitScreen_thenValidAnswerSelected_thenClickContinue_thenNavigateBack_errorIsNotShown_andSelectedValueStored() {
        runWithFeatureEnabled(OLD_ENGLAND_CONTACT_CASE_FLOW) {
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
    }

    @Test
    fun givenContactIsolation_whenSelectingYesToAgeLimitQuestionAndYesToVaccinatedQuestionAndNoToDateQuestionAndClickingConfirm_thenErrorIsShown() {
        runWithFeatureEnabled(OLD_ENGLAND_CONTACT_CASE_FLOW) {
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
        exposureNotificationRiskyContactIsolationAdviceRobot.checkIsNotIsolatingAsMinorViewState(
            country = ENGLAND,
            Default
        )
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

        selectFullyVaccinatedAnswers()

        verifyEnglandFullyVaccinatedAnswersOnReviewScreen()

        exposureNotificationReviewRobot.clickSubmitButton()

        exposureNotificationRiskyContactIsolationAdviceRobot.checkActivityIsDisplayed()
        exposureNotificationRiskyContactIsolationAdviceRobot.checkIsInNotIsolatingAsFullyVaccinatedViewState(
            ENGLAND,
            Default
        )
    }

    @Test
    fun whenBackOnAgeLimitScreenFromReviewScreen_backButtonLeadsToExposureNotificationScreen() {
        runWithFeatureEnabled(OLD_ENGLAND_CONTACT_CASE_FLOW) {
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
    }

    @Test
    fun whenBackOnVaccinationStatusScreenFromReviewScreen_backButtonLeadsToAgeLimitScreen() {
        runWithFeatureEnabled(OLD_ENGLAND_CONTACT_CASE_FLOW) {
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
    }

    @Test
    fun whenChangingVaccinationStatusOnReviewScreen_navigatesDirectlyToVaccinationStatusScreen_andKeepsVaccinationAnswers() {
        runWithFeatureEnabled(OLD_ENGLAND_CONTACT_CASE_FLOW) {
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
    }

    @Test
    fun whenReceivesExposureNotification_forNewAdviceJourney_navigatesToRiskyContactIsolationOptOutActivity_clickPrimaryButton_navigateToStatusActivity_England() {
        runWithFeature(OLD_ENGLAND_CONTACT_CASE_FLOW, false) {
            runNewAdviceJourney(acknowledgementAction = {
                assertBrowserIsOpened(string.risky_contact_opt_out_primary_button_url) {
                    riskyContactIsolationOptOutRobot.clickPrimaryButton_opensInExternalBrowser()
                }
            }, country = PostCodeDistrict.ENGLAND)
        }
    }

    @Test
    fun whenReceivesExposureNotification_forNewAdviceJourney_navigatesToRiskyContactIsolationOptOutActivity_clickPrimaryButton_navigateToStatusActivity_Wales() {
        runWithFeature(OLD_WALES_CONTACT_CASE_FLOW, false) {
            runNewAdviceJourney(acknowledgementAction = {
                assertBrowserIsOpened(string.risky_contact_opt_out_primary_button_url_wales) {
                    riskyContactIsolationOptOutRobot.clickPrimaryButton_opensInExternalBrowser()
                }
            }, country = WALES)
        }
    }

    @Test
    fun whenReceivesExposureNotification_forNewAdviceJourney_navigatesToRiskyContactIsolationOptOutActivity_clickSecButton_navigateToStatusActivity() {
        runWithFeature(OLD_ENGLAND_CONTACT_CASE_FLOW, false) {
            runNewAdviceJourney(
                acknowledgementAction = {
                    riskyContactIsolationOptOutRobot.clickSecondaryButton()
                },
                country = PostCodeDistrict.ENGLAND
            )
        }
    }

    @Test
    fun whenReceivesExposureNotification_forNewAdviceJourney_navigatesToRiskyContactIsolationOptOutActivity_clickSecButton_navigateToStatusActivity_Wales() {
        runWithFeature(OLD_WALES_CONTACT_CASE_FLOW, false) {
            runNewAdviceJourney(acknowledgementAction = {
                riskyContactIsolationOptOutRobot.clickSecondaryButton()
            }, country = WALES)
        }
    }

    private fun runNewAdviceJourney(
        acknowledgementAction: () -> Unit,
        country: PostCodeDistrict = PostCodeDistrict.ENGLAND
    ) {
        when (country) {
            WALES -> givenLocalAuthorityIsInWales()
            else -> givenLocalAuthorityIsInEngland()
        }

        givenContactIsolation()

        startTestActivity<ExposureNotificationActivity>()

        exposureNotificationRobot.checkActivityIsDisplayed()

        exposureNotificationRobot.clickContinueButton()

        riskyContactIsolationOptOutRobot.checkActivityIsDisplayed()

        acknowledgementAction()

        statusRobot.checkActivityIsDisplayed()
        statusRobot.checkIsolationViewIsNotDisplayed()
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

    private fun selectFullyVaccinatedAnswers() {
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
