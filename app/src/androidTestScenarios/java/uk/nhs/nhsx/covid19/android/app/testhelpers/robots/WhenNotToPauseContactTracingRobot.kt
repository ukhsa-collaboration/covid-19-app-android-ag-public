package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import uk.nhs.nhsx.covid19.android.app.R

class WhenNotToPauseContactTracingRobot {
    fun checkActivityIsDisplayed() {
        checkActivityTitleIsDisplayed(R.string.when_not_to_pause_contact_tracing_title)
    }

    fun pressBackArrow() {
        onView(withContentDescription(R.string.go_back))
            .perform(click())
    }
}
