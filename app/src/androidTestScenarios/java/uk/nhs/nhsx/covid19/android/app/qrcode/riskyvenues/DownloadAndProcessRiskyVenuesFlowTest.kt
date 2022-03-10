package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxItem
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.UserInboxItem.ShowVenueAlert
import uk.nhs.nhsx.covid19.android.app.qrcode.Venue
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType.BOOK_TEST
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType.INFORM
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenue
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenuesResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyWindow
import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DownloadAndProcessRiskyVenuesFlowTest : EspressoTest() {

    private val venue1 = Venue(
        id = "3KR9JX59",
        organizationPartName = "Venue1"
    )

    private val venue2 = Venue(
        id = "2V542M5J",
        organizationPartName = "Venue2"
    )

    private val venue3 = Venue(
        id = "MX5X235W",
        organizationPartName = "Venue3"
    )

    private val venue4 = Venue(
        id = "94RK34RY",
        organizationPartName = "Venue4"
    )

    private val venue1Instant = Instant.parse("2020-08-02T00:00:00Z")

    private val venue2Instant = Instant.parse("2020-08-02T02:00:00Z")

    private val venue3Instant = Instant.parse("2020-08-02T04:00:00Z")

    private val venue4Instant = Instant.parse("2020-08-02T06:00:00Z")

    private val riskyVenues1to2 = listOf(
        RiskyVenue(
            venue1.id,
            RiskyWindow(
                from = Instant.parse("2020-08-01T00:00:00Z"),
                to = Instant.parse("2020-08-30T23:59:59Z")
            ),
            messageType = INFORM
        ),
        RiskyVenue(
            venue2.id,
            RiskyWindow(
                from = Instant.parse("2020-08-01T00:00:00Z"),
                to = Instant.parse("2020-08-30T23:59:59Z")
            ),
            messageType = BOOK_TEST
        )
    )

    private val riskyVenues3 = riskyVenues1to2 + listOf(
        RiskyVenue(
            venue3.id,
            RiskyWindow(
                from = Instant.parse("2020-08-01T00:00:00Z"),
                to = Instant.parse("2020-08-30T23:59:59Z")
            ),
            messageType = INFORM
        )
    )

    private val riskyVenues4 = riskyVenues3 + listOf(
        RiskyVenue(
            venue4.id,
            RiskyWindow(
                from = Instant.parse("2020-08-01T00:00:00Z"),
                to = Instant.parse("2020-08-30T23:59:59Z")
            ),
            messageType = INFORM
        )
    )

    @Before
    fun setUp() {
        testAppContext.setLocalAuthority(TestApplicationContext.ENGLISH_LOCAL_AUTHORITY)
    }

    @After
    fun tearDown() {
        testAppContext.clock.reset()
    }

    @Test
    fun visitAndMarkRiskyMultipleVenues() {
        runBlocking {
            val visitedVenuesStorage = testAppContext.getVisitedVenuesStorage()
            val downloadAndProcessRiskyVenues = testAppContext.getDownloadAndProcessRiskyVenues()
            val userInbox = testAppContext.getUserInbox()

            visitedVenuesStorage.removeAllVenueVisits()

            testAppContext.clock.currentInstant = venue1Instant
            visitedVenuesStorage
                .finishLastVisitAndAddNewVenue(venue1)

            testAppContext.clock.currentInstant = venue2Instant
            visitedVenuesStorage
                .finishLastVisitAndAddNewVenue(venue2)

            testAppContext.clock.currentInstant = venue3Instant
            visitedVenuesStorage
                .finishLastVisitAndAddNewVenue(venue3)

            testAppContext.clock.currentInstant = venue4Instant
            visitedVenuesStorage
                .finishLastVisitAndAddNewVenue(venue4)

            testAppContext.riskyVenuesApi.riskyVenuesResponse =
                RiskyVenuesResponse(venues = riskyVenues1to2)

            downloadAndProcessRiskyVenues.invoke(clearOutdatedVisits = false)

            val alert1 = userInbox.fetchInbox() as UserInboxItem
            assertEquals(ShowVenueAlert(venue2.id, BOOK_TEST), alert1)
            testAppContext.getRiskyVenueAlertProvider().riskyVenueAlert = null

            testAppContext.riskyVenuesApi.riskyVenuesResponse =
                RiskyVenuesResponse(venues = riskyVenues3)

            downloadAndProcessRiskyVenues.invoke(clearOutdatedVisits = false)

            val alert2 = userInbox.fetchInbox() as UserInboxItem
            assertEquals(ShowVenueAlert(venue3.id, INFORM), alert2)
            testAppContext.getRiskyVenueAlertProvider().riskyVenueAlert = null

            testAppContext.riskyVenuesApi.riskyVenuesResponse =
                RiskyVenuesResponse(venues = riskyVenues4)

            downloadAndProcessRiskyVenues.invoke(clearOutdatedVisits = false)

            val alert3 = userInbox.fetchInbox() as UserInboxItem
            assertEquals(ShowVenueAlert(venue4.id, INFORM), alert3)
            testAppContext.getRiskyVenueAlertProvider().riskyVenueAlert = null

            downloadAndProcessRiskyVenues.invoke(clearOutdatedVisits = false)

            assertNull(userInbox.fetchInbox())
        }
    }

    @Test
    fun visitBookTestTypeRiskyVenue() {
        runBlocking {
            val visitedVenuesStorage = testAppContext.getVisitedVenuesStorage()
            val downloadAndProcessRiskyVenues = testAppContext.getDownloadAndProcessRiskyVenues()
            val lastVisitedBookTestTypeVenueDate = testAppContext.getLastVisitedBookTestTypeVenueDateProvider()

            visitedVenuesStorage.removeAllVenueVisits()

            val venue = Venue(
                id = "74ZK34RY",
                organizationPartName = "Venue"
            )

            val bookTestTypeRiskyVenue = RiskyVenue(
                venue.id,
                RiskyWindow(
                    from = Instant.parse("2020-08-01T15:00:00Z"),
                    to = Instant.parse("2020-08-02T00:00:00Z")
                ),
                messageType = BOOK_TEST
            )

            testAppContext.clock.currentInstant = Instant.parse("2020-08-01T20:00:00Z")

            val initialLatestDate = lastVisitedBookTestTypeVenueDate.lastVisitedVenue?.latestDate
            assertNull(initialLatestDate)

            visitedVenuesStorage.finishLastVisitAndAddNewVenue(venue)

            testAppContext.riskyVenuesApi.riskyVenuesResponse =
                RiskyVenuesResponse(venues = listOf(bookTestTypeRiskyVenue))

            downloadAndProcessRiskyVenues.invoke(clearOutdatedVisits = false)

            val latestDate = lastVisitedBookTestTypeVenueDate.lastVisitedVenue?.latestDate
            assertEquals(LocalDate.parse("2020-08-01"), latestDate)
        }
    }
}
