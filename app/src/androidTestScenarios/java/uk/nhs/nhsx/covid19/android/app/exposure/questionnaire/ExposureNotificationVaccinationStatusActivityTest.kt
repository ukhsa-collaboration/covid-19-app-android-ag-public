package uk.nhs.nhsx.covid19.android.app.exposure.questionnaire

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.LANDSCAPE
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.PORTRAIT
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.testhelpers.assertBrowserIsOpened
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ExposureNotificationVaccinationStatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.setScreenOrientation
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.IsolationSetupHelper
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.LocalAuthoritySetupHelper
import uk.nhs.nhsx.covid19.android.app.util.uiLongFormat
import java.time.LocalDate
import kotlin.test.assertTrue

class ExposureNotificationVaccinationStatusActivityTest : EspressoTest(), IsolationSetupHelper,
    LocalAuthoritySetupHelper {
    private val vaccinationStatusRobot = ExposureNotificationVaccinationStatusRobot()
    override val isolationHelper = IsolationHelper(testAppContext.clock)

    @Test
    fun whenNotInIndexCase_subtitleIsDisplayed() {
        givenContactIsolation()
        startTestActivity<ExposureNotificationVaccinationStatusActivity>()

        waitFor { vaccinationStatusRobot.checkActivityIsDisplayed() }
        vaccinationStatusRobot.checkSubtitleDisplayed(displayed = true)
    }

    @Test
    fun whenInIndexCase_subtitleIsNotDisplayed() {
        givenSelfAssessmentAndContactIsolation()
        startTestActivity<ExposureNotificationVaccinationStatusActivity>()

        waitFor { vaccinationStatusRobot.checkActivityIsDisplayed() }
        vaccinationStatusRobot.checkSubtitleDisplayed(displayed = false)
    }

    @Test
    fun whenUserClicksContinue_errorIsDisplayed() {
        givenContactIsolation()
        startTestActivity<ExposureNotificationVaccinationStatusActivity>()

        waitFor { vaccinationStatusRobot.checkActivityIsDisplayed() }

        waitFor { vaccinationStatusRobot.checkErrorVisible(false) }
        setScreenOrientation(LANDSCAPE)
        waitFor { vaccinationStatusRobot.checkErrorVisible(false) }
        setScreenOrientation(PORTRAIT)
        waitFor { vaccinationStatusRobot.checkErrorVisible(false) }

        waitFor { vaccinationStatusRobot.checkDosesNothingSelected() }
        vaccinationStatusRobot.clickContinueButton()

        setScreenOrientation(LANDSCAPE)
        waitFor { vaccinationStatusRobot.checkErrorVisible(true) }
        setScreenOrientation(PORTRAIT)
        waitFor { vaccinationStatusRobot.checkErrorVisible(true) }
    }

    @Test
    fun whenUserHasMadeSelection_selectionSurvivesRotation() {
        givenContactIsolation()
        startTestActivity<ExposureNotificationVaccinationStatusActivity>()

        waitFor { vaccinationStatusRobot.checkActivityIsDisplayed() }
        waitFor { vaccinationStatusRobot.checkDosesNothingSelected() }

        vaccinationStatusRobot.clickDosesYesButton()
        waitFor { vaccinationStatusRobot.checkDosesYesSelected() }
        setScreenOrientation(LANDSCAPE)
        waitFor { vaccinationStatusRobot.checkDosesYesSelected() }
        setScreenOrientation(PORTRAIT)
        waitFor { vaccinationStatusRobot.checkDosesYesSelected() }

        vaccinationStatusRobot.clickDosesNoButton()
        waitFor { vaccinationStatusRobot.checkDosesNoSelected() }
        setScreenOrientation(LANDSCAPE)
        waitFor { vaccinationStatusRobot.checkDosesNoSelected() }
        setScreenOrientation(PORTRAIT)
        waitFor { vaccinationStatusRobot.checkDosesNoSelected() }
    }

    @Test
    fun whenSelectYesToDosesQuestion_dateQuestionIsDisplayed_andDateIs15DaysBeforeEncounterDate() {
        givenContactIsolation()
        startTestActivity<ExposureNotificationVaccinationStatusActivity>()

        waitFor { vaccinationStatusRobot.checkActivityIsDisplayed() }

        vaccinationStatusRobot.clickDosesYesButton()

        waitFor { vaccinationStatusRobot.checkDosesDateQuestionContainerDisplayed(true) }

        setScreenOrientation(LANDSCAPE)

        waitFor { vaccinationStatusRobot.checkDosesDateQuestionContainerDisplayed(true) }

        val expectedDate = LocalDate.now(testAppContext.clock).minusDays(17)
        val expectedDateString = expectedDate.uiLongFormat(testAppContext.app)
        waitFor { vaccinationStatusRobot.checkDosesDateQuestionDisplayedWithDate(expectedDateString) }
    }

    @Test
    fun whenSecondQuestionIsShown_andOptionIsSelected_andAnswerToFirstQuestionChangedToNoThenYes_thenSecondQuestionHasNoOptionSelected() {
        givenContactIsolation()
        startTestActivity<ExposureNotificationVaccinationStatusActivity>()

        waitFor { vaccinationStatusRobot.checkActivityIsDisplayed() }

        vaccinationStatusRobot.clickDosesYesButton()

        waitFor { vaccinationStatusRobot.checkDosesDateQuestionContainerDisplayed(true) }

        vaccinationStatusRobot.clickDateNoButton()

        vaccinationStatusRobot.clickDosesNoButton()

        waitFor { vaccinationStatusRobot.checkDosesDateQuestionContainerDisplayed(false) }

        vaccinationStatusRobot.clickDosesYesButton()

        waitFor { vaccinationStatusRobot.checkDosesDateQuestionContainerDisplayed(true) }

        waitFor { vaccinationStatusRobot.checkDateNothingSelected() }
    }

    @Test
    fun whenInEngland_whenAllDosesIsYes_whenFourthQuestionIsAnswered_answerToFirstQuestionChangedToNo_secondQuestionHasNoOptionSelected() {
        givenLocalAuthorityIsInEngland()
        givenContactIsolation()
        startTestActivity<ExposureNotificationVaccinationStatusActivity>()

        waitFor { vaccinationStatusRobot.checkActivityIsDisplayed() }

        vaccinationStatusRobot.clickDosesYesButton()

        waitFor { vaccinationStatusRobot.checkDosesDateQuestionContainerDisplayed(true) }

        vaccinationStatusRobot.clickDateNoButton()
        vaccinationStatusRobot.clickClinicalTrialNoButton()
        vaccinationStatusRobot.clickMedicallyExemptNoButton()

        vaccinationStatusRobot.clickDosesNoButton()

        waitFor { vaccinationStatusRobot.checkDosesDateQuestionContainerDisplayed(false) }

        waitFor { vaccinationStatusRobot.checkMedicallyExemptNothingSelected() }
    }

    @Test
    fun whenInWales_verifyMedicallyExemptQuestionIsNotDisplayed() {
        givenLocalAuthorityIsInWales()
        givenContactIsolation()

        startTestActivity<ExposureNotificationVaccinationStatusActivity>()

        waitFor { vaccinationStatusRobot.checkActivityIsDisplayed() }

        vaccinationStatusRobot.clickDosesYesButton()

        waitFor { vaccinationStatusRobot.checkDosesDateQuestionContainerDisplayed(true) }

        vaccinationStatusRobot.clickDateNoButton()

        waitFor { vaccinationStatusRobot.checkClinicalTrialQuestionContainerDisplayed(true) }

        vaccinationStatusRobot.clickClinicalTrialNoButton()

        waitFor { vaccinationStatusRobot.checkMedicallyExemptQuestionContainerDisplayed(false) }
    }

    @Test
    fun whenInEngland_verifyMedicallyExemptQuestionIsDisplayed() {
        givenLocalAuthorityIsInEngland()
        givenContactIsolation()

        startTestActivity<ExposureNotificationVaccinationStatusActivity>()

        waitFor { vaccinationStatusRobot.checkActivityIsDisplayed() }

        vaccinationStatusRobot.clickDosesYesButton()

        waitFor { vaccinationStatusRobot.checkDosesDateQuestionContainerDisplayed(true) }

        vaccinationStatusRobot.clickDateNoButton()

        waitFor { vaccinationStatusRobot.checkClinicalTrialQuestionContainerDisplayed(true) }

        vaccinationStatusRobot.clickClinicalTrialNoButton()

        waitFor { vaccinationStatusRobot.checkMedicallyExemptQuestionContainerDisplayed(true) }
    }

    @Test
    fun whenInEngland_verifyClinicalTrialQuestionIsNotDisplayed() {
        givenLocalAuthorityIsInEngland()
        givenContactIsolation()

        startTestActivity<ExposureNotificationVaccinationStatusActivity>()

        waitFor { vaccinationStatusRobot.checkActivityIsDisplayed() }

        vaccinationStatusRobot.clickDosesNoButton()

        waitFor { vaccinationStatusRobot.checkClinicalTrialQuestionContainerDisplayed(false) }
    }

    @Test
    fun whenInEngland_selectNoToMedicallyExemptQuestion_verifyClinicalTrialQuestionIsDisplayed() {
        givenLocalAuthorityIsInEngland()
        givenContactIsolation()

        startTestActivity<ExposureNotificationVaccinationStatusActivity>()

        waitFor { vaccinationStatusRobot.checkActivityIsDisplayed() }

        vaccinationStatusRobot.clickDosesNoButton()

        waitFor { vaccinationStatusRobot.checkMedicallyExemptQuestionContainerDisplayed(true) }

        vaccinationStatusRobot.clickMedicallyExemptNoButton()

        waitFor { vaccinationStatusRobot.checkClinicalTrialQuestionContainerDisplayed(true) }
    }

    @Test
    fun whenInWales_whenSelectNoToDosesQuestion_verifyClinicalTrialQuestionIsDisplayed() {
        givenLocalAuthorityIsInWales()
        givenContactIsolation()

        startTestActivity<ExposureNotificationVaccinationStatusActivity>()

        waitFor { vaccinationStatusRobot.checkActivityIsDisplayed() }

        vaccinationStatusRobot.clickDosesNoButton()

        waitFor { vaccinationStatusRobot.checkClinicalTrialQuestionContainerDisplayed(true) }
    }

    @Test
    fun whenSelectYesToDosesQuestion_andNoToVaccinationDate_dateQuestionIsDisplayed() {
        givenContactIsolation()
        startTestActivity<ExposureNotificationVaccinationStatusActivity>()

        waitFor { vaccinationStatusRobot.checkActivityIsDisplayed() }

        vaccinationStatusRobot.clickDosesYesButton()

        waitFor { vaccinationStatusRobot.checkDosesDateQuestionContainerDisplayed(true) }

        vaccinationStatusRobot.clickDateNoButton()

        waitFor { vaccinationStatusRobot.checkClinicalTrialQuestionContainerDisplayed(true) }

        val expectedDate = LocalDate.now(testAppContext.clock).minusDays(17)
        val expectedDateString = expectedDate.uiLongFormat(testAppContext.app)
        waitFor { vaccinationStatusRobot.checkDosesDateQuestionDisplayedWithDate(expectedDateString) }
    }

    @Test
    fun approvedVaccineUrlIsEnglandSpecificForUserWithEnglishLocalAuthority() {
        givenLocalAuthorityIsInEngland()
        givenContactIsolation()
        startTestActivity<ExposureNotificationVaccinationStatusActivity>()
        waitFor { vaccinationStatusRobot.checkActivityIsDisplayed() }

        assertBrowserIsOpened(R.string.exposure_notification_vaccination_status_all_doses_question_link_url) {
            vaccinationStatusRobot.clickApprovedVaccinesLink()
        }
    }

    @Test
    fun approvedVaccineUrlIsWalesSpecificForUserWithWelshLocalAuthority() {
        givenLocalAuthorityIsInWales()
        givenContactIsolation()
        startTestActivity<ExposureNotificationVaccinationStatusActivity>()
        waitFor { vaccinationStatusRobot.checkActivityIsDisplayed() }

        assertBrowserIsOpened(R.string.exposure_notification_vaccination_status_all_doses_question_link_url_wls) {
            vaccinationStatusRobot.clickApprovedVaccinesLink()
        }
    }

    @Test
    fun whenNotInContactIsolation_activityFinishes() {
        val activity = startTestActivity<ExposureNotificationVaccinationStatusActivity>()

        waitFor { assertTrue(activity!!.isDestroyed) }
    }
}
