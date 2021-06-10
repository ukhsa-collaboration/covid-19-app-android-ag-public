package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.R.string

class UnknownTestResultRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.unknown_test_result_title))
            .check(matches(isDisplayed()))
    }

    fun clickCloseButton() {
        onView(ViewMatchers.withContentDescription(string.close)).perform(ViewActions.click())
    }
}
