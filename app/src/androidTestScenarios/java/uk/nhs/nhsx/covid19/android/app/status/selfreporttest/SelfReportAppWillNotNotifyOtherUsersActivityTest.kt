package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.executeWithTheUserDecliningExposureKeySharing
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.AppWillNotNotifyOtherUsersRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelfReportShareKeysInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestKitTypeRobot

class SelfReportAppWillNotNotifyOtherUsersActivityTest : EspressoTest() {

    private val appWillNotNotifyOtherUsersRobot = AppWillNotNotifyOtherUsersRobot()
    private val selfReportShareKeysInformationRobot = SelfReportShareKeysInformationRobot()
    private val testKitTypeRobot = TestKitTypeRobot()

    @Test
    fun showThisActivityWhenDecliningExposureKeySharing() {
        startSelfReportShareKeysInformationWithPositiveTest()

        waitFor { selfReportShareKeysInformationRobot.checkActivityIsDisplayed() }

        testAppContext.executeWithTheUserDecliningExposureKeySharing {

            selfReportShareKeysInformationRobot.clickContinueButton()

            waitFor { appWillNotNotifyOtherUsersRobot.checkActivityIsDisplayed() }
        }
    }

    @Test
    fun pressBack_goToSelfReportShareKeysInformationActivity() {
        startActivityWithExtras()

        waitFor { appWillNotNotifyOtherUsersRobot.checkActivityIsDisplayed() }

        testAppContext.device.pressBack()

        waitFor { selfReportShareKeysInformationRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun pressContinue_goToTestKitTypeActivity() {
        startActivityWithExtras()

        waitFor { appWillNotNotifyOtherUsersRobot.checkActivityIsDisplayed() }

        appWillNotNotifyOtherUsersRobot.clickContinue()

        waitFor { testKitTypeRobot.checkActivityIsDisplayed() }
    }

    private fun startSelfReportShareKeysInformationWithPositiveTest() {
        startTestActivity<SelfReportShareKeysInformationActivity> {
            putExtra(
                SelfReportShareKeysInformationActivity.SELF_REPORT_QUESTIONS_DATA_KEY, selfReportTestQuestions
            )
        }
    }

    private fun startActivityWithExtras() {
        startTestActivity<SelfReportAppWillNotNotifyOtherUsersActivity> {
            putExtra(
                SelfReportAppWillNotNotifyOtherUsersActivity.SELF_REPORT_QUESTIONS_DATA_KEY, selfReportTestQuestions
            )
        }
    }

    private val selfReportTestQuestions = SelfReportTestQuestions(
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
