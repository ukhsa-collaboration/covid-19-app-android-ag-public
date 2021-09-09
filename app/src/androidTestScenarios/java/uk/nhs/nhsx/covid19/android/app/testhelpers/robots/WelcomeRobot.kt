package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.isDisplayed

class WelcomeRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.welcomeTitle))
            .check(matches(isDisplayed()))
    }

    fun clickConfirmOnboarding() {
        onView(withId(R.id.confirmOnboarding))
            .perform(click())
    }

    fun checkAgeConfirmationDialogIsDisplayed() {
        onView(withText(R.string.onboarding_age_confirmation_title))
            .check(matches(isDisplayed()))
    }

    fun clickConfirmAgePositive() {
        clickDialogPositiveButton()
    }

    fun clickConfirmAgeNegative() {
        clickDialogNegativeButton()
    }
}
