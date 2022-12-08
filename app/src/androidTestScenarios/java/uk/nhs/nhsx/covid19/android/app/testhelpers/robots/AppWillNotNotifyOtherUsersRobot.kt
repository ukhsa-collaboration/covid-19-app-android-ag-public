package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import uk.nhs.nhsx.covid19.android.app.R.id
import uk.nhs.nhsx.covid19.android.app.testhelpers.NestedScrollViewScrollToAction
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class AppWillNotNotifyOtherUsersRobot : HasActivity {
    override val containerId: Int
        get() = id.selfReportAppWillNotNotifyOtherUsers

    fun clickContinue() {
        Espresso.onView(ViewMatchers.withId(id.buttonContinue))
            .perform(NestedScrollViewScrollToAction(), ViewActions.click())
    }
}
