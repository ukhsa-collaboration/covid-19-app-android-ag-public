package uk.nhs.nhsx.covid19.android.app.common

import org.junit.Test
import java.util.Locale
import kotlin.test.assertEquals

class TranslatableStringTest {

    val testSubject = TranslatableString(mapOf("en-GB" to "Hello", "zh-CN" to "你好", "ko-KR" to "test"))

    @Test
    fun `returns translation according to current locale`() {
        assertEquals("你好", testSubject.translate(Locale.SIMPLIFIED_CHINESE))
    }

    @Test
    fun `uses language code if country code is unknown`() {
        assertEquals("Hello", testSubject.translate(Locale.CANADA))
    }

    @Test
    fun `returns fallback language translation if language is not supported`() {
        assertEquals("Hello", testSubject.translate(Locale.KOREA))
    }

    @Test
    fun `uses language code if country code is not present`() {
        assertEquals("Hello", testSubject.translate(Locale.ENGLISH))
    }

    @Test
    fun `fall backs to GB English if cannot find a match`() {
        assertEquals("Hello", testSubject.translate(Locale.FRANCE))
    }

    @Test
    fun `fall backs to English if cannot find a match and GB English is not available`() {
        val testSubject = TranslatableString(mapOf("en" to "Hello", "zh-CN" to "你好", "ko-KR" to "test"))
        assertEquals("Hello", testSubject.translate(Locale.FRANCE))
    }

    @Test
    fun `returns empty string if cannot find a match and fallback language translation is not present`() {
        val testSubject = TranslatableString(mapOf("zh-CN" to "你好"))

        assertEquals("", testSubject.translate(Locale.UK))
    }

    @Test
    fun `find best match if country code is not present in the list of translations`() {
        val testSubject = TranslatableString(mapOf("en" to "Hello"))

        assertEquals("Hello", testSubject.translate(Locale.UK))
    }
}
