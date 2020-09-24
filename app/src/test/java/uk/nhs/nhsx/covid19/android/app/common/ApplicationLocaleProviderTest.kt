package uk.nhs.nhsx.covid19.android.app.common

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.ApplicationLocaleProvider.Companion.APPLICATION_LANGUAGE_KEY
import java.util.Locale
import kotlin.test.assertEquals

class ApplicationLocaleProviderTest {
    private val sharedPreferences = mockk<SharedPreferences>(relaxed = true)
    private val sharedPreferencesEditor = mockk<SharedPreferences.Editor>(relaxed = true)

    private val languageCode = "en"

    private val testSubject = ApplicationLocaleProvider(sharedPreferences, languageCode)

    @Before
    fun setUp() {
        every {
            sharedPreferences.edit()
        } returns sharedPreferencesEditor
    }

    @Test
    fun `no saved language returns default locale`() = runBlocking {
        every { sharedPreferences.all[APPLICATION_LANGUAGE_KEY] } returns null

        val result = testSubject.getLocale()

        assertEquals(Locale.getDefault(), result)
    }

    @Test
    fun `saved language returns locale with that language`() {
        every { sharedPreferences.all[APPLICATION_LANGUAGE_KEY] } returns "en"

        val result = testSubject.getLocale()

        assertEquals(Locale(testSubject.language!!), result)
    }
}
