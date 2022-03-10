package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import com.jeroenmols.featureflag.framework.FeatureFlag.NEW_ENGLAND_CONTACT_CASE_JOURNEY
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EvaluateTestingAdviceToShow.TestingAdviceToShow.Default
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EvaluateTestingAdviceToShow.TestingAdviceToShow.WalesWithinAdviceWindow
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.ExposureNotificationVaccinationStatusActivity
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.OptOutResponseEntry
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType.ClinicalTrial
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType.DoseDate
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType.FullyVaccinated
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType.MedicallyExempt
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry.ENGLAND
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry.WALES
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ExposureNotificationAgeLimitRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ExposureNotificationReviewRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ExposureNotificationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ExposureNotificationVaccinationStatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.RiskyContactIsolationAdviceRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeature
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.IsolationSetupHelper
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.LocalAuthoritySetupHelper
import java.time.LocalDate

class RiskyContactOutcomeTest : EspressoTest(), LocalAuthoritySetupHelper, IsolationSetupHelper {

    private val exposureNotificationRobot = ExposureNotificationRobot()
    private val exposureNotificationAgeLimitRobot = ExposureNotificationAgeLimitRobot()
    private val exposureNotificationVaccinationStatusRobot = ExposureNotificationVaccinationStatusRobot()
    private val exposureNotificationReviewRobot = ExposureNotificationReviewRobot(testAppContext)
    private val exposureNotificationRiskyContactIsolationAdviceRobot = RiskyContactIsolationAdviceRobot()

    override val isolationHelper = IsolationHelper(testAppContext.clock)

    //region England
    @Test
    fun givenContactIsolation_whenSelectingNoToAgeLimitQuestionAndClickingConfirm_thenNavigatesToIsolatingScreenOptingOut() {
        runWithFeature(NEW_ENGLAND_CONTACT_CASE_JOURNEY, enabled = false) {
            givenLocalAuthorityIsInEngland()
            givenContactIsolation()

            navigateToAgeLimitActivity()

            exposureNotificationAgeLimitRobot.clickNoButton()

            exposureNotificationAgeLimitRobot.clickContinueButton()

            waitFor { exposureNotificationReviewRobot.checkActivityIsDisplayed() }

            exposureNotificationReviewRobot.verifyReviewViewState(
                ageResponse = false,
                vaccinationStatusResponses = emptyList()
            )

            exposureNotificationReviewRobot.clickSubmitButton()

            exposureNotificationRiskyContactIsolationAdviceRobot.checkActivityIsDisplayed()

            exposureNotificationRiskyContactIsolationAdviceRobot.checkIsNotIsolatingAsMinorViewState(
                country = ENGLAND,
                testingAdviceToShow = Default
            )
        }
    }

    @Test
    fun givenContactIsolation_whenSelectingYesToAgeLimitAndVaccinationQuestionsAndClickingConfirm_thenNavigatesToNotIsolatingAsFullyVaccinatedScreen() {
        runWithFeature(NEW_ENGLAND_CONTACT_CASE_JOURNEY, enabled = false) {
            givenLocalAuthorityIsInEngland()
            givenContactIsolation()

            navigateToAgeLimitActivity()

            exposureNotificationAgeLimitRobot.clickYesButton()

            exposureNotificationAgeLimitRobot.clickContinueButton()

            waitFor { exposureNotificationVaccinationStatusRobot.checkActivityIsDisplayed() }

            exposureNotificationVaccinationStatusRobot.clickDosesYesButton()

            waitFor { exposureNotificationVaccinationStatusRobot.checkDosesDateQuestionContainerDisplayed(true) }

            exposureNotificationVaccinationStatusRobot.clickDateYesButton()

            exposureNotificationVaccinationStatusRobot.clickContinueButton()

            waitFor { exposureNotificationReviewRobot.checkActivityIsDisplayed() }

            exposureNotificationReviewRobot.verifyReviewViewState(
                vaccinationStatusResponses = listOf(
                    OptOutResponseEntry(questionType = FullyVaccinated, response = true),
                    OptOutResponseEntry(questionType = DoseDate, response = true)
                )
            )

            exposureNotificationReviewRobot.clickSubmitButton()

            exposureNotificationRiskyContactIsolationAdviceRobot.checkActivityIsDisplayed()

            exposureNotificationRiskyContactIsolationAdviceRobot.checkIsInNotIsolatingAsFullyVaccinatedViewState(
                country = ENGLAND,
                testingAdviceToShow = Default
            )
        }
    }

