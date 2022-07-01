package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import uk.nhs.nhsx.covid19.android.app.R.id
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class PositiveSymptomsNoIsolationRobot : HasActivity {
    override val containerId: Int
        get() = id.positiveSymptomsNoIsolationContainer

    fun checkIsPositiveSymptomsNoIsolationTitleDisplayed() {
        onView(ViewMatchers.withId(id.positiveSymptomsNoIsolationTitle))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
}
