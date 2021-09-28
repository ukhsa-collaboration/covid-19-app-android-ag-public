package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.notifications.RiskyVenueAlert
import uk.nhs.nhsx.covid19.android.app.qrcode.Venue
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType.BOOK_TEST
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SymptomsAfterRiskyVenueRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestOrderingRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.VenueAlertBookTestRobot
import java.time.Instant
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VenueAlertBookTestActivityTest : EspressoTest() {

    private val venueAlertBookTestRobot = VenueAlertBookTestRobot()
    private val testOrderingRobot = TestOrderingRobot()
    private val symptomsAfterRiskyVenueVisitRobot = SymptomsAfterRiskyVenueRobot()
    private val isolationHelper = IsolationHelper(testAppContext.clock)

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

        venueAlertBookTestRobot.checkActivityIsDisplayed()
    }

    @Test
    fun venueScreenFinishesIfVenueIsNoLongerStored() {
        val activity = startActivity("UNKNOWN")

        waitFor { assertTrue(activity!!.isDestroyed) }
    }

    @Test
    fun venueScreenFinishesIfVenueIsNull() {
        val activity = startTestActivity<VenueAlertBookTestActivity>()

        waitFor { assertTrue(activity!!.isDestroyed) }
    }

    @Test
    fun venueScreenFinishesIfVenueIsEmpty() {
        val activity = startActivity("")

        waitFor { assertTrue(activity!!.isDestroyed) }
    }

    @Test
    fun venueScreenFinishedWhenClickingIllDoItLater() {
        testAppContext.getRiskyVenueAlertProvider().riskyVenueAlert = RiskyVenueAlert("1", BOOK_TEST)

        val activity = startActivity("1")

        venueAlertBookTestRobot.checkActivityIsDisplayed()
        venueAlertBookTestRobot.clickIllDoItLaterButton()

        waitFor { assertTrue(activity!!.isDestroyed) }

        val item = testAppContext.getUserInbox().fetchInbox()
        assertNull(item)
    }

    @Test
    fun venueScreenFinishedWhenClickingCloseButton() {
        testAppContext.getRiskyVenueAlertProvider().riskyVenueAlert = RiskyVenueAlert("1", BOOK_TEST)

        val activity = startActivity("1")

        venueAlertBookTestRobot.checkActivityIsDisplayed()
        venueAlertBookTestRobot.clickCloseButton()

        waitFor { assertTrue(activity!!.isDestroyed) }

        val item = testAppContext.getUserInbox().fetchInbox()
        assertNull(item)
    }

    @Test
    fun whenActiveIndexCaseIsolation_clickingBookTest_navigateToBookTestScreen() {
        testAppContext.getRiskyVenueAlertProvider().riskyVenueAlert = RiskyVenueAlert("1", BOOK_TEST)

        testAppContext.setState(isolationHelper.selfAssessment().asIsolation())

        startActivity("1")

        venueAlertBookTestRobot.checkActivityIsDisplayed()
        venueAlertBookTestRobot.clickBookTestButton()

        testOrderingRobot.checkActivityIsDisplayed()

        val item = testAppContext.getUserInbox().fetchInbox()
        assertNull(item)
    }

    @Test
    fun whenNotIsolating_clickingBookTest_navigateToSymptomsAfterRiskyVenueVisit() {
        testAppContext.getRiskyVenueAlertProvider().riskyVenueAlert = RiskyVenueAlert("1", BOOK_TEST)

        startActivity("1")

        venueAlertBookTestRobot.checkActivityIsDisplayed()
        venueAlertBookTestRobot.clickBookTestButton()

        symptomsAfterRiskyVenueVisitRobot.checkActivityIsDisplayed()

        val item = testAppContext.getUserInbox().fetchInbox()
        assertNull(item)
    }

    @Test
    fun whenIsolatingAsContactCase_clickingBookTest_navigateToSymptomsAfterRiskyVenueVisit() {
        testAppContext.getRiskyVenueAlertProvider().riskyVenueAlert = RiskyVenueAlert("1", BOOK_TEST)

        startActivity("1")

        venueAlertBookTestRobot.checkActivityIsDisplayed()
        venueAlertBookTestRobot.clickBookTestButton()

        symptomsAfterRiskyVenueVisitRobot.checkActivityIsDisplayed()

        val item = testAppContext.getUserInbox().fetchInbox()
        assertNull(item)
    }

    @Test
    fun venueScreenFinishesWhenClickingBackButton() {
        testAppContext.getRiskyVenueAlertProvider().riskyVenueAlert = RiskyVenueAlert("1", BOOK_TEST)

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
