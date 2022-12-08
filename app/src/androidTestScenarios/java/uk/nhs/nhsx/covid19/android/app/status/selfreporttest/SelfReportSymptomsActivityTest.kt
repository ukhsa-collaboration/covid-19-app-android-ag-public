package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.LANDSCAPE
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.PORTRAIT
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ReportedTestRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelectTestDateRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelfReportCheckAnswersRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelfReportSymptomsOnsetRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelfReportSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.setScreenOrientation
import java.time.LocalDate

class SelfReportSymptomsActivityTest : EspressoTest() {

    private val selfReportSymptomsRobot = SelfReportSymptomsRobot()
    private val selectTestDateRobot = SelectTestDateRobot()
    private val selfReportSymptomsOnsetRobot = SelfReportSymptomsOnsetRobot()
    private val reportedTestRobot = ReportedTestRobot()
    private val selfReportCheckAnswersRobot = SelfReportCheckAnswersRobot()

    @Test
    fun showErrorStateWhenSymptomsOptionNotSelectedAndContinueIsClicked() {
        startActivityWithExtras()

        selfReportSymptomsRobot.checkActivityIsDisplayed()

        selfReportSymptomsRobot.checkNothingIsSelected()
        selfReportSymptomsRobot.checkErrorIsVisible(false)

        selfReportSymptomsRobot.clickContinueButton()

        waitFor { selfReportSymptomsRobot.checkErrorIsVisible(true) }
    }

    @Test
    fun symptomsOptionSelected_choiceSurvivesRotation() {
        startActivityWithExtras()

        selfReportSymptomsRobot.checkActivityIsDisplayed()

        selfReportSymptomsRobot.checkNothingIsSelected()
        selfReportSymptomsRobot.clickYesButton()

        waitFor { selfReportSymptomsRobot.checkYesIsSelected() }
        setScreenOrientation(LANDSCAPE)
        waitFor { selfReportSymptomsRobot.checkYesIsSelected() }
        setScreenOrientation(PORTRAIT)
        waitFor { selfReportSymptomsRobot.checkYesIsSelected() }
    }

    @Test
    fun symptomsOptionSelected_canChangeSelection() {
        startActivityWithExtras()

        selfReportSymptomsRobot.checkActivityIsDisplayed()

        selfReportSymptomsRobot.checkNothingIsSelected()
        selfReportSymptomsRobot.clickNoButton()
        waitFor { selfReportSymptomsRobot.checkNoIsSelected() }

        selfReportSymptomsRobot.clickYesButton()
        waitFor { selfReportSymptomsRobot.checkYesIsSelected() }
    }

    @Test
    fun pressBack_goToTestDateActivity() {
        startActivityWithExtras()

        selfReportSymptomsRobot.checkActivityIsDisplayed()

        testAppContext.device.pressBack()

        selectTestDateRobot.checkActivityIsDisplayed()
    }

    @Test
    fun startActivityWithSelectionFromPrevious_selectionShouldBeShown() {
        startActivityWithSymptomsOptionYesAlreadyChosen()

        selfReportSymptomsRobot.checkActivityIsDisplayed()

        selfReportSymptomsRobot.checkYesIsSelected()
    }

    @Test
    fun pressContinueWithYesAnswer_goToSymptomsOnsetActivity() {
        startActivityWithExtras()

        selfReportSymptomsRobot.checkActivityIsDisplayed()

        selfReportSymptomsRobot.clickYesButton()
        selfReportSymptomsRobot.clickContinueButton()

        waitFor { selfReportSymptomsOnsetRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun pressContinueWithNoAnswerAndLFDFromNHS_goToReportedTestActivity() {
        startActivityWithLFDFromNHS()

        selfReportSymptomsRobot.checkActivityIsDisplayed()

        selfReportSymptomsRobot.clickNoButton()
        selfReportSymptomsRobot.clickContinueButton()

        waitFor { reportedTestRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun pressContinueWithNoAnswerAndLFDNotFromNHS_goToCheckAnswersActivity() {
        startActivityWithExtras()

        selfReportSymptomsRobot.checkActivityIsDisplayed()

        selfReportSymptomsRobot.clickNoButton()
        selfReportSymptomsRobot.clickContinueButton()

        waitFor { selfReportCheckAnswersRobot.checkActivityIsDisplayed() }
    }

    private fun startActivityWithExtras() {
        startTestActivity<SelfReportSymptomsActivity> {
            putExtra(
                SelfReportSymptomsActivity.SELF_REPORT_QUESTIONS_DATA_KEY, selfReportTestQuestions
            )
        }
    }

    private fun startActivityWithSymptomsOptionYesAlreadyChosen() {
        startTestActivity<SelfReportSymptomsActivity> {
            putExtra(
                SelfReportSymptomsActivity.SELF_REPORT_QUESTIONS_DATA_KEY, selfReportTestQuestions.copy(hadSymptoms = true)
            )
        }
    }

    private fun startActivityWithLFDFromNHS() {
        startTestActivity<SelfReportSymptomsActivity> {
            putExtra(
                SelfReportSymptomsActivity.SELF_REPORT_QUESTIONS_DATA_KEY, selfReportTestQuestions.copy(isNHSTest = true)
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
        ChosenDate(false, LocalDate.now()),
        null,
        null,
        null
    )
}
