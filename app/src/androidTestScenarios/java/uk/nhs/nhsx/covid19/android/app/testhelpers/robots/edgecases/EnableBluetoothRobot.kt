package uk.nhs.nhsx.covid19.android.app.testhelpers.robots.edgecases

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import uk.nhs.nhsx.covid19.android.app.R

class EnableBluetoothRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.edgeCaseTitle))
            .check(matches(withText(R.string.enable_bluetooth_title)))
    }
}
