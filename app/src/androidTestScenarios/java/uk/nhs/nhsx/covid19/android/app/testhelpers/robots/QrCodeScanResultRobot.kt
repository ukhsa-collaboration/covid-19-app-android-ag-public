package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.withDrawable

class QrCodeScanResultRobot {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    fun checkAnimationIconIsDisplayed() {
        onView(withId(R.id.animationIcon))
            .check(matches(isDisplayed()))
            .check(matches(withDrawable(R.drawable.tick_final_2083)))

        onView(withId(R.id.errorResultIcon))
            .check(matches(not(isDisplayed())))
    }

    fun checkSuccessTitleAndVenueIsDisplayed(venueName: String) {
        onView(withText(context.getString(R.string.qr_code_success_title_and_venue_name, venueName)))
            .check(matches(isDisplayed()))
    }

    fun checkFailureIconIsDisplayed() {
        onView(withId(R.id.errorResultIcon))
            .check(matches(isDisplayed()))
            .check(matches(withDrawable(R.drawable.ic_qr_code_failure)))

        onView(withId(R.id.animationIcon))
            .check(matches(not(isDisplayed())))
    }

    fun checkQrCodeNotRecognizedTitleIsDisplayed() {
        onView(withId(R.id.titleTextView))
            .check(matches(isDisplayed()))
    }

    fun checkCameraIconIsDisplayed() {
        onView(withId(R.id.errorResultIcon))
            .check(matches(isDisplayed()))
            .check(matches(withDrawable(R.drawable.ic_camera)))

        onView(withId(R.id.animationIcon))
            .check(matches(not(isDisplayed())))
    }

    fun checkPermissionDeniedTitleIsDisplayed() {
        onView(withText(R.string.qr_code_permission_denied_title))
            .check(matches(isDisplayed()))
    }

    fun checkNotSupportedTitleIsDisplayed() {
        onView(withText(R.string.qr_code_unsupported_title))
            .check(matches(isDisplayed()))
    }

    fun checkDateTimeFormat(successVenueDateTime: String) {
        onView(withId(R.id.successVenueDateTime))
            .check(matches(withText(successVenueDateTime)))
    }

    fun cancelCheckIn() {
        clickOn(R.id.textCancelCheckIn)
    }

    fun clickBackToHomeButton() {
        onView(withId(R.id.actionButton)).perform(click())
    }
}
