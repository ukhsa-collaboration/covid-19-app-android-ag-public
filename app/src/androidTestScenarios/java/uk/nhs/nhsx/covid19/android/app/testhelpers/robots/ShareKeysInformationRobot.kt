package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.R.id
import uk.nhs.nhsx.covid19.android.app.R.string

class ShareKeysInformationRobot {

    fun checkActivityIsDisplayed() {
        checkActivityTitleIsDisplayed(R.string.submit_keys_information_title)
        onView(withText(context.getString(string.submit_keys_information_text)))
            .check(matches(isDisplayed()))
    }

    fun clickContinueButton() {
        onView(withId(id.shareKeysConfirm))
            .perform(scrollTo(), click())
    }
}
