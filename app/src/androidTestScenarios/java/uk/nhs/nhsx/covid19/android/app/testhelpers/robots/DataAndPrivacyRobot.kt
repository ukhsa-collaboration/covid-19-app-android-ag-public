package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import uk.nhs.nhsx.covid19.android.app.R

class DataAndPrivacyRobot {
    fun checkActivityIsDisplayed() {
        onView(withId(R.id.privacyNoticeLink))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    fun clickConfirmOnboarding() {
        // clickOn(R.id.buttonAgree)
        onView(withId(R.id.buttonAgree)).perform(scrollTo(), click())
    }
}
