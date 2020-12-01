package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import uk.nhs.nhsx.covid19.android.app.R.string

class EnableExposureNotificationsRobot {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    fun checkActivityIsDisplayed() {
        onView(withText(context.getString(string.enable_exposure_notifications_title)))
            .check(
                matches(isDisplayed())
            )
    }
}
