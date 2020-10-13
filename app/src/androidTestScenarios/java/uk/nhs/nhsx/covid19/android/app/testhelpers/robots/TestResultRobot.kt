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

    fun checkActivityDisplaysNegativeAndAlreadyFinishedIsolation() {
        onView(withText(context.getString(R.string.test_result_no_self_isolation_description))).check(
            matches(isDisplayed())
        )
    }

    fun checkActivityDisplaysVoidAndAlreadyFinishedIsolation() {
        onView(withText(context.getString(R.string.test_result_void_already_not_in_isolation_subtitle)))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    fun checkActivityDisplaysPositiveAndFinishIsolation() {
        onView(withId(R.id.goodNewsSubtitle))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    fun checkActivityDisplaysNegativeAndFinishIsolation() {
        onView(withText(context.getString(R.string.test_result_no_self_isolation_description)))
            .check(matches(isDisplayed()))
    }

    fun checkActivityDisplaysPositiveAndContinueSelfIsolation() {
        onView(withText(context.getString(R.string.test_result_positive_continue_self_isolation_title_1))).check(
            matches(isDisplayed())
        )

        onView(withText(context.getString(R.string.state_test_positive_info))).check(
            matches(isDisplayed())
        )
    }

    fun checkActivityDisplaysNegativeAndContinueSelfIsolation() {
        onView(withText(context.getString(R.string.test_result_positive_continue_self_isolation_title_1))).check(
            matches(isDisplayed())
        )

        onView(withText(context.getString(R.string.state_test_negative_info))).check(
            matches(isDisplayed())
        )
    }

    fun checkActivityDisplaysVoidAndContinueSelfIsolation() {
        onView(withText(context.getString(R.string.test_result_positive_continue_self_isolation_title_1))).check(
            matches(isDisplayed())
        )

        onView(withText(context.getString(R.string.state_test_void_info))).check(
            matches(isDisplayed())
        )
    }

    fun checkActivityDisplaysPositiveThenNegativeAndStayInIsolation() {
        onView(withText(context.getString(R.string.test_result_positive_continue_self_isolation_title_1))).check(
            matches(isDisplayed())
        )

        onView(withText(context.getString(R.string.state_test_positive_then_negative_info))).check(
            matches(isDisplayed())
        )
    }

    fun checkActivityDisplaysPositiveAndSelfIsolate() {
        onView(withText(context.getString(R.string.state_test_positive_info))).check(
            matches(isDisplayed())
        )

        onView(withText(context.getString(R.string.test_result_negative_then_positive_continue_explanation))).check(
            matches(isDisplayed())
        )
    }

    fun checkActivityDisplaysVoidAndNoIsolate() {
        onView(withText(R.string.test_result_void_already_not_in_isolation_subtitle))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withText(R.string.test_result_no_self_isolation_description))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    fun clickIsolationActionButton() {
        onView(withId(R.id.isolationRequestActionButton)).perform(scrollTo(), click())
    }

    fun clickGoodNewsActionButton() {
        onView(withId(R.id.goodNewsActionButton)).perform(scrollTo(), click())
    }

    fun checkGoodNewsActionButtonShowsBookFreeTest() {
        onView(withId(R.id.goodNewsActionButton)).check(
            matches(withText(context.getString(R.string.book_free_test)))
        )
    }

    fun checkIsolationActionButtonShowsBookFreeTest() {
        onView(withId(R.id.isolationRequestActionButton)).check(
            matches(withText(context.getString(R.string.book_free_test)))
        )
    }

    fun checkGoodNewsActionButtonShowsContinue() {
        onView(withId(R.id.goodNewsActionButton)).check(
            matches(withText(context.getString(R.string.continue_button)))
        )
    }

    fun checkGoodNewsActionButtonShowsBackHome() {
        onView(withId(R.id.goodNewsActionButton)).check(
            matches(withText(context.getString(R.string.back_to_home)))
        )
    }

    fun checkIsolationActionButtonShowsContinue() {
        onView(withId(R.id.isolationRequestActionButton)).check(
            matches(withText(context.getString(R.string.continue_button)))
        )
    }

    fun checkIsolationActionButtonShowsBackHome() {
        onView(withId(R.id.isolationRequestActionButton)).check(
            matches(withText(context.getString(R.string.back_to_home)))
        )
    }
}
