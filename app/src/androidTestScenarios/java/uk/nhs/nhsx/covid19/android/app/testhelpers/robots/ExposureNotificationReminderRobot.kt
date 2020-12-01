package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import uk.nhs.nhsx.covid19.android.app.R

class ExposureNotificationReminderRobot {

    fun checkDialogIsDisplayed() {
        onView(withId(R.id.exposure_notification_reminder_container))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }

    fun clickDontRemindMe() {
        onView(withId(R.id.dont_remind))
            .inRoot(isDialog())
            .perform(click())
    }
}
