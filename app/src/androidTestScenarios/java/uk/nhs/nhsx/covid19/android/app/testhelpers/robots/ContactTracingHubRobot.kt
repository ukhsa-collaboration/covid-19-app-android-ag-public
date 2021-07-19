package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import uk.nhs.nhsx.covid19.android.app.R

class ContactTracingHubRobot {

    fun checkActivityIsDisplayed() {
        checkActivityTitleIsDisplayed(R.string.contact_tracing_hub_title)
    }

    fun checkContactTracingToggledOnIsDisplayed() {
        onView(withId(R.id.contactTracingStatus))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.contact_tracing_hub_status_on)))
        onView(withId(R.id.encounterDetectionSwitch))
            .check(matches(isChecked()))
    }

    fun checkContactTracingToggledOffIsDisplayed() {
        onView(withId(R.id.contactTracingStatus))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.contact_tracing_hub_status_off)))
        onView(withId(R.id.encounterDetectionSwitch))
            .check(matches(isNotChecked()))
    }

    fun clickContactTracingToggle() {
        onView(withId(R.id.optionContactTracing))
            .perform(click())
    }

    fun checkErrorIsDisplayed() {
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(isDisplayed()))
    }

    fun clickWhenNotToPauseContactTracing() {
        onView(withId(R.id.optionWhenNotToPause))
            .perform(scrollTo(), click())
    }
}
