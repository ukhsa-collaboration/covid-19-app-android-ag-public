package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.LANDSCAPE
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.PORTRAIT
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ReportedTestRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelectTestDateRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelfReportCheckAnswersRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelfReportSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestKitTypeRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestOriginRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.setScreenOrientation
import java.time.LocalDate

class SelectTestDateActivityTest : EspressoTest() {

    private val selectTestDateRobot = SelectTestDateRobot()
    private val testOriginRobot = TestOriginRobot()
    private val testKitTypeRobot = TestKitTypeRobot()
    private val selfReportSymptomsRobot = SelfReportSymptomsRobot()
    private val reportedTestRobot = ReportedTestRobot()
    private val selfReportCheckAnswersRobot = SelfReportCheckAnswersRobot()
    private val isolationHelper = IsolationHelper(testAppContext.clock)

    @Test
    fun showErrorStateWhenNoTestDateIsSelectedAndContinueIsClicked() {
        startActivityWithExtras()

        selectTestDateRobot.checkActivityIsDisplayed()

        selectTestDateRobot.checkNoDateIsSelected()
        selectTestDateRobot.checkDoNotRememberDateIsNotChecked()
        selectTestDateRobot.checkErrorIsVisible(false)

        selectTestDateRobot.clickContinueButton()

        waitFor { selectTestDateRobot.checkErrorIsVisible(true) }
    }

    @Test
    fun cannotRememberChecked_choiceSurvivesRotation() {
        startActivityWithExtras()

        selectTestDateRobot.checkActivityIsDisplayed()

        selectTestDateRobot.checkDoNotRememberDateIsNotChecked()
        selectTestDateRobot.selectCannotRememberDate()

        waitFor { selectTestDateRobot.checkDoNotRememberDateIsChecked() }
        setScreenOrientation(LANDSCAPE)
        waitFor { selectTestDateRobot.checkDoNotRememberDateIsChecked() }
        setScreenOrientation(PORTRAIT)
        waitFor { selectTestDateRobot.checkDoNotRememberDateIsChecked() }
    }

    @Test
    fun selectDoNotRememberDate_canChangeToSpecificDate() {
        startActivityWithExtras()

        selectTestDateRobot.checkActivityIsDisplayed()

        selectTestDateRobot.checkDoNotRememberDateIsNotChecked()

        selectTestDateRobot.selectCannotRememberDate()

        selectTestDateRobot.checkDoNotRememberDateIsChecked()

        selectTestDateRobot.checkErrorIsVisible(false)

        selectTestDateRobot.clickSelectDate()

        selectTestDateRobot.selectDayOfMonth(LocalDate.now().dayOfMonth)

        selectTestDateRobot.checkDoNotRememberDateIsNotChecked()

        selectTestDateRobot.checkErrorIsVisible(false)
    }

    @Test
    fun pressBackWithLFD_goToTestOriginActivity() {
        startActivityWithExtras()

        selectTestDateRobot.checkActivityIsDisplayed()

        testAppContext.device.pressBack()

        waitFor { testOriginRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun pressBackWithPCR_goToTestKitTypeActivity() {
        startActivityWithExtrasPCRTest()

        selectTestDateRobot.checkActivityIsDisplayed()

        testAppContext.device.pressBack()

        waitFor { testKitTypeRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun pressContinue_notInIsolation_goToSymptomsActivity() {
        startActivityWithExtras()

        selectTestDateRobot.checkActivityIsDisplayed()

        selectTestDateRobot.selectCannotRememberDate()

        selectTestDateRobot.clickContinueButton()

        waitFor { selfReportSymptomsRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun pressContinueWithPCR_InIsolation_goToCheckAnswers() {
        startActivityWithExtrasPCRTest()
        testAppContext.setState(isolationHelper.selfAssessment().asIsolation())

        selectTestDateRobot.checkActivityIsDisplayed()

        selectTestDateRobot.selectCannotRememberDate()

        selectTestDateRobot.clickContinueButton()

        waitFor { selfReportCheckAnswersRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun pressContinueWithNHSLFD_InIsolation_goToReportedTest() {
        startActivityWithExtrasLFDFomNHS()
        testAppContext.setState(isolationHelper.selfAssessment().asIsolation())

        selectTestDateRobot.checkActivityIsDisplayed()

        selectTestDateRobot.selectCannotRememberDate()

        selectTestDateRobot.clickContinueButton()

        waitFor { reportedTestRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun startActivityWithCannotRememberSelectionFromPrevious_selectionShouldBeShown() {
        startActivityWithCannotRememberAlreadyChosen()

        selectTestDateRobot.checkActivityIsDisplayed()

        waitFor { selectTestDateRobot.checkDoNotRememberDateIsChecked() }
    }

    @Test
    fun startActivityWithTestDateSelectionFromPrevious_selectionShouldBeShown() {
        startActivityWithTestDateAlreadyChosen()

        selectTestDateRobot.checkActivityIsDisplayed()

        waitFor { selectTestDateRobot.checkDateIsChosen() }
    }

    private fun startActivityWithExtras() {
        startTestActivity<SelectTestDateActivity> {
            putExtra(
                SelectTestDateActivity.SELF_REPORT_QUESTIONS_DATA_KEY, selfReportTestQuestions
            )
        }
    }

    private fun startActivityWithExtrasPCRTest() {
        startTestActivity<SelectTestDateActivity> {
            putExtra(
                SelectTestDateActivity.SELF_REPORT_QUESTIONS_DATA_KEY, selfReportTestQuestions.copy(testKitType = LAB_RESULT)
            )
        }
    }

    private fun startActivityWithExtrasLFDFomNHS() {
        startTestActivity<SelectTestDateActivity> {
            putExtra(
                SelectTestDateActivity.SELF_REPORT_QUESTIONS_DATA_KEY, selfReportTestQuestions.copy(testKitType = RAPID_SELF_REPORTED, isNHSTest = true)
            )
        }
    }

    private fun startActivityWithCannotRememberAlreadyChosen() {
        startTestActivity<SelectTestDateActivity> {
            putExtra(
                SelectTestDateActivity.SELF_REPORT_QUESTIONS_DATA_KEY, selfReportTestQuestions.copy(
                    testEndDate = ChosenDate(false, LocalDate.now()))
            )
        }
    }

    private fun startActivityWithTestDateAlreadyChosen() {
        startTestActivity<SelectTestDateActivity> {
            putExtra(
                SelectTestDateActivity.SELF_REPORT_QUESTIONS_DATA_KEY, selfReportTestQuestions.copy(
                    testEndDate = ChosenDate(true, LocalDate.now()))
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
        null,
        null,
        null,
        null
    )
}
