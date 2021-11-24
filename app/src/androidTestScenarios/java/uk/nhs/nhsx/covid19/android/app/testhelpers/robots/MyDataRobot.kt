package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class MyDataRobot : HasActivity {

    override val containerId: Int
        get() = R.id.myDataContainer

    fun checkLastTestResultIsDisplayed(
        shouldKitTypeBeVisible: Boolean = true,
        requiresConfirmatoryTest: Boolean,
        receivedFollowUpTest: String?
    ) {
        onView(withId(R.id.lastTestResultSection))
            .check(matches(isDisplayed()))
        onView(withText(R.string.user_data_test_end))
            .check(matches(isDisplayed()))
        onView(withText(R.string.about_test_result))
            .check(matches(isDisplayed()))
        onView(withText(R.string.user_data_acknowledged_date))
            .check(matches(isDisplayed()))

        if (shouldKitTypeBeVisible) {
            onView(withText(R.string.about_test_kit_type))
                .check(matches(isDisplayed()))
        } else {
            onView(withText(R.string.about_test_kit_type))
                .check(doesNotExist())
        }

        if (requiresConfirmatoryTest) {
            if (receivedFollowUpTest != null) {
                onView(withText(R.string.about_test_follow_up_state_complete))
                    .check(matches(isDisplayed()))
                onView(withText(R.string.about_test_follow_up_date))
                    .check(matches(isDisplayed()))
            } else {
                onView(withText(R.string.about_test_follow_up_state_pending))
                    .check(matches(isDisplayed()))
                onView(withText(R.string.about_test_follow_up_date))
                    .check(doesNotExist())
            }
        } else {
            onView(withText(R.string.about_test_follow_up_date))
                .check(doesNotExist())
            onView(withText(R.string.about_test_follow_up_state_not_required))
                .check(matches(isDisplayed()))
        }
    }

    fun checkLastTestResultIsNotDisplayed() {
        onView(withId(R.id.lastTestResultSection))
            .check(matches(not(isDisplayed())))
        onView(withText(R.string.user_data_test_end))
            .check(doesNotExist())
        onView(withText(R.string.about_test_result))
            .check(doesNotExist())
        onView(withText(R.string.user_data_acknowledged_date))
            .check(doesNotExist())
        onView(withText(R.string.about_test_kit_type))
            .check(doesNotExist())
        onView(withText(R.string.about_test_follow_up_date))
            .check(doesNotExist())
        onView(withText(R.string.about_test_follow_up_state_complete))
            .check(doesNotExist())
        onView(withText(R.string.about_test_follow_up_state_pending))
            .check(doesNotExist())
        onView(withText(R.string.about_test_follow_up_state_not_required))
            .check(doesNotExist())
    }

    fun checkEmptyViewIsShown() {
        onView(withId(R.id.noRecordsView))
            .check(matches(isDisplayed()))
        onView(withId(R.id.viewContent))
            .check(matches(not(isDisplayed())))
    }

    fun checkLastVisitedBookTestTypeVenueDateIsDisplayed(dateText: String) {
        onView(withId(R.id.riskyVenueSection))
            .check(matches(isDisplayed()))
        onView(withText(R.string.about_my_data_last_visited))
            .check(matches(isDisplayed()))
        onView(withText(dateText))
            .check(matches(isDisplayed()))
    }

    fun checkExposureNotificationIsDisplayed() {
        onView(withId(R.id.exposureNotificationSection))
            .check(matches(isDisplayed()))
    }

    fun checkEncounterIsDisplayed() {
        onView(withText(R.string.about_encounter_date))
            .check(matches(isDisplayed()))
    }

    fun checkExposureNotificationDateIsDisplayed() {
        onView(withText(R.string.about_notification_date))
            .check(matches(isDisplayed()))
    }

    fun checkOptOutOfContactIsolationDateIsDisplayed() {
        onView(withText(R.string.about_contact_isolation_opt_out_date))
            .check(matches(isDisplayed()))
    }

    fun checkLastDayOfIsolationIsDisplayed() {
        onView(withId(R.id.selfIsolationSection))
            .check(matches(isDisplayed()))
        onView(withText(R.string.about_my_data_last_day_of_isolation))
            .check(matches(isDisplayed()))
    }

    fun checkLastDayOfIsolationDisplaysText(expected: String) {
        onView(withText(expected))
            .check(matches(isDisplayed()))
    }

    fun checkLastDayOfIsolationIsNotDisplayed() {
        onView(withId(R.id.selfIsolationSection))
            .check(matches(not(isDisplayed())))
        onView(withText(R.string.about_my_data_last_day_of_isolation))
            .check(doesNotExist())
    }

    fun checkSymptomsAreDisplayed() {
        onView(withId(R.id.symptomsInformationSection))
            .check(matches(isDisplayed()))
        onView(withText(R.string.about_my_data_symptom_onset_date))
            .check(matches(isDisplayed()))
    }
}
