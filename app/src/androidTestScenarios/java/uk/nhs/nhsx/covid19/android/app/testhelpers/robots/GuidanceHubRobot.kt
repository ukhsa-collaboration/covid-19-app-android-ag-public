package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withId
import uk.nhs.nhsx.covid19.android.app.R.id
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class GuidanceHubRobot : HasActivity {
    override val containerId: Int
        get() = id.guidanceHubContainer

    fun clickItemGuidanceForEngland() {
        onView(withId(id.itemForEnglandGuidance)).perform(scrollTo(), click())
    }

    fun clickItemCheckSymptoms() {
        onView(withId(id.itemCheckSymptomsGuidance)).perform(scrollTo(), click())
    }

    fun clickItemLatest() {
        onView(withId(id.itemLatestGuidance)).perform(scrollTo(), click())
    }

    fun clickItemPositiveTestResult() {
        onView(withId(id.itemPositiveTestResultGuidance)).perform(scrollTo(), click())
    }

    fun clickItemTravellingAbroad() {
        onView(withId(id.itemTravellingAbroadGuidance)).perform(scrollTo(), click())
    }

    fun clickItemCheckSSP() {
        onView(withId(id.itemCheckSSPGuidance)).perform(scrollTo(), click())
    }

    fun clickItemEnquiries() {
        onView(withId(id.itemCovidEnquiriesGuidance)).perform(scrollTo(), click())
    }
}
