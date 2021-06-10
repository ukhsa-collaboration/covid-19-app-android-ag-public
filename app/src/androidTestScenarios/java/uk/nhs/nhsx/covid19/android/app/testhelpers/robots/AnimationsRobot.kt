package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import uk.nhs.nhsx.covid19.android.app.R

class AnimationsRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.animationsContainer))
            .check(matches(isDisplayed()))
    }

    fun clickToggle() {
        onView(withId(R.id.homeScreenAnimationSwitch))
            .perform(ViewActions.click())
    }

    fun checkAnimationsAreDisabled() {
        onView(withText(R.string.animations_status_off))
            .check(matches(isDisplayed()))
        onView(withId(R.id.homeScreenAnimationSwitch))
            .check(matches(ViewMatchers.isNotChecked()))
    }

    fun checkAnimationDialogIsDisplayed() {
        onView(withText(R.string.animations_dialog_title))
            .check(matches(isDisplayed()))
    }

    fun clickDialogOkay() {
        clickDialogPositiveButton()
    }
}
