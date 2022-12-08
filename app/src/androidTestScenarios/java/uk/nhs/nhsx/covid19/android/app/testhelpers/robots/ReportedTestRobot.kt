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

class ReportedTestRobot : HasActivity {
    override val containerId: Int
        get() = id.selfReportReportedTestContainer

    fun checkNothingIsSelected() {
        onView(withId(id.binaryVerticalRadioButtonOption1))
            .check(matches(not(isChecked())))
        onView(withId(id.binaryVerticalRadioButtonOption2))
            .check(matches(not(isChecked())))
    }

    fun clickYesButton() {
        onView(withId(id.binaryVerticalRadioButtonOption1))
            .perform(click())
    }

    fun clickNoButton() {
        onView(withId(id.binaryVerticalRadioButtonOption2))
            .perform(click())
    }

    fun clickContinueButton() {
        onView(withId(id.selfReportReportedTestContinueButton))
            .perform(nestedScrollTo(), click())
    }

    fun checkYesIsSelected() {
        onView(withId(id.binaryVerticalRadioButtonOption1))
            .check(matches(isChecked()))
    }

    fun checkNoIsSelected() {
        onView(withId(id.binaryVerticalRadioButtonOption2))
            .check(matches(isChecked()))
    }

    fun checkErrorIsVisible(shouldBeVisible: Boolean) {
        onView(withId(id.selfReportReportedTestErrorView)).apply {
            if (shouldBeVisible) perform(nestedScrollTo())
                .check(matches(if (shouldBeVisible) isDisplayed() else not(isDisplayed())))
        }

        onView(withId(id.selfReportReportedTestErrorIndicator)).apply {
            if (shouldBeVisible) perform(nestedScrollTo())
                .check(matches(if (shouldBeVisible) isDisplayed() else not(isDisplayed())))
        }

        onView(withId(id.selfReportReportedTestErrorText)).apply {
            if (shouldBeVisible) perform(nestedScrollTo())
                .check(matches(if (shouldBeVisible) isDisplayed() else not(isDisplayed())))
        }
    }
}
