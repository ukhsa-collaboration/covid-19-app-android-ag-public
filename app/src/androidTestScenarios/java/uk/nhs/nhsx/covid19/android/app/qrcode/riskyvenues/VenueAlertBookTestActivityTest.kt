package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowVenueAlert
import uk.nhs.nhsx.covid19.android.app.qrcode.Venue
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.remote.data.MessageType.BOOK_TEST
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestOrderingRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.VenueAlertBookTestRobot
import java.time.Instant
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VenueAlertBookTestActivityTest : EspressoTest() {

    private val venueAlertBookTestRobot = VenueAlertBookTestRobot()
    private val testOrderingRobot = TestOrderingRobot()

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
    fun venueScreenShowingCorrectly() = notReported {
        startActivity("1")

        venueAlertBookTestRobot.checkTitleIsDisplayed()
    }

    @Test
    fun venueScreenFinishesIfVenueIsNoLongerStored() = notReported {
        val activity = startActivity("UNKNOWN")

        waitFor { assertTrue(activity!!.isDestroyed) }
    }

    @Test
    fun venueScreenFinishesIfVenueIsNull() = notReported {
        val activity = startTestActivity<VenueAlertBookTestActivity>()

        waitFor { assertTrue(activity!!.isDestroyed) }
    }

    @Test
    fun venueScreenFinishesIfVenueIsEmpty() = notReported {
        val activity = startActivity("")

        waitFor { assertTrue(activity!!.isDestroyed) }
    }

    @Test
    fun venueScreenFinishedWhenClickingIllDoItLater() = notReported {
        testAppContext.getUserInbox().addUserInboxItem(ShowVenueAlert("1", BOOK_TEST))
        val activity = startActivity("1")

        venueAlertBookTestRobot.checkTitleIsDisplayed()
        venueAlertBookTestRobot.clickIllDoItLaterButton()

        waitFor { assertTrue(activity!!.isDestroyed) }

        val item = testAppContext.getUserInbox().fetchInbox()
        assertNull(item)
    }

    @Test
    fun venueScreenLeadsToBookTestScreenWhenClickingBookTest() = notReported {
        testAppContext.getUserInbox().addUserInboxItem(ShowVenueAlert("1", BOOK_TEST))
        startActivity("1")
        venueAlertBookTestRobot.checkTitleIsDisplayed()
        venueAlertBookTestRobot.clickBookTestButton()
        testOrderingRobot.checkActivityIsDisplayed()

        val item = testAppContext.getUserInbox().fetchInbox()
        assertNull(item)
    }

    @Test
    fun venueScreenFinishesWhenClickingBackButton() = notReported {
        testAppContext.getUserInbox().addUserInboxItem(ShowVenueAlert("1", BOOK_TEST))

        val activity = startActivity("1")

        testAppContext.device.pressBack()

        waitFor { assertTrue(activity!!.isDestroyed) }

        val item = testAppContext.getUserInbox().fetchInbox()
        assertNull(item)
    }

    private fun startActivity(venueId: String) =
        startTestActivity<VenueAlertBookTestActivity> {
            putExtra(VenueAlertBookTestActivity.EXTRA_VENUE_ID, venueId)
        }
}
