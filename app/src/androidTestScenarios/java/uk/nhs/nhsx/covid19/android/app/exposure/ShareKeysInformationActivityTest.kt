package uk.nhs.nhsx.covid19.android.app.exposure

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.Status
import java.time.Instant
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result.Error
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result.ResolutionRequired
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result.Success
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult

class ShareKeysInformationActivityTest : EspressoTest() {

    private val shareKeysInformationRobot = ShareKeysInformationRobot()
    private val statusRobot = StatusRobot()

    private val positiveTestResult = ReceivedTestResult(
        diagnosisKeySubmissionToken = "token1",
        testEndDate = Instant.now(),
        testResult = POSITIVE,
        testKitType = LAB_RESULT,
        diagnosisKeySubmissionSupported = true
    )

    @Before
    fun setUp() {
        testAppContext.getExposureNotificationApi().temporaryExposureKeyHistoryResult = Success()
        testAppContext.getExposureNotificationApi().activationResult = Success()
        testAppContext.getExposureNotificationApi().setEnabled(true)
    }

    @Test
    fun showScreen() = notReported {
        startTestActivity<ShareKeysInformationActivity> {
            putExtra("EXTRA_TEST_RESULT", positiveTestResult)
        }

        shareKeysInformationRobot.checkActivityIsDisplayed()
    }

    @Test
    fun clickContinueButton_whenExposureKeyHistorySuccess_shouldShowStatusActivity() = notReported {
        testAppContext.getExposureNotificationApi().temporaryExposureKeyHistoryResult = Success()

        startTestActivity<ShareKeysInformationActivity> {
            putExtra("EXTRA_TEST_RESULT", positiveTestResult)
        }

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickIUnderstandButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun clickContinueButton_whenExposureKeyHistoryResolutionRequiredAndNotSuccessful_shouldShowStatusActivity() = notReported {
        testAppContext.setTemporaryExposureKeyHistoryResolutionRequired(testAppContext.app, false)

        startTestActivity<ShareKeysInformationActivity> {
            putExtra("EXTRA_TEST_RESULT", positiveTestResult)
        }

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickIUnderstandButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun clickContinueButton_whenExposureKeyHistoryResolutionRequiredAndSuccessful_shouldStartExposureNotifications_whenSuccessful_shouldShowStatusActivity() = notReported {
        testAppContext.setTemporaryExposureKeyHistoryResolutionRequired(testAppContext.app, true)

        startTestActivity<ShareKeysInformationActivity> {
            putExtra("EXTRA_TEST_RESULT", positiveTestResult)
        }

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickIUnderstandButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun clickContinueButton_whenExposureKeyHistoryResolutionRequiredAndSuccessful_shouldStartExposureNotifications_whenResolutionRequiredAndSuccessful_shouldShowStatusActivity() = notReported {
        testAppContext.setTemporaryExposureKeyHistoryResolutionRequired(testAppContext.app, true)
        testAppContext.setExposureNotificationResolutionRequired(testAppContext.app, true)

        startTestActivity<ShareKeysInformationActivity> {
            putExtra("EXTRA_TEST_RESULT", positiveTestResult)
        }

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickIUnderstandButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun clickContinueButton_whenExposureKeyHistoryErrorWithoutDeveloperError_shouldFinish() = notReported {
        testAppContext.getExposureNotificationApi().temporaryExposureKeyHistoryResult = Error()
        testAppContext.getExposureNotificationApi().setEnabled(true)

        val activity = startTestActivity<ShareKeysInformationActivity> {
            putExtra("EXTRA_TEST_RESULT", positiveTestResult)
        }

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickIUnderstandButton()

        waitFor { assertTrue(activity!!.isDestroyed) }
    }

    @Test
    fun clickContinueButton_whenExposureKeyHistoryErrorWithDeveloperError_shouldStartExposureNotifications_whenActivationSuccessful_shouldShowStatusActivity() = notReported {
        testAppContext.getExposureNotificationApi().temporaryExposureKeyHistoryResult = Error(Status(ConnectionResult.DEVELOPER_ERROR), Success())
        testAppContext.getExposureNotificationApi().setEnabled(false)
        testAppContext.getExposureNotificationApi().activationResult = Success()

        startTestActivity<ShareKeysInformationActivity> {
            putExtra("EXTRA_TEST_RESULT", positiveTestResult)
        }

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickIUnderstandButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun clickContinueButton_whenExposureKeyHistoryErrorWithDeveloperError_shouldStartExposureNotifications_whenResolutionRequiredAndSuccessful_shouldStatusActivity() = notReported {
        testAppContext.getExposureNotificationApi().temporaryExposureKeyHistoryResult = Error(Status(ConnectionResult.DEVELOPER_ERROR), Success())
        testAppContext.getExposureNotificationApi().setEnabled(false)
        testAppContext.setExposureNotificationResolutionRequired(testAppContext.app, true)

        startTestActivity<ShareKeysInformationActivity> {
            putExtra("EXTRA_TEST_RESULT", positiveTestResult)
        }

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickIUnderstandButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun clickContinueButton_whenExposureKeyHistoryErrorWithDeveloperError_shouldStartExposureNotifications_whenResolutionRequiredAndNotSuccessful_shouldDoNothing() = notReported {
        testAppContext.getExposureNotificationApi().temporaryExposureKeyHistoryResult = Error(Status(ConnectionResult.DEVELOPER_ERROR), Success())
        testAppContext.getExposureNotificationApi().setEnabled(false)
        testAppContext.setExposureNotificationResolutionRequired(testAppContext.app, false)

        startTestActivity<ShareKeysInformationActivity> {
            putExtra("EXTRA_TEST_RESULT", positiveTestResult)
        }

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickIUnderstandButton()

        shareKeysInformationRobot.checkActivityIsDisplayed()
    }

    @Test
    fun clickContinueButton_whenExposureKeyHistoryErrorWithDeveloperError_shouldStartExposureNotifications_whenResolutionRequiredAndAgainResolutionRequiredAndSuccessful_shouldStatusActivity() = notReported {
        testAppContext.getExposureNotificationApi().temporaryExposureKeyHistoryResult = Error(Status(ConnectionResult.DEVELOPER_ERROR), Success())
        testAppContext.getExposureNotificationApi().setEnabled(false)
        val resolutionIntent1 = createExposureNotificationResolutionPendingIntent(testAppContext.app, true)
        val resolutionIntent2 = createExposureNotificationResolutionPendingIntent(testAppContext.app, true)
        testAppContext.getExposureNotificationApi().activationResult =
            ResolutionRequired(resolutionIntent1, ResolutionRequired(resolutionIntent2, Success()))

        startTestActivity<ShareKeysInformationActivity> {
            putExtra("EXTRA_TEST_RESULT", positiveTestResult)
        }

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickIUnderstandButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }
}
