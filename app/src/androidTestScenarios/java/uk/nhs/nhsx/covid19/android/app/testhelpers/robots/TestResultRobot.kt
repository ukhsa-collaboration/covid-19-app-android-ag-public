package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.R

class TestResultRobot {

    fun checkActivityDisplaysNegativeNotInIsolation() {
        onView(withText(R.string.test_result_negative_no_self_isolation_subtitle_text))
            .check(matches(isDisplayed()))
        onView(withText(R.string.test_result_no_self_isolation_description))
            .check(matches(isDisplayed()))
    }

    fun checkActivityDisplaysNegativeWillBeInIsolation() {
        onView(withText(R.string.test_result_positive_continue_self_isolation_title_1))
            .check(matches(isDisplayed()))

        onView(withText(R.string.state_test_negative_info))
            .check(matches(isDisplayed()))
    }

    fun checkActivityDisplaysNegativeWontBeInIsolation() {
        onView(withText(R.string.test_result_negative_no_self_isolation_subtitle_text))
            .check(matches(isDisplayed()))
        onView(withText(R.string.test_result_no_self_isolation_description))
            .check(matches(isDisplayed()))
    }

    fun checkActivityDisplaysPositiveWillBeInIsolation() {
        onView(withText(R.string.state_test_positive_info))
            .check(matches(isDisplayed()))

        onView(withText(R.string.test_result_negative_then_positive_continue_explanation))
            .check(matches(isDisplayed()))
    }

    fun checkActivityDisplaysPositiveContinueIsolation() {
        onView(withText(R.string.test_result_positive_continue_self_isolation_title_1))
            .check(matches(isDisplayed()))

        onView(withText(R.string.state_test_positive_info))
            .check(matches(isDisplayed()))

        onView(withText(R.string.test_result_positive_continue_self_isolate_explanation_1))
            .check(matches(isDisplayed()))
    }

    fun checkActivityDisplaysPositiveContinueIsolationNoChange() {
        onView(withText(R.string.test_result_positive_continue_self_isolation_title_1))
            .check(matches(isDisplayed()))

        onView(withText(R.string.state_test_positive_info))
            .check(matches(isDisplayed()))

        onView(withText(R.string.test_result_positive_continue_self_isolate_no_change_explanation_1))
            .check(matches(isDisplayed()))
    }

    fun checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest() {
        onView(withText(R.string.self_isolate_for))
            .check(matches(isDisplayed()))

        onView(withText(R.string.test_result_positive_self_isolate_and_book_test_title_3))
            .check(matches(isDisplayed()))

        onView(withText(R.string.state_test_positive_and_book_test_info))
            .check(matches(isDisplayed()))
    }

    fun checkActivityDisplaysPositiveWontBeInIsolation() {
        onView(withId(R.id.goodNewsSubtitle))
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        onView(withText(R.string.test_result_positive_no_self_isolation_subtitle))
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        onView(withText(R.string.test_result_no_self_isolation_description))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    fun checkActivityDisplaysPositiveThenNegativeWillBeInIsolation() {
        onView(withText(R.string.test_result_positive_continue_self_isolation_title_1))
            .check(matches(isDisplayed()))

        onView(withText(R.string.state_test_positive_then_negative_info))
            .check(matches(isDisplayed()))
    }

    fun checkActivityDisplaysVoidNotInIsolation() {
        onView(withText(R.string.test_result_void_already_not_in_isolation_subtitle))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withText(R.string.test_result_no_self_isolation_description))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    fun checkActivityDisplaysVoidWillBeInIsolation() {
        onView(withText(R.string.test_result_positive_continue_self_isolation_title_1))
            .check(matches(isDisplayed()))

        onView(withText(R.string.state_test_void_info))
            .check(matches(isDisplayed()))
    }

    fun clickIsolationActionButton() {
        onView(withId(R.id.isolationRequestActionButton)).perform(scrollTo(), click())
    }

    fun clickGoodNewsActionButton() {
        onView(withId(R.id.goodNewsActionButton)).perform(scrollTo(), click())
    }

    fun clickCloseButton() {
        onView(withContentDescription(R.string.close)).perform(click())
    }

    fun checkGoodNewsActionButtonShowsOrderFreeTest() {
        onView(withId(R.id.goodNewsActionButton))
            .check(matches(withText(R.string.book_free_test)))
    }

    fun checkIsolationActionButtonShowsBookFreeTest() {
        onView(withId(R.id.isolationRequestActionButton))
            .check(matches(withText(R.string.book_free_test)))
    }

    fun checkGoodNewsActionButtonShowsContinue() {
        onView(withId(R.id.goodNewsActionButton))
            .check(matches(withText(R.string.continue_button)))
    }

    fun checkGoodNewsActionButtonShowsBackHome() {
        onView(withId(R.id.goodNewsActionButton))
            .check(matches(withText(R.string.back_to_home)))
    }

    fun checkIsolationActionButtonShowsContinue() {
        onView(withId(R.id.isolationRequestActionButton))
            .check(matches(withText(R.string.continue_button)))
    }

    fun checkIsolationActionButtonShowsBackHome() {
        onView(withId(R.id.isolationRequestActionButton))
            .check(matches(withText(R.string.back_to_home)))
    }

    fun checkExposureLinkIsDisplayed() {
        onView(withId(R.id.exposureFaqsLink))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    fun checkExposureLinkIsNotDisplayed() {
        onView(withId(R.id.isolationRequestActionButton))
            .perform(scrollTo())

        onView(withId(R.id.exposureFaqsLink))
            .check(matches(not(isDisplayed())))
    }
}
