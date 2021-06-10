package uk.nhs.nhsx.covid19.android.app.status

import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidSendLocalInfoNotification
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessagesResponse
import javax.inject.Inject

class ShowLocalMessageNotificationIfNeeded @Inject constructor(
    private val notificationProvider: NotificationProvider,
    private val getFirstMessageOfTypeNotification: GetFirstMessageOfTypeNotification,
    private val analyticsEventProcessor: AnalyticsEventProcessor,
) {
    suspend operator fun invoke(previousMessage: LocalMessagesResponse?, receivedMessage: LocalMessagesResponse) {
        val receivedNotification = getFirstMessageOfTypeNotification(receivedMessage) ?: return
        val previousNotification = getFirstMessageOfTypeNotification(previousMessage)

        val shouldShowNotification = receivedNotification.hasContentChangedSince(previousNotification)

        if (!shouldShowNotification) return

        val notificationMessage = receivedNotification.message.translations.translateOrNull()

        if (notificationMessage == null) {
            Timber.d("Notification message could not be translated")
            return
        }

        with(notificationMessage) {
            if (head != null && body != null) {
                notificationProvider.showLocalMessageNotification(title = head, message = body)
            }
        }
        analyticsEventProcessor.track(DidSendLocalInfoNotification)
    }

    private fun MessageWithId.hasContentChangedSince(previousMessageWithId: MessageWithId?): Boolean =
        this.messageId != previousMessageWithId?.messageId ||
            this.message.contentVersion != previousMessageWithId.message.contentVersion
}
