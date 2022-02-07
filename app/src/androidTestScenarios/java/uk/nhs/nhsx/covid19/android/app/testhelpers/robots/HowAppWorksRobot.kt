package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import uk.nhs.nhsx.covid19.android.app.R.id
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class HowAppWorksRobot : HasActivity {
    override val containerId: Int
        get() = id.howAppWorksContainer

    fun clickContinueOnboarding() {
        onView(withId(id.continueHowAppWorks))
            .perform(click())
    }
}
