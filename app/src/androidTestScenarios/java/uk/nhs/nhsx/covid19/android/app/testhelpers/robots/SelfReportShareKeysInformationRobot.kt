package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers
import uk.nhs.nhsx.covid19.android.app.R.id
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class SelfReportShareKeysInformationRobot : HasActivity {
    override val containerId: Int
        get() = id.selfReportShareKeysInformationContainer

    fun clickContinueButton() {
        onView(ViewMatchers.withId(id.selfReportShareKeysConfirm))
            .perform(scrollTo(), click())
    }
}
