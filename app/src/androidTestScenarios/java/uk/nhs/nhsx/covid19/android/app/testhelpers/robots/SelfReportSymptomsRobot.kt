package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import uk.nhs.nhsx.covid19.android.app.R.id
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import uk.nhs.nhsx.covid19.android.app.testhelpers.nestedScrollTo
import org.hamcrest.Matchers.not
import androidx.test.espresso.assertion.ViewAssertions.matches

class SelfReportSymptomsRobot : HasActivity {
    override val containerId: Int
        get() = id.selfReportSymptomsContainer

    fun checkNothingIsSelected() {
        onView(withId(id.binaryRadioButtonOption1))
            .check(matches(not(isChecked())))
        onView(withId(id.binaryRadioButtonOption2))
            .check(matches(not(isChecked())))
    }

    fun clickYesButton() {
        onView(withId(id.binaryRadioButtonOption1))
            .perform(click())
    }

    fun clickNoButton() {
        onView(withId(id.binaryRadioButtonOption2))
            .perform(click())
    }

    fun clickContinueButton() {
        onView(withId(id.selfReportSymptomsContinueButton))
            .perform(click())
    }

    fun checkYesIsSelected() {
        onView(withId(id.binaryRadioButtonOption1))
            .check(matches(isChecked()))
    }

    fun checkNoIsSelected() {
        onView(withId(id.binaryRadioButtonOption2))
            .check(matches(isChecked()))
    }

    fun checkErrorIsVisible(shouldBeVisible: Boolean) {
        onView(withId(id.selfReportSymptomsErrorView)).apply {
            if (shouldBeVisible) perform(nestedScrollTo())
                .check(matches(if (shouldBeVisible) isDisplayed() else not(isDisplayed())))
        }

        onView(withId(id.selfReportSymptomsErrorIndicator)).apply {
            if (shouldBeVisible) perform(nestedScrollTo())
                .check(matches(if (shouldBeVisible) isDisplayed() else not(isDisplayed())))
        }

        onView(withId(id.selfReportSymptomsErrorText)).apply {
            if (shouldBeVisible) perform(nestedScrollTo())
                .check(matches(if (shouldBeVisible) isDisplayed() else not(isDisplayed())))
        }
    }
}
