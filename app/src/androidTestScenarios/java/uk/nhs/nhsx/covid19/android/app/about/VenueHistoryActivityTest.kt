package uk.nhs.nhsx.covid19.android.app.about

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.qrcode.Venue
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.LANDSCAPE
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.PORTRAIT
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.retry.RetryFlakyTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.VenueHistoryRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.setScreenOrientation
import java.time.Instant
import java.time.LocalDate

class VenueHistoryActivityTest : EspressoTest() {

    private val venueHistoryRobot = VenueHistoryRobot(context = testAppContext.app)

    private val visitA = VenueVisit(
        venue = Venue("A", "Venue1", postCode = null),
        from = Instant.parse("2020-07-25T10:00:00Z"),
        to = Instant.parse("2020-07-25T12:00:00Z")
    )

    private val visitB = VenueVisit(
        venue = Venue("B", "Venue2", postCode = "CM1"),
        from = Instant.parse("2020-07-25T14:00:00Z"),
        to = Instant.parse("2020-07-25T16:00:00Z")
    )

    private val visitC = VenueVisit(
        venue = Venue("A", "Venue1"),
        from = Instant.parse("2020-07-26T10:00:00Z"),
        to = Instant.parse("2020-07-26T12:00:00Z")
    )

    private val visitD = VenueVisit(
        venue = Venue("B", "Venue2"),
        from = Instant.parse("2020-07-26T14:00:00Z"),
        to = Instant.parse("2020-07-26T16:00:00Z")
    )

    private val visits = listOf(visitA, visitB, visitC, visitD)

    @Before
    fun setUp() = runBlocking {
        testAppContext.getVisitedVenuesStorage().setVisits(visits)
    }

    @Test
    fun testActivity() = notReported {
        startTestActivity<VenueHistoryActivity>()

        venueHistoryRobot.checkActivityIsDisplayed()

        venueHistoryRobot.checkDateIsDisplayedAtPosition(LocalDate.parse("2020-07-26"), 0)
        venueHistoryRobot.checkVisitIsDisplayedAtPosition(visitD, 1)
        venueHistoryRobot.checkVisitIsDisplayedAtPosition(visitC, 2)
        venueHistoryRobot.checkDateIsDisplayedAtPosition(LocalDate.parse("2020-07-25"), 3)
        venueHistoryRobot.checkVisitIsDisplayedAtPosition(visitB, 4)
        venueHistoryRobot.checkVisitIsDisplayedAtPosition(visitA, 5)
    }

    @Test
    @RetryFlakyTest
    fun testDeletion() = notReported {
        startTestActivity<VenueHistoryActivity>()

        venueHistoryRobot.checkActivityIsDisplayed()

        venueHistoryRobot.checkDateIsDisplayedAtPosition(LocalDate.parse("2020-07-26"), 0)
        venueHistoryRobot.checkVisitIsDisplayedAtPosition(visitD, 1)
        venueHistoryRobot.checkVisitIsDisplayedAtPosition(visitC, 2)
        venueHistoryRobot.checkDateIsDisplayedAtPosition(LocalDate.parse("2020-07-25"), 3)
        venueHistoryRobot.checkVisitIsDisplayedAtPosition(visitB, 4)
        venueHistoryRobot.checkVisitIsDisplayedAtPosition(visitA, 5)

        waitFor { venueHistoryRobot.checkEditButtonIsDisplayed() }

        venueHistoryRobot.clickEditButton()

        waitFor { venueHistoryRobot.checkDoneButtonIsDisplayed() }

        venueHistoryRobot.checkDateIsDisplayedAtPosition(LocalDate.parse("2020-07-26"), 0)
        venueHistoryRobot.checkVisitIsDisplayedAtPosition(visitD, 1)
        venueHistoryRobot.checkVisitIsDisplayedAtPosition(visitC, 2)
        venueHistoryRobot.checkDateIsDisplayedAtPosition(LocalDate.parse("2020-07-25"), 3)
        venueHistoryRobot.checkVisitIsDisplayedAtPosition(visitB, 4)
        venueHistoryRobot.checkVisitIsDisplayedAtPosition(visitA, 5)

        venueHistoryRobot.checkDeleteIconIsDisplayedAtPosition(1)
        venueHistoryRobot.checkDeleteIconIsDisplayedAtPosition(2)
        venueHistoryRobot.checkDeleteIconIsDisplayedAtPosition(4)
        venueHistoryRobot.checkDeleteIconIsDisplayedAtPosition(5)

        venueHistoryRobot.clickDeleteIconOnPosition(1)

        waitFor { venueHistoryRobot.checkConfirmDeletionDialogIsDisplayed() }

        setScreenOrientation(LANDSCAPE)

        waitFor { venueHistoryRobot.checkConfirmDeletionDialogIsDisplayed() }

        setScreenOrientation(PORTRAIT)

        waitFor { venueHistoryRobot.checkConfirmDeletionDialogIsDisplayed() }

        venueHistoryRobot.clickConfirmDeletionInDialog()

        waitFor { venueHistoryRobot.checkDoneButtonIsDisplayed() }

        deleteVenueVisitOnPosition(1)

        waitFor { venueHistoryRobot.checkDateIsDisplayedAtPosition(LocalDate.parse("2020-07-25"), 0) }
        venueHistoryRobot.checkVisitIsDisplayedAtPosition(visitB, 1)
        venueHistoryRobot.checkVisitIsDisplayedAtPosition(visitA, 2)

        deleteVenueVisitOnPosition(1)

        deleteVenueVisitOnPosition(1)

        waitFor { venueHistoryRobot.checkDoneButtonIsNotDisplayed() }

        venueHistoryRobot.checkEmptyStateIsDisplayed()
    }

    private fun deleteVenueVisitOnPosition(position: Int) {
        venueHistoryRobot.clickDeleteIconOnPosition(position)

        waitFor { venueHistoryRobot.checkConfirmDeletionDialogIsDisplayed() }

        venueHistoryRobot.clickConfirmDeletionInDialog()
    }
}
