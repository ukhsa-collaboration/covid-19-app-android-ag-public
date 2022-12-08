package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.LANDSCAPE
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.PORTRAIT
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.AppWillNotNotifyOtherUsersRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelectTestDateRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelfReportShareKeysInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestKitTypeRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestOriginRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.setScreenOrientation

class TestKitTypeActivityTest : EspressoTest() {
    private val testKitTypeRobot = TestKitTypeRobot()
    private val selfReportShareKeysInformationRobot = SelfReportShareKeysInformationRobot()
    private val testOriginRobot = TestOriginRobot()
    private val selectTestDateRobot = SelectTestDateRobot()
    private val appWillNotNotifyOtherUsersRobot = AppWillNotNotifyOtherUsersRobot()

    @Test
    fun showErrorStateWhenNoTestTypeIsSelectedAndContinueIsClicked() {
        startActivityWithExtras()

        testKitTypeRobot.checkActivityIsDisplayed()

        testKitTypeRobot.checkNothingIsSelected()
        testKitTypeRobot.checkErrorIsVisible(false)

        testKitTypeRobot.clickContinueButton()

        testKitTypeRobot.checkErrorIsVisible(true)
    }

    @Test
    fun testTypeSelected_choiceSurvivesRotation() {
        startActivityWithExtras()

        testKitTypeRobot.checkActivityIsDisplayed()

        testKitTypeRobot.checkNothingIsSelected()
        testKitTypeRobot.clickLFDButton()

        waitFor { testKitTypeRobot.checkLFDIsSelected() }
        setScreenOrientation(LANDSCAPE)
        waitFor { testKitTypeRobot.checkLFDIsSelected() }
        setScreenOrientation(PORTRAIT)
        waitFor { testKitTypeRobot.checkLFDIsSelected() }
    }

    @Test
    fun testTypeSelected_canChangeSelection() {
        startActivityWithExtras()

        testKitTypeRobot.checkActivityIsDisplayed()

        testKitTypeRobot.checkNothingIsSelected()
        testKitTypeRobot.clickPCRButton()
        waitFor { testKitTypeRobot.checkPCRIsSelected() }

        testKitTypeRobot.clickLFDButton()
        waitFor { testKitTypeRobot.checkLFDIsSelected() }
    }

    @Test
    fun pressBackWithoutExposureKeys_goToSelfReportShareKeysInformationActivity() {
        startActivityWithoutExposureKeys()

        waitFor { testKitTypeRobot.checkActivityIsDisplayed() }

        testAppContext.device.pressBack()

        waitFor { appWillNotNotifyOtherUsersRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun pressBackWithExposureKeys_goToSelfReportShareKeysInformationActivity() {
        startActivityWithExtras()

        testKitTypeRobot.checkActivityIsDisplayed()

        testAppContext.device.pressBack()

        selfReportShareKeysInformationRobot.checkActivityIsDisplayed()
    }

    @Test
    fun clickContinueWithLFDTestKitType_goToTestOriginActivity() {
        startActivityWithExtras()

        testKitTypeRobot.checkActivityIsDisplayed()

        testKitTypeRobot.clickLFDButton()
        testKitTypeRobot.clickContinueButton()

        testOriginRobot.checkActivityIsDisplayed()
    }

    @Test
    fun clickContinueWithPCRTestKitType_goToTestDateActivity() {
        startActivityWithExtras()

        testKitTypeRobot.checkActivityIsDisplayed()

        testKitTypeRobot.clickPCRButton()
        testKitTypeRobot.clickContinueButton()

        selectTestDateRobot.checkActivityIsDisplayed()
    }

    @Test
    fun startActivityWithSelectionFromPrevious_selectionShouldBeShown() {
        startActivityWithTestKitTypeAlreadyChosen()

        testKitTypeRobot.checkActivityIsDisplayed()

        testKitTypeRobot.checkPCRIsSelected()
    }

    private fun startActivityWithExtras() {
        startTestActivity<TestKitTypeActivity> {
            putExtra(
                TestKitTypeActivity.SELF_REPORT_QUESTIONS_DATA_KEY, selfReportTestQuestions
            )
        }
    }

    private fun startActivityWithoutExposureKeys() {
        startTestActivity<TestKitTypeActivity> {
            putExtra(
                TestKitTypeActivity.SELF_REPORT_QUESTIONS_DATA_KEY, selfReportTestQuestionsWithoutExposureKeys
            )
        }
    }

    private fun startActivityWithTestKitTypeAlreadyChosen() {
        startTestActivity<TestKitTypeActivity> {
            putExtra(
                TestKitTypeActivity.SELF_REPORT_QUESTIONS_DATA_KEY, selfReportTestQuestions.copy(testKitType = LAB_RESULT)
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
        null,
        null,
        null,
        null,
        null,
        null
    )

    private val selfReportTestQuestionsWithoutExposureKeys = SelfReportTestQuestions(
        POSITIVE,
        null,
        null,
        null,
        null,
        null,
        null,
        null
    )
}
