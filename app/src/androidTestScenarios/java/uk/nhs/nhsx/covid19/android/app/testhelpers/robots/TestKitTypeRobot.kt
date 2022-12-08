package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import uk.nhs.nhsx.covid19.android.app.R.id
import uk.nhs.nhsx.covid19.android.app.testhelpers.nestedScrollTo
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity
import org.hamcrest.Matchers.not
import androidx.test.espresso.assertion.ViewAssertions.matches

class TestKitTypeRobot : HasActivity {
    override val containerId: Int
        get() = id.testKitTypeContainer

    fun checkNothingIsSelected() {
        onView(withId(id.binaryVerticalRadioButtonOption1))
            .check(matches(not(isChecked())))
        onView(withId(id.binaryVerticalRadioButtonOption2))
            .check(matches(not(isChecked())))
    }

    fun clickLFDButton() {
        onView(withId(id.binaryVerticalRadioButtonOption1))
            .perform(click())
    }

    fun clickPCRButton() {
        onView(withId(id.binaryVerticalRadioButtonOption2))
            .perform(click())
    }

    fun clickContinueButton() {
        onView(withId(id.selfReportTestKitTypeContinueButton))
            .perform(click())
    }

    fun checkLFDIsSelected() {
        onView(withId(id.binaryVerticalRadioButtonOption1))
            .check(matches(isChecked()))
    }

    fun checkPCRIsSelected() {
        onView(withId(id.binaryVerticalRadioButtonOption2))
            .check(matches(isChecked()))
    }

    fun checkErrorIsVisible(shouldBeVisible: Boolean) {
        onView(withId(id.selfReportTestKitTypeErrorView)).apply {
            if (shouldBeVisible) perform(nestedScrollTo())
                .check(matches(if (shouldBeVisible) isDisplayed() else not(isDisplayed())))
        }

        onView(withId(id.selfReportTestKitTypeErrorIndicator)).apply {
            if (shouldBeVisible) perform(nestedScrollTo())
                .check(matches(if (shouldBeVisible) isDisplayed() else not(isDisplayed())))
        }

        onView(withId(id.selfReportTestKitTypeErrorText)).apply {
            if (shouldBeVisible) perform(nestedScrollTo())
                .check(matches(if (shouldBeVisible) isDisplayed() else not(isDisplayed())))
        }
    }
}
