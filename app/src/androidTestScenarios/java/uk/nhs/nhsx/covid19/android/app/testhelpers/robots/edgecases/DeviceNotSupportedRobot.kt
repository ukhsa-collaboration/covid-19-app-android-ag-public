package uk.nhs.nhsx.covid19.android.app.testhelpers.robots.edgecases

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import uk.nhs.nhsx.covid19.android.app.R

class DeviceNotSupportedRobot {

    fun checkActivityIsDisplayed() {
        onView(withText(R.string.cant_run_app))
            .check(matches(isDisplayed()))

        onView(withText(R.string.device_not_supported_next_steps_text))
            .check(matches(isDisplayed()))
    }
}
