package uk.nhs.nhsx.covid19.android.app.common

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.SupportedLanguage
import uk.nhs.nhsx.covid19.android.app.SupportedLanguage.ENGLISH
import uk.nhs.nhsx.covid19.android.app.SupportedLanguage.WELSH
import uk.nhs.nhsx.covid19.android.app.common.ApplicationLocaleProvider.Companion.APPLICATION_LANGUAGE_KEY
import uk.nhs.nhsx.covid19.android.app.settings.languages.GetDefaultSystemLanguage
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ApplicationLocaleProviderTest {
    private val sharedPreferences = mockk<SharedPreferences>(relaxed = true)
    private val sharedPreferencesEditor = mockk<SharedPreferences.Editor>(relaxed = true)
    private val getDefaultSystemLanguage = mockk<GetDefaultSystemLanguage>()

    private val testSubject = ApplicationLocaleProvider(sharedPreferences, getDefaultSystemLanguage)

    @Before
    fun setUp() {
        every {
            sharedPreferences.edit()
        } returns sharedPreferencesEditor
    }

    @Test
    fun `no saved language returns default locale`() = runBlocking {
        every { sharedPreferences.all[APPLICATION_LANGUAGE_KEY] } returns null
        every { getDefaultSystemLanguage() } returns ENGLISH

        val result = testSubject.getLocale()

        assertEquals(Locale(ENGLISH.code!!), result)
    }

    @Test
    fun `saved language returns locale with that language`() {
        every { sharedPreferences.all[APPLICATION_LANGUAGE_KEY] } returns "en"

        val result = testSubject.getLocale()

        assertEquals(Locale(testSubject.languageCode!!), result)
    }

    @Test
    fun `no saved language returns no language`() = runBlocking {
        every { sharedPreferences.all[APPLICATION_LANGUAGE_KEY] } returns null

        val result = testSubject.getUserSelectedLanguage()

        assertNull(result)
    }

    @Test
    fun `saved supported language returns that language`() {
        every { sharedPreferences.all[APPLICATION_LANGUAGE_KEY] } returns "cy"

        val result = testSubject.getUserSelectedLanguage()

        assertEquals(WELSH, result)
    }

    @Test
    fun `saved unsupported language returns no language`() {
        every { sharedPreferences.all[APPLICATION_LANGUAGE_KEY] } returns "xy"

        val result = testSubject.getUserSelectedLanguage()

        assertNull(result)
    }

    @Test
    fun `getDefaultSystemLanguage delegates call to interactor`() {
        val expectedDefaultSystemLanguage = mockk<SupportedLanguage>()
        every { getDefaultSystemLanguage() } returns expectedDefaultSystemLanguage

        val result = getDefaultSystemLanguage()

        assertEquals(expectedDefaultSystemLanguage, result)
    }
}
