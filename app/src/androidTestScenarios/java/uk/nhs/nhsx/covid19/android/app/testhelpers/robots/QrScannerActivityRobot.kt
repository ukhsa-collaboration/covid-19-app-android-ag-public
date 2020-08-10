package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import uk.nhs.nhsx.covid19.android.app.R.id

class QrScannerActivityRobot {

    fun checkActivityIsDisplayed() {
        Espresso.onView(ViewMatchers.withId(id.scannerSurfaceView))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
}