    @Test
    fun givenContactIsolation_whenSelectingYesToAgeLimit_thenYesToAllDoses_thenNoToDate_thenYesToClinicalTrial_navigatesToNotIsolatingAsFullyVaccinatedScreen() {
        runWithFeature(NEW_ENGLAND_CONTACT_CASE_JOURNEY, enabled = false) {
            givenLocalAuthorityIsInEngland()
            givenContactIsolation()

            navigateToAgeLimitActivity()

            exposureNotificationAgeLimitRobot.clickYesButton()

            exposureNotificationAgeLimitRobot.clickContinueButton()

            waitFor { exposureNotificationVaccinationStatusRobot.checkActivityIsDisplayed() }

            exposureNotificationVaccinationStatusRobot.clickDosesYesButton()

            waitFor { exposureNotificationVaccinationStatusRobot.checkDosesDateQuestionContainerDisplayed(true) }

            exposureNotificationVaccinationStatusRobot.clickDateNoButton()

            exposureNotificationVaccinationStatusRobot.clickClinicalTrialYesButton()

            exposureNotificationVaccinationStatusRobot.clickContinueButton()

            waitFor { exposureNotificationReviewRobot.checkActivityIsDisplayed() }

            exposureNotificationReviewRobot.verifyReviewViewState(
                vaccinationStatusResponses = listOf(
                    OptOutResponseEntry(questionType = FullyVaccinated, response = true),
                    OptOutResponseEntry(questionType = DoseDate, response = false),
                    OptOutResponseEntry(questionType = ClinicalTrial, response = true)
                )
            )

            exposureNotificationReviewRobot.clickSubmitButton()

            exposureNotificationRiskyContactIsolationAdviceRobot.checkActivityIsDisplayed()

            exposureNotificationRiskyContactIsolationAdviceRobot.checkIsInNotIsolatingAsFullyVaccinatedViewState(
                country = ENGLAND,
                testingAdviceToShow = Default
            )
        }
    }

