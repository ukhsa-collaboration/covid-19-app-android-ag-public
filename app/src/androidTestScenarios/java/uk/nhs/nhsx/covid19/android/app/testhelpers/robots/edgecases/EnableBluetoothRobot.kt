package uk.nhs.nhsx.covid19.android.app.testhelpers.robots.edgecases

import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import uk.nhs.nhsx.covid19.android.app.R

class EnableBluetoothRobot {

    fun checkActivityIsDisplayed() {
        Espresso.onView(ViewMatchers.withId(R.id.edgeCaseTitle))
            .check(ViewAssertions.matches(ViewMatchers.withText(R.string.enable_bluetooth_title)))
    }
}
