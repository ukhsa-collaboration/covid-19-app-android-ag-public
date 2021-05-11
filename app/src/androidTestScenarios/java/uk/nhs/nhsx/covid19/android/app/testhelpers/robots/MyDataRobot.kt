package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.appcompat.widget.AppCompatTextView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.R

class MyDataRobot {
    fun checkActivityIsDisplayed() {
        onView(
            Matchers.allOf(
                Matchers.instanceOf(AppCompatTextView::class.java),
                ViewMatchers.withParent(withId(R.id.toolbar))
            )
        ).check(matches(withText(R.string.settings_my_data)))
    }

    fun checkLastTestResultIsDisplayed(
        shouldKitTypeBeVisible: Boolean = true,
        requiresConfirmatoryTest: Boolean,
        receivedFollowUpTest: String?
    ) {
        onView(withId(R.id.titleLatestResult))
            .check(matches(isDisplayed()))
        onView(withId(R.id.testEndDateContainer))
            .check(matches(isDisplayed()))
        onView(withId(R.id.latestResultValueContainer))
            .check(matches(isDisplayed()))
        onView(withId(R.id.testAcknowledgedDateContainer))
            .check(matches(isDisplayed()))

        if (shouldKitTypeBeVisible) {
            onView(withId(R.id.latestResultKitTypeContainer))
                .check(matches(isDisplayed()))
        } else {
            onView(withId(R.id.latestResultKitTypeContainer))
                .check(matches(not(isDisplayed())))
        }

        if (requiresConfirmatoryTest) {
            if (receivedFollowUpTest != null) {
                onView(withId(R.id.followUpState))
                    .check(matches(withText("Complete")))

                with(onView(withId(R.id.followUpDate))) {
                    check(matches(isDisplayed()))
                    check(matches(withText(receivedFollowUpTest)))
                }
            } else {
                onView(withId(R.id.followUpState))
                    .check(matches(withText("Pending")))

                onView(withId(R.id.followUpDate))
                    .check(matches(not(isDisplayed())))
            }
        } else {
            onView(withId(R.id.followUpDate))
                .check(matches(not(isDisplayed())))
            onView(withId(R.id.followUpState))
                .check(matches(withText("Not required")))
        }
    }

    fun checkLastTestResultIsNotDisplayed() {
        onView(withId(R.id.titleLatestResult))
            .check(matches(not(isDisplayed())))
        onView(withId(R.id.testEndDateContainer))
            .check(matches(not(isDisplayed())))
        onView(withId(R.id.latestResultValueContainer))
            .check(matches(not(isDisplayed())))
        onView(withId(R.id.latestResultKitTypeContainer))
            .check(matches(not(isDisplayed())))
        onView(withId(R.id.followUpTestDateContainer))
            .check(matches(not(isDisplayed())))
        onView(withId(R.id.followUpTestStatusContainer))
            .check(matches(not(isDisplayed())))
        onView(withId(R.id.testAcknowledgedDateContainer))
            .check(matches(not(isDisplayed())))
    }

    fun checkEmptyViewIsShown() {
        onView(withId(R.id.noRecordsView))
            .check(matches(isDisplayed()))
        onView(withId(R.id.viewContent))
            .check(matches(not(isDisplayed())))
    }

    fun checkLastVisitedBookTestTypeVenueDateIsDisplayed(dateText: String) {
        onView(withId(R.id.titleLastRiskyVenueVisit))
            .check(matches(isDisplayed()))
        onView(withId(R.id.lastRiskyVenueVisitSection))
            .check(matches(isDisplayed()))
        onView(withId(R.id.lastRiskyVenueVisitDate))
            .check(matches(allOf(isDisplayed(), withText(dateText))))
    }

    fun checkExposureNotificationIsDisplayed() {
        onView(withId(R.id.titleExposureNotification))
            .check(matches(isDisplayed()))
    }

    fun checkEncounterIsDisplayed() {
        onView(withId(R.id.encounterDataSection))
            .check(matches(isDisplayed()))
    }

    fun checkExposureNotificationDateIsDisplayed() {
        onView(withId(R.id.exposureNotificationDataSection))
            .check(matches(isDisplayed()))
    }

    fun checkExposureNotificationDateIsNotDisplayed() {
        onView(withId(R.id.exposureNotificationDataSection))
            .check(matches(not(isDisplayed())))
    }

    fun checkLastDayOfIsolationIsDisplayed() {
        onView(withId(R.id.titleLastDayOfIsolation))
            .check(matches(isDisplayed()))
        onView(withId(R.id.lastDayOfIsolationSection))
            .check(matches(isDisplayed()))
    }

    fun checkLastDayOfIsolationDisplaysText(expected: String) {
        onView(withId(R.id.lastDayOfIsolationDate))
            .check(matches(withText(expected)))
    }

    fun checkLastDayOfIsolationIsNotDisplayed() {
        onView(withId(R.id.titleLastDayOfIsolation))
            .check(matches(not(isDisplayed())))
        onView(withId(R.id.lastDayOfIsolationSection))
            .check(matches(not(isDisplayed())))
    }

    fun checkDailyContactTestingOptInDateIsDisplayed() {
        onView(withId(R.id.titleDailyContactTestingOptIn))
            .check(matches(isDisplayed()))
        onView(withId(R.id.dailyContactTestingSection))
            .check(matches(isDisplayed()))
    }

    fun checkDailyContactTestingOptInDateIsNotDisplayed() {
        onView(withId(R.id.titleDailyContactTestingOptIn))
            .check(matches(not(isDisplayed())))
        onView(withId(R.id.dailyContactTestingSection))
            .check(matches(not(isDisplayed())))
    }

    fun checkSymptomsAreDisplayed() {
        onView(withId(R.id.titleSymptoms))
            .check(matches(isDisplayed()))
        onView(withId(R.id.symptomsDataSection))
            .check(matches(isDisplayed()))
    }
}
