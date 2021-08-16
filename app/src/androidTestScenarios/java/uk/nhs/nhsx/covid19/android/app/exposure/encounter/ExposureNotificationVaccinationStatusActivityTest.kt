package uk.nhs.nhsx.covid19.android.app.exposure.encounter

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

class ExposureNotificationVaccinationStatusActivityTest : EspressoTest(), IsolationSetupHelper,
    LocalAuthoritySetupHelper {
    private val vaccinationStatusRobot = ExposureNotificationVaccinationStatusRobot()
    override val isolationHelper = IsolationHelper(testAppContext.clock)

    @Test
    fun whenUserClicksContinue_errorIsDisplayed() {
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
}
