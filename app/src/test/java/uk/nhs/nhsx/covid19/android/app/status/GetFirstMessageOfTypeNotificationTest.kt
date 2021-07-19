package uk.nhs.nhsx.covid19.android.app.status

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalInformation.Notification
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalInformation.Unknown
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessagesResponse
import kotlin.test.assertEquals

class GetFirstMessageOfTypeNotificationTest {

    private val localAuthorityProvider = mockk<LocalAuthorityProvider>()
    private val substitutePlaceholdersInMessageWithId = mockk<SubstitutePlaceholdersInNotificationWithId>()

    private val getFirstMessageOfTypeNotificationTest = GetFirstMessageOfTypeNotification(
        localAuthorityProvider,
        substitutePlaceholdersInMessageWithId
    )

    @Test
    fun `when passed LocalMessagesResponse is null then return null`() = runBlocking {
        assertNull(getFirstMessageOfTypeNotificationTest(null))
    }

    @Test
    fun `when local authority id can not be found then return null`() = runBlocking {
        val localMessagesResponse = mockk<LocalMessagesResponse>()
        every { localMessagesResponse.localAuthorities } returns mapOf()
        every { localAuthorityProvider.value } returns null

        assertNull(getFirstMessageOfTypeNotificationTest(localMessagesResponse))
    }

    @Test
    fun `when local authority id and wildcard not found in LocalMessagesResponse then return null`() = runBlocking {
        val localMessagesResponse = mockk<LocalMessagesResponse>()
        every { localMessagesResponse.localAuthorities } returns mapOf()
        every { localAuthorityProvider.value } returns localAuthorityId

        assertNull(getFirstMessageOfTypeNotificationTest(localMessagesResponse))
    }

    @Test
    fun `when local authority id is found in LocalMessagesResponse but maps to an empty list then return null`() =
        runBlocking {
            val localMessagesResponse = mockk<LocalMessagesResponse>()
            every { localMessagesResponse.localAuthorities } returns mapOf(localAuthorityId to listOf())
            every { localAuthorityProvider.value } returns localAuthorityId

            assertNull(getFirstMessageOfTypeNotificationTest(localMessagesResponse))
        }

    @Test
    fun `when wildcard is present but maps to an empty list then return null`() =
        runBlocking {
            val localMessagesResponse = mockk<LocalMessagesResponse>()
            every { localMessagesResponse.localAuthorities } returns mapOf("*" to listOf())
            every { localAuthorityProvider.value } returns localAuthorityId

            assertNull(getFirstMessageOfTypeNotificationTest(localMessagesResponse))
        }

    @Test
    fun `successfully return the first message of type notification`() = runBlocking {
        every { localAuthorityProvider.value } returns localAuthorityId

        val expectedMessageWithId = NotificationWithId(messageId = "message1", message = message1)
        val expectedResult = mockk<NotificationWithId>()

        coEvery { substitutePlaceholdersInMessageWithId(expectedMessageWithId) } returns expectedResult

        val result = getFirstMessageOfTypeNotificationTest(localMessagesResponse)

        assertEquals(expectedResult, result)
    }

    @Test
    fun `when message id of users local authority is not in LocalMessagesResponse then return null`() = runBlocking {
        every { localAuthorityProvider.value } returns "test1"

        assertNull(getFirstMessageOfTypeNotificationTest(localMessagesResponse))

        coVerify(exactly = 0) { substitutePlaceholdersInMessageWithId(any()) }
    }

    @Test
    fun `when no messages of type notification are found in LocalMessagesResponse then return null`() = runBlocking {
        every { localAuthorityProvider.value } returns "test2"

        assertNull(getFirstMessageOfTypeNotificationTest(localMessagesResponse))

        coVerify(exactly = 0) { substitutePlaceholdersInMessageWithId(any()) }
    }

    @Test
    fun `specific message has priority over wildcard message`() = runBlocking {
        every { localAuthorityProvider.value } returns localAuthorityId

        val expectedMessageWithId = NotificationWithId(messageId = "message1", message = message1)
        val expectedResult = mockk<NotificationWithId>()

        coEvery { substitutePlaceholdersInMessageWithId(expectedMessageWithId) } returns expectedResult

        val result = getFirstMessageOfTypeNotificationTest(localMessagesResponseWithWildcard)

        assertEquals(expectedResult, result)
    }

    @Test
    fun `wildcard message applies to any local authority not already in map`() = runBlocking {
        every { localAuthorityProvider.value } returns "anotherLocalAuthority"

        val expectedMessageWithId = NotificationWithId(messageId = "wildcard-message", message = wildcardMessage)
        val expectedResult = mockk<NotificationWithId>()

        coEvery { substitutePlaceholdersInMessageWithId(expectedMessageWithId) } returns expectedResult

        val result = getFirstMessageOfTypeNotificationTest(localMessagesResponseWithWildcard)

        assertEquals(expectedResult, result)
    }

    @Test
    fun `wildcard message is selected when specific local authority message is not of type notification`() = runBlocking {
        every { localAuthorityProvider.value } returns "authorityWithUnknown"

        val expectedMessageWithId = NotificationWithId(messageId = "wildcard-message", message = wildcardMessage)
        val expectedResult = mockk<NotificationWithId>()

        coEvery { substitutePlaceholdersInMessageWithId(expectedMessageWithId) } returns expectedResult

        val result = getFirstMessageOfTypeNotificationTest(localMessagesResponseWithWildcard)

        assertEquals(expectedResult, result)
    }

    private val localAuthorityId = "S00001"

    private val wildcardMessage = notificationMessage()
    private val message1 = notificationMessage()

    private val localMessagesResponse = LocalMessagesResponse(
        localAuthorities = mapOf(
            "test1" to listOf("nonexistent-message"),
            localAuthorityId to listOf("message3", "message1", "message2"),
            "test2" to listOf("mm3")
        ),
        messages = mapOf(
            "message3" to Unknown,
            "message2" to notificationMessage(),
            "message1" to message1,
            "mm3" to Unknown
        )
    )

    private val localMessagesResponseWithWildcard = LocalMessagesResponse(
        localAuthorities = mapOf(
            "*" to listOf("wildcard-message"),
            localAuthorityId to listOf("message1", "message2"),
            "authorityWithUnknown" to listOf("unknown-message")
        ),
        messages = mapOf(
            "wildcard-message" to wildcardMessage,
            "message2" to notificationMessage(),
            "message1" to message1,
            "unknown-message" to Unknown
        )
    )

    private fun notificationMessage() = Notification(
        updated = mockk(),
        contentVersion = 1,
        translations = mockk()
    )
}
