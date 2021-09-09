package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.nestedScrollTo

class ExposureNotificationAgeLimitRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.ageLimitTitle))
            .check(matches(isDisplayed()))
    }

    fun checkDateLabel(expectedDate: String) {
        onView(withId(R.id.exposureNotificationAgeLimitDate))
            .check(
                matches(
                    withText(
                        context.getString(R.string.exposure_notification_age_subtitle_template, expectedDate)
                    )
                )
            )
    }

    fun clickYesButton() {
        onView(withId(R.id.binaryRadioButtonOption1))
            .perform(click())
    }

    fun clickNoButton() {
        onView(withId(R.id.binaryRadioButtonOption2))
            .perform(click())
    }

    fun checkErrorVisible(shouldBeVisible: Boolean) {
        onView(withId(R.id.ageLimitErrorView)).apply {
            if (shouldBeVisible) perform(nestedScrollTo())
        }
            .check(matches(if (shouldBeVisible) isDisplayed() else not(isDisplayed())))
    }

    fun checkNothingSelected() {
        onView(withId(R.id.binaryRadioButtonOption1))
            .check(matches(not(isChecked())))
        onView(withId(R.id.binaryRadioButtonOption2))
            .check(matches(not(isChecked())))
    }

    fun checkYesSelected() {
        onView(withId(R.id.binaryRadioButtonOption1))
            .check(matches(isChecked()))
        onView(withId(R.id.binaryRadioButtonOption2))
            .check(matches(not(isChecked())))
    }

    fun checkNoSelected() {
        onView(withId(R.id.binaryRadioButtonOption2))
            .check(matches(isChecked()))
        onView(withId(R.id.binaryRadioButtonOption1))
            .check(matches(not(isChecked())))
    }

    fun clickContinueButton() {
        onView(withId(R.id.continueButton))
            .perform(nestedScrollTo(), click())
    }

    fun checkSubtitleDisplayed(displayed: Boolean) {
        onView(withId(R.id.ageLimitSubtitle)).apply {
            if (displayed) perform(nestedScrollTo())
        }
            .check(matches(if (displayed) isDisplayed() else not(isDisplayed())))
    }
}
