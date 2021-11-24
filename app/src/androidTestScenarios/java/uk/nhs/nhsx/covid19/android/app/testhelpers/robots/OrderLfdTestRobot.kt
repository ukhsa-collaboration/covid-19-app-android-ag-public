package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class OrderLfdTestRobot : HasActivity {

    override val containerId: Int
        get() = R.id.testOrderingContent

    fun clickOrderTestButton() {
        onView(ViewMatchers.withId(R.id.orderTestButton))
            .perform(scrollTo(), click())
    }

    fun clickIAlreadyHaveKitButton() {
        onView(ViewMatchers.withId(R.id.alreadyHaveTestKitButton))
            .perform(scrollTo(), click())
    }
}
