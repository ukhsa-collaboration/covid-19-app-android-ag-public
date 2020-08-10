package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.R.id

class PositiveSymptomsRobot {
    fun checkActivityIsDisplayed() {
        Espresso.onView(ViewMatchers.withId(id.stateIcon))
            .check(matches(isDisplayed()))
    }

    fun checkTestOrderingButtonIsDisplayed() {
        Espresso.onView(ViewMatchers.withId(id.stateActionButton))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    fun checkTestOrderingButtonIsNotDisplayed() {
        Espresso.onView(ViewMatchers.withId(id.stateActionButton))
            .check(matches(not(isDisplayed())))
    }

    fun clickTestOrderingButton() {
        Espresso.onView(ViewMatchers.withId(id.stateActionButton))
            .perform(scrollTo(), click())
    }
}
