package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity
import uk.nhs.nhsx.covid19.android.app.util.uiFormat
import java.time.LocalDate

class IsolationExpirationRobot : HasActivity {

    override val containerId: Int
        get() = R.id.isolationExpirationContainer

    fun checkIsolationWillFinish(expiryDate: LocalDate) {
        val willFinishText = context.getString(
            R.string.your_isolation_will_finish,
            expiryDate.minusDays(1).uiFormat(context)
        )
        onView(withId(R.id.expirationDescription))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
            .check(matches(withText(willFinishText)))
    }

    fun checkIsolationWillFinishWales(numberOfIsolationDays: Int) {
        val willFinishText = context.getString(
            R.string.your_isolation_are_ending_soon_wales,
            numberOfIsolationDays - 1
        )
        onView(withId(R.id.expirationDescription))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
            .check(matches(withText(willFinishText)))
    }

    fun checkIsolationHasFinished(expiryDate: LocalDate) {
        val hasFinishedText = context.getString(
            R.string.expiration_notification_description_passed,
            expiryDate.minusDays(1).uiFormat(context)
        )

        onView(withId(R.id.expirationDescription))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
            .check(matches(withText(hasFinishedText)))
    }

    fun checkIsolationHasFinishedWales(numberOfIsolationDays: Int) {
        val hasFinishedText = context.getString(
            R.string.expiration_notification_description_passed_wales,
            numberOfIsolationDays - 1
        )

        onView(withId(R.id.expirationDescription))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
            .check(matches(withText(hasFinishedText)))
    }

    fun checkDateFormat(date: String) {
        val hasFinishedText = context.getString(
            R.string.expiration_notification_description_passed, date
        )

        onView(withId(R.id.expirationDescription))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
            .check(matches(withText(hasFinishedText)))
    }

    fun clickPrimaryBackToHomeButton() {
        onView(withId(R.id.primaryReturnToHomeScreenButton))
            .perform(scrollTo(), click())
    }

    fun clickSecondaryBackToHomeButton() {
        onView(withId(R.id.secondaryReturnToHomeScreenButton))
            .perform(scrollTo(), click())
    }

    fun clickCovidGuidanceLinkButton() {
        onView(withId(R.id.covidGuidanceLinkButton))
            .perform(scrollTo(), click())
    }
}
