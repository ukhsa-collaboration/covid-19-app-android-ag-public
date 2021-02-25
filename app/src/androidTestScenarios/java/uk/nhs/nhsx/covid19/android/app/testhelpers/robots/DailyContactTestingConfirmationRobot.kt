package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.schibsted.spain.barista.interaction.BaristaDialogInteractions.clickDialogNegativeButton
import com.schibsted.spain.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton
import uk.nhs.nhsx.covid19.android.app.R

class DailyContactTestingConfirmationRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.confirmDailyContactTesting))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    fun clickConfirmOptInToOpenDialog() {
        onView(withId(R.id.confirmDailyContactTesting))
            .perform(scrollTo(), click())
    }

    fun checkDailyContactTestingOptInConfirmationDialogIsDisplayed() {
        onView(withText(R.string.daily_contact_testing_confirmation_dialog_text))
            .check(matches(isDisplayed()))
    }

    fun clickDialogConfirmOptIn() {
        clickDialogPositiveButton()
    }

    fun clickDialogCancel() {
        clickDialogNegativeButton()
    }
}
