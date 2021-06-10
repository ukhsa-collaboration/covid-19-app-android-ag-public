package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import uk.nhs.nhsx.covid19.android.app.R.id
import uk.nhs.nhsx.covid19.android.app.R.string

class ShareKeysResultRobot {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    fun checkActivityIsDisplayed() {
        onView(withText(context.getString(string.share_keys_success_title)))
            .check(
                matches(isDisplayed())
            )

        onView(withText(context.getString(string.back_to_home)))
            .check(
                matches(isDisplayed())
            )
    }

    fun checkActivityWithContinueButtonIsDisplayed() {
        onView(withText(context.getString(string.share_keys_success_title)))
            .check(
                matches(isDisplayed())
            )

        onView(withText(context.getString(string.continue_button)))
            .check(
                matches(isDisplayed())
            )
    }

    fun clickActionButton() {
        onView(withId(id.actionButton))
            .perform(click())
    }
}
