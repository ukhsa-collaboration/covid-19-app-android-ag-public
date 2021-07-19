package uk.nhs.nhsx.covid19.android.app.status

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidSendLocalInfoNotification
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalInformation.Notification
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessagesResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.NotificationMessage
import uk.nhs.nhsx.covid19.android.app.remote.data.TranslatableNotificationMessage

class ShowLocalMessageNotificationIfNeededTest {

    private val notificationProvider = mockk<NotificationProvider>(relaxUnitFun = true)
    private val getFirstMessageOfTypeNotification = mockk<GetFirstMessageOfTypeNotification>()
    private val analyticsEventProcessor = mockk<AnalyticsEventProcessor>(relaxUnitFun = true)

    private val showLocalInformationNotificationIfNeeded =
        ShowLocalMessageNotificationIfNeeded(notificationProvider, getFirstMessageOfTypeNotification, analyticsEventProcessor)

    @Before
    fun setUp() {
        coEvery { getFirstMessageOfTypeNotification(previousResponse) } returns previousNotificationWithId
        coEvery { getFirstMessageOfTypeNotification(receivedResponse) } returns receivedNotificationWithId
        every { receivedLocalMessage.translations } returns receivedTranslations
        every { previousLocalMessage.contentVersion } returns 1
        every { receivedLocalMessage.contentVersion } returns 1
        every { receivedTranslations.translateOrNull() } returns expectedTranslation
    }

    @Test
    fun `when received message is null then do not show notification`() = runBlocking {
        coEvery { getFirstMessageOfTypeNotification(receivedResponse) } returns null

        showLocalInformationNotificationIfNeeded(previousResponse, receivedResponse)

        verify(exactly = 0) { notificationProvider.showLocalMessageNotification(any(), any()) }
        verify(exactly = 0) { analyticsEventProcessor.track(any()) }
    }

    @Test
    fun `when neither messageId nor contentVersion change then should not show notification`() = runBlocking {
        coEvery { getFirstMessageOfTypeNotification(receivedResponse) } returns receivedNotificationWithId

        showLocalInformationNotificationIfNeeded(previousResponse, receivedResponse)

        verify(exactly = 0) { notificationProvider.showLocalMessageNotification(any(), any()) }
        verify(exactly = 0) { analyticsEventProcessor.track(any()) }
    }

    @Test
    fun `when messageId changes then should show notification`() = runBlocking {
        coEvery { getFirstMessageOfTypeNotification(receivedResponse) } returns
            receivedNotificationWithId.copy(messageId = "message2")

        showLocalInformationNotificationIfNeeded(previousResponse, receivedResponse)

        with(expectedTranslation) {
            verify { notificationProvider.showLocalMessageNotification(head, body) }
        }
        verify { analyticsEventProcessor.track(DidSendLocalInfoNotification) }
    }

    @Test
    fun `when contentVersion changes then should show notification`() = runBlocking {
        coEvery { getFirstMessageOfTypeNotification(receivedResponse) } returns
            receivedNotificationWithId.copy(messageId = "message2")
        every { receivedLocalMessage.contentVersion } returns 2

        showLocalInformationNotificationIfNeeded(previousResponse, receivedResponse)

        with(expectedTranslation) {
            verify { notificationProvider.showLocalMessageNotification(head, body) }
        }
        verify { analyticsEventProcessor.track(DidSendLocalInfoNotification) }
    }

    @Test
    fun `when changes are detected but translation returns null then should not show notification`() = runBlocking {
        coEvery { getFirstMessageOfTypeNotification(receivedResponse) } returns
            receivedNotificationWithId.copy(messageId = "message2")
        every { receivedTranslations.translateOrNull() } returns null

        showLocalInformationNotificationIfNeeded(previousResponse, receivedResponse)

        verify(exactly = 0) { notificationProvider.showLocalMessageNotification(any(), any()) }
        verify(exactly = 0) { analyticsEventProcessor.track(any()) }
    }

    private val previousResponse = mockk<LocalMessagesResponse>()
    private val receivedResponse = mockk<LocalMessagesResponse>()

    private val previousLocalMessage = mockk<Notification>()
    private val previousNotificationWithId = NotificationWithId(messageId = "message1", message = previousLocalMessage)
    private val receivedLocalMessage = mockk<Notification>()
    private val receivedNotificationWithId = NotificationWithId(messageId = "message1", message = receivedLocalMessage)
    private val receivedTranslations = mockk<TranslatableNotificationMessage>()
    private val expectedTranslation = NotificationMessage(head = "head", body = "body", mockk())
}
