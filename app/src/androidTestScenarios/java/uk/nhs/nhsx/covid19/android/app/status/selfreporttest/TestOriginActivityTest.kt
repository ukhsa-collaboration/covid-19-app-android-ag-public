package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.LANDSCAPE
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.PORTRAIT
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelectTestDateRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestKitTypeRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestOriginRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.setScreenOrientation

class TestOriginActivityTest : EspressoTest() {
    private val testOriginRobot = TestOriginRobot()
    private val testKitTypeRobot = TestKitTypeRobot()
    private val selectTestDateRobot = SelectTestDateRobot()

    @Test
    fun showErrorStateWhenNoTestOriginIsSelectedAndContinueIsClicked() {
        startActivityWithExtras()

        testOriginRobot.checkActivityIsDisplayed()

        testOriginRobot.checkNothingIsSelected()
        testOriginRobot.checkErrorIsVisible(false)

        testOriginRobot.clickContinueButton()

        waitFor { testOriginRobot.checkErrorIsVisible(true) }
    }

    @Test
    fun testOriginSelected_choiceSurvivesRotation() {
        startActivityWithExtras()

        testOriginRobot.checkActivityIsDisplayed()

        testOriginRobot.checkNothingIsSelected()
        testOriginRobot.clickYesButton()

        waitFor { testOriginRobot.checkYesIsSelected() }
        setScreenOrientation(LANDSCAPE)
        waitFor { testOriginRobot.checkYesIsSelected() }
        setScreenOrientation(PORTRAIT)
        waitFor { testOriginRobot.checkYesIsSelected() }
    }

    @Test
    fun testOriginSelected_canChangeSelection() {
        startActivityWithExtras()

        testOriginRobot.checkActivityIsDisplayed()

        testOriginRobot.checkNothingIsSelected()
        testOriginRobot.clickNoButton()
        waitFor { testOriginRobot.checkNoIsSelected() }

        testOriginRobot.clickYesButton()
        waitFor { testOriginRobot.checkYesIsSelected() }
    }

    @Test
    fun pressBack_goToTestKitTypeActivity() {
        startActivityWithExtras()

        testOriginRobot.checkActivityIsDisplayed()

        testAppContext.device.pressBack()

        waitFor { testKitTypeRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun pressContinue_goToTestDateActivity() {
        startActivityWithExtras()

        testOriginRobot.checkActivityIsDisplayed()

        testOriginRobot.clickYesButton()
        testOriginRobot.clickContinueButton()

        waitFor { selectTestDateRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun startActivityWithSelectionFromPrevious_selectionShouldBeShown() {
        startActivityWithTestOriginAlreadyChosen()

        testOriginRobot.checkActivityIsDisplayed()

        waitFor { testOriginRobot.checkYesIsSelected() }
    }

    private fun startActivityWithExtras() {
        startTestActivity<TestOriginActivity> {
            putExtra(
                TestOriginActivity.SELF_REPORT_QUESTIONS_DATA_KEY, selfReportTestQuestions
            )
        }
    }

    private fun startActivityWithTestOriginAlreadyChosen() {
        startTestActivity<TestOriginActivity> {
            putExtra(
                TestOriginActivity.SELF_REPORT_QUESTIONS_DATA_KEY, selfReportTestQuestions.copy(isNHSTest = true)
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
        null,
        null,
        null,
        null,
        null
    )
}
