package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.R.plurals

class ExposureNotificationReminderRobot {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    fun checkDialogIsDisplayed() {
        onView(withId(R.id.exposure_notification_reminder_container))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }

    fun clickCancelButton() {
        onView(withId(R.id.cancel))
            .inRoot(isDialog())
            .perform(click())
    }

    fun clickRemindMeIn4Hours() {
        val text = context.resources.getQuantityString(plurals.resume_contact_tracing_hours, 4, 4)
        onView(withText(text))
            .perform(click())
    }

    fun checkConfirmationDialogIsDisplayed() {
        onView(withText(R.string.contact_tracing_notification_reminder_dialog_message))
            .check(matches(isDisplayed()))
    }

    fun clickConfirmationDialogOk() {
        onView(withText(R.string.okay))
            .perform(click())
    }
}
