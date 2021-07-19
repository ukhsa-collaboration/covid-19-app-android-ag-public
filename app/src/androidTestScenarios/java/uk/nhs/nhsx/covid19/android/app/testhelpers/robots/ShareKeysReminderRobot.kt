package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import uk.nhs.nhsx.covid19.android.app.R

class ShareKeysReminderRobot {

    fun checkActivityIsDisplayed() {
        onView(withText(context.getString(R.string.share_keys_reminder_information_text)))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    fun clickShareResultsButton() {
        onView(withId(R.id.shareResultsButton))
            .perform(scrollTo(), click())
    }

    fun clickDoNotShareResultsButton() {
        onView(withId(R.id.doNotShareResultsButton))
            .perform(scrollTo(), click())
    }
}
