package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result.Success
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysReminderRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import java.time.Instant
import kotlin.test.assertTrue

class ShareKeysReminderActivityTest : EspressoTest() {
    private val shareKeysReminderRobot = ShareKeysReminderRobot()
    private val shareKeysResultRobot = ShareKeysResultRobot()
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
    fun showScreen() = notReported {
        startTestActivity<ShareKeysReminderActivity>()
        shareKeysReminderRobot.checkActivityIsDisplayed()
    }

    @Test
    fun clickShareKeysButton_whenExposureKeyHistorySuccess_shouldShowResultScreen_thenStatusScreen() =
        notReported {
            startTestActivity<ShareKeysReminderActivity>()

            shareKeysReminderRobot.checkActivityIsDisplayed()

            shareKeysReminderRobot.clickShareResultsButton()

            waitFor { shareKeysResultRobot.checkActivityIsDisplayed() }

            shareKeysResultRobot.clickActionButton()

            waitFor { statusRobot.checkActivityIsDisplayed() }
        }

    @Test
    fun clickShareKeysButton_whenExposureKeyHistoryErrorWithDeveloperError_shouldStartExposureNotifications_whenActivationSuccessful_shouldShowStatusActivity() =
        notReported {
            shareKeysTestHelper.whenExposureNotificationsInitiallyDisabled()

            startTestActivity<ShareKeysReminderActivity>()

            shareKeysReminderRobot.checkActivityIsDisplayed()

            shareKeysReminderRobot.clickShareResultsButton()

            waitFor { shareKeysResultRobot.checkActivityIsDisplayed() }

            shareKeysResultRobot.clickActionButton()

            waitFor { statusRobot.checkActivityIsDisplayed() }
        }

    @Test
    fun clickShareKeysButton_whenExposureKeyHistoryErrorWithDeveloperError_shouldStartENs_whenResolutionRequiredAndSuccessful_shouldStatusActivity() =
        notReported {
            shareKeysTestHelper.whenExposureNotificationsInitiallyDisabled()
            shareKeysTestHelper.whenExposureNotificationResolutionRequired(successful = true)

            startTestActivity<ShareKeysReminderActivity>()

            shareKeysReminderRobot.checkActivityIsDisplayed()

            shareKeysReminderRobot.clickShareResultsButton()

            waitFor { shareKeysResultRobot.checkActivityIsDisplayed() }

            shareKeysResultRobot.clickActionButton()

            waitFor { statusRobot.checkActivityIsDisplayed() }
        }

    @Test
    fun clickShareKeysButton_whenExpKeyHistoryErrorWithDeveloperError_shouldStartENs_whenResolutionRequiredAndNotSuccessful_shouldDoNothing() =
        notReported {
            shareKeysTestHelper.whenExposureNotificationsInitiallyDisabled()
            shareKeysTestHelper.whenExposureNotificationResolutionRequired(successful = false)

            startTestActivity<ShareKeysReminderActivity>()

            shareKeysReminderRobot.checkActivityIsDisplayed()

            shareKeysReminderRobot.clickShareResultsButton()

            shareKeysReminderRobot.checkActivityIsDisplayed()
        }

    @Test
    fun clickShareKeysButton_whenExpKeyHistoryErrorWithDevError_shouldStartENs_whenResolutionRequiredThenResolutionRequiredThenSuccessful_shouldStatusActivity() =
        notReported {
            shareKeysTestHelper.whenExposureNotificationsInitiallyDisabled()
            shareKeysTestHelper.whenResolutionRequiredThenResolutionRequiredThenSuccessful()

            startTestActivity<ShareKeysReminderActivity>()

            shareKeysReminderRobot.checkActivityIsDisplayed()

            shareKeysReminderRobot.clickShareResultsButton()

            waitFor { shareKeysResultRobot.checkActivityIsDisplayed() }

            shareKeysResultRobot.clickActionButton()

            waitFor { statusRobot.checkActivityIsDisplayed() }
        }

    @Test
    fun clickShareKeysButton_whenExposureKeyHistoryErrorWithoutDeveloperError_shouldFinish() = notReported {
        shareKeysTestHelper.whenExposureKeyHistoryErrorWithoutDeveloperError()

        val activity = startTestActivity<ShareKeysReminderActivity>()

        shareKeysReminderRobot.checkActivityIsDisplayed()

        shareKeysReminderRobot.clickShareResultsButton()

        waitFor { assertTrue(activity!!.isDestroyed) }
    }

    @Test
    fun clickShareKeysButton_whenExpKeyHistoryResolutionRequiredAndSuccessful_shouldStartENs_whenResolutionRequiredAndSuccessful_shouldShowStatusActivity() =
        notReported {
            shareKeysTestHelper.whenExposureNotificationResolutionRequired(true)
            shareKeysTestHelper.whenTemporaryExposureKeyHistoryResolutionRequired(true)

            startTestActivity<ShareKeysReminderActivity>()

            shareKeysReminderRobot.checkActivityIsDisplayed()

            shareKeysReminderRobot.clickShareResultsButton()

            waitFor { shareKeysResultRobot.checkActivityIsDisplayed() }

            shareKeysResultRobot.clickActionButton()

            waitFor { statusRobot.checkActivityIsDisplayed() }
        }
}
