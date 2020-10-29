package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import androidx.security.crypto.EncryptedFile
import com.squareup.moshi.Moshi.Builder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.qrcode.Venue
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.state.StateJson
import uk.nhs.nhsx.covid19.android.app.util.EncryptedFileInfo
import uk.nhs.nhsx.covid19.android.app.util.adapters.InstantAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.LocalDateAdapter
import uk.nhs.nhsx.covid19.android.app.util.readText
import uk.nhs.nhsx.covid19.android.app.util.writeText
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VisitedVenuesStorageTest {
    private val file = mockk<File>(relaxed = true)
    private val encryptedFile = mockk<EncryptedFile>(relaxed = true)
    private val encryptedFileInfo = EncryptedFileInfo(file, encryptedFile)

    private val moshi = Builder()
        .add(StateJson.stateMoshiAdapter)
        .add(LocalDateAdapter())
        .add(InstantAdapter())
        .build()

    val testSubject = VisitedVenuesStorage(moshi, encryptedFileInfo)

    @Before
    fun setUp() {
        mockkStatic("uk.nhs.nhsx.covid19.android.app.util.EncryptionUtilsKt")
    }

    @Test
    fun `test getting venues empty list`() = runBlocking {
        every { encryptedFile.readText() } returns ""

        val receivedVisits = testSubject.getVisits()
        verify { encryptedFile.readText() }

        assertEquals(listOf(), receivedVisits)
    }

    @Test
    fun `test getting venues corrupt json`() = runBlocking {
        every { encryptedFile.readText() } returns "abc"

        val receivedVisits = testSubject.getVisits()
        verify { encryptedFile.readText() }

        assertEquals(listOf(), receivedVisits)
    }

    @Test
    fun `test getting venues actual values`() = runBlocking {
        every { encryptedFile.readText() } returns createJson()

        val receivedVisits = testSubject.getVisits()
        verify { encryptedFile.readText() }

        assertEquals(listOf(VENUE_VISIT), receivedVisits)
    }

    @Test
    fun `test get visit by venue id`() = runBlocking {
        every { encryptedFile.readText() } returns createJson()

        val venueVisit = testSubject.getVisitByVenueId(VENUE_VISIT.venue.id)
        verify { encryptedFile.readText() }

        assertEquals(VENUE_VISIT, venueVisit)
    }

    @Test
    fun `test get visit by venue id returns null`() = runBlocking {
        every { encryptedFile.readText() } returns createJson()

        val venueVisit = testSubject.getVisitByVenueId("42")
        verify { encryptedFile.readText() }

        assertNull(venueVisit)
    }

    @Test
    fun `remove all venue visits`() = runBlocking {
        every { encryptedFile.readText() } returns createJson()

        testSubject.removeAllVenueVisits()
        verify { file.delete() }
    }

    @Test
    fun `remove venue visit with index`() = runBlocking {
        every { encryptedFile.readText() } returns createJson()

        testSubject.removeVenueVisit(0)
        verify { file.delete() }
        verify { encryptedFile.writeText("[]") }
    }

    @Test
    fun `remove last venue visit`() = runBlocking {
        every { encryptedFile.readText() } returns createJson()

        testSubject.removeLastVisit()
        verify { file.delete() }
        verify { encryptedFile.writeText("[]") }
    }

    @Test
    fun `remove last venue visit with empty list`() = runBlocking {
        every { encryptedFile.readText() } returns "[]"

        testSubject.removeLastVisit()
        verify(exactly = 0) { encryptedFile.writeText(any()) }
    }

    @Test
    fun `test set visits`() = runBlocking {
        every { encryptedFile.readText() } returns ""

        testSubject.setVisits(listOf(VENUE_VISIT))
        verify { file.delete() }
        verify { encryptedFile.writeText(createJson()) }
    }

    @Test
    fun `mark visit as risky no match`() = runBlocking {
        every { encryptedFile.readText() } returns createJson()

        testSubject.markAsWasInRiskyList(listOf())
        verify { file.delete() }
        verify { encryptedFile.writeText(createJson()) }
    }

    @Test
    fun `mark visit as risky matching`() = runBlocking {
        every { encryptedFile.readText() } returns createJson()

        testSubject.markAsWasInRiskyList(listOf(VENUE_VISIT.venue.id))
        verify { file.delete() }
        verify { encryptedFile.writeText(createJson(wasInRiskyList = true)) }
    }

    @Test
    fun `end visit and start new one`() = runBlocking {
        every { encryptedFile.readText() } returns createJson()

        testSubject.finishLastVisitAndAddNewVenue(
            Venue("3", "opn"),
            Clock.fixed(Instant.parse(AFTERNOON), ZoneOffset.UTC)
        )

        val expectedJson =
            """
            [{"venue":{"id":"2","opn":"organizationPartName"},"from":"$MORNING","to":"2014-12-21T15:30:00Z","wasInRiskyList":false},{"venue":{"id":"3","opn":"opn"},"from":"$AFTERNOON","to":"$END_OF_DAY","wasInRiskyList":false}]
            """.trimIndent()

        verify { encryptedFile.writeText(expectedJson) }
    }

    @Test
    fun `end visit and start new one with empty list`() = runBlocking {
        every { encryptedFile.readText() } returns "[]"

        testSubject.finishLastVisitAndAddNewVenue(
            Venue("3", "opn"),
            Clock.fixed(Instant.parse(AFTERNOON), ZoneOffset.UTC)
        )

        val expectedJson =
            """
            [{"venue":{"id":"3","opn":"opn"},"from":"$AFTERNOON","to":"$END_OF_DAY","wasInRiskyList":false}]
            """.trimIndent()

        verify { encryptedFile.writeText(expectedJson) }
    }

    @Test
    fun `end visit and start new one after the other has ended`() = runBlocking {
        every { encryptedFile.readText() } returns createJson(
            start = MORNING,
            end = AFTERNOON_ROUNDED_UP
        )

        testSubject.finishLastVisitAndAddNewVenue(
            Venue("3", "opn"),
            Clock.fixed(Instant.parse(EVENING), ZoneOffset.UTC)
        )

        val expectedJson =
            """
            [{"venue":{"id":"2","opn":"organizationPartName"},"from":"$MORNING","to":"$AFTERNOON_ROUNDED_UP","wasInRiskyList":false},{"venue":{"id":"3","opn":"opn"},"from":"$EVENING","to":"$END_OF_DAY","wasInRiskyList":false}]
            """.trimIndent()

        verify { encryptedFile.writeText(expectedJson) }
    }

    companion object {
        fun createJson(
            start: String = MORNING,
            end: String = END_OF_DAY,
            wasInRiskyList: Boolean = false
        ) =
            """
            [{"venue":{"id":"2","opn":"organizationPartName"},"from":"$start","to":"$end","wasInRiskyList":$wasInRiskyList}]
            """.trimIndent()

        private const val MORNING = "2014-12-21T10:15:00Z"
        private const val AFTERNOON = "2014-12-21T15:15:00Z"
        private const val AFTERNOON_ROUNDED_UP = "2014-12-21T15:15:00Z"
        private const val EVENING = "2014-12-21T20:15:00Z"
        private const val END_OF_DAY = "2014-12-22T00:00:00Z"

        private val VENUE_VISIT = VenueVisit(
            Venue("2", "organizationPartName"),
            Instant.parse(MORNING),
            Instant.parse(END_OF_DAY),
            false
        )
    }
}
