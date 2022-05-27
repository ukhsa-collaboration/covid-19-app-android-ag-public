package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.nestedScrollTo

class HowDoYouFeelRobot {
    fun checkActivityIsDisplayed() {
        onView(withId(R.id.howDoYouFeelContainer))
            .check(matches(isDisplayed()))
    }

    fun clickYesButton() {
        onView(withId(R.id.binaryRadioButtonOption1))
            .perform(click())
    }

    fun clickNoButton() {
        onView(withId(R.id.binaryRadioButtonOption2))
            .perform(click())
    }

    fun checkErrorVisible(shouldBeVisible: Boolean) {
        onView(withId(R.id.howDoYouFeelErrorView)).apply {
            if (shouldBeVisible) perform(nestedScrollTo())
                .check(matches(if (shouldBeVisible) isDisplayed() else not(isDisplayed())))
        }
    }

    fun checkNothingSelected() {
        onView(withId(R.id.binaryRadioButtonOption1))
            .check(matches(not(isChecked())))
        onView(withId(R.id.binaryRadioButtonOption2))
            .check(matches(not(isChecked())))
    }

    fun checkYesSelected() {
        onView(withId(R.id.binaryRadioButtonOption1))
            .check(matches(isChecked()))
    }

    fun checkNoSelected() {
        onView(withId(R.id.binaryRadioButtonOption2))
            .check(matches(isChecked()))
    }

    fun clickContinueButton() {
        onView(withId(R.id.howDoYouFeelContinueButton))
            .perform(click())
    }
}
