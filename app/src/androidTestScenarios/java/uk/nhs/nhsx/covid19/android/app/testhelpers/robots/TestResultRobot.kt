package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import uk.nhs.nhsx.covid19.android.app.R

class TestResultRobot {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    fun checkActivityDisplaysPositiveAndFinishIsolation() {
        onView(withText(context.getString(R.string.test_result_positive_no_self_isolation_description))).check(
            matches(isDisplayed())
        )
    }

    fun checkActivityDisplaysNegativeAndFinishIsolation() {
        onView(withText(context.getString(R.string.test_result_negative_no_self_isolation_description))).check(
            matches(isDisplayed())
        )
    }

    fun checkActivityDisplaysPositiveAndContinueSelfIsolation() {
        onView(withText(context.getString(R.string.state_test_positive_info))).check(
            matches(isDisplayed())
        )
    }

    fun checkActivityDisplaysNegativeAndContinueSelfIsolation() {
        onView(withText(context.getString(R.string.state_test_negative_info))).check(
            matches(isDisplayed())
        )
    }

    fun clickIsolationActionButton() {
        onView(withId(R.id.isolationRequestActionButton)).perform(scrollTo(), click())
    }

    fun clickGoodNewsActionButton() {
        onView(withId(R.id.goodNewsActionButton)).perform(scrollTo(), click())
    }
}
