package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import uk.nhs.nhsx.covid19.android.app.R.id
import uk.nhs.nhsx.covid19.android.app.R.string

class LinkTestResultSymptomsRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(id.linkTestResultSymptomsTitle))
            .check(matches(isDisplayed()))
            .check(matches(withText(string.link_test_result_symptoms_title)))
    }

    fun clickYes() {
        onView(withId(id.linkTestResultSymptomsButtonYes))
            .perform(click())
    }

    fun clickNo() {
        onView(withId(id.linkTestResultSymptomsButtonNo))
            .perform(click())
    }
}
