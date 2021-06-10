package uk.nhs.nhsx.covid19.android.app.state

import uk.nhs.nhsx.covid19.android.app.notifications.ExposureNotificationRetryAlarmController
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.ShouldShowEncounterDetectionActivityProvider
import javax.inject.Inject

class ExposureNotificationHandler @Inject constructor(
    private val shouldShowEncounterDetectionActivityProvider: ShouldShowEncounterDetectionActivityProvider,
    private val notificationProvider: NotificationProvider,
    private val exposureNotificationRetryAlarmController: ExposureNotificationRetryAlarmController
) {

    fun show() {
        shouldShowEncounterDetectionActivityProvider.value = true
        notificationProvider.showExposureNotification()
        exposureNotificationRetryAlarmController.setupNextAlarm()
    }

    fun cancel() {
        shouldShowEncounterDetectionActivityProvider.value = null
        exposureNotificationRetryAlarmController.cancel()
    }
}
