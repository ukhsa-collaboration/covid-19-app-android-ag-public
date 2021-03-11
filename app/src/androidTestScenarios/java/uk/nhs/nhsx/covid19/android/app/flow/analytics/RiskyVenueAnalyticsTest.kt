package uk.nhs.nhsx.covid19.android.app.flow.analytics

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.qrcode.Venue
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.remote.data.MessageType.BOOK_TEST
import uk.nhs.nhsx.covid19.android.app.remote.data.MessageType.INFORM
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenue
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenuesResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyWindow
import uk.nhs.nhsx.covid19.android.app.report.notReported
import java.time.Instant

class RiskyVenueAnalyticsTest : AnalyticsTest() {

    @Before
    override fun setUp() {
        super.setUp()
        testAppContext.getVisitedVenuesStorage().removeAllVenueVisits()
    }

    @Test
    fun receiveRiskyVenueWithTypeM1() = notReported {
        runBlocking {
            // Current date: 1st Jan
            // Starting state: App running normally
            assertAnalyticsPacketIsNormal()

            testAppContext.getVisitedVenuesStorage().setVisits(listOf(visitVenue1))
            testAppContext.riskyVenuesApi.riskyVenuesResponse = RiskyVenuesResponse(venues = riskyVenues)

            // Current date: 2nd Jan -> Analytics packet for: 1st Jan
            assertOnFields {
                assertEquals(1, Metrics::receivedRiskyVenueM1Warning)
            }

            // Current date: 3rd Jan -> Analytics packet for: 2nd Jan
            assertAnalyticsPacketIsNormal()
        }
    }

    @Test
    fun receiveRiskyVenueWithTypeM1AndM2() = notReported {
        runBlocking {
            // Current date: 1st Jan
            // Starting state: App running normally
            assertAnalyticsPacketIsNormal()

            testAppContext.getVisitedVenuesStorage().setVisits(listOf(visitVenue1, visitVenue2))
            testAppContext.riskyVenuesApi.riskyVenuesResponse = RiskyVenuesResponse(venues = riskyVenues)

            // Current date: 2nd Jan -> Analytics packet for: 1st Jan
            assertOnFields {
                assertEquals(1, Metrics::receivedRiskyVenueM2Warning)
                assertPresent(Metrics::hasReceivedRiskyVenueM2WarningBackgroundTick)
            }

            // Dates: 3rd-11th Jan -> Analytics packets for: 2nd-10th Jan
            assertOnFieldsForDateRange(3..11) {
                assertPresent(Metrics::hasReceivedRiskyVenueM2WarningBackgroundTick)
            }

            // Current date: 12th Jan -> Analytics packet for: 11th Jan
            assertAnalyticsPacketIsNormal()
        }
    }

    private val venue1 = Venue(
        id = "3KR9JX59",
        organizationPartName = "Venue1"
    )

    private val venue2 = Venue(
        id = "2V542M5J",
        organizationPartName = "Venue2"
    )

    private val visitVenue1 = VenueVisit(
        venue = venue1,
        from = Instant.parse("2020-01-01T02:00:00Z"),
        to = Instant.parse("2020-01-01T04:00:00Z")
    )

    private val visitVenue2 = VenueVisit(
        venue = venue2,
        from = Instant.parse("2020-01-01T05:00:00Z"),
        to = Instant.parse("2020-01-01T06:00:00Z")
    )

    private val riskyVenues = listOf(
        RiskyVenue(
            venue1.id,
            RiskyWindow(
                from = Instant.parse("2020-01-01T00:00:00Z"),
                to = Instant.parse("2020-01-31T23:59:59Z")
            ),
            messageType = INFORM
        ),
        RiskyVenue(
            venue2.id,
            RiskyWindow(
                from = Instant.parse("2020-01-01T00:00:00Z"),
                to = Instant.parse("2020-01-30T23:59:59Z")
            ),
            messageType = BOOK_TEST
        )
    )
}
