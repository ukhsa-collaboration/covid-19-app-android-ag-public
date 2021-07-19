package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.notifications.RiskyVenueAlert
import uk.nhs.nhsx.covid19.android.app.qrcode.Venue
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType.INFORM
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.VenueAlertInformRobot
import java.time.Instant
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VenueAlertInformActivityTest : EspressoTest() {

    private val venueAlertInformRobot = VenueAlertInformRobot()

    private val visits = listOf(
        VenueVisit(
            venue = Venue("1", "Venue1"),
            from = Instant.parse("2020-07-25T10:00:00Z"),
            to = Instant.parse("2020-07-25T12:00:00Z")
        )
    )

    @Before
    fun setUp() = runBlocking {
        testAppContext.getVisitedVenuesStorage().setVisits(visits)
    }

    @Test
    fun venueScreenShowingCorrectly() {
        startActivity("1")

        venueAlertInformRobot.checkVenueTitleIsDisplayed()
    }

    @Test
    fun venueScreenFinishesIfVenueIsNoLongerStored() {
        val activity = startActivity("UNKNOWN")

        waitFor { assertTrue(activity!!.isDestroyed) }
    }

    @Test
    fun venueScreenFinishesIfVenueIsNull() {
        val activity = startTestActivity<VenueAlertInformActivity>()

        waitFor { assertTrue(activity!!.isDestroyed) }
    }

    @Test
    fun venueScreenFinishesIfVenueIsEmpty() {
        val activity = startActivity("")

        waitFor { assertTrue(activity!!.isDestroyed) }
    }

    @Test
    fun venueScreenFinishesWhenClickingReturnHome() {
        testAppContext.getRiskyVenueAlertProvider().riskyVenueAlert = RiskyVenueAlert("1", INFORM)

        val activity = startActivity("1")
        venueAlertInformRobot.clickReturnHomeButton()

        waitFor { assertTrue(activity!!.isDestroyed) }

        val item = testAppContext.getUserInbox().fetchInbox()
        assertNull(item)
    }

    @Test
    fun venueScreenFinishesWhenClickingBackButton() {
        testAppContext.getRiskyVenueAlertProvider().riskyVenueAlert = RiskyVenueAlert("1", INFORM)

        val activity = startActivity("1")

        testAppContext.device.pressBack()

        waitFor { assertTrue(activity!!.isDestroyed) }

        val item = testAppContext.getUserInbox().fetchInbox()
        assertNull(item)
    }

    private fun startActivity(venueId: String) =
        startTestActivity<VenueAlertInformActivity> {
            putExtra(VenueAlertInformActivity.EXTRA_VENUE_ID, venueId)
        }
}
