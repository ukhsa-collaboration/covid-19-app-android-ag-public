package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import android.content.Context
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn
import org.hamcrest.Matchers
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.R

class StatusRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.statusContainer))
            .check(matches(isDisplayed()))
    }

    fun checkReportSymptomsIsDisplayed() {
        onView(withId(R.id.optionReportSymptoms))
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

    fun clickActivateContactTracingButton() {
        onView(withId(R.id.activateContactTracingButton))
            .perform(click())
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

    fun clickToggleContactTracing() {
        clickOn((R.id.optionToggleContactTracing))
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

    fun checkSettingsIsEnabled() {
        onView(withId(R.id.optionSettings))
            .check(matches(isEnabled()))
    }

    fun checkAreaRiskViewIsEnabled() {
        onView(withId(R.id.riskAreaView))
            .check(matches(isEnabled()))
    }

    fun checkToggleContactTracingIsEnabled() {
        onView(withId(R.id.optionToggleContactTracing))
            .check(matches(isEnabled()))
    }

    fun checkOrderTestIsDisplayed() {
        onView(withId(R.id.optionOrderTest))
            .check(matches(isDisplayed()))
    }

    fun checkOrderTestIsNotDisplayed() {
        onView(withId(R.id.optionOrderTest))
            .check(matches(not(isDisplayed())))
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

    fun checkContactTracingActiveIsDisplayed() {
        onView(withId(R.id.contactTracingActiveView))
            .check(matches(isDisplayed()))
        onView(withId(R.id.activateContactTracingButton))
            .check(matches(not(isDisplayed())))
    }

    fun checkContactTracingStoppedIsDisplayed() {
        onView(withId(R.id.contactTracingStoppedView))
            .check(matches(isDisplayed()))
        onView(withId(R.id.activateContactTracingButton))
            .check(matches(isDisplayed()))
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
                withId(R.id.mainActionsContainer),
                ViewMatchers.hasDescendant(withText(translatedWord))
            )
        ).check(matches(isDisplayed()))
    }
}
