package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.LANDSCAPE
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.PORTRAIT
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ReportedTestRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelfReportCheckAnswersRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelfReportSymptomsOnsetRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelfReportSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.setScreenOrientation
import java.time.LocalDate

class SelfReportSymptomsOnsetActivityTest : EspressoTest() {

    private val selfReportSymptomsOnsetRobot = SelfReportSymptomsOnsetRobot()
    private val selfReportSymptomsRobot = SelfReportSymptomsRobot()
    private val reportedTestRobot = ReportedTestRobot()
    private val selfReportCheckAnswersRobot = SelfReportCheckAnswersRobot()

    @Test
    fun showErrorStateWhenNoSymptomsOnsetDateIsSelectedAndContinueIsClicked() {
        startActivityWithExtras()

        selfReportSymptomsOnsetRobot.checkActivityIsDisplayed()

        selfReportSymptomsOnsetRobot.checkNoDateIsSelected()
        selfReportSymptomsOnsetRobot.checkDoNotRememberDateIsNotChecked()
        selfReportSymptomsOnsetRobot.checkErrorIsVisible(false)

        selfReportSymptomsOnsetRobot.clickContinueButton()

        waitFor { selfReportSymptomsOnsetRobot.checkErrorIsVisible(true) }
    }

    @Test
    fun cannotRememberChecked_choiceSurvivesRotation() {
        startActivityWithExtras()

        selfReportSymptomsOnsetRobot.checkActivityIsDisplayed()

        selfReportSymptomsOnsetRobot.checkDoNotRememberDateIsNotChecked()
        selfReportSymptomsOnsetRobot.selectCannotRememberDate()

        waitFor { selfReportSymptomsOnsetRobot.checkDoNotRememberDateIsChecked() }
        setScreenOrientation(LANDSCAPE)
        waitFor { selfReportSymptomsOnsetRobot.checkDoNotRememberDateIsChecked() }
        setScreenOrientation(PORTRAIT)
        waitFor { selfReportSymptomsOnsetRobot.checkDoNotRememberDateIsChecked() }
    }

    @Test
    fun selectDoNotRememberDate_canChangeToSpecificDate() {
        startActivityWithExtras()

        selfReportSymptomsOnsetRobot.checkActivityIsDisplayed()

        selfReportSymptomsOnsetRobot.checkDoNotRememberDateIsNotChecked()

        selfReportSymptomsOnsetRobot.selectCannotRememberDate()

        selfReportSymptomsOnsetRobot.checkDoNotRememberDateIsChecked()

        selfReportSymptomsOnsetRobot.checkErrorIsVisible(false)

        selfReportSymptomsOnsetRobot.clickSelectDate()

        selfReportSymptomsOnsetRobot.selectDayOfMonth(LocalDate.now(testAppContext.clock).dayOfMonth)

        selfReportSymptomsOnsetRobot.checkDoNotRememberDateIsNotChecked()

        selfReportSymptomsOnsetRobot.checkErrorIsVisible(false)
    }

    @Test
    fun pressBack_goToSymptomsActivity() {
        startActivityWithExtras()

        selfReportSymptomsOnsetRobot.checkActivityIsDisplayed()

        testAppContext.device.pressBack()

        waitFor { selfReportSymptomsRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun pressContinueWithCannotRememberSymptomsDateAndLFDNotFromNHS_goToCheckAnswersActivity() {
        startActivityWithExtras()

        selfReportSymptomsOnsetRobot.checkActivityIsDisplayed()

        selfReportSymptomsOnsetRobot.selectCannotRememberDate()
        selfReportSymptomsOnsetRobot.clickContinueButton()

        waitFor { selfReportCheckAnswersRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun pressContinueWithCannotRememberSymptomsDateAndLFDFromNHS_goToReportedTestActivity() {
        startActivityWithLFDFromNHS()

        selfReportSymptomsOnsetRobot.checkActivityIsDisplayed()

        selfReportSymptomsOnsetRobot.selectCannotRememberDate()
        selfReportSymptomsOnsetRobot.clickContinueButton()

        waitFor { reportedTestRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun startActivityWithCannotRememberSelectionFromPrevious_selectionShouldBeShown() {
        startActivityWithCannotRememberAlreadyChosen()

        selfReportSymptomsOnsetRobot.checkActivityIsDisplayed()

        waitFor { selfReportSymptomsOnsetRobot.checkDoNotRememberDateIsChecked() }
    }

    @Test
    fun startActivityWithSymptomsDateSelectionFromPrevious_selectionShouldBeShown() {
        startActivityWithSymptomsOnsetDateAlreadyChosen()

        selfReportSymptomsOnsetRobot.checkActivityIsDisplayed()

        waitFor { selfReportSymptomsOnsetRobot.checkDateIsChosen() }
    }

    private fun startActivityWithExtras() {
        startTestActivity<SelfReportSymptomsOnsetActivity> {
            putExtra(
                SelfReportSymptomsOnsetActivity.SELF_REPORT_QUESTIONS_DATA_KEY, selfReportTestQuestions
            )
        }
    }

    private fun startActivityWithCannotRememberAlreadyChosen() {
        startTestActivity<SelfReportSymptomsOnsetActivity> {
            putExtra(
                SelfReportSymptomsOnsetActivity.SELF_REPORT_QUESTIONS_DATA_KEY, selfReportTestQuestions
                    .copy(symptomsOnsetDate = ChosenDate(false, LocalDate.now(testAppContext.clock)))
            )
        }
    }

    private fun startActivityWithSymptomsOnsetDateAlreadyChosen() {
        startTestActivity<SelfReportSymptomsOnsetActivity> {
            putExtra(
                SelfReportSymptomsOnsetActivity.SELF_REPORT_QUESTIONS_DATA_KEY, selfReportTestQuestions
                    .copy(symptomsOnsetDate = ChosenDate(true, LocalDate.now(testAppContext.clock)))
            )
        }
    }

    private fun startActivityWithLFDFromNHS() {
        startTestActivity<SelfReportSymptomsOnsetActivity> {
            putExtra(
                SelfReportSymptomsOnsetActivity.SELF_REPORT_QUESTIONS_DATA_KEY, selfReportTestQuestions
                    .copy(isNHSTest = true)
            )
        }
    }

    private val selfReportTestQuestions = SelfReportTestQuestions(
        POSITIVE,
        temporaryExposureKeys = listOf(
            NHSTemporaryExposureKey(
                key = "key",
                rollingStartNumber = 2
            )
        ),
        RAPID_SELF_REPORTED,
        false,
        ChosenDate(false, LocalDate.now(testAppContext.clock)),
        true,
        null,
        null
    )
}
