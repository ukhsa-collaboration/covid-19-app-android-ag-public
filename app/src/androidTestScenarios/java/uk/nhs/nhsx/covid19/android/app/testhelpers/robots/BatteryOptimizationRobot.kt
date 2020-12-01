package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.appcompat.widget.AppCompatImageButton
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.instanceOf
import uk.nhs.nhsx.covid19.android.app.R

class BatteryOptimizationRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.batteryOptimizationAllowButton))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    fun clickAllowButton() {
        onView(withId(R.id.batteryOptimizationAllowButton))
            .perform(scrollTo())
            .perform(click())
    }

    fun clickCloseButton() {
        onView(
            allOf(
                instanceOf(AppCompatImageButton::class.java),
                withParent(withId(R.id.toolbar))
            )
        ).perform(click())
    }
}
