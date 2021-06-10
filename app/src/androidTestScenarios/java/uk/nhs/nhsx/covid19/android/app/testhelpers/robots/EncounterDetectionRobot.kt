package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import uk.nhs.nhsx.covid19.android.app.R

class EncounterDetectionRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.remainingDaysInIsolation))
            .check(matches(isDisplayed()))
    }

    fun checkNumberOfDaysTextIs(expectedValue: String) {
        onView(withId(R.id.remainingDaysInIsolation))
            .check(matches(withText(expectedValue)))
    }

    fun clickIUnderstandButton() {
        onView(withId(R.id.understandButton))
            .perform(scrollTo(), click())
    }
}
