package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import uk.nhs.nhsx.covid19.android.app.R

class EnableExposureNotificationsRobot {

    fun checkActivityIsDisplayed() {
        onView(withText(context.getString(R.string.enable_exposure_notifications_title)))
            .check(matches(isDisplayed()))
    }

    fun clickEnableExposureNotificationsButton() {
        onView(withId(R.id.takeActionButton))
            .perform(click())
    }

    fun checkErrorIsDisplayed() {
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(isDisplayed()))
    }
}
