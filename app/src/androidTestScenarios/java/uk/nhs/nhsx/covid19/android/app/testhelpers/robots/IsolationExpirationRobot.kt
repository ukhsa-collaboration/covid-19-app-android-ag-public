package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.util.uiFormat
import java.time.LocalDate

class IsolationExpirationRobot {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.isolationExpirationContainer))
            .check(matches(isDisplayed()))
    }

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

    fun checkDateFormat(date: String) {
        val hasFinishedText = context.getString(
            R.string.expiration_notification_description_passed, date
        )

        onView(withId(R.id.expirationDescription))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
            .check(matches(withText(hasFinishedText)))
    }

    fun clickBackToHomeButton() {
        onView(withId(R.id.buttonReturnToHomeScreen))
            .perform(scrollTo(), click())
    }
}
