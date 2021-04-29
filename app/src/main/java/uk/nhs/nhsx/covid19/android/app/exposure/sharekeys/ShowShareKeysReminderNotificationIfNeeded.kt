package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.CanShareKeys.CanShareKeysResult.NoKeySharingPossible
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.CanShareKeys.CanShareKeysResult.KeySharingPossible
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

class ShowShareKeysReminderNotificationIfNeeded @Inject constructor(
    private val notificationProvider: NotificationProvider,
    private val keySharingInfoProvider: KeySharingInfoProvider,
    private val canShareKeys: CanShareKeys,
    private val clock: Clock
) {
    operator fun invoke() {
        when (val canShareKeysResult = canShareKeys()) {
            NoKeySharingPossible -> return
            is KeySharingPossible -> showNotificationIfNecessary(canShareKeysResult.keySharingInfo)
        }
    }

    private fun showNotificationIfNecessary(keySharingInfo: KeySharingInfo) {
        if (keySharingInfo.wasAcknowledgedMoreThan24HoursAgo(clock) && keySharingInfo.notificationSentDate == null) {
            keySharingInfoProvider.setNotificationSentDate(Instant.now(clock))
            notificationProvider.showShareKeysReminderNotification()
        }
    }
}
