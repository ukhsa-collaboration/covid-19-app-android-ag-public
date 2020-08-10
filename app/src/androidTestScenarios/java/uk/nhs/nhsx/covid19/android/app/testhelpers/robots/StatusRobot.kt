package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.NestedScrollViewScrollToAction

class StatusRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.statusContainer)).check(matches(isDisplayed()))
    }

    fun checkReportSymptomsIsNotDisplayed() {
        onView(withId(R.id.optionReportSymptoms)).check(matches(not(isDisplayed())))
    }

    fun clickReportSymptoms() {
        onView(withId(R.id.optionReportSymptoms)).perform(NestedScrollViewScrollToAction(), click())
    }

    fun clickOrderTest() {
        onView(withId(R.id.optionOrderTest)).perform(NestedScrollViewScrollToAction(), click())
    }

    fun checkScanQrCodeOptionIsNotDisplayed() {
        onView(withId(R.id.optionVenueCheckIn)).check(matches(not(isDisplayed())))
    }

    fun checkIsolationViewIsDisplayed() {
        onView(withId(R.id.isolationView)).check(matches(isDisplayed()))
    }
}
