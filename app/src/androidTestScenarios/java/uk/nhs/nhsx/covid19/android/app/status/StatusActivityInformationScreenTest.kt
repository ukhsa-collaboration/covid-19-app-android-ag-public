package uk.nhs.nhsx.covid19.android.app.status

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.notifications.RiskyVenueAlert
import uk.nhs.nhsx.covid19.android.app.qrcode.Venue
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.remote.data.MessageType.INFORM
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.VenueAlertInformRobot
import java.time.Instant

class StatusActivityInformationScreenTest : EspressoTest() {

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
    fun venueScreenShowingCorrectly() = notReported {
        testAppContext.getRiskyVenueAlertProvider().riskyVenueAlert = RiskyVenueAlert("1", INFORM)

        startTestActivity<StatusActivity> { }

        venueAlertInformRobot.checkVenueTitleIsDisplayed()
    }
}
