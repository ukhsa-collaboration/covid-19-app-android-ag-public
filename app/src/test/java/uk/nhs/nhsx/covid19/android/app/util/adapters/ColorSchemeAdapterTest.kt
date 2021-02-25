package uk.nhs.nhsx.covid19.android.app.util.adapters

import com.squareup.moshi.Moshi
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.MAROON
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.NEUTRAL
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ColorSchemeAdapterTest {

    private val moshi = Moshi.Builder().add(ColorSchemeAdapter()).build()

    private val testSubject = moshi.adapter(ColorScheme::class.java)

    private val colorSchemeJson =
        """"maroon""""
    private val unknownColorSchemeJson =
        """"this-color-scheme-does-not-exist""""

    @Test
    fun `convert ColorScheme to json`() {
        val result = testSubject.toJson(MAROON)

        assertEquals(colorSchemeJson, result)
    }

    @Test
    fun `parse ColorScheme from json`() {
        val result = testSubject.fromJson(colorSchemeJson)

        assertNotNull(result)

        assertEquals(MAROON, result)
    }

    @Test
    fun `parse unknown ColorScheme from json falls back to neutral`() {
        val result = testSubject.fromJson(unknownColorSchemeJson)

        assertNotNull(result)

        assertEquals(NEUTRAL, result)
    }
}
