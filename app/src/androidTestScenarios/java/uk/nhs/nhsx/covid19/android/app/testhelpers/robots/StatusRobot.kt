package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import android.content.Context
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.schibsted.spain.barista.assertion.BaristaCheckedAssertions.assertChecked
import com.schibsted.spain.barista.assertion.BaristaCheckedAssertions.assertUnchecked
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn
import org.hamcrest.Matchers
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.R.id

class StatusRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.statusContainer))
            .check(matches(isDisplayed()))
    }

    fun checkReportSymptomsIsNotDisplayed() {
        onView(withId(R.id.optionReportSymptoms))
            .check(matches(not(isDisplayed())))
    }

    fun clickAreaRiskView() {
        clickOn(R.id.riskAreaView)
    }

    fun checkAreaRiskViewIsDisplayed() {
        onView(withId(R.id.riskAreaView))
            .check(matches(isDisplayed()))
    }

    fun clickVenueCheckIn() {
        clickOn(R.id.optionVenueCheckIn)
    }

    fun clickSettings() {
        clickOn(R.id.optionSettings)
    }

    fun clickReadAdvice() {
        clickOn(R.id.optionReadAdvice)
    }

    fun clickReportSymptoms() {
        clickOn(R.id.optionReportSymptoms)
    }

    fun clickOrderTest() {
        clickOn(R.id.optionOrderTest)
    }

    fun clickFinancialSupport() {
        clickOn(R.id.optionIsolationPayment)
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

    fun checkReadAdviceIsEnabled() {
        onView(withId(R.id.optionReadAdvice))
            .check(matches(isEnabled()))
    }

    fun checkVenueCheckInIsEnabled() {
        onView(withId(R.id.optionVenueCheckIn))
            .check(matches(isEnabled()))
    }

    fun checkReportSymptomsIsEnabled() {
        onView(withId(R.id.optionReportSymptoms))
            .check(matches(isEnabled()))
    }

    fun checkOrderTestIsEnabled() {
        onView(withId(R.id.optionOrderTest))
            .check(matches(isEnabled()))
    }

    fun checkFinancialSupportIsEnabled() {
        onView(withId(R.id.optionIsolationPayment))
            .check(matches(isEnabled()))
    }

    fun checkLinkTestResultIsEnabled() {
        onView(withId(R.id.optionLinkTestResult))
            .check(matches(isEnabled()))
    }

    fun checkMoreAboutAppIsEnabled() {
        onView(withId(R.id.optionAboutTheApp))
            .check(matches(isEnabled()))
    }

    fun checkEncounterDetectionSwitchIsEnabled() {
        onView(withId(R.id.optionContactTracing))
            .check(matches(isEnabled()))
    }

    fun checkSettingsIsEnabled() {
        onView(withId(R.id.optionSettings))
            .check(matches(isEnabled()))
    }

    fun checkAreaRiskViewIsEnabled() {
        onView(withId(R.id.riskAreaView))
            .check(matches(isEnabled()))
    }

    fun checkScanQrCodeOptionIsNotDisplayed() {
        onView(withId(R.id.optionVenueCheckIn))
            .check(matches(not(isDisplayed())))
    }

    fun checkIsolationViewIsDisplayed() {
        onView(withId(R.id.isolationView))
            .check(matches(isDisplayed()))
    }

    fun checkIsolationViewIsNotDisplayed() {
        onView(withId(R.id.isolationView))
            .check(matches(not(isDisplayed())))
    }

    fun checkIsolationSubtitleIsDisplayedWithText(context: Context, expected: String) {
        onView(withId(R.id.subTitleIsolationCountdown))
            .check(matches(withText(context.getString(R.string.isolation_until_date, expected))))
    }

    fun checkEncounterDetectionSwitchIsChecked() {
        assertChecked(R.id.encounterDetectionSwitch)
    }

    fun checkEncounterDetectionSwitchIsNotChecked() {
        assertUnchecked(R.id.encounterDetectionSwitch)
    }

    fun checkIsolationPaymentButtonIsDisplayed() {
        onView(withId(R.id.optionIsolationPayment))
            .check(matches(isDisplayed()))
    }

    fun checkIsolationPaymentButtonIsNotDisplayed() {
        onView(withId(R.id.optionIsolationPayment))
            .check(matches(not(isDisplayed())))
    }

    fun checkErrorIsDisplayed() {
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(isDisplayed()))
    }

    fun checkVenueOptionIsTranslatedTo(translatedWord: String) {
        onView(
            Matchers.allOf(
                withId(id.mainActionsContainer),
                ViewMatchers.hasDescendant(withText(translatedWord))
            )
        ).check(matches(isDisplayed()))
    }
}
