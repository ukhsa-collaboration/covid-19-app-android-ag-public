package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.R

class TestingHubRobot {

    fun checkActivityIsDisplayed() {
        checkActivityTitleIsDisplayed(R.string.testing_hub_title)
    }

    fun checkBookPcrTestIsDisplayed() {
        onView(withText(R.string.testing_hub_book_test_title))
            .check(matches(isDisplayed()))
    }

    fun checkBookLfdTestIsDisplayed() {
        onView(withText(R.string.testing_hub_book_lfd_test_title))
            .check(matches(isDisplayed()))
    }

    fun clickBookTest() {
        onView(withId(R.id.itemBookTest))
            .perform(scrollTo(), click())
    }

    fun clickEnterTestResult() {
        onView(withId(R.id.itemEnterTestResult))
            .perform(scrollTo(), click())
    }
}
