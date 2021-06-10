package uk.nhs.nhsx.covid19.android.app.status

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.postcode.GetLocalAuthorityName
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessage
import uk.nhs.nhsx.covid19.android.app.remote.data.TranslatableLocalMessage
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SubstitutePlaceholdersInMessageWithIdTest {

    private val postCodeProvider = mockk<PostCodeProvider>()
    private val getLocalAuthorityName = mockk<GetLocalAuthorityName>()

    private val translateMessageWithId = SubstitutePlaceholdersInMessageWithId(postCodeProvider, getLocalAuthorityName)

    private val postCode = "SE1"
    private val localAuthorityName = "Somethingsborough"

    @Before
    fun setUp() {
        every { postCodeProvider.value } returns postCode
        coEvery { getLocalAuthorityName() } returns localAuthorityName
    }

    @Test
    fun `when the app does not know of a post code then return null`() = runBlocking {
        every { postCodeProvider.value } returns null

        assertNull(translateMessageWithId(mockk()))
    }

    @Test
    fun `when the app can not resolve a local authority name then return null`() = runBlocking {
        coEvery { getLocalAuthorityName() } returns null

        assertNull(translateMessageWithId(mockk()))
    }

    @Test
    fun `when a post code and local authority name are present then placeholders in MessageWithId are substituted with corresponding values`() =
        runBlocking {
            val translations = mockk<TranslatableLocalMessage>()
            val messageWithId = MessageWithId(
                messageId = "messageId",
                message = LocalMessage(
                    type = mockk(),
                    updated = mockk(),
                    contentVersion = mockk(relaxed = true),
                    translations
                )
            )

            val translatableLocalMessage = mockk<TranslatableLocalMessage>()
            every { translations.replacePlaceholders(postCode, localAuthorityName) } returns translatableLocalMessage

            val result = translateMessageWithId(messageWithId)

            assertEquals(
                expected = messageWithId.copy(message = messageWithId.message.copy(translations = translatableLocalMessage)),
                actual = result
            )
        }
}
