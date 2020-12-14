package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.qrcode.Venue
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.VenueAlertRobot
import java.time.Instant
import kotlin.test.assertTrue

class VenueAlertActivityTest : EspressoTest() {

    private val venueAlertRobot = VenueAlertRobot()
    private val visits = listOf(
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

    @Before
    fun setUp() = runBlocking {
        testAppContext.getVisitedVenuesStorage().setVisits(visits)
    }

    @Test
    fun venueScreenShowingCorrectly() = notReported {
        startTestActivity<VenueAlertActivity> {
            putExtra(
                VenueAlertActivity.EXTRA_VENUE_ID,
                "1"
            )
        }

        venueAlertRobot.checkVenueTitleIsDisplayed()
    }

    @Test
    fun venueScreenFinishesIfVenueIsNoLongerStored() = notReported {
        val activity = startTestActivity<VenueAlertActivity> {
            putExtra(
                VenueAlertActivity.EXTRA_VENUE_ID,
                "UNKNOWN"
            )
        }

        waitFor { assertTrue(activity!!.isDestroyed) }
    }

    @Test
    fun venueScreenFinishesIfVenueIsNull() = notReported {
        val activity = startTestActivity<VenueAlertActivity>()

        waitFor { assertTrue(activity!!.isDestroyed) }
    }

    @Test
    fun venueScreenFinishesIfVenueIsEmpty() = notReported {
        val activity = startTestActivity<VenueAlertActivity> {
            putExtra(
                VenueAlertActivity.EXTRA_VENUE_ID,
                ""
            )
        }

        waitFor { assertTrue(activity!!.isDestroyed) }
    }
}
