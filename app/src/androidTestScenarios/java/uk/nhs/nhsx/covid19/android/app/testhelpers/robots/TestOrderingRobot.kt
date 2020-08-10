package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import uk.nhs.nhsx.covid19.android.app.R.id

class TestOrderingRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(id.testOrderingContainer))
            .check(matches(isDisplayed()))
    }

    fun clickOrderTestButton() {
        onView(withId(id.orderTest))
            .perform(scrollTo(), ViewActions.click())
    }
}
