package uk.nhs.nhsx.covid19.android.app.about

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.qrcode.Venue
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.retry.RetryFlakyTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.VenueHistoryRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.verifyForOrientations
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
    fun testActivity() = with(venueHistoryRobot) {
        startTestActivity<VenueHistoryActivity>()

        checkActivityIsDisplayed()

        checkVisitPositions()
    }

    @Test
    @RetryFlakyTest
    fun testDeletion() = with(venueHistoryRobot) {
        startTestActivity<VenueHistoryActivity>()
        checkActivityIsDisplayed()

        checkVisitPositions()

        checkEditButtonIsDisplayed()
        clickEditButton()
        checkDoneButtonIsDisplayed()

        checkVisitPositions()

        checkDeleteIconIsDisplayedAtPosition(1)
        checkDeleteIconIsDisplayedAtPosition(2)
        checkDeleteIconIsDisplayedAtPosition(4)
        checkDeleteIconIsDisplayedAtPosition(5)

        clickDeleteIconOnPosition(1)

        verifyForOrientations {
            checkConfirmDeletionDialogIsDisplayed()
        }

        clickConfirmDeletionInDialog()

        checkDoneButtonIsDisplayed()

        deleteVenueVisitOnPosition(1)

        checkDateIsDisplayedAtPosition(LocalDate.parse("2020-07-25"), 0)
        checkVisitIsDisplayedAtPosition(visitB, 1)
        checkVisitIsDisplayedAtPosition(visitA, 2)

        deleteVenueVisitOnPosition(1)

        deleteVenueVisitOnPosition(1)

        checkDoneButtonIsNotDisplayed()

        checkEmptyStateIsDisplayed()
    }

    private fun checkVisitPositions() = with(venueHistoryRobot) {
        checkDateIsDisplayedAtPosition(LocalDate.parse("2020-07-26"), 0)
        checkVisitIsDisplayedAtPosition(visitD, 1)
        checkVisitIsDisplayedAtPosition(visitC, 2)
        checkDateIsDisplayedAtPosition(LocalDate.parse("2020-07-25"), 3)
        checkVisitIsDisplayedAtPosition(visitB, 4)
        checkVisitIsDisplayedAtPosition(visitA, 5)
    }

    private fun deleteVenueVisitOnPosition(position: Int) = with(venueHistoryRobot) {
        clickDeleteIconOnPosition(position)

        checkConfirmDeletionDialogIsDisplayed()

        clickConfirmDeletionInDialog()
    }
}
