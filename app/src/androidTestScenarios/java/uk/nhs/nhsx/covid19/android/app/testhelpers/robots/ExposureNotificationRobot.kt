package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import uk.nhs.nhsx.covid19.android.app.R

class ExposureNotificationRobot {

    fun checkActivityIsDisplayed() {
        onView(withText(R.string.exposure_notification_title))
            .check(matches(isDisplayed()))
    }

    fun clickContinueButton() {
        onView(withId(R.id.primaryActionButton))
            .perform(scrollTo(), click())
    }
}
