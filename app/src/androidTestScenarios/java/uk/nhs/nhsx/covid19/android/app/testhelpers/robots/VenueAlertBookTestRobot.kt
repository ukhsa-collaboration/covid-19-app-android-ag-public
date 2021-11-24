package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.R.string
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class VenueAlertBookTestRobot : HasActivity {
    override val containerId: Int
        get() = R.id.venueAlertM2title

    fun clickIllDoItLaterButton() {
        onView(withId(R.id.buttonReturnToHomeScreen))
            .perform(scrollTo(), click())
    }

    fun clickBookTestButton() {
        onView(withId(R.id.buttonBookTest))
            .perform(scrollTo(), click())
    }

    fun clickCloseButton() {
        onView(withContentDescription(string.close)).perform(click())
    }
}
