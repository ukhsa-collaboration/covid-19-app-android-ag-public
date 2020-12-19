package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.squareup.moshi.Moshi.Builder
import kotlinx.coroutines.runBlocking
import org.json.JSONException
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uk.nhs.nhsx.covid19.android.app.qrcode.Venue
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.state.StateJson
import uk.nhs.nhsx.covid19.android.app.testhelpers.MockClock
import uk.nhs.nhsx.covid19.android.app.util.EncryptionUtils
import uk.nhs.nhsx.covid19.android.app.util.adapters.InstantAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.LocalDateAdapter
import uk.nhs.nhsx.covid19.android.app.util.roundDownToNearestQuarter
import uk.nhs.nhsx.covid19.android.app.util.roundUpToNearestQuarter
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

@RunWith(AndroidJUnit4::class)
class VisitedVenuesStorageIntegrationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val moshi = Builder()
        .add(StateJson.stateMoshiAdapter)
        .add(LocalDateAdapter())
        .add(InstantAdapter())
        .build()

    private val from = Instant.parse("2014-12-21T10:23:00Z")
    private val fromRounded = from.roundDownToNearestQuarter()
    private val clock = MockClock(currentInstant = from)

    private val encryptedFileInfo = EncryptionUtils.createEncryptedFile(context, "venues")
    private val file = encryptedFileInfo.file

    private val testSubject = VisitedVenuesStorage(
        moshi,
        encryptedFileInfo,
        clock
    )

    @Before
    fun setUp() {
        file.delete()
        clock.currentInstant = from
    }

    @Test
    fun addingVenueCreatesVenuesFile() = runBlocking {
        testSubject.finishLastVisitAndAddNewVenue(VENUE_1)

        assertTrue(file.exists())
    }

    @Test
    fun addedVenueCanBeReadBack() = runBlocking {
        testSubject.finishLastVisitAndAddNewVenue(VENUE_1)

        val visits = testSubject.getVisits()
        assertEquals(1, visits.size)
        assertEquals(VENUE_1, visits[0].venue)
        assertEquals(fromRounded, visits[0].from)
    }

    @Test
    fun deletingFileClearsVenuesStorage() = runBlocking {
        testSubject.finishLastVisitAndAddNewVenue(VENUE_1)

        file.delete()

        val venues = testSubject.getVisits()
        assertEquals(0, venues.size)
    }

    @Test
    fun addingVenueToExistingFileUpdatesEncryptedFile() = runBlocking {
        testSubject.finishLastVisitAndAddNewVenue(VENUE_1)
        testSubject.finishLastVisitAndAddNewVenue(VENUE_2)

        val visits = testSubject.getVisits()
        assertEquals(2, visits.size)
        assertEquals(VENUE_1, visits[0].venue)
        assertEquals(fromRounded, visits[0].from)
        assertEquals(VENUE_2, visits[1].venue)
        assertEquals(fromRounded, visits[1].from)
    }

    @Test
    fun addingVenueSetsToDateToTomorrowMidnight() = runBlocking {
        testSubject.finishLastVisitAndAddNewVenue(VENUE_1)
        val nextMidnight = Instant.parse("2014-12-22T00:00:00Z")

        val visits = testSubject.getVisits()
        assertEquals(nextMidnight, visits[0].to)
    }

    @Test
    fun addingVenueUpdatesLastVenueToTimestamp() = runBlocking {
        testSubject.finishLastVisitAndAddNewVenue(VENUE_1)

        val nextCheckInTime = Instant.parse("2014-12-21T10:01:00Z")
        clock.currentInstant = nextCheckInTime
        testSubject.finishLastVisitAndAddNewVenue(VENUE_2)

        val visits = testSubject.getVisits()
        val checkOutTime = nextCheckInTime.roundUpToNearestQuarter()
        assertEquals(checkOutTime, visits[0].to)
    }

    @Test
    fun markVenuesAsInRiskyListWhenVenueWasRisky() = runBlocking {
        testSubject.setVisits(
            listOf(
                VenueVisit(
                    venue = Venue("1", "Venue1"),
                    from = Instant.parse("2020-07-25T10:00:00Z"),
                    to = Instant.parse("2020-07-25T12:00:00Z")
                ),
                VenueVisit(
                    venue = Venue("2", "Venue2"),
                    from = Instant.parse("2020-07-25T10:00:00Z"),
                    to = Instant.parse("2020-07-25T12:00:00Z")
                )
            )
        )

        testSubject.markAsWasInRiskyList(listOf("1"))

        assertEquals(true, testSubject.getVisits()[0].wasInRiskyList)
        assertEquals(false, testSubject.getVisits()[1].wasInRiskyList)
    }

    @Test
    fun markVenuesAsInRiskyListWhenNoVenueWasRisky() = runBlocking {
        testSubject.setVisits(
            listOf(
                VenueVisit(
                    venue = Venue("1", "Venue1"),
                    from = Instant.parse("2020-07-25T10:00:00Z"),
                    to = Instant.parse("2020-07-25T12:00:00Z")
                ),
                VenueVisit(
                    venue = Venue("2", "Venue2"),
                    from = Instant.parse("2020-07-25T10:00:00Z"),
                    to = Instant.parse("2020-07-25T12:00:00Z")
                )
            )
        )

        testSubject.markAsWasInRiskyList(listOf("3"))

        assertEquals(false, testSubject.getVisits()[0].wasInRiskyList)
        assertEquals(false, testSubject.getVisits()[1].wasInRiskyList)
    }

    @Test
    fun setVenuesReplacesContent() = runBlocking {
        val to = Instant.parse("2014-12-21T12:39:00Z")

        testSubject.finishLastVisitAndAddNewVenue(VENUE_1)

        assertEquals(1, testSubject.getVisits().size)

        val visit = VenueVisit(VENUE_2, from, to)

        testSubject.setVisits(listOf(visit))

        val visits = testSubject.getVisits()
        assertEquals(1, visits.size)
        assertEquals(VENUE_2, visits[0].venue)
        assertEquals(from, visits[0].from)
        assertEquals(to, visits[0].to)
    }

    @Test
    fun venuesFileIsEncrypted() = runBlocking {
        testSubject.finishLastVisitAndAddNewVenue(VENUE_1)

        try {
            val rawFileContents = file.readText()
            JSONObject(rawFileContents)
            fail("File contents not encrypted")
        } catch (_: JSONException) {
        }
    }

    @Test
    fun cancelCheckInRemovesVisitFromStorage() = runBlocking {
        testSubject.finishLastVisitAndAddNewVenue(VENUE_1)
        testSubject.finishLastVisitAndAddNewVenue(VENUE_2)

        testSubject.removeLastVisit()

        val visits = testSubject.getVisits()

        assertEquals(1, visits.size)
        assertEquals(VENUE_1, visits[0].venue)
    }

    @Test
    fun callingRemoveAllVenueVisitsRemovesVisitsFile() = runBlocking {
        testSubject.finishLastVisitAndAddNewVenue(VENUE_1)

        testSubject.removeAllVenueVisits()

        val venues = testSubject.getVisits()
        assertEquals(0, venues.size)
    }

    @Test
    fun getVisitByVenueIdReturnsCorrectVenueVisit() = runBlocking {
        testSubject.finishLastVisitAndAddNewVenue(VENUE_1)

        val venueVisit = testSubject.getVisitByVenueId(VENUE_1.id)

        assertEquals(VENUE_1.id, venueVisit?.venue?.id)
    }

    companion object {
        private val VENUE_1 = Venue(
            "2",
            organizationPartName = "organizationPartName"
        )

        private val VENUE_2 = Venue(
            "3",
            organizationPartName = "organizationPartName"
        )
    }
}
