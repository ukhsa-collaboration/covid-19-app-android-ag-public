package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import uk.nhs.nhsx.covid19.android.app.R

class SymptomsAfterRiskyVenueRobot {
    fun checkActivityIsDisplayed() {
        checkActivityTitleIsDisplayed(R.string.symptoms_after_risky_venue_title)
    }

    fun clickHasSymptomsButton() {
        onView(withId(R.id.hasSymptomsButton))
            .perform(scrollTo(), click())
    }

    fun clickNoSymptomsButton() {
        onView(withId(R.id.noSymptomsButton))
            .perform(scrollTo(), click())
    }

    fun clickCancelButton() {
        onView(ViewMatchers.withContentDescription(R.string.cancel)).perform(click())
    }

    fun checkCancelDialogIsDisplayed() {
        onView(withText(R.string.symptoms_after_risky_venue_cancel_dialog_title))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }

    fun checkCancelDialogIsNotDisplayed() {
        onView(withText(R.string.symptoms_after_risky_venue_cancel_dialog_title))
            .check(doesNotExist())
    }

    fun clickDialogLeaveButton() {
        clickDialogPositiveButton()
    }

    fun clickDialogStayButton() {
        clickDialogNegativeButton()
    }
}
