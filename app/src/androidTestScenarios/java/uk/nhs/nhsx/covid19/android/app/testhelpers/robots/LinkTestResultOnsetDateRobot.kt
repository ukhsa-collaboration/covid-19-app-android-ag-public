package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.schibsted.spain.barista.assertion.BaristaCheckedAssertions.assertChecked
import com.schibsted.spain.barista.assertion.BaristaCheckedAssertions.assertUnchecked
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.R.id

class LinkTestResultOnsetDateRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(id.linkTestResultOnsetDateTitle))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.link_test_result_onset_date_title)))
    }

    fun clickContinueButton() {
        clickOn(id.linkTestResultOnsetDateContinueButton)
    }

    fun confirmErrorIsShown() {
        onView(withId(id.linkTestResultOnsetDateErrorContainer))
            .check(matches(isDisplayed()))
    }

    fun selectCannotRememberDate() {
        clickOn(id.linkTestResultOnsetDateCheckboxNoDate)
    }

    fun checkDoNotRememberDateIsChecked() {
        assertChecked(id.linkTestResultOnsetDateCheckboxNoDate)
    }

    fun checkDoNotRememberDateIsNotChecked() {
        assertUnchecked(id.linkTestResultOnsetDateCheckboxNoDate)
    }

    fun confirmErrorIsNotShown() {
        onView(withId(id.linkTestResultOnsetDateErrorContainer))
            .check(matches(not(isDisplayed())))
    }

    fun clickSelectDate() {
        clickOn(id.linkTestResultOnsetDateSelectDateContainer)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    fun selectDayOfMonth(dayOfMonth: Int) {
        onView(
            CoreMatchers.allOf(
                ViewMatchers.isDescendantOfA(ViewMatchers.withTagValue(CoreMatchers.equalTo("MONTHS_VIEW_GROUP_TAG"))),
                ViewMatchers.isCompletelyDisplayed(),
                withText(dayOfMonth.toString())
            )
        )
            .perform(ViewActions.click())
        onView(ViewMatchers.withTagValue(Matchers.`is`("CONFIRM_BUTTON_TAG"))).perform(ViewActions.click())
    }
}