    private fun navigateToAgeLimitActivity() {
        startTestActivity<ExposureNotificationActivity>()

        exposureNotificationRobot.checkActivityIsDisplayed()

        exposureNotificationRobot.clickContinueButton()

        waitFor { exposureNotificationAgeLimitRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun givenIsInEngland_inContactIsolation_whenNoToFullyVaccinated_thenNoToMedicallyExempt_thenNoToClinicalTrial_navToNewlyIsolating() {
        givenLocalAuthorityIsInEngland()
        givenContactIsolation()

        startTestActivity<ExposureNotificationVaccinationStatusActivity>()

        with(exposureNotificationVaccinationStatusRobot) {
            checkActivityIsDisplayed()
            clickDosesNoButton()
            clickMedicallyExemptNoButton()
            clickClinicalTrialNoButton()
            clickContinueButton()
        }

        with(exposureNotificationReviewRobot) {
            checkActivityIsDisplayed()

            verifyReviewViewState(
                vaccinationStatusResponses = listOf(
                    OptOutResponseEntry(questionType = FullyVaccinated, response = false),
                    OptOutResponseEntry(questionType = MedicallyExempt, response = false),
                    OptOutResponseEntry(questionType = ClinicalTrial, response = false)
                )
            )

            clickSubmitButton()
        }

        exposureNotificationRiskyContactIsolationAdviceRobot.checkActivityIsDisplayed()

        exposureNotificationRiskyContactIsolationAdviceRobot.checkActivityIsDisplayed()
        exposureNotificationRiskyContactIsolationAdviceRobot.checkIsInNewlyIsolatingViewState(
            ENGLAND,
            remainingDaysInIsolation = 9,
            testingAdviceToShow = Default
        )
    }

    @Test
    fun givenIsInEngland_inContactIsolation_whenNoToFullyVaccinated_thenNoToMedicallyExempt_thenYesToClinicalTrial_navigatesToNotIsolatingAsFullyVaccinated() {
        runWithFeature(NEW_ENGLAND_CONTACT_CASE_JOURNEY, enabled = false) {
            givenLocalAuthorityIsInEngland()
            givenContactIsolation()

            startTestActivity<ExposureNotificationVaccinationStatusActivity>()

            exposureNotificationVaccinationStatusRobot.checkActivityIsDisplayed()

            exposureNotificationVaccinationStatusRobot.clickDosesNoButton()

            exposureNotificationVaccinationStatusRobot.clickMedicallyExemptNoButton()

            exposureNotificationVaccinationStatusRobot.clickClinicalTrialYesButton()

            exposureNotificationVaccinationStatusRobot.clickContinueButton()

            waitFor { exposureNotificationReviewRobot.checkActivityIsDisplayed() }

            exposureNotificationReviewRobot.verifyReviewViewState(
                vaccinationStatusResponses = listOf(
                    OptOutResponseEntry(questionType = FullyVaccinated, response = false),
                    OptOutResponseEntry(questionType = MedicallyExempt, response = false),
                    OptOutResponseEntry(questionType = ClinicalTrial, response = true)
                )
            )

            exposureNotificationReviewRobot.clickSubmitButton()

            exposureNotificationRiskyContactIsolationAdviceRobot.checkActivityIsDisplayed()

            exposureNotificationRiskyContactIsolationAdviceRobot.checkIsInNotIsolatingAsFullyVaccinatedViewState(
                country = ENGLAND,
                testingAdviceToShow = Default
            )
        }
    }

    @Test
    fun givenIsInEngland_inContactIsolation_whenYesToFullyVaccinated_thenNoToDate_thenNoToClinicalTrial_thenNoToMedicallyExempt_navigatesToIsolationScreen() {
        runWithFeature(NEW_ENGLAND_CONTACT_CASE_JOURNEY, enabled = false) {
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

            waitFor { exposureNotificationReviewRobot.checkActivityIsDisplayed() }

            exposureNotificationReviewRobot.verifyReviewViewState(
                vaccinationStatusResponses = listOf(
                    OptOutResponseEntry(questionType = FullyVaccinated, response = true),
                    OptOutResponseEntry(questionType = DoseDate, response = false),
                    OptOutResponseEntry(questionType = ClinicalTrial, response = false),
                    OptOutResponseEntry(questionType = MedicallyExempt, response = false)
                )
            )

            exposureNotificationReviewRobot.clickSubmitButton()

            exposureNotificationRiskyContactIsolationAdviceRobot.checkActivityIsDisplayed()

            exposureNotificationRiskyContactIsolationAdviceRobot.checkIsInNewlyIsolatingViewState(
                ENGLAND,
                remainingDaysInIsolation = 9,
                testingAdviceToShow = Default
            )
        }
    }

    @Test
    fun givenIsInEngland_inContactIsolation_whenNoToFullyVaccinated_thenYesToMedicallyExempt_navigatesToNotIsolatingAsMedicallyExemptScreen() {
        runWithFeature(NEW_ENGLAND_CONTACT_CASE_JOURNEY, enabled = false) {
            givenLocalAuthorityIsInEngland()
            givenContactIsolation()

            startTestActivity<ExposureNotificationVaccinationStatusActivity>()

            exposureNotificationVaccinationStatusRobot.checkActivityIsDisplayed()

            exposureNotificationVaccinationStatusRobot.clickDosesNoButton()

            exposureNotificationVaccinationStatusRobot.clickMedicallyExemptYesButton()

            exposureNotificationVaccinationStatusRobot.clickContinueButton()

            waitFor { exposureNotificationReviewRobot.checkActivityIsDisplayed() }

            exposureNotificationReviewRobot.verifyReviewViewState(
                vaccinationStatusResponses = listOf(
                    OptOutResponseEntry(questionType = FullyVaccinated, response = false),
                    OptOutResponseEntry(questionType = MedicallyExempt, response = true)
                )
            )

            exposureNotificationReviewRobot.clickSubmitButton()

            exposureNotificationRiskyContactIsolationAdviceRobot.checkActivityIsDisplayed()

            exposureNotificationRiskyContactIsolationAdviceRobot.checkIsInNotIsolatingAsMedicallyExemptViewStateForEngland()
        }
    }

    @Test
    fun givenIsInEngland_inContactIsolation_whenYesToFullyVaccinated_thenNoToDate_thenNoToClinicalTrial_thenYesToExempt_navigatesToNotIsolatingAsMedicallyExempt() {
        runWithFeature(NEW_ENGLAND_CONTACT_CASE_JOURNEY, enabled = false) {
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

            waitFor { exposureNotificationReviewRobot.checkActivityIsDisplayed() }

            exposureNotificationReviewRobot.verifyReviewViewState(
                vaccinationStatusResponses = listOf(
                    OptOutResponseEntry(questionType = FullyVaccinated, response = true),
                    OptOutResponseEntry(questionType = DoseDate, response = false),
                    OptOutResponseEntry(questionType = ClinicalTrial, response = false),
                    OptOutResponseEntry(questionType = MedicallyExempt, response = true)
                )
            )

            exposureNotificationReviewRobot.clickSubmitButton()

            exposureNotificationRiskyContactIsolationAdviceRobot.checkActivityIsDisplayed()

            exposureNotificationRiskyContactIsolationAdviceRobot.checkIsInNotIsolatingAsMedicallyExemptViewStateForEngland()
        }
    }

    //endregion

    //region Wales

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

        waitFor { exposureNotificationReviewRobot.checkActivityIsDisplayed() }

        exposureNotificationReviewRobot.verifyReviewViewState(
            vaccinationStatusResponses = listOf(
                OptOutResponseEntry(questionType = FullyVaccinated, response = true),
                OptOutResponseEntry(questionType = DoseDate, response = false),
                OptOutResponseEntry(questionType = ClinicalTrial, response = false)
            )
        )

        exposureNotificationReviewRobot.clickSubmitButton()

        exposureNotificationRiskyContactIsolationAdviceRobot.checkActivityIsDisplayed()

        exposureNotificationRiskyContactIsolationAdviceRobot.checkIsInNewlyIsolatingViewState(
            WALES,
            remainingDaysInIsolation = 9,
            testingAdviceToShow = WalesWithinAdviceWindow(date = LocalDate.now(testAppContext.clock).plusDays(6))
        )
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

        waitFor { exposureNotificationReviewRobot.checkActivityIsDisplayed() }

        exposureNotificationReviewRobot.verifyReviewViewState(
            vaccinationStatusResponses = listOf(
                OptOutResponseEntry(questionType = FullyVaccinated, response = true),
                OptOutResponseEntry(questionType = DoseDate, response = false),
                OptOutResponseEntry(questionType = ClinicalTrial, response = true)
            )
        )

        exposureNotificationReviewRobot.clickSubmitButton()

        exposureNotificationRiskyContactIsolationAdviceRobot.checkActivityIsDisplayed()

        exposureNotificationRiskyContactIsolationAdviceRobot.checkIsInNotIsolatingAsFullyVaccinatedViewState(
            country = WALES,
            testingAdviceToShow = WalesWithinAdviceWindow(date = LocalDate.now(testAppContext.clock).plusDays(6))
        )
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

        waitFor { exposureNotificationReviewRobot.checkActivityIsDisplayed() }

        exposureNotificationReviewRobot.verifyReviewViewState(
            vaccinationStatusResponses = listOf(
                OptOutResponseEntry(questionType = FullyVaccinated, response = false),
                OptOutResponseEntry(questionType = ClinicalTrial, response = false)
            )
        )

        exposureNotificationReviewRobot.clickSubmitButton()

        exposureNotificationRiskyContactIsolationAdviceRobot.checkActivityIsDisplayed()

        exposureNotificationRiskyContactIsolationAdviceRobot.checkIsInNewlyIsolatingViewState(
            WALES,
            remainingDaysInIsolation = 9,
            testingAdviceToShow = WalesWithinAdviceWindow(date = LocalDate.now(testAppContext.clock).plusDays(6))
        )
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

        waitFor { exposureNotificationReviewRobot.checkActivityIsDisplayed() }

        exposureNotificationReviewRobot.verifyReviewViewState(
            vaccinationStatusResponses = listOf(
                OptOutResponseEntry(questionType = FullyVaccinated, response = false),
                OptOutResponseEntry(questionType = ClinicalTrial, response = true)
            )
        )

        exposureNotificationReviewRobot.clickSubmitButton()

        exposureNotificationRiskyContactIsolationAdviceRobot.checkActivityIsDisplayed()

        exposureNotificationRiskyContactIsolationAdviceRobot.checkIsInNotIsolatingAsFullyVaccinatedViewState(
            country = WALES,
            testingAdviceToShow = WalesWithinAdviceWindow(date = LocalDate.now(testAppContext.clock).plusDays(6))
        )
    }
    //endregion
}
