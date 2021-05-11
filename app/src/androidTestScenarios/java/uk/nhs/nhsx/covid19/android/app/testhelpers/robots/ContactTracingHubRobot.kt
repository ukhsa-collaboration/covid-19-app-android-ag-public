package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.appcompat.widget.AppCompatTextView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.instanceOf
import uk.nhs.nhsx.covid19.android.app.R

class ContactTracingHubRobot {

    fun checkActivityIsDisplayed() {
        onView(
            allOf(
                instanceOf(AppCompatTextView::class.java),
                withParent(withId(R.id.toolbar))
            )
        ).check(matches(withText(R.string.contact_tracing_hub_title)))
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
}
