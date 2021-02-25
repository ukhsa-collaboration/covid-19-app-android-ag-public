package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.core.text.HtmlCompat
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.CoreMatchers.not
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.stringFromResId

class PostCodeRobot {

    fun enterPostCode(postCode: String) {
        onView(withId(R.id.postCodeEditText)).perform(
            scrollTo(),
            replaceText(postCode)
        )
        closeSoftKeyboard()
    }

    fun clickContinue() {
        onView(withId(R.id.postCodeContinue)).perform(click())
    }

    fun checkActivityIsDisplayed() {
        checkContinueButtonIsDisplayed()
    }

    fun checkContinueButtonIsDisplayed() {
        onView(withId(R.id.postCodeContinue)).check(matches(isDisplayed()))
    }

    fun checkActivityDoesNotExist() {
        onView(withId(R.id.postCodeContinue)).check(doesNotExist())
    }

    fun checkTitleIsDisplayed() {
        onView(withText(R.string.post_code_title)).check(matches(isDisplayed()))
    }

    fun checkExampleIsDisplayed() {
        onView(withText(R.string.post_code_example)).check(matches(isDisplayed()))
    }

    fun checkErrorContainerIsNotDisplayed() {
        onView(withId(R.id.errorInfoContainer)).check(matches(not(isDisplayed())))
    }

    fun checkErrorTitleIsDisplayed() {
        onView(withText(R.string.post_code_invalid_title))
            .perform(scrollTo()).check(matches(isDisplayed()))
    }

    fun checkErrorContainerForNotSupportedPostCodeIsDisplayed() {
        onView(withId(R.id.errorText)).perform(scrollTo())
            .check(matches(withText(R.string.postcode_not_supported)))
    }

    fun checkEditTextIs(expectedValue: String) {
        onView(withId(R.id.postCodeEditText)).check(matches(withText(expectedValue)))
    }

    fun checkRationaleIsVisible() {
        onView(withText(R.string.post_code_rationale_title)).check(matches(isDisplayed()))

        val postCodeRationaleRes: String = stringFromResId(R.string.post_code_rationale)
        val postCodeRationale =
            HtmlCompat.fromHtml(postCodeRationaleRes, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()

        onView(withText(postCodeRationale)).check(matches(isDisplayed()))
    }
}
