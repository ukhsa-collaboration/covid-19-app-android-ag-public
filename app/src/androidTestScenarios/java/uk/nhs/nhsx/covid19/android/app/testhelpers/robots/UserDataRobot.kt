package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import uk.nhs.nhsx.covid19.android.app.R

class UserDataRobot {
    fun checkActivityIsDisplayed() {
        onView(withText(R.string.about_manage_my_data))
            .check(matches(isDisplayed()))
    }

    fun userClicksOnDeleteAllDataButton() {
        onView(withId(R.id.actionDeleteAllData)).perform(scrollTo(), click())
    }

    fun userClicksDeleteDataOnDialog() {
        onView(withText(R.string.about_delete_positive_text)).perform(click())
    }
}
