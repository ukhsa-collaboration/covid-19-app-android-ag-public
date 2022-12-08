package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import uk.nhs.nhsx.covid19.android.app.R.id
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity
import androidx.test.espresso.matcher.ViewMatchers
import uk.nhs.nhsx.covid19.android.app.testhelpers.nestedScrollTo
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

class SelfReportSymptomsOnsetRobot : HasActivity {
    override val containerId: Int
        get() = id.selfReportSymptomsOnsetDateContainer

    fun selectCannotRememberDate() {
        onView(withId(id.selfReportSymptomsOnsetDateSelectCheckboxNoDate))
            .perform(NestedScrollViewScrollToAction(), click())
    }

    fun checkDoNotRememberDateIsChecked() {
        onView(withId(id.selfReportSymptomsOnsetDateSelectCheckboxNoDate))
            .check(matches(isChecked()))
    }

    fun checkDoNotRememberDateIsNotChecked() {
        onView(withId(id.selfReportSymptomsOnsetDateSelectCheckboxNoDate))
            .check(matches(ViewMatchers.isNotChecked()))
    }

    fun clickSelectDate() {
        onView(withId(id.selfReportSymptomsOnsetDateSelectDateContainer))
            .perform(NestedScrollViewScrollToAction(), click())
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    fun selectDayOfMonth(dayOfMonth: Int) {
        datePickerSelectDayOfMonth(dayOfMonth)
    }

    fun clickContinueButton() {
        onView(withId(id.selfReportSymptomsOnsetDateContinueButton))
            .perform(nestedScrollTo(), click())
    }

    fun checkErrorIsVisible(shouldBeVisible: Boolean) {
        onView(withId(id.selfReportSymptomsOnsetDateErrorView)).apply {
            if (shouldBeVisible) perform(nestedScrollTo())
                .check(matches(if (shouldBeVisible) isDisplayed() else not(isDisplayed())))
        }

        onView(withId(id.selfReportSymptomsOnsetDateErrorIndicator)).apply {
            if (shouldBeVisible) perform(nestedScrollTo())
                .check(matches(if (shouldBeVisible) isDisplayed() else not(isDisplayed())))
        }

        onView(withId(id.selfReportSymptomsOnsetDateErrorText)).apply {
            if (shouldBeVisible) perform(nestedScrollTo())
                .check(matches(if (shouldBeVisible) isDisplayed() else not(isDisplayed())))
        }
    }

    fun checkNoDateIsSelected() {
        onView(withId(id.textSelectDate))
            .check(matches(ViewMatchers.withText(context.getString(string.self_report_symptoms_date_date_picker_box_label))))
    }

    fun checkDateIsChosen() {
        onView(withId(id.textSelectDate))
            .check(matches(not(ViewMatchers.withText(context.getString(string.self_report_symptoms_date_date_picker_box_label)))))
    }
}
