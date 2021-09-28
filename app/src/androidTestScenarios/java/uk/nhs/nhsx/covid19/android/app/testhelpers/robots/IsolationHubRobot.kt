package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.R

class IsolationHubRobot {

    fun checkActivityIsDisplayed() {
        checkActivityTitleIsDisplayed(R.string.isolation_hub_title)
    }

    fun checkItemBookTestIsDisplayed() {
        onView(withId(R.id.itemBookTest))
            .check(matches(isDisplayed()))
    }

    fun checkItemBookTestIsNotDisplayed() {
        onView(withId(R.id.itemBookTest))
            .check(matches(not(isDisplayed())))
    }

    fun clickItemBookATest() {
        onView(withId(R.id.itemBookTest))
            .perform(click())
    }

    fun checkItemIsolationPaymentIsDisplayed() {
        onView(withId(R.id.itemIsolationPayment))
            .check(matches(isDisplayed()))
    }

    fun checkItemIsolationPaymentIsNotDisplayed() {
        onView(withId(R.id.itemIsolationPayment))
            .check(matches(not(isDisplayed())))
    }

    fun clickItemIsolationPayment() {
        onView(withId(R.id.itemIsolationPayment))
            .perform(click())
    }

    fun checkItemIsolationNoteIsDisplayed() {
        onView(withId(R.id.itemIsolationNote))
            .check(matches(isDisplayed()))
    }

    fun clickItemIsolationNote() {
        onView(withId(R.id.itemIsolationNote))
            .perform(click())
    }
}
