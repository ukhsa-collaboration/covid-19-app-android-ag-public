package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import uk.nhs.nhsx.covid19.android.app.R

class LinkTestResultRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.linkTestResultContinue))
            .check(matches(isDisplayed()))
    }

    fun enterCtaToken(ctaToken: String) {
        onView(withId(R.id.enterCodeEditText)).perform(
            scrollTo(),
            replaceText(ctaToken)
        )
        closeSoftKeyboard()
    }

    fun clickContinue() {
        onView(withId(R.id.linkTestResultContinue)).perform(click())
    }

    fun checkErrorInvalidTokenIsDisplayed() {
        onView(withText(R.string.valid_auth_code_is_required))
            .check(matches(isDisplayed()))
    }

    fun checkErrorNoConnectionIsDisplayed() {
        onView(withText(R.string.link_test_result_error_no_connection))
            .check(matches(isDisplayed()))
    }

    fun checkErrorUnexpectedIsDisplayed() {
        onView(withText(R.string.link_test_result_error_unknown))
            .check(matches(isDisplayed()))
    }
}
