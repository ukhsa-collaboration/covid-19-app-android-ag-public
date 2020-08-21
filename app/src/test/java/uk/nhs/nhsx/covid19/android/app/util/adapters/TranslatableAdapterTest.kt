package uk.nhs.nhsx.covid19.android.app.util.adapters

import com.squareup.moshi.Moshi
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.Translatable
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TranslatableAdapterTest {

    private val moshi = Moshi.Builder().add(TranslatableAdapter()).build()

    private val testSubject = moshi.adapter(Translatable::class.java)

    private val translatableJson =
        """{"en-GB":"A high temperature (fever)","bn-BD":"উচ্চ তাপমাত্রা (জ্বর)","gu-IN":"ઉંચુ તાપમાન (તાવ)"}"""

    @Test
    fun `convert Translatable to json`() {
        val translatable = Translatable(
            mapOf(
                "en-GB" to "A high temperature (fever)",
                "bn-BD" to "উচ্চ তাপমাত্রা (জ্বর)",
                "gu-IN" to "ઉંચુ તાપમાન (તાવ)"
            )
        )

        val result = testSubject.toJson(translatable)

        assertEquals(translatableJson, result)
    }

    @Test
    fun `parse Translatable from json`() {
        val result = testSubject.fromJson(translatableJson)

        assertNotNull(result)

        assertEquals("A high temperature (fever)", result.translations["en-GB"])
        assertEquals("উচ্চ তাপমাত্রা (জ্বর)", result.translations["bn-BD"])
        assertEquals("ઉંચુ તાપમાન (તાવ)", result.translations["gu-IN"])
    }
}
