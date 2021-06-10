package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.R.string

class BookFollowUpTestRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.bookFollowUpTestTitle))
            .check(matches(isDisplayed()))
    }

    fun clickBookFollowUpTestButton() {
        onView(withId(R.id.bookFollowUpTestButton))
            .perform(scrollTo(), click())
    }

    fun clickCloseButton() {
        onView(ViewMatchers.withContentDescription(string.close)).perform(click())
    }
}
