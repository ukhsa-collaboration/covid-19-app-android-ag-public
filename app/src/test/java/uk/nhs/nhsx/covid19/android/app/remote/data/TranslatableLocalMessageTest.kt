package uk.nhs.nhsx.covid19.android.app.remote.data

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.ContentBlockType.PARAGRAPH
import kotlin.test.assertEquals

class TranslatableLocalMessageTest {
    private val postCode = "AL1"
    private val localAuthority = "St. Albans"

    @Test
    fun `replacePlaceholders replaces local authority and post code placeholders with relevant strings`() {
        val translatableLocalMessage = TranslatableLocalMessage(
            mapOf(
                "en" to translationWithText(
                    head = "Head with [postcode] and [local authority]",
                    body = "Body with [postcode] and [local authority]",
                    text = "Text with [postcode] and [local authority]"
                ),
                "pl" to translationWithText(
                    head = "Head with [postcode] and [local authority]",
                    body = "Body with [postcode] and [local authority]",
                    text = "Text with [postcode] and [local authority]"
                )
            )
        )

        val replaced = translatableLocalMessage.replacePlaceholders(postCode, localAuthority)

        val expected = TranslatableLocalMessage(
            mapOf(
                "en" to translationWithText(
                    head = "Head with $postCode and $localAuthority",
                    body = "Body with $postCode and $localAuthority",
                    text = "Text with $postCode and $localAuthority"
                ),
                "pl" to translationWithText(
                    head = "Head with $postCode and $localAuthority",
                    body = "Body with $postCode and $localAuthority",
                    text = "Text with $postCode and $localAuthority"
                )
            )
        )
        assertEquals(expected, replaced)
    }

    private fun translationWithText(head: String, body: String, text: String) = LocalMessageTranslation(
        head = head,
        body = body,
        content = listOf(
            ContentBlock(type = PARAGRAPH, text = text)
        )
    )
}
