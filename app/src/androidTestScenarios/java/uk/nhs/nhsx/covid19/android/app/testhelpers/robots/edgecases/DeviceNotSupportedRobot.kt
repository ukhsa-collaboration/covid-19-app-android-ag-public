package uk.nhs.nhsx.covid19.android.app.testhelpers.robots.edgecases

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import uk.nhs.nhsx.covid19.android.app.R

class DeviceNotSupportedRobot {

    fun checkActivityIsDisplayed() {
        onView(withText(R.string.unsupported_device))
            .check(matches(isDisplayed()))
    }
}
