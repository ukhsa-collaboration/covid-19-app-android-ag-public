package uk.nhs.nhsx.covid19.android.app.exposure

import com.google.android.gms.common.api.ApiException
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.notifications.ExposureNotificationReminderAlarmController
import javax.inject.Inject

class ExposureNotificationManager @Inject constructor(
    private val exposureNotificationApi: ExposureNotificationApi,
    private val exposureNotificationReminderAlarmController: ExposureNotificationReminderAlarmController,
) {

    suspend fun startExposureNotifications(): ExposureNotificationActivationResult =
        try {
            val enabled = exposureNotificationApi.isEnabled()
            Timber.d("exposureNotificationClient isOn = $enabled")
            exposureNotificationApi.start()
            exposureNotificationReminderAlarmController.cancel()
            ExposureNotificationActivationResult.Success
        } catch (apiException: ApiException) {
            if (apiException.status.hasResolution()) {
                ExposureNotificationActivationResult.ResolutionRequired(apiException.status)
            } else {
                ExposureNotificationActivationResult.Error(apiException)
            }
        } catch (exception: Exception) {
            ExposureNotificationActivationResult.Error(exception)
        }

    suspend fun stopExposureNotifications() {
        exposureNotificationApi.stop()
    }

    suspend fun isEnabled(): Boolean = exposureNotificationApi.isEnabled()
}
