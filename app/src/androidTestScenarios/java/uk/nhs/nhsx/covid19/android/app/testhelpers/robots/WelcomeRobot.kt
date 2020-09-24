package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn
import com.schibsted.spain.barista.interaction.BaristaDialogInteractions.clickDialogNegativeButton
import com.schibsted.spain.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.isDisplayed

class WelcomeRobot {

    fun checkActivityIsDisplayed() {
        assertDisplayed(R.id.confirmOnboarding)
    }

    fun clickConfirmOnboarding() {
        clickOn(R.id.confirmOnboarding)
    }

    fun checkAgeConfirmationDialogIsDisplayed() {
        onView(withText(R.string.onboarding_age_confirmation_title)).check(matches(isDisplayed()))
    }

    fun clickConfirmAgePositive() {
        clickDialogPositiveButton()
    }

    fun clickConfirmAgeNegative() {
        clickDialogNegativeButton()
    }

    fun isActivityDisplayed() = onView(withId(R.id.confirmOnboarding)).isDisplayed()
}
