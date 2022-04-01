package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import android.content.Context
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.NestedScrollViewScrollToAction
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.positional.isFollowedBy
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.positional.isPrecededBy
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity
import uk.nhs.nhsx.covid19.android.app.util.uiFormat
import java.time.LocalDate

class StatusRobot : HasActivity {
    override val containerId: Int
        get() = R.id.statusContainer

    fun checkReportSymptomsIsDisplayed() {
        onView(withId(R.id.optionReportSymptoms))
            .check(matches(isDisplayed()))
    }

    fun checkReportSymptomsIsNotDisplayed() {
        onView(withId(R.id.optionReportSymptoms))
            .check(matches(not(isDisplayed())))
    }

    fun clickAreaRiskView() {
        onView(withId(R.id.riskAreaView))
            .perform(click())
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
        onView(withId(R.id.optionVenueCheckIn))
            .perform(NestedScrollViewScrollToAction(), click())
    }

    fun clickSettings() {
        onView(withId(R.id.optionSettings))
            .perform(NestedScrollViewScrollToAction(), click())
    }

    fun checkNoAnimationIsDisplayed(isIsolating: Boolean) {
        val staticImageResId = if (isIsolating) R.id.imgCircleIsolationStatic else R.id.imgCircleStatic
        onView(withId(staticImageResId)).check(matches(isDisplayed()))
    }

    fun checkStaticImageIsNotDisplayed(isIsolating: Boolean) {
        val staticImageResId = if (isIsolating) R.id.imgCircleIsolationStatic else R.id.imgCircleStatic
        onView(withId(staticImageResId)).check(matches(not(isDisplayed())))
    }

    fun clickReportSymptoms() {
        onView(withId(R.id.optionReportSymptoms))
            .perform(NestedScrollViewScrollToAction(), click())
    }

    fun clickTestingHub() {
        onView(withId(R.id.optionTestingHub))
            .perform(NestedScrollViewScrollToAction(), click())
    }

    fun clickLinkTestResult() {
        onView(withId(R.id.optionLinkTestResult))
            .perform(NestedScrollViewScrollToAction(), click())
    }

    fun clickMoreAboutApp() {
        onView(withId(R.id.optionAboutTheApp))
            .perform(NestedScrollViewScrollToAction(), click())
    }

    fun clickManageContactTracing() {
        onView(withId(R.id.optionToggleContactTracing))
            .perform(NestedScrollViewScrollToAction(), click())
    }

    fun clickLocalMessageBanner() {
        onView(withId(R.id.localMessageBanner))
            .perform(click())
    }

    fun clickIsolationHub() {
        onView(withId(R.id.optionIsolationHub))
            .perform(NestedScrollViewScrollToAction(), click())
    }

    fun clickLocalData() {
        onView(withId(R.id.optionLocalData))
            .perform(NestedScrollViewScrollToAction(), click())
    }

    fun checkVenueCheckInIsEnabled() {
        onView(withId(R.id.optionVenueCheckIn))
            .check(matches(isEnabled()))
    }

    fun checkReportSymptomsIsEnabled() {
        onView(withId(R.id.optionReportSymptoms))
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

    fun checkTestingHubIsEnabled() {
        onView(withId(R.id.optionTestingHub))
            .check(matches(isEnabled()))
    }

    fun checkIsolationHubIsEnabled() {
        onView(withId(R.id.optionIsolationHub))
            .check(matches(isEnabled()))
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

    fun checkLocalMessageBannerIsDisplayed() {
        onView(withId(R.id.localMessageBanner))
            .check(matches(isDisplayed()))
    }

    fun checkLocalMessageBannerIsNotDisplayed() {
        onView(withId(R.id.localMessageBanner))
            .check(matches(not(isDisplayed())))
    }

    fun checkIsolationHubIsDisplayed() {
        onView(withId(R.id.optionIsolationHub))
            .check(matches(isDisplayed()))
    }

    fun checkIsolationHubIsNotDisplayed() {
        onView(withId(R.id.optionIsolationHub))
            .check(matches(not(isDisplayed())))
    }

    fun checkBluetoothStoppedViewIsNotDisplayed() {
        onView(withId(R.id.bluetoothStoppedView))
            .check(matches(not(isDisplayed())))
    }

    fun checkBluetoothStoppedViewIsDisplayed() {
        onView(withId(R.id.bluetoothStoppedView))
            .check(matches(isDisplayed()))
    }

    fun checkLocalDataIsDisplayedAfterCheckInVenueButton() {
        onView(withId(R.id.optionLocalData))
            .check(matches(isPrecededBy(R.id.optionVenueCheckIn)))
    }

    fun checkLocalDataIsDisplayedBeforeSettingsButton() {
        onView(withId(R.id.optionLocalData))
            .check(matches(isFollowedBy(R.id.optionSettings)))
    }

    fun checkLocalDataIsNotDisplayed() {
        onView(withId(R.id.optionLocalData))
            .check(matches(not(isDisplayed())))
    }

    fun checkVenueCheckIsNotDisplayed() {
        onView(withId(R.id.optionVenueCheckIn))
            .check(matches(not(isDisplayed())))
    }

    fun checkTestingHubIsNotDisplayed() {
        onView(withId(R.id.optionTestingHub))
            .check(matches(not(isDisplayed())))
    }

    fun checkTestingHubIsDisplayed() {
        onView(withId(R.id.optionTestingHub))
            .check(matches(isDisplayed()))
    }

    fun checkIsolationViewHasCorrectContentDescriptionForWales(lastDayOfIsolation: LocalDate) {
        onView(withId(R.id.isolationView))
            .check(matches(withContentDescription(context.resources.getQuantityString(
                R.plurals.isolation_view_accessibility_description,
                4,
                lastDayOfIsolation.uiFormat(context),
                "4"
            ))))
    }

    fun checkIsolationViewHasCorrectContentDescriptionForEngland(lastDayOfIsolation: LocalDate) {
        onView(withId(R.id.isolationView))
            .check(matches(withContentDescription(context.resources.getQuantityString(
                R.plurals.isolation_view_accessibility_description_england,
                9,
                lastDayOfIsolation.uiFormat(context),
                "9"
            ))))
    }
}
