package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import android.content.Context
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matchers
import org.hamcrest.core.AllOf.allOf
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.qrcode.uiDate
import uk.nhs.nhsx.covid19.android.app.testhelpers.clickChildViewWithId
import uk.nhs.nhsx.covid19.android.app.testhelpers.withViewAtPosition
import uk.nhs.nhsx.covid19.android.app.util.uiFormat
import java.time.LocalDate

class VenueHistoryRobot(
    private val context: Context
) {

    fun checkActivityIsDisplayed() {
        onView(
            Matchers.allOf(
                Matchers.instanceOf(AppCompatTextView::class.java),
                ViewMatchers.withParent(withId(R.id.toolbar))
            )
        ).check(matches(withText(R.string.title_venue_history)))
    }

    fun checkEditButtonIsDisplayed() {
        onView(withId(R.id.menuEditAction))
            .check(matches(withText(R.string.edit)))
    }

    fun checkDoneButtonIsDisplayed() {
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

    fun checkDateIsDisplayedAtPosition(date: LocalDate, position: Int) {
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

    fun checkConfirmDeletionDialogIsDisplayed() {
        onView(withId(android.R.id.button1))
            .inRoot(RootMatchers.isDialog())
            .check(matches(isDisplayed()))
    }

    fun clickConfirmDeletionInDialog() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val uiDevice = UiDevice.getInstance(instrumentation)

        // Search for correct button in the dialog.
        val confirmString = instrumentation.targetContext.getString(R.string.confirm)
        val buttonUpperCase = uiDevice.findObject(UiSelector().text(confirmString.toUpperCase()))
        val button = uiDevice.findObject(UiSelector().text(confirmString))
        if (buttonUpperCase.exists() && buttonUpperCase.isEnabled) {
            buttonUpperCase.click()
        } else if (button.exists() && button.isEnabled) {
            button.click()
        }
    }

    fun checkEmptyStateIsDisplayed() {
        onView(withId(R.id.venueHistoryEmpty))
            .check(matches(isDisplayed()))
        onView(withId(R.id.venueHistoryList))
            .check(matches(not(isDisplayed())))
    }
}
