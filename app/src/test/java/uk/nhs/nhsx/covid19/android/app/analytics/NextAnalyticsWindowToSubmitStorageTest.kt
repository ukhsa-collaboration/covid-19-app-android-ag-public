package uk.nhs.nhsx.covid19.android.app.analytics

import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.covid19.android.app.util.ProviderTest
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectation
import java.time.Instant
import kotlin.test.assertEquals

class NextAnalyticsWindowToSubmitStorageTest : ProviderTest<NextAnalyticsWindowToSubmitStorage, Instant?>() {

    private val oldestLogEntryInstant = mockk<GetOldestLogEntryInstant>()
    override val getTestSubject: (Moshi, SharedPreferences) -> NextAnalyticsWindowToSubmitStorage =
        { moshi, sharedPreferences -> NextAnalyticsWindowToSubmitStorage(oldestLogEntryInstant, moshi, sharedPreferences) }
    override val property = NextAnalyticsWindowToSubmitStorage::windowStartDate
    override val key = NextAnalyticsWindowToSubmitStorage.VALUE_KEY
    override val defaultValue: Instant?
        get() = oldestLogEntryInstant()
    override val expectations: List<ProviderTestExpectation<Instant?>> = listOf(
        ProviderTestExpectation(json = jsonInstant, objectValue = instant)
    )

    @BeforeEach
    fun setUpMock() {
        every { oldestLogEntryInstant() } returns null
    }

    @Test
    fun `returns null if no events and no window is stored`() {
        sharedPreferencesReturns(null)

        assertEquals(null, testSubject.windowStartDate)
    }

    @Test
    fun `returns instant of oldest log entry if no window is stored`() {
        every { oldestLogEntryInstant() } returns instant
        sharedPreferencesReturns(null)

        assertEquals(instant, testSubject.windowStartDate)
    }

    companion object {
        private const val jsonInstant =
            """"2020-10-10T08:00:00Z""""
        private val instant = Instant.parse("2020-10-10T08:00:00Z")
    }
}
