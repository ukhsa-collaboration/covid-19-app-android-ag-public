package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result.Success
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import java.time.Instant
import kotlin.test.assertTrue

class ShareKeysInformationActivityTest : EspressoTest() {
    private val shareKeysInformationRobot = ShareKeysInformationRobot()
    private val shareResultRobot = ShareKeysResultRobot()
    private val statusRobot = StatusRobot()

    private val shareKeysTestHelper = ShareKeysTestHelper(testAppContext)

    @Before
    fun setUp() = runBlocking {
        testAppContext.getExposureNotificationApi().temporaryExposureKeyHistoryResult = Success()
        testAppContext.getExposureNotificationApi().activationResult = Success()
        testAppContext.getExposureNotificationApi().setEnabled(true)
        testAppContext.getKeySharingInfoProvider().keySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = "token",
            acknowledgedDate = Instant.parse("2020-07-10T01:00:00.00Z")
        )
        testAppContext.clock.currentInstant = Instant.parse("2020-07-11T01:00:00.00Z")
    }

    @Test
    fun showScreen() {
        startTestActivity<ShareKeysInformationActivity>()
        shareKeysInformationRobot.checkActivityIsDisplayed()
    }

    @Test
    fun clickShareKeysButton_whenExposureKeyHistorySuccess_shouldShowResultScreen_thenStatusScreen() {
        startTestActivity<ShareKeysInformationActivity>()

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickContinueButton()

        waitFor { shareResultRobot.checkActivityIsDisplayed() }

        shareResultRobot.clickActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun clickShareKeysButton_whenExpKeyHistoryResolutionRequiredAndSuccessful_shouldStartENs_whenResolutionRequiredAndSuccessful_shouldShowStatusActivity() {
        shareKeysTestHelper.whenExposureNotificationResolutionRequired(true)
        shareKeysTestHelper.whenTemporaryExposureKeyHistoryResolutionRequired(true)

        startTestActivity<ShareKeysInformationActivity>()

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickContinueButton()

        waitFor { shareResultRobot.checkActivityIsDisplayed() }

        shareResultRobot.clickActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun clickShareKeysButton_whenExposureKeyHistoryErrorWithoutDeveloperError_shouldFinish() {
        shareKeysTestHelper.whenExposureKeyHistoryErrorWithoutDeveloperError()

        val activity = startTestActivity<ShareKeysInformationActivity>()

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickContinueButton()

        waitFor { assertTrue(activity!!.isDestroyed) }
    }

    @Test
    fun clickShareKeysButton_whenExposureKeyHistoryErrorWithDeveloperError_shouldStartExposureNotifications_whenActivationSuccessful_shouldShowStatusActivity() {
        shareKeysTestHelper.whenExposureNotificationsInitiallyDisabled()

        startTestActivity<ShareKeysInformationActivity>()

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickContinueButton()

        waitFor { shareResultRobot.checkActivityIsDisplayed() }

        shareResultRobot.clickActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun clickShareKeysButton_whenExposureKeyHistoryErrorWithDeveloperError_shouldStartENs_whenResolutionRequiredAndSuccessful_shouldStatusActivity() {
        shareKeysTestHelper.whenExposureNotificationsInitiallyDisabled()
        shareKeysTestHelper.whenExposureNotificationResolutionRequired(successful = true)

        startTestActivity<ShareKeysInformationActivity>()

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickContinueButton()

        waitFor { shareResultRobot.checkActivityIsDisplayed() }

        shareResultRobot.clickActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun clickShareKeysButton_whenExposureKeyHistoryErrorWithDeveloperError_shouldStartENs_whenResolutionRequiredAndNotSuccessful_shouldDoNothing() {
        shareKeysTestHelper.whenExposureNotificationsInitiallyDisabled()
        shareKeysTestHelper.whenExposureNotificationResolutionRequired(successful = false)

        startTestActivity<ShareKeysInformationActivity>()

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickContinueButton()

        shareKeysInformationRobot.checkActivityIsDisplayed()
    }

    @Test
    fun clickShareKeysButton_whenExpKeyHistoryErrorWithDevError_shouldStartENs_whenResolutionRequiredThenResolutionRequiredThenSuccessful_shouldStatusActivity() {
        shareKeysTestHelper.whenExposureNotificationsInitiallyDisabled()
        shareKeysTestHelper.whenResolutionRequiredThenResolutionRequiredThenSuccessful()

        startTestActivity<ShareKeysInformationActivity>()

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickContinueButton()

        waitFor { shareResultRobot.checkActivityIsDisplayed() }

        shareResultRobot.clickActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun clickShareKeysButton_whenExposureKeyHistoryDenied_thenShowStatusActivity() {
        shareKeysTestHelper.whenExposureKeyHistoryDenied()

        startTestActivity<ShareKeysInformationActivity>()

        shareKeysInformationRobot.clickContinueButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun pressBack_doNothing() {
        val activity = startTestActivity<ShareKeysInformationActivity>()

        testAppContext.device.pressBack()

        waitFor { Assert.assertFalse(activity!!.isDestroyed) }
    }
}
