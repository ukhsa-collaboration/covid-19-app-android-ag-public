package uk.nhs.nhsx.covid19.android.app.state

import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowEncounterDetection
import uk.nhs.nhsx.covid19.android.app.notifications.ExposureNotificationRetryAlarmController
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.UserInbox
import javax.inject.Inject

class ExposureNotificationHandler @Inject constructor(
    private val userInbox: UserInbox,
    private val notificationProvider: NotificationProvider,
    private val exposureNotificationRetryAlarmController: ExposureNotificationRetryAlarmController
) {

    fun show() {
        userInbox.addUserInboxItem(ShowEncounterDetection)
        notificationProvider.showExposureNotification()
        exposureNotificationRetryAlarmController.setupNextAlarm()
    }

    fun cancel() {
        userInbox.clearItem(ShowEncounterDetection)
        exposureNotificationRetryAlarmController.cancel()
    }
}
