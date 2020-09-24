package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import uk.nhs.nhsx.covid19.android.app.R

class QrCodeScanResultRobot {

    fun checkSuccessTitleIsDisplayed() {
        onView(withText(R.string.qr_code_success_title))
            .check(matches(isDisplayed()))
    }

    fun checkVenueNameIsDisplayed(venueName: String) {
        onView(withId(R.id.successVenueName))
            .check(matches(withText(venueName)))
    }

    fun checkQrCodeNotRecognizedTitleIsDisplayed() {
        onView(withId(R.id.titleTextView))
            .check(matches(isDisplayed()))
    }

    fun checkPermissionDeniedTitleIsDisplayed() {
        onView(withText(R.string.qr_code_permission_denied_title))
            .check(matches(isDisplayed()))
    }

    fun checkNotSupportedTitleIsDisplayed() {
        onView(withText(R.string.qr_code_unsupported_title))
            .check(matches(isDisplayed()))
    }

    fun clickBackToHomeButton() {
        onView(withId(R.id.actionButton)).perform(click())
    }
}
