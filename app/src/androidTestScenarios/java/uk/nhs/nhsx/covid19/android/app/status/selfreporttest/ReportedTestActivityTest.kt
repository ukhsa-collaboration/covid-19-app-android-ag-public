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

class ReportedTestActivityTest : EspressoTest() {
    private val reportedTestRobot = ReportedTestRobot()
    private val selectTestDateRobot = SelectTestDateRobot()
    private val selfReportSymptomsRobot = SelfReportSymptomsRobot()
    private val selfReportSymptomsOnsetRobot = SelfReportSymptomsOnsetRobot()
    private val selfReportCheckAnswersRobot = SelfReportCheckAnswersRobot()

    @Test
    fun showErrorStateWhenNoOptionIsSelectedAndContinueIsClicked() {
        startActivityWithExtras()

        reportedTestRobot.checkActivityIsDisplayed()

        reportedTestRobot.checkNothingIsSelected()
        reportedTestRobot.checkErrorIsVisible(false)

        reportedTestRobot.clickContinueButton()

        waitFor { reportedTestRobot.checkErrorIsVisible(true) }
    }

    @Test
    fun reportedTestOptionSelected_choiceSurvivesRotation() {
        startActivityWithExtras()

        reportedTestRobot.checkActivityIsDisplayed()

        reportedTestRobot.checkNothingIsSelected()
        reportedTestRobot.clickYesButton()

        waitFor { reportedTestRobot.checkYesIsSelected() }
        setScreenOrientation(LANDSCAPE)
        waitFor { reportedTestRobot.checkYesIsSelected() }
        setScreenOrientation(PORTRAIT)
        waitFor { reportedTestRobot.checkYesIsSelected() }
    }

    @Test
    fun reportedTestOptionSelected_canChangeSelection() {
        startActivityWithExtras()

        reportedTestRobot.checkActivityIsDisplayed()

        reportedTestRobot.checkNothingIsSelected()
        reportedTestRobot.clickNoButton()
        waitFor { reportedTestRobot.checkNoIsSelected() }

        reportedTestRobot.clickYesButton()
        waitFor { reportedTestRobot.checkYesIsSelected() }
    }

    @Test
    fun pressContinue_goToCheckAnswersActivity() {
        startActivityWithExtras()

        reportedTestRobot.checkActivityIsDisplayed()

        reportedTestRobot.checkNothingIsSelected()
        reportedTestRobot.clickNoButton()

        reportedTestRobot.clickContinueButton()

        waitFor { selfReportCheckAnswersRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun pressBackWithSymptoms_goToSymptomsOnsetActivity() {
        startActivityWithExtras()

        reportedTestRobot.checkActivityIsDisplayed()

        testAppContext.device.pressBack()

        waitFor { selfReportSymptomsOnsetRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun pressBackWithSymptomsAnsweredNo_goToSymptomsActivity() {
        startActivityWithoutSymptoms()

        reportedTestRobot.checkActivityIsDisplayed()

        testAppContext.device.pressBack()

        waitFor { selfReportSymptomsRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun pressBackWithoutSymptomsAnswer_goToSelectTestDateActivity() {
        startActivityWithoutSymptomsAnswer()

        reportedTestRobot.checkActivityIsDisplayed()

        testAppContext.device.pressBack()

        waitFor { selectTestDateRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun startActivityWithYesSelectionFromPrevious_selectionShouldBeShown() {
        startActivityWithSelectionFromPrevious(reportedTest = true)

        reportedTestRobot.checkActivityIsDisplayed()

        waitFor { reportedTestRobot.checkYesIsSelected() }
    }

    @Test
    fun startActivityWithNoSelectionFromPrevious_selectionShouldBeShown() {
        startActivityWithSelectionFromPrevious(reportedTest = false)

        reportedTestRobot.checkActivityIsDisplayed()

        waitFor { reportedTestRobot.checkNoIsSelected() }
    }

    private fun startActivityWithExtras() {
        startTestActivity<ReportedTestActivity> {
            putExtra(
                ReportedTestActivity.SELF_REPORT_QUESTIONS_DATA_KEY, selfReportTestQuestions
            )
        }
    }

    private fun startActivityWithoutSymptoms() {
        startTestActivity<ReportedTestActivity> {
            putExtra(
                ReportedTestActivity.SELF_REPORT_QUESTIONS_DATA_KEY, selfReportTestQuestions.copy(hadSymptoms = false, symptomsOnsetDate = null)
            )
        }
    }

    private fun startActivityWithoutSymptomsAnswer() {
        startTestActivity<ReportedTestActivity> {
            putExtra(
                ReportedTestActivity.SELF_REPORT_QUESTIONS_DATA_KEY, selfReportTestQuestions.copy(hadSymptoms = null, symptomsOnsetDate = null)
            )
        }
    }

    private fun startActivityWithSelectionFromPrevious(reportedTest: Boolean) {
        startTestActivity<ReportedTestActivity> {
            putExtra(
                ReportedTestActivity.SELF_REPORT_QUESTIONS_DATA_KEY, selfReportTestQuestions.copy(hasReportedResult = reportedTest)
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
        true,
        ChosenDate(false, LocalDate.now(testAppContext.clock)),
        true,
        ChosenDate(false, LocalDate.now(testAppContext.clock)),
        null
    )
}
