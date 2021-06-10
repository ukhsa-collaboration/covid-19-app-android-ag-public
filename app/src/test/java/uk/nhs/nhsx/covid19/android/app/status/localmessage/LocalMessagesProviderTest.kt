package uk.nhs.nhsx.covid19.android.app.status.localmessage

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.ContentBlock
import uk.nhs.nhsx.covid19.android.app.remote.data.ContentBlockType.PARAGRAPH
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessage
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessageTranslation
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessageType.NOTIFICATION
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessagesResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.TranslatableLocalMessage
import uk.nhs.nhsx.covid19.android.app.util.adapters.ContentBlockTypeAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.InstantAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.LocalMessageTypeAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.TranslatableLocalMessageAdapter
import java.time.Instant

class LocalMessagesProviderTest {
    private val moshi = Moshi.Builder()
        .add(TranslatableLocalMessageAdapter())
        .add(LocalMessageTypeAdapter())
        .add(ContentBlockTypeAdapter())
        .add(InstantAdapter())
        .build()
    private val mockLocalMessagesStorage = mockk<LocalMessagesStorage>(relaxUnitFun = true)

    private val testSubject = LocalMessagesProvider(
        mockLocalMessagesStorage,
        moshi
    )

    @Test
    fun `verify empty`() {
        every { mockLocalMessagesStorage.value } returns null

        assertNull(testSubject.localMessages)
    }

    @Test
    fun `verify deserialization of json`() {
        every { mockLocalMessagesStorage.value } returns localMessagesJson

        assertEquals(localMessagesResponse, testSubject.localMessages)
    }

    @Test
    fun `verify serialization to json`() {
        testSubject.localMessages = localMessagesResponse

        verify { mockLocalMessagesStorage.value = localMessagesJson }
    }

    private val localMessagesResponse = LocalMessagesResponse(
        localAuthorities = mapOf(
            "ABCD1234" to listOf("message1")
        ),
        messages = mapOf(
            "message1" to LocalMessage(
                type = NOTIFICATION,
                updated = Instant.parse("2021-05-19T14:59:13Z"),
                contentVersion = 1,
                translations = TranslatableLocalMessage(
                    mapOf(
                        "en" to LocalMessageTranslation(
                            head = "A new variant of concern is in your area",
                            body = "This is the body of the notification",
                            content = listOf(
                                ContentBlock(
                                    type = PARAGRAPH,
                                    text = "There have been reported cases of a new variant in [postcode]. Here are some key pieces of information to help you stay safe"
                                ),
                                ContentBlock(
                                    type = PARAGRAPH,
                                    text = "There have been reported cases of a new variant in [postcode]. Here are some key pieces of information to help you stay safe",
                                    link = "http://example.com",
                                    linkText = "Click me"
                                )
                            )
                        )
                    )
                )
            )
        )
    )

    private val localMessagesJson =
        """{"las":{"ABCD1234":["message1"]},"messages":{"message1":{"type":"notification","updated":"2021-05-19T14:59:13Z","contentVersion":1,"translations":{"en":{"head":"A new variant of concern is in your area","body":"This is the body of the notification","content":[{"type":"para","text":"There have been reported cases of a new variant in [postcode]. Here are some key pieces of information to help you stay safe"},{"type":"para","text":"There have been reported cases of a new variant in [postcode]. Here are some key pieces of information to help you stay safe","link":"http://example.com","linkText":"Click me"}]}}}}}"""
}
