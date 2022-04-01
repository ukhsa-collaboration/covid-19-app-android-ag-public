package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry.ENGLAND
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.withStateStringResource
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class ExposureNotificationRobot : HasActivity {
    override val containerId: Int
        get() = R.id.exposureNotificationScrollView

    fun clickContinueButton() {
        onView(withId(R.id.primaryActionButton))
            .perform(scrollTo(), click())
    }

    fun checkEncounterDateIsDisplayed(date: String) {
        val expectedText = context.getString(R.string.contact_case_exposure_info_screen_exposure_date, date)
        onView(withId(R.id.closeContactDate))
            .check(matches(withText(expectedText)))
    }

    fun checkIsolationAdviceIsDisplayed(displayed: Boolean, country: SupportedCountry) {
        val expectedText = if (country == ENGLAND) {
            R.string.contact_case_exposure_info_screen_information_england
        } else {
            R.string.contact_case_exposure_info_screen_information_wales
        }

        onView(withId(R.id.selfIsolationWarning)).apply {
            if (displayed) perform(scrollTo())
        }
            .check(
                if (displayed)
                    matches(allOf(isDisplayed(), withStateStringResource(expectedText)))
                else
                    matches(not(isDisplayed()))
            )
    }

    fun checkTestingAdviceIsDisplayed(displayed: Boolean) {
        onView(withId(R.id.testingInformationContainer)).apply {
            if (displayed) perform(scrollTo())
        }
            .check(if (displayed) matches(isDisplayed()) else matches(not(isDisplayed())))
    }

    fun checkWalesStringAreDisplayed() {
        onView(withText(R.string.contact_case_exposure_info_screen_title_wales))
            .check(matches(isDisplayed()))
        onView(withText(R.string.contact_case_exposure_info_screen_how_close_contacts_are_calculated_heading_wales))
            .check(matches(isDisplayed()))
        onView(withText(R.string.contact_case_exposure_info_screen_continue_button_wales))
            .check(matches(isDisplayed()))
    }
}
