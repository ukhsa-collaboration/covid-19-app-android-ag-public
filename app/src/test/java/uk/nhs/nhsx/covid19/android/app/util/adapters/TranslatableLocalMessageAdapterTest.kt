package uk.nhs.nhsx.covid19.android.app.util.adapters

import com.squareup.moshi.Moshi
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.ContentBlock
import uk.nhs.nhsx.covid19.android.app.remote.data.ContentBlockType.PARAGRAPH
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessageTranslation
import uk.nhs.nhsx.covid19.android.app.remote.data.TranslatableLocalMessage
import kotlin.test.assertEquals

class TranslatableLocalMessageAdapterTest {
    private val moshi = Moshi.Builder()
        .add(TranslatableLocalMessageAdapter())
        .add(LocalMessageTypeAdapter())
        .add(ContentBlockTypeAdapter())
        .build()

    private val testSubject = moshi.adapter(TranslatableLocalMessage::class.java)

    private val translatableJson =
        """{"en":{"head":"A new variant of concern is in your area","body":"This is the body of the notification","content":[{"type":"para","text":"There have been reported cases of a new variant in [postcode]. Here are some key pieces of information to help you stay safe"},{"type":"para","text":"There have been reported cases of a new variant in [postcode]. Here are some key pieces of information to help you stay safe","link":"http://example.com","linkText":"Click me"}]}}"""

    private val translatable = TranslatableLocalMessage(
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

    @Test
    fun `convert TranslatableLocalMessage to json`() {
        val result = testSubject.toJson(translatable)

        assertEquals(translatableJson, result)
    }

    @Test
    fun `parse TranslatableLocalMessage from json`() {
        val result = testSubject.fromJson(translatableJson)

        assertEquals(translatable, result)
    }
}
