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
import uk.nhs.nhsx.covid19.android.app.R.id

class SymptomsAdviceIsolateRobot {
    fun checkActivityIsDisplayed() {
        onView(withId(id.preDaysTextView))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    fun checkStateInfoViewForPositiveSymptoms() {
        onView(withId(id.stateTextView))
            .check(matches(withText(R.string.state_index_info)))
    }

    fun checkStateInfoViewForNegativeSymptoms() {
        onView(withId(id.stateTextView))
            .check(matches(withText(R.string.you_do_not_appear_to_have_symptoms)))
    }

    fun checkBottomActionButtonIsDisplayed() {
        onView(withId(id.stateActionButton))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    fun checkBottomActionButtonIsNotDisplayed() {
        onView(withId(id.stateActionButton))
            .check(matches(not(isDisplayed())))
    }

    fun clickBottomActionButton() {
        onView(withId(id.stateActionButton))
            .perform(scrollTo(), click())
    }
}
