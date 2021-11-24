package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class ProgressRobot : HasActivity {
    override val containerId: Int
        get() = R.id.progressContainer

    fun checkErrorIsDisplayed() {
        onView(withId(R.id.errorStateContainer))
            .check(matches(isDisplayed()))
    }

    fun checkLoadingIsDisplayed() {
        onView(withId(R.id.loadingProgress))
            .check(matches(isDisplayed()))
    }

    fun clickTryAgainButton() {
        onView(withId(R.id.buttonTryAgain))
            .perform(click())
    }

    fun clickCancelButton() {
        onView(withContentDescription(R.string.close))
            .perform(click())
    }
}
