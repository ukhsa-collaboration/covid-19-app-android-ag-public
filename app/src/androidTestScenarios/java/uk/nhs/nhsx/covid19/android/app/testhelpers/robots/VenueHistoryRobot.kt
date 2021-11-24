package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import android.content.Context
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.CoreMatchers.not
import org.hamcrest.core.AllOf.allOf
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.qrcode.uiDate
import uk.nhs.nhsx.covid19.android.app.testhelpers.clickChildViewWithId
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.waitFor
import uk.nhs.nhsx.covid19.android.app.testhelpers.withViewAtPosition
import uk.nhs.nhsx.covid19.android.app.util.uiFormat
import java.time.LocalDate

class VenueHistoryRobot(
    private val context: Context
) : HasActivity {

    override val containerId: Int
        get() = R.id.venueHistoryContainer

    fun checkEditButtonIsDisplayed() = waitFor {
        onView(withId(R.id.menuEditAction))
            .check(matches(withText(R.string.edit)))
    }

    fun checkDoneButtonIsDisplayed() = waitFor {
        onView(withId(R.id.menuEditAction))
            .check(matches(withText(R.string.done_button_text)))
    }

    fun checkDoneButtonIsNotDisplayed() {
        onView(withId(R.id.menuEditAction))
            .check(doesNotExist())
    }

    fun clickEditButton() {
        onView(withId(R.id.menuEditAction))
            .perform(click())
    }

    fun checkDateIsDisplayedAtPosition(date: LocalDate, position: Int) = waitFor {
        val formattedDate = date.uiFormat(context)
        onView(withId(R.id.venueHistoryList))
            .check(
                matches(
                    withViewAtPosition(
                        position,
                        hasDescendant(
                            allOf(withId(R.id.dateHeader), withText(formattedDate), isDisplayed())
                        )
                    )
                )
            )
    }

    fun checkVisitIsDisplayedAtPosition(venueVisit: VenueVisit, position: Int) {
        onView(withId(R.id.venueHistoryList))
            .check(
                matches(
                    withViewAtPosition(
                        position,
                        allOf(
                            hasDescendant(
                                allOf(
                                    withId(R.id.textVenueName),
                                    withText(venueVisit.venue.organizationPartName),
                                    isDisplayed()
                                )
                            ),
                            hasDescendant(
                                allOf(
                                    withId(R.id.textVenuePostCode),
                                    if (venueVisit.venue.postCode == null)
                                        withText(R.string.venue_history_postcode_unavailable)
                                    else withText(venueVisit.venue.postCode),
                                    isDisplayed()
                                )
                            ),
                            hasDescendant(
                                allOf(withId(R.id.textVenueId), withText(venueVisit.venue.id), isDisplayed())
                            ),
                            hasDescendant(
                                allOf(withId(R.id.textDate), withText(venueVisit.uiDate(context)), isDisplayed())
                            )
                        )
                    )
                )
            )
    }

    fun checkDeleteIconIsDisplayedAtPosition(position: Int) {
        onView(withId(R.id.venueHistoryList))
            .check(
                matches(
                    withViewAtPosition(
                        position,
                        hasDescendant(
                            allOf(
                                withId(R.id.imageDeleteVenueVisit),
                                isDisplayed()
                            )
                        )
                    )
                )
            )
    }

    fun clickDeleteIconOnPosition(position: Int) {
        onView(withId(R.id.venueHistoryList))
            .perform(
                actionOnItemAtPosition<ViewHolder>(
                    position,
                    clickChildViewWithId(R.id.imageDeleteVenueVisit)
                )
            )
    }

    fun checkConfirmDeletionDialogIsDisplayed() = waitFor {
        onView(withText(R.string.delete_single_venue_visit_title))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }

    fun clickConfirmDeletionInDialog() {
        clickDialogPositiveButton()
    }

    fun checkEmptyStateIsDisplayed() {
        onView(withId(R.id.venueHistoryEmpty))
            .check(matches(isDisplayed()))
        onView(withId(R.id.venueHistoryList))
            .check(matches(not(isDisplayed())))
    }
}
