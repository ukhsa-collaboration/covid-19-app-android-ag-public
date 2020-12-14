package uk.nhs.nhsx.covid19.android.app.exposure

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result.Error
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result.ResolutionRequired
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result.Success
import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext

private var nextRequestCode = 1

fun TestApplicationContext.setExposureNotificationResolutionRequired(context: Context, successful: Boolean) {
    val pendingIntent = createExposureNotificationResolutionPendingIntent(context, successful)
    getExposureNotificationApi().activationResult = ResolutionRequired(pendingIntent, if (successful) Success() else Error())
}

fun TestApplicationContext.setTemporaryExposureKeyHistoryResolutionRequired(context: Context, successful: Boolean) {
    val pendingIntent = createExposureNotificationResolutionPendingIntent(context, successful)
    getExposureNotificationApi().temporaryExposureKeyHistoryResult = ResolutionRequired(pendingIntent, if (successful) Success() else Error())
}

fun createExposureNotificationResolutionPendingIntent(context: Context, successful: Boolean): PendingIntent {
    val intent = Intent(context, MockExposureNotificationActivationActivity::class.java).apply {
        putExtra(
            MockExposureNotificationActivationActivity.EXPOSURE_NOTIFICATION_ACTIVATION_RESULT_EXTRA,
            if (successful) Activity.RESULT_OK else Activity.RESULT_CANCELED
        )
    }

    return PendingIntent.getActivity(
        context,
        nextRequestCode++,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT
    )
}
