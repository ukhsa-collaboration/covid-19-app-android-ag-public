package uk.nhs.nhsx.covid19.android.app.status

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidSendLocalInfoNotification
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessage
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessageTranslation
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessagesResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.TranslatableLocalMessage

class ShowLocalMessageNotificationIfNeededTest {

    private val notificationProvider = mockk<NotificationProvider>(relaxUnitFun = true)
    private val getFirstMessageOfTypeNotification = mockk<GetFirstMessageOfTypeNotification>()
    private val analyticsEventProcessor = mockk<AnalyticsEventProcessor>(relaxUnitFun = true)

    private val showLocalInformationNotificationIfNeeded =
        ShowLocalMessageNotificationIfNeeded(notificationProvider, getFirstMessageOfTypeNotification, analyticsEventProcessor)

    @Before
    fun setUp() {
        coEvery { getFirstMessageOfTypeNotification(previousResponse) } returns previousMessageWithId
        coEvery { getFirstMessageOfTypeNotification(receivedResponse) } returns receivedMessageWithId
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
        coVerify(exactly = 0) { analyticsEventProcessor.track(any()) }
    }

    @Test
    fun `when neither messageId nor contentVersion change then should not show notification`() = runBlocking {
        coEvery { getFirstMessageOfTypeNotification(receivedResponse) } returns receivedMessageWithId

        showLocalInformationNotificationIfNeeded(previousResponse, receivedResponse)

        verify(exactly = 0) { notificationProvider.showLocalMessageNotification(any(), any()) }
        coVerify(exactly = 0) { analyticsEventProcessor.track(any()) }
    }

    @Test
    fun `when messageId changes then should show notification`() = runBlocking {
        coEvery { getFirstMessageOfTypeNotification(receivedResponse) } returns
            receivedMessageWithId.copy(messageId = "message2")

        showLocalInformationNotificationIfNeeded(previousResponse, receivedResponse)

        with(expectedTranslation) {
            verify { notificationProvider.showLocalMessageNotification(head!!, body!!) }
        }
        coVerify { analyticsEventProcessor.track(DidSendLocalInfoNotification) }
    }

    @Test
    fun `when contentVersion changes then should show notification`() = runBlocking {
        coEvery { getFirstMessageOfTypeNotification(receivedResponse) } returns
            receivedMessageWithId.copy(messageId = "message2")
        every { receivedLocalMessage.contentVersion } returns 2

        showLocalInformationNotificationIfNeeded(previousResponse, receivedResponse)

        with(expectedTranslation) {
            verify { notificationProvider.showLocalMessageNotification(head!!, body!!) }
        }
        coVerify { analyticsEventProcessor.track(DidSendLocalInfoNotification) }
    }

    @Test
    fun `when changes are detected but translation returns null then should not show notification`() = runBlocking {
        coEvery { getFirstMessageOfTypeNotification(receivedResponse) } returns
            receivedMessageWithId.copy(messageId = "message2")
        every { receivedTranslations.translateOrNull() } returns null

        showLocalInformationNotificationIfNeeded(previousResponse, receivedResponse)

        verify(exactly = 0) { notificationProvider.showLocalMessageNotification(any(), any()) }
        coVerify(exactly = 0) { analyticsEventProcessor.track(any()) }
    }

    private val previousResponse = mockk<LocalMessagesResponse>()
    private val receivedResponse = mockk<LocalMessagesResponse>()

    private val previousLocalMessage = mockk<LocalMessage>()
    private val previousMessageWithId = MessageWithId(messageId = "message1", message = previousLocalMessage)
    private val receivedLocalMessage = mockk<LocalMessage>()
    private val receivedMessageWithId = MessageWithId(messageId = "message1", message = receivedLocalMessage)
    private val receivedTranslations = mockk<TranslatableLocalMessage>()
    private val expectedTranslation = LocalMessageTranslation(head = "head", body = "body", mockk())
}
