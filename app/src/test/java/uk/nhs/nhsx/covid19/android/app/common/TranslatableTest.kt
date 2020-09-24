package uk.nhs.nhsx.covid19.android.app.common

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Locale
import kotlin.test.assertEquals

class TranslatableTest {

    val testSubject = Translatable(mapOf("en-GB" to "Hello", "zh-CN" to "你好", "ko-KR" to "test"))

    @Before
    fun setUp() {
        mockkStatic(Locale::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(Locale::class)
    }

    @Test
    fun `returns translation according to current locale`() {
        every { Locale.getDefault() } returns Locale.SIMPLIFIED_CHINESE

        assertEquals("你好", testSubject.translate())
    }

    @Test
    fun `uses language code if country code is unknown`() {
        every { Locale.getDefault() } returns Locale.CANADA

        assertEquals("Hello", testSubject.translate())
    }

    @Test
    fun `returns fallback language translation if language is not supported`() {
        every { Locale.getDefault() } returns Locale.KOREA

        assertEquals("Hello", testSubject.translate())
    }

    @Test
    fun `uses language code if country code is not present`() {
        every { Locale.getDefault() } returns Locale.ENGLISH

        assertEquals("Hello", testSubject.translate())
    }

    @Test
    fun `fall backs to English if cannot find a match`() {
        every { Locale.getDefault() } returns Locale.FRANCE

        assertEquals("Hello", testSubject.translate())
    }

    @Test
    fun `returns empty string if cannot find a match and fallback language translation is not present`() {
        every { Locale.getDefault() } returns Locale.UK

        val testSubject = Translatable(mapOf("zh-CN" to "你好"))

        assertEquals("", testSubject.translate())
    }

    @Test
    fun `find best match if country code is not present in the list of translations`() {
        every { Locale.getDefault() } returns Locale.UK

        val testSubject = Translatable(mapOf("en" to "Hello"))

        assertEquals("Hello", testSubject.translate())
    }
}
