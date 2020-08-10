package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import uk.nhs.nhsx.covid19.android.app.R

class MoreAboutAppRobot {
    fun checkActivityIsDisplayed() {
        onView(withText(R.string.about_this_app_title))
            .check(matches(isDisplayed()))
    }

    fun clickSeeData() {
        onView(withId(R.id.linkManageData)).perform(scrollTo()).perform(click())
    }
}
