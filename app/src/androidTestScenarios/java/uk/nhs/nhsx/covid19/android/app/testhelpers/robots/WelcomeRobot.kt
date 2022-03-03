package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.CoreMatchers.not
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.isDisplayed
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.waitFor

class WelcomeRobot : HasActivity {
    override val containerId: Int
        get() = R.id.welcomeTitle

    fun clickConfirmOnboarding() {
        onView(withId(R.id.confirmOnboarding))
            .perform(click())
    }

    fun checkAgeConfirmationDialogIsDisplayed() = waitFor {
        onView(withText(R.string.onboarding_age_confirmation_title))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }

    fun clickConfirmAgePositive() {
        clickDialogPositiveButton()
    }

    fun clickConfirmAgeNegative() {
        clickDialogNegativeButton()
    }

    fun checkVenueCheckInGroupIsDisplayed() = waitFor {
        onView(withId(R.id.venueCheckInTitle))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    fun checkVenueCheckInGroupIsNotDisplayed() = waitFor {
        onView(withId(R.id.venueCheckInTitle))
            .perform(scrollTo())
            .check(matches(not(isDisplayed())))
    }
}
