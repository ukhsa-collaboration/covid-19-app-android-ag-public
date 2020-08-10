package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.repeatedlyUntil
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import uk.nhs.nhsx.covid19.android.app.R

class AuthCodeRobot {
    fun checkActivityIsDisplayed() {
        onView(withId(R.id.authCodeTitle))
            .check(matches(isDisplayed()))
    }

    fun enterAuthCode() {
        onView(withId(R.id.authCodeEditText)).perform(
            scrollTo(),
            repeatedlyUntil(
                typeText("1"),
                withText("1111-1111"),
                20
            )
        )
    }

    fun clickContinue() {
        onView(withId(R.id.authCodeContinue)).perform(click())
    }
}
