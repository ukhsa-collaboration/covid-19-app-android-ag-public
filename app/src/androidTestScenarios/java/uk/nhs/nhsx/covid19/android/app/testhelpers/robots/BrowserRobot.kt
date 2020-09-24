package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.R.id

class BrowserRobot {

    fun checkActivityIsDisplayed() {
        Espresso.onView(ViewMatchers.withId(id.browserCloseButton))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    fun clickCloseButton() {
        onView(withId(R.id.browserCloseButton))
            .perform(click())
    }
}
