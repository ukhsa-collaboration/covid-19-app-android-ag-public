package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.R

class TestingHubRobot {

    fun checkActivityIsDisplayed() {
        checkActivityTitleIsDisplayed(R.string.testing_hub_title)
    }

    fun checkBookTestIsDisplayed() {
        onView(withId(R.id.itemBookTest))
            .check(matches(isDisplayed()))
    }

    fun checkBookTestIsNotDisplayed() {
        onView(withId(R.id.itemBookTest))
            .check(matches(not(isDisplayed())))
    }

    fun clickBookTest() {
        onView(withId(R.id.itemBookTest))
            .perform(scrollTo(), click())
    }

    fun checkFindOutAboutTestingIsDisplayed() {
        onView(withId(R.id.itemFindOutAboutTesting))
            .check(matches(isDisplayed()))
    }

    fun checkFindOutAboutTestingIsNotDisplayed() {
        onView(withId(R.id.itemFindOutAboutTesting))
            .check(matches(not(isDisplayed())))
    }

    fun clickFindOutAboutTesting() {
        onView(withId(R.id.itemFindOutAboutTesting))
            .perform(scrollTo(), click())
    }

    fun clickEnterTestResult() {
        onView(withId(R.id.itemEnterTestResult))
            .perform(scrollTo(), click())
    }
}
