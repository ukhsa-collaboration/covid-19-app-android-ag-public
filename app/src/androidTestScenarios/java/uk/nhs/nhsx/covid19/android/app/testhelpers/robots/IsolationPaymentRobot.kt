package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withId
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class IsolationPaymentRobot : HasActivity {

    override val containerId: Int
        get() = R.id.isolationPaymentContent

    fun clickEligibilityButton() {
        onView(withId(R.id.isolationPaymentButton))
            .perform(scrollTo(), click())
    }
}
