package uk.nhs.nhsx.covid19.android.app.settings.languages

import android.content.res.Configuration
import android.content.res.Resources
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.SupportedLanguage.ENGLISH
import uk.nhs.nhsx.covid19.android.app.SupportedLanguage.WELSH
import uk.nhs.nhsx.covid19.android.app.common.BuildVersionProvider
import java.util.Locale
import kotlin.test.assertEquals

class GetDefaultSystemLanguageTest {

    private val buildVersionProvider = mockk<BuildVersionProvider>()

    private val getDefaultSystemLanguage = GetDefaultSystemLanguage(buildVersionProvider)

    @Before
    fun setUp() {
        mockkStatic(Resources::class)
        every { buildVersionProvider.version() } returns 24
    }

    @Test
    fun `supported system language for API version 24 or above`() {
        every { Resources.getSystem().configuration.locales.getFirstMatch(any()) } returns Locale("cy")

        val result = getDefaultSystemLanguage()

        assertEquals(WELSH, result)
    }

    @Test
    fun `supported system language for API versions 23 and below`() {
        every { buildVersionProvider.version() } returns 23
        mockkStatic(Resources::class)
        every { Resources.getSystem().configuration } returns Configuration().apply { locale = Locale("cy") }

        val result = getDefaultSystemLanguage()

        assertEquals(WELSH, result)

        unmockkStatic(Resources::class)
    }

    @Test
    fun `fall back to English language when default system language is not supported`() {
        every { Resources.getSystem().configuration.locales.getFirstMatch(any()) } returns null

        val result = getDefaultSystemLanguage()

        assertEquals(ENGLISH, result)
    }
}
