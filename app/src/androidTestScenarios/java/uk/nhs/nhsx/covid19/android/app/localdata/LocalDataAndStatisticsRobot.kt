package uk.nhs.nhsx.covid19.android.app.localdata

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.isAccessibilityHeading
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class LocalDataAndStatisticsRobot : HasActivity {
    override val containerId = R.id.localAreaDataAndStatisticsContainer

    fun checkAccessibilityHeadingsAreSet() {
        onView(withId(R.id.localAreaDataAndStatisticsHeading))
            .check(matches(isAccessibilityHeading()))
        onView(withId(R.id.localAreaDataAndStatisticsLocalAuthorityTitle))
            .check(matches(isAccessibilityHeading()))
        onView(withId(R.id.localAreaDataAndStatisticsTestedPositiveTitle))
            .check(matches(isAccessibilityHeading()))
    }
}
