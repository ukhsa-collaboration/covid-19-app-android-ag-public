package uk.nhs.nhsx.covid19.android.app.status.localmessage

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalInformation.Notification
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessagesResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.NotificationMessage
import uk.nhs.nhsx.covid19.android.app.remote.data.TranslatableNotificationMessage
import uk.nhs.nhsx.covid19.android.app.status.GetFirstMessageOfTypeNotification
import uk.nhs.nhsx.covid19.android.app.status.NotificationWithId
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetLocalMessageFromStorageTest {

    private val localMessagesProvider = mockk<LocalMessagesProvider>()
    private val getFirstMessageOfTypeNotification = mockk<GetFirstMessageOfTypeNotification>()

    private val getLocalMessageFromStorage =
        GetLocalMessageFromStorage(localMessagesProvider, getFirstMessageOfTypeNotification)

    @Test
    fun `verify getFirstMessageOfTypeNotification is called with messages stored in LocalMessagesProvider`() =
        runBlocking {
            val expectedLocalMessages = mockk<LocalMessagesResponse>()
            val messageWithId = mockk<NotificationWithId>()
            val notification = mockk<Notification>()
            val translatableNotificationMessage = mockk<TranslatableNotificationMessage>()
            val expectedTranslation = mockk<NotificationMessage>()

            every { localMessagesProvider.localMessages } returns expectedLocalMessages
            coEvery { getFirstMessageOfTypeNotification(expectedLocalMessages) } returns messageWithId
            every { messageWithId.message } returns notification
            every { notification.translations } returns translatableNotificationMessage
            every { translatableNotificationMessage.translateOrNull() } returns expectedTranslation

            val result = getLocalMessageFromStorage()

            assertEquals(expectedTranslation, result)

            coVerify { getFirstMessageOfTypeNotification(expectedLocalMessages) }
            verify { translatableNotificationMessage.translateOrNull() }
        }

    @Test
    fun `when no first message of type notification found then return null`() = runBlocking {
        every { localMessagesProvider.localMessages } returns mockk()
        coEvery { getFirstMessageOfTypeNotification(any()) } returns null

        assertNull(getLocalMessageFromStorage())
    }
}
