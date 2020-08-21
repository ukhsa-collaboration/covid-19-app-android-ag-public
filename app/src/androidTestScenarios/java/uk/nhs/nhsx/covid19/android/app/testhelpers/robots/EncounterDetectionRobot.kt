package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import uk.nhs.nhsx.covid19.android.app.R.id

class EncounterDetectionRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(id.isolationDays))
            .check(matches(isDisplayed()))
    }

    fun checkNumberOfDaysTextIs(expectedValue: String) {
        onView(withId(id.isolationDays))
            .check(matches(withText(expectedValue)))
    }

    fun clickIUnderstandButton() {
        scrollToBottom()

        onView(withId(id.understandButton))
            .perform(ViewActions.click())
    }

    private fun scrollToBottom() {
        onView(withId(id.container))
            .perform(ViewActions.swipeUp())
    }
}
