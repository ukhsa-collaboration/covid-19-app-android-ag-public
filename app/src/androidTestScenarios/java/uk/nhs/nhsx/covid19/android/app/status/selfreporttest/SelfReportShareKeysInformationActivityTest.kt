package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result.Success
import uk.nhs.nhsx.covid19.android.app.exposure.executeWithTheUserDecliningExposureKeySharing
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.AppWillNotNotifyOtherUsersRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelfReportShareKeysInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestKitTypeRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestTypeRobot

class SelfReportShareKeysInformationActivityTest : EspressoTest() {
    private val selfReportShareKeysInformationRobot = SelfReportShareKeysInformationRobot()
    private val testTypeRobot = TestTypeRobot()
    private val testKitTypeRobot = TestKitTypeRobot()
    private val appWillNotNotifyOtherUsersRobot = AppWillNotNotifyOtherUsersRobot()

    @Before
    fun setUp() = runBlocking {
        testAppContext.getExposureNotificationApi().temporaryExposureKeyHistoryResult = Success()
        testAppContext.getExposureNotificationApi().setEnabled(true)
    }

    @Test
    fun showScreen() {
        startActivityWithExtras()
        selfReportShareKeysInformationRobot.checkActivityIsDisplayed()
    }

    @Test
    fun clickContinueButton_whenExposureKeyHistorySuccess_shouldNavigateToTestKitTypeActivity() {
        startActivityWithExtras()

        selfReportShareKeysInformationRobot.checkActivityIsDisplayed()

        selfReportShareKeysInformationRobot.clickContinueButton()

        waitFor { testKitTypeRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun clickContinueButton_whenExposureKeySharingIsDeclined_shouldNavigateToAppWillNotNotifyOtherUsersActivity() {
        startActivityWithExtras()

        selfReportShareKeysInformationRobot.checkActivityIsDisplayed()

        testAppContext.executeWithTheUserDecliningExposureKeySharing {
            selfReportShareKeysInformationRobot.clickContinueButton()

            waitFor { appWillNotNotifyOtherUsersRobot.checkActivityIsDisplayed() }
        }
    }

    @Test
    fun pressBack_goToTestTypeActivity() {
        startActivityWithExtras()

        testAppContext.device.pressBack()

        testTypeRobot.checkActivityIsDisplayed()
    }

    private fun startActivityWithExtras() {
        startTestActivity<SelfReportShareKeysInformationActivity> {
            putExtra(
                SelfReportShareKeysInformationActivity.SELF_REPORT_QUESTIONS_DATA_KEY, SelfReportTestQuestions(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                )
            )
        }
    }
}
