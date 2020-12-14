package uk.nhs.nhsx.covid19.android.app.exposure.keysdownload

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.keysdownload.LastDownloadedKeyTimeProvider.Companion.LAST_DOWNLOADED_KEY
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class LastDownloadedKeyTimeProviderTest {

    private val sharedPreferences = mockk<SharedPreferences>(relaxed = true)
    private val sharedPreferencesEditor = mockk<SharedPreferences.Editor>(relaxed = true)

    private val testSubject = LastDownloadedKeyTimeProvider(sharedPreferences)

    @Before
    fun setUp() {
        every { sharedPreferences.edit() } returns sharedPreferencesEditor
    }

    @Test
    fun `fetching latest stored time when it was not set before should return null`() {
        every { sharedPreferences.all[LAST_DOWNLOADED_KEY] } returns null

        assertEquals(expected = null, actual = testSubject.getLatestStoredTime())
    }

    @Test
    fun `fetching latest stored time when value is stored in shared preferences should return correct value`() {
        every { sharedPreferences.all[LAST_DOWNLOADED_KEY] } returns TEST_TIME_IN_MS

        val actual = testSubject.getLatestStoredTime()
        val expected = Instant.ofEpochMilli(TEST_TIME_IN_MS).atZone(ZoneOffset.UTC).toLocalDateTime()

        assertEquals(expected, actual)
    }

    @Test
    fun `save last stored date time`() {
        testSubject.saveLastStoredTime(TEST_TIME_FORMATTED)

        verify { sharedPreferencesEditor.putLong(LAST_DOWNLOADED_KEY, TEST_TIME_IN_MS) }
    }

    companion object {
        private const val TEST_TIME_IN_MS = 1606780800000
        private const val TEST_TIME_FORMATTED = "2020120100"
    }
}
