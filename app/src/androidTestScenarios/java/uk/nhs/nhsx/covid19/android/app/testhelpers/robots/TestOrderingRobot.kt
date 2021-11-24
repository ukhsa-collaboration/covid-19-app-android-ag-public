package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withId
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class TestOrderingRobot : HasActivity {

    override val containerId: Int
        get() = R.id.testOrderingContainer

    fun clickOrderTestButton() {
        onView(withId(R.id.orderTest))
            .perform(scrollTo(), click())
    }
}
