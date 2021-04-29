package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.Status
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result.Error
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result.ResolutionRequired
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result.Success
import uk.nhs.nhsx.covid19.android.app.exposure.createExposureNotificationResolutionPendingIntent
import uk.nhs.nhsx.covid19.android.app.exposure.setExposureNotificationResolutionRequired
import uk.nhs.nhsx.covid19.android.app.exposure.setTemporaryExposureKeyHistoryResolutionRequired
import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext

class ShareKeysTestHelper(private val testAppContext: TestApplicationContext) {

    fun whenExposureNotificationsInitiallyDisabled() {
        testAppContext.getExposureNotificationApi().temporaryExposureKeyHistoryResult = Error(
            Status(ConnectionResult.DEVELOPER_ERROR),
            Success()
        )
        testAppContext.getExposureNotificationApi().setEnabled(false)
    }

    fun whenExposureKeyHistoryErrorWithoutDeveloperError() {
        testAppContext.getExposureNotificationApi().temporaryExposureKeyHistoryResult = Error()
    }

    fun whenExposureNotificationResolutionRequired(successful: Boolean) {
        testAppContext.setExposureNotificationResolutionRequired(testAppContext.app, successful)
    }

    fun whenTemporaryExposureKeyHistoryResolutionRequired(successful: Boolean) {
        testAppContext.setTemporaryExposureKeyHistoryResolutionRequired(testAppContext.app, successful)
    }

    fun whenResolutionRequiredThenResolutionRequiredThenSuccessful() {
        val resolutionIntent1 = createExposureNotificationResolutionPendingIntent(testAppContext.app, successful = true)
        val resolutionIntent2 = createExposureNotificationResolutionPendingIntent(testAppContext.app, successful = true)
        testAppContext.getExposureNotificationApi().activationResult =
            ResolutionRequired(resolutionIntent1, ResolutionRequired(resolutionIntent2, Success()))
    }

    fun whenExposureKeyHistoryDenied() {
        val resolutionIntent = createExposureNotificationResolutionPendingIntent(testAppContext.app, successful = false)
        testAppContext.getExposureNotificationApi().temporaryExposureKeyHistoryResult =
            ResolutionRequired(resolutionIntent, Error())
    }
}
