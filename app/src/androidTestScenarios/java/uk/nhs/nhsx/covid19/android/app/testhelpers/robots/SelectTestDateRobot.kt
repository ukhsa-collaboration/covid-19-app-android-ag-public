package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.matcher.ViewMatchers
import uk.nhs.nhsx.covid19.android.app.R.id
import uk.nhs.nhsx.covid19.android.app.testhelpers.nestedScrollTo
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Matchers.not
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.platform.app.InstrumentationRegistry
import uk.nhs.nhsx.covid19.android.app.R.string
import uk.nhs.nhsx.covid19.android.app.testhelpers.NestedScrollViewScrollToAction

class SelectTestDateRobot : HasActivity {
    override val containerId: Int
        get() = id.selectTestDateContainer

    fun selectCannotRememberDate() {
        onView(withId(id.selfReportTestDateSelectCheckboxNoDate))
            .perform(NestedScrollViewScrollToAction(), click())
    }

    fun checkDoNotRememberDateIsChecked() {
        onView(withId(id.selfReportTestDateSelectCheckboxNoDate))
            .check(matches(isChecked()))
    }

    fun checkDoNotRememberDateIsNotChecked() {
        onView(withId(id.selfReportTestDateSelectCheckboxNoDate))
            .check(matches(ViewMatchers.isNotChecked()))
    }

    fun clickSelectDate() {
        onView(withId(id.selfReportTestDateSelectDateContainer))
            .perform(NestedScrollViewScrollToAction(), click())
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    fun selectDayOfMonth(dayOfMonth: Int) {
        datePickerSelectDayOfMonth(dayOfMonth)
    }

    fun clickContinueButton() {
        onView(withId(id.selfReportTestDateContinueButton))
            .perform(nestedScrollTo(), click())
    }

    fun checkErrorIsVisible(shouldBeVisible: Boolean) {
        onView(withId(id.selfReportTestDateErrorView)).apply {
            if (shouldBeVisible) perform(nestedScrollTo())
                .check(matches(if (shouldBeVisible) isDisplayed() else not(isDisplayed())))
        }

        onView(withId(id.selfReportTestDateErrorIndicator)).apply {
            if (shouldBeVisible) perform(nestedScrollTo())
                .check(matches(if (shouldBeVisible) isDisplayed() else not(isDisplayed())))
        }

        onView(withId(id.selfReportTestDateErrorText)).apply {
            if (shouldBeVisible) perform(nestedScrollTo())
                .check(matches(if (shouldBeVisible) isDisplayed() else not(isDisplayed())))
        }
    }

    fun checkNoDateIsSelected() {
        onView(withId(id.textSelectDate))
            .check(matches(ViewMatchers.withText(context.getString(string.self_report_test_date_date_picker_box_label))))
    }

    fun checkDateIsChosen() {
        onView(withId(id.textSelectDate))
            .check(matches(not(ViewMatchers.withText(context.getString(string.self_report_test_date_date_picker_box_label)))))
    }
}
