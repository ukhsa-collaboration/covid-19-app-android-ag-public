package uk.nhs.nhsx.covid19.android.app.common

import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Build.VERSION_CODES
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.SupportedLanguage.ENGLISH
import uk.nhs.nhsx.covid19.android.app.SupportedLanguage.WELSH
import uk.nhs.nhsx.covid19.android.app.common.ApplicationLocaleProvider.Companion.APPLICATION_LANGUAGE_KEY
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ApplicationLocaleProviderTest {
    private val sharedPreferences = mockk<SharedPreferences>(relaxed = true)
    private val sharedPreferencesEditor = mockk<SharedPreferences.Editor>(relaxed = true)
    private val versionProvider = mockk<BuildVersionProvider>()

    private val testSubject = ApplicationLocaleProvider(sharedPreferences, versionProvider)

    @Before
    fun setUp() {
        mockkStatic(Resources::class)
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
    fun `supported system language for API version N or above`() {
        every { Resources.getSystem().configuration.locales.getFirstMatch(any()) } returns Locale("cy")
        every { versionProvider.version() } returns VERSION_CODES.N

        val result = testSubject.getSystemLanguage()

        assertEquals(WELSH, result)
    }

    @Test
    fun `unsupported system language returns English language for API version N or above`() {
        every { Resources.getSystem().configuration.locales.getFirstMatch(any()) } returns null
        every { versionProvider.version() } returns VERSION_CODES.N

        val result = testSubject.getSystemLanguage()

        assertEquals(ENGLISH, result)
    }
}
