package uk.nhs.nhsx.covid19.android.app.state

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.util.adapters.InstantAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.LocalDateAdapter
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals

class StateStorageTest {

    private val moshi = Moshi.Builder()
        .add(StateJson.stateMoshiAdapter)
        .add(LocalDateAdapter())
        .add(InstantAdapter())
        .build()

    private val statusStringStorage = mockk<StateStringStorage>(relaxed = true)

    private val testSubject =
        StateStorage(
            statusStringStorage,
            moshi
        )

    @Test
    fun `parses default case properly`() {
        every { statusStringStorage.prefsValue } returns """[$DEFAULT]"""

        val parsedState = testSubject.state

        assertEquals(Default(), parsedState)
    }

    @Test
    fun `parses index case properly`() {
        every { statusStringStorage.prefsValue } returns """[$INDEX_CASE]"""

        val parsedState = testSubject.state

        assertEquals(
            Isolation(startDate, expiryDate, indexCase = IndexCase(onsetDate)),
            parsedState
        )
    }

    @Test
    fun `parses contact case properly`() {
        every { statusStringStorage.prefsValue } returns """[$CONTACT_CASE]"""

        val parsedState = testSubject.state

        assertEquals(
            Isolation(startDate, expiryDate, contactCase = ContactCase(startDate)),
            parsedState
        )
    }

    @Test
    fun `parses invalid data as default state`() {
        every { statusStringStorage.prefsValue } returns """[$INVALID_CASE]"""

        val parsedState = testSubject.state

        assertEquals(Default(), parsedState)
    }

    @Test
    fun `parses partial data as default state`() {
        every { statusStringStorage.prefsValue } returns """[{"type":"PositiveCase","testDate":"2020-05-21T10:00:00Z","version":1}]"""

        val parsedState = testSubject.state

        assertEquals(Default(), parsedState)
    }

    @Test
    fun `parses history`() {
        every { statusStringStorage.prefsValue } returns """[$INDEX_CASE, $CONTACT_CASE, $DEFAULT]"""

        val parsedHistory = testSubject.getHistory()

        val expected = listOf(
            Isolation(startDate, expiryDate, indexCase = IndexCase(onsetDate)),
            Isolation(startDate, expiryDate, contactCase = ContactCase(startDate)),
            Default()
        )
        assertEquals(expected, parsedHistory)
    }

    @Test
    fun `saves updated history`() {
        val updatedHistory = listOf(
            Isolation(startDate, expiryDate, indexCase = IndexCase(onsetDate)),
            Isolation(startDate, expiryDate, contactCase = ContactCase(startDate)),
            Default()
        )

        testSubject.updateHistory(updatedHistory)

        verify {
            statusStringStorage.prefsValue =
                """[$INDEX_CASE,$CONTACT_CASE,$DEFAULT]"""
        }
    }

    companion object {
        private val startDate = Instant.parse("2020-05-21T10:00:00Z")
        private val expiryDate = LocalDate.of(2020, 7, 22)
        private val onsetDate = LocalDate.parse("2020-05-21")

        const val DEFAULT =
            """{"type":"Default","version":1}"""
        const val INDEX_CASE =
            """{"type":"Isolation","isolationStart":"2020-05-21T10:00:00Z","expiryDate":"2020-07-22","indexCase":{"symptomsOnsetDate":"2020-05-21"},"version":1}"""
        const val CONTACT_CASE =
            """{"type":"Isolation","isolationStart":"2020-05-21T10:00:00Z","expiryDate":"2020-07-22","contactCase":{"startDate":"2020-05-21T10:00:00Z"},"version":1}"""
        const val INVALID_CASE =
            """{"type":"UnknownCase","testDate":1594733801229,"expiryDate":1595338601229,"version":1}"""
    }
}
