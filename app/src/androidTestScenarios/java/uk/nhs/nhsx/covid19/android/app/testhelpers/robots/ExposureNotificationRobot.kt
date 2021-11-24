package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.R

class ExposureNotificationRobot {

    fun checkActivityIsDisplayed() {
        onView(withText(R.string.exposure_notification_title))
            .check(matches(isDisplayed()))
    }

    fun clickContinueButton() {
        onView(withId(R.id.primaryActionButton))
            .perform(scrollTo(), click())
    }

    fun checkEncounterDateIsDisplayed(date: String) {
        val expectedText = context.getString(R.string.contact_case_exposure_info_screen_exposure_date, date)
        onView(withId(R.id.closeContactDate))
            .check(matches(withText(expectedText)))
    }

    fun checkTestingAndIsolationAdviceIsDisplayed(displayed: Boolean) {
        onView(withId(R.id.selfIsolationWarning)).apply {
            if (displayed) perform(scrollTo())
        }
            .check(if (displayed) matches(isDisplayed()) else matches(not(isDisplayed())))

        onView(withId(R.id.testingInformationContainer)).apply {
            if (displayed) perform(scrollTo())
        }
            .check(if (displayed) matches(isDisplayed()) else matches(not(isDisplayed())))
    }
}
