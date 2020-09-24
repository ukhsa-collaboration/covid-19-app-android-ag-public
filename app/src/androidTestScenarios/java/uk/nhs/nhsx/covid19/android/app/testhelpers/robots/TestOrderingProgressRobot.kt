package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.R.id

class TestOrderingProgressRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(id.testOrderingProgressContainer))
            .check(matches(isDisplayed()))
    }

    fun checkActivityNotIsDisplayed() {
        onView(withId(id.testOrderingProgressContainer))
            .check(matches(not(isDisplayed())))
    }

    fun checkErrorIsDisplayed() {
        onView(withId(id.errorStateContainer))
            .check(matches(isDisplayed()))
    }

    fun clickTryAgainButton() {
        onView(withId(id.buttonTryAgain))
            .perform(ViewActions.click())
    }
}
