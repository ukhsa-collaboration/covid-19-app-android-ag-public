package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.NestedScrollViewScrollToAction

class LinkTestResultOnsetDateRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.linkTestResultOnsetDateTitle))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.link_test_result_onset_date_title)))
    }

    fun clickContinueButton() {
        onView(withId(R.id.linkTestResultOnsetDateContinueButton))
            .perform(click())
    }

    fun confirmErrorIsShown() {
        onView(withId(R.id.linkTestResultOnsetDateErrorContainer))
            .check(matches(isDisplayed()))
    }

    fun selectCannotRememberDate() {
        onView(withId(R.id.linkTestResultOnsetDateCheckboxNoDate))
            .perform(NestedScrollViewScrollToAction(), click())
    }

    fun checkDoNotRememberDateIsChecked() {
        onView(withId(R.id.linkTestResultOnsetDateCheckboxNoDate))
            .check(matches(isChecked()))
    }

    fun checkDoNotRememberDateIsNotChecked() {
        onView(withId(R.id.linkTestResultOnsetDateCheckboxNoDate))
            .check(matches(isNotChecked()))
    }

    fun confirmErrorIsNotShown() {
        onView(withId(R.id.linkTestResultOnsetDateErrorContainer))
            .check(matches(not(isDisplayed())))
    }

    fun clickSelectDate() {
        onView(withId(R.id.linkTestResultOnsetDateSelectDateContainer))
            .perform(NestedScrollViewScrollToAction(), click())
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    fun selectDayOfMonth(dayOfMonth: Int) {
        datePickerSelectDayOfMonth(dayOfMonth)
    }
}
