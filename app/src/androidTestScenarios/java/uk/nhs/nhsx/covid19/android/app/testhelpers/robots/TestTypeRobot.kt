package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.R.id
import uk.nhs.nhsx.covid19.android.app.testhelpers.nestedScrollTo
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class TestTypeRobot : HasActivity {
    override val containerId: Int
        get() = id.testTypeContainer

    fun checkNothingIsSelected() {
        onView(withId(id.tripleVerticalRadioButtonOption1))
            .check(matches(not(isChecked())))
        onView(withId(id.tripleVerticalRadioButtonOption2))
            .check(matches(not(isChecked())))
        onView(withId(id.tripleVerticalRadioButtonOption3))
            .check(matches(not(isChecked())))
    }

    fun clickPositiveButton() {
        onView(withId(id.tripleVerticalRadioButtonOption1))
            .perform(ViewActions.click())
    }

    fun clickNegativeButton() {
        onView(withId(id.tripleVerticalRadioButtonOption2))
            .perform(ViewActions.click())
    }

    fun clickVoidButton() {
        onView(withId(id.tripleVerticalRadioButtonOption3))
            .perform(ViewActions.click())
    }

    fun clickContinueButton() {
        onView(withId(id.selfReportTestTypeContinueButton))
            .perform(ViewActions.click())
    }

    fun checkPositiveIsSelected() {
        onView(withId(id.tripleVerticalRadioButtonOption1))
            .check(matches(isChecked()))
    }

    fun checkNegativeIsSelected() {
        onView(withId(id.tripleVerticalRadioButtonOption2))
            .check(matches(isChecked()))
    }

    fun checkVoidIsSelected() {
        onView(withId(id.tripleVerticalRadioButtonOption3))
            .check(matches(isChecked()))
    }

    fun checkErrorIsVisible(shouldBeVisible: Boolean) {
        onView(withId(id.selfReportTestTypeErrorView)).apply {
            if (shouldBeVisible) perform(nestedScrollTo())
                .check(matches(if (shouldBeVisible) isDisplayed() else not(isDisplayed())))
        }

        onView(withId(id.enterTestTypeErrorIndicator)).apply {
            if (shouldBeVisible) perform(nestedScrollTo())
                .check(matches(if (shouldBeVisible) isDisplayed() else not(isDisplayed())))
        }

        onView(withId(id.selfReportTestTypeErrorText)).apply {
            if (shouldBeVisible) perform(nestedScrollTo())
                .check(matches(if (shouldBeVisible) isDisplayed() else not(isDisplayed())))
        }
    }
}
