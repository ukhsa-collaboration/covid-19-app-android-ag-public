package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.CoreMatchers.not
import uk.nhs.nhsx.covid19.android.app.R
import java.lang.Thread.sleep

class EditPostalDistrictRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.continuePostCode))
            .check(matches(isDisplayed()))
    }

    fun clickSavePostDistrictCode() {
        onView(withId(R.id.continuePostCode))
            .perform(click())
    }

    fun checkErrorTitleForInvalidPostCodeIsDisplayed() {
        onView(withId(R.id.errorTextTitle))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.post_code_invalid_title)))
    }

    fun checkErrorTitleForNotSupportedPostCodeIsDisplayed() {
        onView(withId(R.id.errorText))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.postcode_not_supported)))
    }

    fun checkInvalidPostDistrictErrorIsDisplayed() {
        onView(withText(R.string.post_code_invalid_title))
            .check(matches(isDisplayed()))
    }

    fun enterPostDistrictCode(postDistrictCode: String) {
        onView(withId(R.id.postCodeEditText))
            .perform(scrollTo(), click())
            .perform(typeText(postDistrictCode))
            .perform(closeSoftKeyboard())
        sleep(500)
    }

    fun checkErrorContainerIsNotDisplayed() {
        onView(withId(R.id.errorInfoContainer)).check(matches(not(isDisplayed())))
    }
}
