package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.schibsted.spain.barista.assertion.BaristaCheckedAssertions.assertChecked
import com.schibsted.spain.barista.assertion.BaristaCheckedAssertions.assertUnchecked
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.R

class StatusRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.statusContainer)).check(matches(isDisplayed()))
    }

    fun checkReportSymptomsIsNotDisplayed() {
        onView(withId(R.id.optionReportSymptoms)).check(matches(not(isDisplayed())))
    }

    fun clickAreaRiskView() {
        clickOn(R.id.riskAreaView)
    }

    fun checkAreaRiskViewIsDisplayed() {
        onView(withId(R.id.riskAreaView)).check(matches(isDisplayed()))
    }

    fun clickVenueCheckIn() {
        clickOn(R.id.optionVenueCheckIn)
    }

    fun clickReportSymptoms() {
        clickOn(R.id.optionReportSymptoms)
    }

    fun clickOrderTest() {
        clickOn(R.id.optionOrderTest)
    }

    fun clickLinkTestResult() {
        clickOn(R.id.optionLinkTestResult)
    }

    fun clickMoreAboutApp() {
        clickOn(R.id.optionAboutTheApp)
    }

    fun clickEncounterDetectionSwitch() {
        clickOn((R.id.optionContactTracing))
    }

    fun checkScanQrCodeOptionIsNotDisplayed() {
        onView(withId(R.id.optionVenueCheckIn)).check(matches(not(isDisplayed())))
    }

    fun checkIsolationViewIsDisplayed() {
        onView(withId(R.id.isolationView)).check(matches(isDisplayed()))
    }

    fun checkIsolationViewIsNotDisplayed() {
        onView(withId(R.id.isolationView)).check(matches(not(isDisplayed())))
    }

    fun checkEncounterDetectionSwitchIsChecked() {
        assertChecked(R.id.encounterDetectionSwitch)
    }

    fun checkEncounterDetectionSwitchIsNotChecked() {
        assertUnchecked(R.id.encounterDetectionSwitch)
    }
}
