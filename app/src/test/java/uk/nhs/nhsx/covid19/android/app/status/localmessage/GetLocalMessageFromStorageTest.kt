package uk.nhs.nhsx.covid19.android.app.status.localmessage

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessage
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessageTranslation
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessagesResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.TranslatableLocalMessage
import uk.nhs.nhsx.covid19.android.app.status.GetFirstMessageOfTypeNotification
import uk.nhs.nhsx.covid19.android.app.status.MessageWithId
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
            val messageWithId = mockk<MessageWithId>()
            val localMessage = mockk<LocalMessage>()
            val translatableLocalMessage = mockk<TranslatableLocalMessage>()
            val expectedTranslation = mockk<LocalMessageTranslation>()

            every { localMessagesProvider.localMessages } returns expectedLocalMessages
            coEvery { getFirstMessageOfTypeNotification(expectedLocalMessages) } returns messageWithId
            every { messageWithId.message } returns localMessage
            every { localMessage.translations } returns translatableLocalMessage
            every { translatableLocalMessage.translateOrNull() } returns expectedTranslation

            val result = getLocalMessageFromStorage()

            assertEquals(expectedTranslation, result)

            coVerify { getFirstMessageOfTypeNotification(expectedLocalMessages) }
            verify { translatableLocalMessage.translateOrNull() }
        }

    @Test
    fun `when no first message of type notification found then return null`() = runBlocking {
        every { localMessagesProvider.localMessages } returns mockk()
        coEvery { getFirstMessageOfTypeNotification(any()) } returns null

        assertNull(getLocalMessageFromStorage())
    }
}
