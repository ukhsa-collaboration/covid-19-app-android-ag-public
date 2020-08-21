package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.R.id

class SymptomsAdviceIsolateRobot {
    fun checkActivityIsDisplayed() {
        Espresso.onView(ViewMatchers.withId(id.stateIcon))
            .check(matches(isDisplayed()))
    }

    fun checkStateInfoViewForPositiveSymptoms() {
        Espresso.onView(ViewMatchers.withId(id.stateTextView))
            .check(matches(withText(R.string.state_index_info)))
    }

    fun checkStateInfoViewForNegativeSymptoms() {
        Espresso.onView(ViewMatchers.withId(id.stateTextView))
            .check(matches(withText(R.string.you_do_not_appear_to_have_symptoms)))
    }

    fun checkBottomActionButtonIsDisplayed() {
        Espresso.onView(ViewMatchers.withId(id.stateActionButton))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    fun checkBottomActionButtonIsNotDisplayed() {
        Espresso.onView(ViewMatchers.withId(id.stateActionButton))
            .check(matches(not(isDisplayed())))
    }

    fun clickBottomActionButton() {
        Espresso.onView(ViewMatchers.withId(id.stateActionButton))
            .perform(scrollTo(), click())
    }
}
