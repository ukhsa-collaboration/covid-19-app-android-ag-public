package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import uk.nhs.nhsx.covid19.android.app.R

class AuthCodeRobot {
    fun checkActivityIsDisplayed() {
        onView(withId(R.id.authCodeContinue))
            .check(matches(isDisplayed()))
    }

    fun enterAuthCode() {
        onView(withId(R.id.authCodeEditText)).perform(
            scrollTo(),
            replaceText("1111-1111")
        )
    }

    fun clickContinue() {
        onView(withId(R.id.authCodeContinue)).perform(click())
    }
}
