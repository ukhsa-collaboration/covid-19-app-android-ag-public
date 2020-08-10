package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import uk.nhs.nhsx.covid19.android.app.R

class DataAndPrivacyRobot {
    fun checkActivityIsDisplayed() {
        onView(withText(R.string.onboarding_privacy_title))
            .check(ViewAssertions.matches(isDisplayed()))
    }

    fun clickConfirmOnboarding() {
        onView(withId(R.id.buttonAgree)).perform(click())
    }
}
